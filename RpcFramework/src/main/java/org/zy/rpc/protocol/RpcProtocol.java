package org.zy.rpc.protocol;

import java.io.Serializable;

/**
 * @package: com.zy.rpc.protocol.serialization
 * @author: zyakmd
 * @description: TODO
 * @date: 2024/4/28 20:10
 */
public class RpcProtocol<T> implements Serializable {

    private MsgHeader header;
    private T body;

    public MsgHeader getHeader(){
        return header;
    }

    public void setHeader(MsgHeader header) {
        this.header = header;
    }

    public T getBody() {
        return body;
    }

    public void setBody(T body) {
        this.body = body;
    }
}
