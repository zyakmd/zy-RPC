package org.zy.rpc.router;

import org.zy.rpc.common.ServiceMeta;
import org.zy.rpc.config.RpcProperties;
import org.zy.rpc.registry.RegistryService;
import org.zy.rpc.spi.SpiLoader;

import java.util.List;

/**
 * @package: org.zy.rpc.router
 * @author: zyakmd
 * @description: 随机
 * @date: 2024/5/11 14:45
 */
public class RandomLoadBalancer implements LoadBalancer{
    @Override
    public RouterRes select(Object[] params, String serviceName) {
        // 获得注册中心
        RegistryService registryService = SpiLoader.getInstance().get(RpcProperties.getInstance().getRegisterType());
        // 从注册中心获取可用服务列表
        List<ServiceMeta> services = registryService.getServices(serviceName);
        // 随机获取
        return RouterRes.buildRouter(services.get((int) ((Math.random()*services.size()) % services.size())), services);
    }
}
