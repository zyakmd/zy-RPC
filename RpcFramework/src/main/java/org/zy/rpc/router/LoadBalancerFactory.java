package org.zy.rpc.router;

import org.zy.rpc.spi.SpiLoader;

/**
 * @package: com.zy.rpc.router
 * @author: zyakmd
 * @description: 负载均衡工厂
 * @date: 2024/4/30 13:22
 */
public class LoadBalancerFactory {

    public static LoadBalancer get(String serviceLoadBalancer) throws Exception {
        return SpiLoader.getInstance().get(serviceLoadBalancer);
    }

    public static void init() throws Exception {
        SpiLoader.getInstance().loadSpi(LoadBalancer.class);
    }

}
