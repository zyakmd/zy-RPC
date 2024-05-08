package org.zy.rpc.common;

import io.netty.util.concurrent.Promise;

/**
 * @package: com.zy.rpc.common
 * @author: zyakmd
 * @description: 返回结果
 * @date: 2024/5/3 18:08
 */
public class RpcFuture<T> {

    private Promise<T> promise;
    private long timeout;

    public Promise<T> getPromise() {
        return promise;
    }

    public void setPromise(Promise<T> promise) {
        this.promise = promise;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public RpcFuture() {
    }

    public RpcFuture(Promise<T> promise, long timeout) {
        this.promise = promise;
        this.timeout = timeout;
    }

}
