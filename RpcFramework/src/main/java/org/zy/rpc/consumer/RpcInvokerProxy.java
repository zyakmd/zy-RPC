package org.zy.rpc.consumer;


import org.zy.rpc.common.*;
import org.zy.rpc.common.constants.FaultTolerantRules;
import org.zy.rpc.common.constants.MsgType;
import org.zy.rpc.common.constants.ProtocolConstants;
import org.zy.rpc.config.RpcProperties;
import org.zy.rpc.filter.FilterConfig;
import org.zy.rpc.filter.FilterData;
import org.zy.rpc.protocol.MsgHeader;
import org.zy.rpc.protocol.RpcProtocol;
import org.zy.rpc.router.LoadBalancer;
import org.zy.rpc.router.LoadBalancerFactory;
import org.zy.rpc.router.RouterRes;
import io.netty.channel.DefaultEventLoop;
import io.netty.util.concurrent.DefaultPromise;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.ObjectUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * @package: com.zy.rpc.consumer
 * @author: zyakmd
 * @description: 调用方的代理
 * @date: 2024/5/1 11:39
 */
@Slf4j
public class RpcInvokerProxy implements InvocationHandler {

    private String serviceVersion;
    private long timeout;
    private String loadBalancerType;
    private String faultTolerantType;
    private long retryCount;

    public RpcInvokerProxy(String serviceVersion, long timeout, String faultTolerantType, String loadBalancerType, long retryCount ) {
        this.serviceVersion = serviceVersion;
        this.timeout = timeout;
        this.loadBalancerType = loadBalancerType;
        // 目前三种容错模式：FailFast、Failover、Failsafe
        this.faultTolerantType = faultTolerantType;
        this.retryCount = retryCount;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        RpcProtocol<RpcRequest> protocol = new RpcProtocol<>();
        // 构建消息头
        MsgHeader header = new MsgHeader();
        // 生成消息id，基于AtomicLong(还有AtomicInteger)等一种原子方式更新的操作类，自增并返回值
        long requestId = RpcRequestHolder.REQUEST_ID_GEN.incrementAndGet();
        header.setMagic(ProtocolConstants.MAGIC);
        header.setVersion(ProtocolConstants.VERSION);
        header.setRequestId(requestId);
        // 这个只是指定序列化方法（json或者别的），不是对内容进行序列化
        final byte[] serialization = RpcProperties.getInstance().getSerialization().getBytes();
        header.setSerializationLen(serialization.length);
        header.setSerializations(serialization);
        // ordinal 返回枚举类序数
        header.setMsgType((byte) MsgType.REQUEST.ordinal());
        header.setStatus((byte) 0x1);
        protocol.setHeader(header);

        // 构建请求体
        RpcRequest request = new RpcRequest();
        request.setServiceVersion(this.serviceVersion);
        request.setClassName(method.getDeclaringClass().getName());
        request.setMethodName(method.getName());
        request.setParameterTypes(method.getParameterTypes());
        // new Type[0]就是起一个模板的作用，指定了返回数组的类型，0是为了节省空间，因为它只是为了说明返回的类型
        request.setData(ObjectUtils.isEmpty(args)?new Object[0]:args);
        request.setDataClass(ObjectUtils.isEmpty(args)? null : args[0].getClass());
        // 一些配置信息
        request.setServiceAttachments(RpcProperties.getInstance().getServiceAttachments());
        request.setClientAttachments(RpcProperties.getInstance().getClientAttachments());

        // 拦截器上下文
        final FilterData filterData = new FilterData(request);
        try {
            FilterConfig.getClientBeforeFilterChain().doFilter(filterData);
        } catch (Throwable throwable){
            throw throwable;
        }
        protocol.setBody(request);

        RpcConsumer rpcConsumer = new RpcConsumer();
        String serviceName = CommonMethod.getServiceKey(request.getClassName(), request.getServiceVersion());
        Object[] params = {request.getData()};

        // 1.获取负载均衡策略
        final LoadBalancer loadBalancer = LoadBalancerFactory.get(loadBalancerType);
        // 2.根据策略获取对应服务
        final RouterRes routerRes = loadBalancer.select(params, serviceName);

        ServiceMeta curServiceMeta = routerRes.getCurServiceMeta();
        final Collection<ServiceMeta> otherServiceMata = routerRes.getOtherServiceMeta();
        long count = 1;
        long retryCount = this.retryCount;
        RpcResponse rpcResponse = null;

        // 重试机制
        while (count <= retryCount){
            // 处理返回数据
            RpcFuture<RpcResponse> future = new RpcFuture<>(new DefaultPromise<>(new DefaultEventLoop()), timeout);
            // Holder
            RpcRequestHolder.REQUEST_MAP.put(requestId, future);
            try {
                // 发送消息
                rpcConsumer.sendRequest(protocol, curServiceMeta);
                // 等待响应数据返回
                rpcResponse = future.getPromise().get(future.getTimeout(), TimeUnit.MILLISECONDS);
                //rpcResponse = future.getPromise().get(10, TimeUnit.SECONDS);
                // 响应有问题
                if(rpcResponse.getException()!=null){
                    log.info("调用出错");
                    throw rpcResponse.getException();
                }
                log.info("提供方调用成功, serviceName: {}",serviceName);
                try {
                    FilterConfig.getClientAfterFilterChain().doFilter(filterData);
                }catch (Throwable e){
                    log.warn("数据拦截异常");
                    throw e;
                }
                return rpcResponse.getData();
            }catch (Throwable e){
                String errorMsg = e.toString();
                // 容错机制
                switch (faultTolerantType){
                    // 快速失败
                    case FaultTolerantRules.FailFast:
                        log.warn("rpc 调用失败, 触发 FailFast 策略,异常信息: {}",errorMsg);
                        return rpcResponse.getException();
                    // 故障转移：尝试别的节点
                    case FaultTolerantRules.Failover:
                        log.warn("rpc 调用失败,第{}次重试,异常信息:{}",count,errorMsg);
                        count++;
                        if (!ObjectUtils.isEmpty(otherServiceMata)){
                            final ServiceMeta next = otherServiceMata.iterator().next();
                            curServiceMeta = next;
                            otherServiceMata.remove(next);
                        }else {
                            final String msg = String.format("rpc 调用失败, 当前已无服务可用serviceName: {%s}, 异常信息: {%s}", serviceName, errorMsg);
                            log.warn(msg);
                            throw new RuntimeException(msg);
                        }
                        break;
                    // 忽视错误
                    case FaultTolerantRules.Failsafe:
                        return null;
                }
            }

        }

        // 超出最大重试次数
        throw new RuntimeException("rpc 调用失败，超过最大重试次数: {}" + retryCount);
    }
}
