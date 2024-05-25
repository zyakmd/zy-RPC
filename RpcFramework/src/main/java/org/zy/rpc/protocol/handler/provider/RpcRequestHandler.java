package org.zy.rpc.protocol.handler.provider;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.zy.rpc.ThreadPool.ThreadPoolFactory;
import org.zy.rpc.common.RpcRequest;
import org.zy.rpc.protocol.RpcProtocol;

/**
 * @package: org.zy.rpc.protocol.handler
 * @author: zyakmd
 * @description: 提供方 ServerBootstrap.childHandler 的响应事件
 * @date: 2024/5/7 17:09
 */
public class RpcRequestHandler extends SimpleChannelInboundHandler<RpcProtocol<RpcRequest>> {

    /**
     * 有新的数据消息到达时，Netty 将会自动调用该方法
     * @param ctx 一个 ChannelHandlerContext 对象，用于获取与当前处理器相关的上下文信息，如 Channel、Pipeline 等
     * @param protocol 一个泛型类型，表示接收到的数据消息，此处我们指定了为 RpcProtocol<RpcRequest>
     * @throws Exception
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcProtocol<RpcRequest> protocol) throws Exception {
        ThreadPoolFactory.submitRequest(ctx, protocol);
        ctx.fireChannelRead(protocol);
    }
}
