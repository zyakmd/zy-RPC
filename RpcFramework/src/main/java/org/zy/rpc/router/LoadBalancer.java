package org.zy.rpc.router;

/**
 * @package: com.zy.rpc.router
 * @author: zyakmd
 * @description: 负载均衡,根据负载均衡获取对应的服务节点(负载均衡包装服务节点)
 * @date: 2024/4/30 13:20
 */
public interface LoadBalancer<T>{

    /**
     * 选择负载均衡策略
     * @param params 入参,可自定义拿到入参后自行处理负载策略
     * @param serviceName 服务key
     * @return 当前服务节点以及其他节点(用于给容错使用)
     */
    RouterRes select(Object[] params, String serviceName);

}
