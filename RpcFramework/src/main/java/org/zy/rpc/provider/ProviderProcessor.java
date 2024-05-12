package org.zy.rpc.provider;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.zy.rpc.ThreadPool.ThreadPoolFactory;
import org.zy.rpc.annotation.RpcService;
import org.zy.rpc.common.CommonMethod;
import org.zy.rpc.common.ServiceMeta;
import org.zy.rpc.config.RpcProperties;
import org.zy.rpc.filter.FilterConfig;
import org.zy.rpc.protocol.coder.RpcDecoder;
import org.zy.rpc.protocol.coder.RpcEncoder;
import org.zy.rpc.protocol.handler.provider.FinishBackHandler;
import org.zy.rpc.protocol.handler.provider.RpcRequestHandler;
import org.zy.rpc.protocol.handler.provider.ServiceBeforeFilterHanlder;
import org.zy.rpc.protocol.serialization.SerializationFactory;
import org.zy.rpc.registry.RegistryFactory;
import org.zy.rpc.registry.RegistryService;
import org.zy.rpc.router.LoadBalancerFactory;
import org.zy.rpc.utils.PropertiesUtils;

import java.util.HashMap;
import java.util.Map;


/**
 * @package: com.zy.rpc.provider
 * @author: zyakmd
 * @description: 服务提供方的处理器，@EnableProviderRpc注解调用
 * @date: 2024/4/26 16:44
 */
public class ProviderProcessor implements EnvironmentAware, InitializingBean, BeanPostProcessor {

    private Logger logger = LoggerFactory.getLogger("提供方处理器-ProviderProcessor");

    RpcProperties rpcProperties;

    /**
     * 服务提供方自己的ip
     */
    private static String serverAddress = "127.0.0.1";

    private final Map<String, Object> rpcServiceMap = new HashMap<>();
    /**
     * 初始化环境，从配置文件读取
     * @param environment
     */
    @Override
    public void setEnvironment(Environment environment) {
        RpcProperties properties = RpcProperties.getInstance();
        PropertiesUtils.init(properties, environment);
        rpcProperties = properties;
        logger.info("配置文件读取成功");
    }

    /**
     * 开启服务，准备监听客户端的serverBootstrap
     * @throws InterruptedException
     */
    private void startRpcServer() throws InterruptedException {
        int serverPort = rpcProperties.getPort();
        // 两个事件轮询器，异步连接的接收和处理逻辑
        EventLoopGroup boss = new NioEventLoopGroup();
        EventLoopGroup worker = new NioEventLoopGroup();
        try {
            // 创建一个服务端的引导类
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(boss, worker)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .channel(NioServerSocketChannel.class)    // 设置通道的IO类型
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            socketChannel.pipeline()    // 添加业务逻辑链，为什么每次都要en再de？
                                    .addLast(new RpcEncoder())
                                    .addLast(new RpcDecoder())
                                    .addLast(new ServiceBeforeFilterHanlder())
                                    .addLast(new RpcRequestHandler())
                                    .addLast(new FinishBackHandler());
                                    //.addLast(new ServiceAfterFilterHandler())
                        }
                    }).childOption(ChannelOption.SO_KEEPALIVE, true);

            ChannelFuture channelFuture = serverBootstrap.bind(this.serverAddress, serverPort).sync();
            logger.info("server addr {} started on port {}", this.serverAddress, serverPort);
            // 在当前线程中同步等待 Channel 关闭的事件发生
            channelFuture.channel().closeFuture().sync();
            logger.info("调用成功");

            // 钩子函数，在JVM结束时再次关闭，维护一致性
            Runtime.getRuntime().addShutdownHook(new Thread(() ->
            {
                logger.info("ShutdownHook execute start...");
                boss.shutdownGracefully();
                logger.info("Netty NioEventLoopGroup shutdownGracefully...");
                worker.shutdownGracefully();
                logger.info("Netty NioEventLoopGroup shutdownGracefully2...");
                logger.info("ShutdownHook execute end...");
            }, "Allen-thread"));
        } finally {
            // 调用完成，销毁管道
            boss.shutdownGracefully();
            worker.shutdownGracefully();
        }
    }

    /**
     * 初始化bean
     * @throws Exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        // channelFuture.channel().closeFuture().sync(); 会阻塞线程，因此单独开一个执行
        Thread t = new Thread(() -> {
            try {
                startRpcServer();
            } catch (Exception e) {
                logger.error("start rpc server error.", e);
            }
        });

        t.setDaemon(true);  // 守护进程，跟随main进程结束而结束
        t.start();
        SerializationFactory.init();
        RegistryFactory.init();
        LoadBalancerFactory.init();
        FilterConfig.initServiceFilter();
        ThreadPoolFactory.setRpcServiceMap(rpcServiceMap);
    }

    /**
     * 服务注册
     * bean实例化之后的后置处理器。
     * 当spring完成对象的创建之后，遍历所有的InstantiationAwareBeanPostProcessor
     * 并执行里面实现的所有postProcessAfterInitialization方法
     * @param bean
     * @param beanName
     * @return
     * @throws BeansException
     */
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> beanClass = bean.getClass();
        RpcService rpcService = beanClass.getAnnotation(RpcService.class);
        if(rpcService != null){
            // 可能会有多个接口,默认选择第一个接口
            String serviceName = beanClass.getInterfaces()[0].getName();
            if (!rpcService.serviceInterface().equals(void.class)){
                serviceName = rpcService.serviceInterface().getName();
            }
            String serviceVersion = rpcService.serviceVersion();
            try {
                // 服务注册
                int servicePort = rpcProperties.getPort();
                // 获取注册中心 ioc
                RegistryService registryService = RegistryFactory.get(rpcProperties.getRegisterType());
                ServiceMeta serviceMeta = new ServiceMeta();
                // 服务提供方地址
                serviceMeta.setServiceAddr("127.0.0.1");    // 不应该是从配置读取吗？
                serviceMeta.setServicePort(servicePort);
                serviceMeta.setServiceVersion(serviceVersion);
                serviceMeta.setServiceName(serviceName);
                registryService.register(serviceMeta);
                // 缓存
                rpcServiceMap.put(CommonMethod.getServiceKey(serviceMeta.getServiceName(),serviceMeta.getServiceVersion()), bean);
                logger.info("register server {} version {}",serviceName,serviceVersion);
            } catch (Exception e) {
                logger.error("failed to register service {}",  serviceVersion, e);
            }
        }

        return bean;
    }
}
