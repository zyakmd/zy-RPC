package org.zy.rpc.protocol.handler.provider;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zy.rpc.common.RpcResponse;
import org.zy.rpc.common.constants.MsgStatus;
import org.zy.rpc.filter.FilterConfig;
import org.zy.rpc.protocol.MsgHeader;
import org.zy.rpc.protocol.RpcProtocol;

/**
 * @package: org.zy.rpc.protocol.handler.provider
 * @author: zyakmd
 * @description: TODO
 * @date: 2024/5/8 21:30
 */
public class FinishBackHandler extends SimpleChannelInboundHandler<RpcProtocol<RpcResponse>> {

    private Logger logger = LoggerFactory.getLogger(FinishBackHandler.class);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcProtocol<RpcResponse> protocol) throws Exception {
        RpcResponse response = new RpcResponse();
        MsgHeader header = protocol.getHeader();
        // data过滤，尚未完成
        //try {
        //    FilterConfig.getServiceAfterFilterChain().doFilter(filterData);
        //} catch (Exception e) {
        //    header.setStatus((byte) MsgStatus.FAILED.ordinal());
        //    response.setException(e);
        //    logger.error("after process request {} error", header.getRequestId(), e);
        //}
        ctx.writeAndFlush(protocol);
    }
}
