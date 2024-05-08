package org.zy.rpc.protocol.handler.consumer;

import org.zy.rpc.common.RpcFuture;
import org.zy.rpc.common.RpcRequestHolder;
import org.zy.rpc.common.RpcResponse;
import org.zy.rpc.protocol.RpcProtocol;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * @package: com.zy.rpc.protocol.handler
 * @author: zyakmd
 * @description: 调用方Bootstrap.handler的响应事件
 * @date: 2024/5/3 18:05
 */
public class RpcResponseHandler extends SimpleChannelInboundHandler<RpcProtocol<RpcResponse>> {

    /**
     * 有新的数据消息到达时，Netty 将会自动调用该方法
     * @param ctx 一个 ChannelHandlerContext 对象，用于获取与当前处理器相关的上下文信息，如 Channel、Pipeline 等
     * @param msg 一个泛型类型，表示接收到的数据消息，此处我们指定了为 RpcProtocol<RpcResponse>
     * @throws Exception
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcProtocol<RpcResponse> msg) throws Exception {
        long requestID = msg.getHeader().getRequestId();
        RpcFuture<RpcResponse> future = RpcRequestHolder.REQUEST_MAP.remove(requestID);
        future.getPromise().setSuccess(msg.getBody());
    }
}
