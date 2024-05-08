package org.zy.rpc.router;

import org.zy.rpc.common.ServiceMeta;
import org.zy.rpc.config.RpcProperties;
import org.zy.rpc.registry.RegistryService;
import org.zy.rpc.spi.SpiLoader;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @package: org.zy.rpc.router
 * @author: zyakmd
 * @description: TODO
 * @date: 2024/5/8 16:25
 */
public class RoundRobinLoadBalancer implements LoadBalancer{

    private static AtomicInteger roundRobinId = new AtomicInteger(0);

    @Override
    public RouterRes select(Object[] params, String serviceName) {
        // 获得注册中心
        RegistryService registryService = SpiLoader.getInstance().get(RpcProperties.getInstance().getRegisterType());
        // 从注册中心获取可用服务列表
        List<ServiceMeta> services = registryService.getServices(serviceName);
        // 轮询ID获取服务
        roundRobinId.addAndGet(1);
        if(roundRobinId.get() == Integer.MAX_VALUE) roundRobinId.set(0);

        return RouterRes.buildRouter(services.get(roundRobinId.get() % services.size()), services);
    }
}
