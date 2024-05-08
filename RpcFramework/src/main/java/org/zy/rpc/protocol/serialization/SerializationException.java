package org.zy.rpc.protocol.serialization;

/**
 * @package: com.zy.rpc.protocol.serialization
 * @author: zyakmd
 * @description: 序列化异常
 * @date: 2024/4/28 20:18
 */
public class SerializationException extends RuntimeException {

    public SerializationException() {
        super();
    }

    public SerializationException(String msg) {
        super(msg);
    }

    public SerializationException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public SerializationException(Throwable cause) {
        super(cause);
    }
}
