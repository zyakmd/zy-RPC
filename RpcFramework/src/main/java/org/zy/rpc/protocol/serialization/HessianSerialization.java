package org.zy.rpc.protocol.serialization;

import com.caucho.hessian.io.HessianInput;
import com.caucho.hessian.io.HessianOutput;
import com.caucho.hessian.io.HessianSerializerInput;
import com.caucho.hessian.io.HessianSerializerOutput;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.rmi.ServerException;

/**
 * @package: org.zy.rpc.protocol.serialization
 * @author: zyakmd
 * @description: Hessian 序列化，字节流传输，二进制格式
 * @date: 2024/5/10 16:39
 */
public class HessianSerialization implements RpcSerialization{
    @Override
    public <T> byte[] serialize(T obj) throws IOException {
        if (obj == null) {
            throw new NullPointerException();
        }
        byte[] results;
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            HessianSerializerOutput output = new HessianSerializerOutput(bos);
            output.writeObject(obj);
            output.flush();
            results =  bos.toByteArray();
        } catch (Exception e){
            throw new SerializationException(e);
        }

        return results;
    }

    @Override
    public <T> T deserialize(byte[] data, Class<T> clz) throws IOException {
        if (data == null) {
            throw new NullPointerException();
        }
        T result;
        try (ByteArrayInputStream is = new ByteArrayInputStream(data)) {
            HessianSerializerInput input = new HessianSerializerInput(is);
            result = (T) input.readObject(clz);
        } catch (Exception e) {
            throw new SerializationException(e);
        }
        return result;
    }
}
