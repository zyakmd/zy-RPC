package org.zy.rpc.consumer;

import org.zy.rpc.common.RpcRequest;
import org.zy.rpc.common.ServiceMeta;
import org.zy.rpc.protocol.RpcProtocol;
import org.zy.rpc.protocol.coder.RpcDecoder;
import org.zy.rpc.protocol.coder.RpcEncoder;
import org.zy.rpc.protocol.handler.consumer.RpcResponseHandler;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.netty.bootstrap.Bootstrap;

/**
 * @package: com.zy.rpc.consumer
 * @author: zyakmd
 * @description: 调用方发送请求
 * @date: 2024/5/1 11:41
 */
public class RpcConsumer {

    private final Bootstrap bootstrap;
    private final EventLoopGroup eventLoopGroup;
    private Logger logger = LoggerFactory.getLogger(RpcConsumer.class);

    public RpcConsumer() {
        // 创建一个客户端的引导类
        bootstrap = new Bootstrap();
        // 创建反应器轮询组，指定轮询的线程数
        eventLoopGroup = new NioEventLoopGroup(4);
        // Bootstrap是Netty提供的一个便利的工厂类，可以通过它来完成客户端或服务端的Netty初始化
        bootstrap.group(eventLoopGroup)     //并设置到Bootstrap引导类实例
                .channel(NioSocketChannel.class)    // 设置通道的IO类型。Netty不止支持Java NIO，也支持阻塞式的OIO。
                .option(ChannelOption.SO_KEEPALIVE, true)   // 设置传输通道的配置选项，第二个表示是否开启TCP底层心跳机制，true为开启，false为关闭。
                .handler(new ChannelInitializer<SocketChannel>() {
                    // 有连接到达时会创建一个通道的子通道，并初始化
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        socketChannel.pipeline()
                                .addLast(new RpcEncoder())
                                .addLast(new RpcDecoder())
                                .addLast(new RpcResponseHandler());
                    }
                });
    }

    /**
     * 发送请求
     * @param protocol 消息
     * @param serviceMeta 服务
     * @throws Exception
     */
    public void sendRequest(RpcProtocol<RpcRequest> protocol, ServiceMeta serviceMeta) throws Exception{
        if (serviceMeta != null) {
            // 连接
            ChannelFuture future = bootstrap.connect(serviceMeta.getServiceAddr(), serviceMeta.getServicePort()).sync();
            future.addListener((ChannelFutureListener) arg0 -> {
                if (future.isSuccess()) {
                    logger.info("连接 rpc server {} 端口 {} 成功.", serviceMeta.getServiceAddr(), serviceMeta.getServicePort());
                } else {
                    logger.error("连接 rpc server {} 端口 {} 失败.", serviceMeta.getServiceAddr(), serviceMeta.getServicePort());
                    future.cause().printStackTrace();
                    eventLoopGroup.shutdownGracefully();
                }
            });
            // 写入数据
            future.channel().writeAndFlush(protocol);
        }
    }

    /**
     * 维护eventLoopGroup的关闭
     * @throws Throwable
     */
    @Override
    protected void finalize() throws Throwable {
        try{
            eventLoopGroup.shutdownGracefully();
            logger.info("成功释放客户端 NioEventLoopGroup 资源");
        }finally {
            super.finalize();
        }
    }
}
