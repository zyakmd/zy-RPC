package org.zy.rpc.common.constants;

/**
 * @package: com.zy.rpc.common.constants
 * @author: zyakmd
 * @description: 可选负载均衡策略
 * @date: 2024/4/30 15:29
 */
public interface LoadBalancerRules {

    String ConsistentHash = "consistentHash";

    String RoundRobin = "roundRobin";

    String Random = "random";

}
