package org.zy.rpc.protocol.serialization;

import java.io.IOException;

/**
 * @package: com.zy.rpc.protocol.serialization
 * @author: zyakmd
 * @description: 序列化接口
 * @date: 2024/4/28 20:16
 */
public interface RpcSerialization {
    <T> byte[] serialize(T obj) throws IOException;

    <T> T deserialize(byte[] data, Class<T> clz) throws IOException;
}
