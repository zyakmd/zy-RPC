package org.zy.rpc.protocol.handler.provider;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.zy.rpc.common.RpcRequest;
import org.zy.rpc.protocol.RpcProtocol;

/**
 * @package: org.zy.rpc.protocol.handler.provider
 * @author: zyakmd
 * @description: TODO
 * @date: 2024/5/12 19:11
 */
public class ServiceBeforeFilterHanlder extends SimpleChannelInboundHandler<RpcProtocol<RpcRequest>> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcProtocol<RpcRequest> protocol) throws Exception {
        // 可以做Filter之类的操作
        // ...
        // 传给下个处理器，会默认调用
        //ctx.fireChannelRead(protocol);
    }
}
