package org.zy.rpc.protocol.serialization;

import org.zy.rpc.spi.SpiLoader;

/**
 * @package: com.zy.rpc.protocol.serialization
 * @author: zyakmd
 * @description: 序列化工厂
 * @date: 2024/4/28 20:53
 */
public class SerializationFactory {

    public static RpcSerialization get(String serializationName) throws Exception{
        return SpiLoader.getInstance().get(serializationName);
    }

    public static void init() throws Exception{
        SpiLoader.getInstance().loadSpi(RpcSerialization.class);
    }
}
