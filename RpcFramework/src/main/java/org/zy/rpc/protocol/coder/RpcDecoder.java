package org.zy.rpc.protocol.coder;

import org.zy.rpc.common.RpcRequest;
import org.zy.rpc.common.RpcResponse;
import org.zy.rpc.common.constants.MsgType;
import org.zy.rpc.common.constants.ProtocolConstants;
import org.zy.rpc.protocol.MsgHeader;
import org.zy.rpc.protocol.RpcProtocol;
import org.zy.rpc.protocol.serialization.RpcSerialization;
import org.zy.rpc.protocol.serialization.SerializationFactory;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

/**
 * @package: com.zy.rpc.protocol.coder
 * @author: zyakmd
 * @description: TODO
 * @date: 2024/5/3 17:58
 */
public class RpcDecoder extends ByteToMessageDecoder {
    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> out) throws Exception {
        // 如果可读字节数少于协议头长度，说明还没有接收完整个协议头，直接返回
        if (byteBuf.readableBytes() < ProtocolConstants.HEADER_TOTAL_LEN) {
            return;
        }
        // 标记当前读取位置，便于后面回退
        byteBuf.markReaderIndex();

        // 读取魔数字段
        short magic = byteBuf.readShort();
        if (magic != ProtocolConstants.MAGIC) {
            throw new IllegalArgumentException("magic number is illegal, " + magic);
        }
        // 读取版本字段
        byte version = byteBuf.readByte();
        // 读取消息类型
        byte msgType = byteBuf.readByte();
        // 读取响应状态
        byte status = byteBuf.readByte();
        // 读取请求 ID
        long requestId = byteBuf.readLong();
        // 获取序列化算法长度
        final int len = byteBuf.readInt();
        if (byteBuf.readableBytes() < len){  // 半包问题
            byteBuf.resetReaderIndex();
            return;
        }
        final byte[] bytes = new byte[len];
        byteBuf.readBytes(bytes);
        final String serialization = new String(bytes);
        // 读取消息体长度
        int dataLength = byteBuf.readInt();
        // 如果可读字节数小于消息体长度，说明还没有接收完整个消息体，回退并返回。 半包问题
        if (byteBuf.readableBytes() < dataLength) {
            // 回退标记位置
            byteBuf.resetReaderIndex();
            return;
        }
        byte[] data = new byte[dataLength];
        // 读取数据
        byteBuf.readBytes(data);

        // 处理消息的类型
        MsgType msgTypeEnum = MsgType.findByType(msgType);
        if (msgTypeEnum == null) {
            return;
        }

        // 构建消息头
        MsgHeader header = new MsgHeader();
        header.setMagic(magic);
        header.setVersion(version);
        header.setStatus(status);
        header.setRequestId(requestId);
        header.setMsgType(msgType);
        header.setSerializations(bytes);
        header.setSerializationLen(len);
        header.setMsgLen(dataLength);
        // 获取序列化器
        RpcSerialization rpcSerialization = SerializationFactory.get(serialization);
        // 根据消息类型进行处理(如果消息类型过多可以使用策略+工厂模式进行管理)
        switch (msgTypeEnum) {
            // 请求消息
            case REQUEST:
                RpcRequest request = rpcSerialization.deserialize(data, RpcRequest.class);
                if (request != null) {
                    RpcProtocol<RpcRequest> protocol = new RpcProtocol<>();
                    protocol.setHeader(header);
                    protocol.setBody(request);
                    out.add(protocol);
                }
                break;
            // 响应消息
            case RESPONSE:
                RpcResponse response = rpcSerialization.deserialize(data, RpcResponse.class);
                if (response != null) {
                    RpcProtocol<RpcResponse> protocol = new RpcProtocol<>();
                    protocol.setHeader(header);
                    protocol.setBody(response);
                    out.add(protocol);
                }
                break;
        }
    }
}
