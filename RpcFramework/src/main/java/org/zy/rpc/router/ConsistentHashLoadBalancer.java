package org.zy.rpc.router;

import org.zy.rpc.common.ServiceMeta;
import org.zy.rpc.config.RpcProperties;
import org.zy.rpc.registry.RegistryService;
import org.zy.rpc.spi.SpiLoader;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * @package: org.zy.rpc.router
 * @author: zyakmd
 * @description: 一致性哈希
 * @date: 2024/5/11 15:11
 */
public class ConsistentHashLoadBalancer implements LoadBalancer{

    // 虚拟节点数量（越多越均匀）
    private int virtualNodeReplicas = 10;

    public void setVirtualNodeReplicas(int virtualNodeReplicas) {
        this.virtualNodeReplicas = virtualNodeReplicas;
    }

    @Override
    public RouterRes select(Object[] params, String serviceName) {
        // 获得注册中心
        RegistryService registryService = SpiLoader.getInstance().get(RpcProperties.getInstance().getRegisterType());
        // 从注册中心获取可用服务列表
        List<ServiceMeta> services = registryService.getServices(serviceName);
        TreeMap<Integer, ServiceMeta> ring = getConsistentHashRing(services);
        // 获取最近的哈希环上节点位置
        Map.Entry<Integer, ServiceMeta> entry = ring.ceilingEntry(params[0].hashCode());
        if (entry == null) {
            // 如果没有找到则使用最小的节点
            entry = ring.firstEntry();
        }
        return RouterRes.buildRouter(entry.getValue(), services);
    }

    /**
     * 生成一个哈希环
     * @param services
     * @return
     */
    private TreeMap<Integer, ServiceMeta> getConsistentHashRing(List<ServiceMeta> services){
        TreeMap<Integer, ServiceMeta> ring = new TreeMap<>();

        for (ServiceMeta service : services) {
            for (int i = 0; i < virtualNodeReplicas; i++) {
                ring.put(( String.join(":", service.getServiceName(), service.getServiceAddr(),
                        Integer.toString(service.getServicePort())) + i).hashCode(), service);
            }
        }
        return ring;
    }

}
