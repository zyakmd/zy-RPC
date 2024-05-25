package org.zy.rpc.ThreadPool;

import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cglib.reflect.FastClass;
import org.springframework.cglib.reflect.FastMethod;
import org.zy.rpc.common.CommonMethod;
import org.zy.rpc.common.RpcRequest;
import org.zy.rpc.common.RpcResponse;
import org.zy.rpc.common.constants.MsgStatus;
import org.zy.rpc.common.constants.MsgType;
import org.zy.rpc.protocol.MsgHeader;
import org.zy.rpc.protocol.RpcProtocol;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @package: org.zy.rpc.ThreadPoll
 * @author: zyakmd
 * @description: 线程池工厂
 * @date: 2024/5/7 19:09
 */
public class ThreadPoolFactory {

    private static Logger logger = LoggerFactory.getLogger(ThreadPoolFactory.class);

    private static ThreadPoolExecutor slowPoll;

    private static ThreadPoolExecutor fastPoll;

    private static volatile ConcurrentHashMap<String, AtomicInteger> slowTaskMap = new ConcurrentHashMap();

    private static int corSize = Runtime.getRuntime().availableProcessors();

    // 缓存服务 该缓存放这里不太好,应该作一个统一 Config 进行管理
    private static Map<String, Object> rpcServiceMap;


    static {
        slowPoll = new ThreadPoolExecutor(corSize / 2, corSize, 60L,
                TimeUnit.SECONDS,
                new LinkedBlockingDeque<>(2000),
                r->{
                    Thread thread = new Thread(r);
                    thread.setName("slow poll-"+r.hashCode());
                    thread.setDaemon(true);
                    return thread;
                });

        fastPoll = new ThreadPoolExecutor(corSize, corSize*2, 60L,
                TimeUnit.SECONDS,
                new LinkedBlockingDeque<>(1000),
                r->{
                    Thread thread = new Thread(r);
                    thread.setName("fast poll-"+r.hashCode());
                    thread.setDaemon(true);
                    return thread;
                });
        startClearMonitor();
    }

    private ThreadPoolFactory(){}


    public static void setRpcServiceMap(Map<String, Object> rpcMap){
        rpcServiceMap = rpcMap;
    }

    /**
     * 清理慢请求map
     */
    private static void startClearMonitor(){
        Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay(()->{
            slowTaskMap.clear();
        },5,5,TimeUnit.MINUTES);
    }

    public static void submitRequest(ChannelHandlerContext ctx, RpcProtocol<RpcRequest> protocol){

        final RpcRequest request = protocol.getBody();
        String key = request.getClassName() + request.getMethodName() + request.getServiceVersion();
        ThreadPoolExecutor poll = fastPoll;
        if (slowTaskMap.containsKey(key) && slowTaskMap.get(key).intValue() >= 10){
            poll = slowPoll;
        }
        poll.submit(()->{
            RpcProtocol<RpcResponse> resProtocol = new RpcProtocol<>();
            final MsgHeader header = protocol.getHeader();
            RpcResponse response = new RpcResponse();
            long startTime = System.currentTimeMillis();

            try {
                final Object result = submit(ctx, protocol);
                response.setData(result);
                response.setDataClass(result == null ? null : result.getClass());
                header.setStatus((byte) MsgStatus.SUCCESS.ordinal());
            } catch (Exception e) {
                // 执行业务失败则将异常返回
                header.setStatus((byte) MsgStatus.FAILED.ordinal());
                response.setException(e);
                logger.error("process request {} error", header.getRequestId(), e);
            }finally {
                long cost = System.currentTimeMillis() - startTime;
                System.out.println("cost time:" + cost);
                // 如果该调用处理时间过长，则放到慢任务Map中，由慢任务池处理
                if(cost > 1000){
                    final AtomicInteger timeOutCount = slowTaskMap.putIfAbsent(key, new AtomicInteger(1));
                    if (timeOutCount!=null){
                        timeOutCount.incrementAndGet();
                    }
                }
            }
            resProtocol.setHeader(header);
            resProtocol.setBody(response);
            logger.info("执行成功: {},{},{},{}",Thread.currentThread().getName(),request.getClassName(),request.getMethodName(),request.getServiceVersion());
            ctx.fireChannelRead(resProtocol);
        });
    }

    private static Object submit(ChannelHandlerContext ctx, RpcProtocol<RpcRequest> protocol) throws Exception{
        RpcProtocol<RpcResponse> resProtocol = new RpcProtocol<>();
        MsgHeader header = protocol.getHeader();
        header.setMsgType((byte) MsgType.RESPONSE.ordinal());
        final RpcRequest request = protocol.getBody();
        // 执行具体业务
        return handle(request);
    }

    // 调用方法
    private static Object handle(RpcRequest request) throws Exception {
        String serviceKey = CommonMethod.getServiceKey(request.getClassName(), request.getServiceVersion());
        // 获取服务信息
        Object serviceBean = rpcServiceMap.get(serviceKey);

        if (serviceBean == null) {
            throw new RuntimeException(String.format("service not exist: %s:%s", request.getClassName(), request.getMethodName()));
        }

        // 获取服务提供方信息并且创建
        Class<?> serviceClass = serviceBean.getClass();
        String methodName = request.getMethodName();
        Class<?>[] parameterTypes = request.getParameterTypes();
        Object[] parameters = {request.getData()};

        FastClass fastClass = FastClass.create(serviceClass);
        int methodIndex = fastClass.getIndex(methodName, parameterTypes);

        // 也可以
        //FastMethod fastMethod = fastClass.getMethod(methodName, parameterTypes);
        //return fastMethod.invoke(serviceBean, parameters);

        // 调用方法并返回结果
        return fastClass.invoke(methodIndex, serviceBean, parameters);
    }

}
