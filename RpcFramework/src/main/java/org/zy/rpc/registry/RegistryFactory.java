package org.zy.rpc.registry;

import org.zy.rpc.spi.SpiLoader;

/**
 * @package: com.zy.rpc.registry
 * @author: zyakmd
 * @description: 注册中心工厂
 * @date: 2024/4/30 11:19
 */
public class RegistryFactory {

    public static RegistryService get(String registryService) throws Exception {
        return SpiLoader.getInstance().get(registryService);
    }

    public static void init() throws Exception {
        SpiLoader.getInstance().loadSpi(RegistryService.class);
    }
}
