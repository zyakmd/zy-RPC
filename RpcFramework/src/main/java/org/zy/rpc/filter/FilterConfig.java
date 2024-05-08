package org.zy.rpc.filter;

import org.zy.rpc.spi.SpiLoader;
import lombok.SneakyThrows;

import java.io.IOException;

/**
 * @package: com.zy.rpc.filter
 * @author: zyakmd
 * @description: 拦截器配置类，统一管理
 * @date: 2024/4/30 15:12
 */
public class FilterConfig {

    private static FilterChain serviceBeforeFilterChain = new FilterChain();
    private static FilterChain serviceAfterFilterChain = new FilterChain();
    private static FilterChain clientBeforeFilterChain = new FilterChain();
    private static FilterChain clientAfterFilterChain = new FilterChain();

    @SneakyThrows
    public static void initServiceFilter() {
        final SpiLoader spiLoader = SpiLoader.getInstance();
        spiLoader.loadSpi(ServiceAfterFilter.class);
        spiLoader.loadSpi(ServiceBeforeFilter.class);
        serviceBeforeFilterChain.addFilter(spiLoader.gets(ServiceBeforeFilter.class));
        serviceAfterFilterChain.addFilter(spiLoader.gets(ServiceAfterFilter.class));
    }

    public static void initClientFilter() throws IOException, ClassNotFoundException {
        final SpiLoader spiLoader = SpiLoader.getInstance();
        spiLoader.loadSpi(ClientAfterFilter.class);
        spiLoader.loadSpi(ClientBeforeFilter.class);
        clientBeforeFilterChain.addFilter(spiLoader.gets(ClientBeforeFilter.class));
        clientAfterFilterChain.addFilter(spiLoader.gets(ClientAfterFilter.class));
    }

    public static FilterChain getServiceBeforeFilterChain() {
        return serviceBeforeFilterChain;
    }

    public static FilterChain getServiceAfterFilterChain() {
        return serviceAfterFilterChain;
    }

    public static FilterChain getClientBeforeFilterChain() {
        return clientBeforeFilterChain;
    }

    public static FilterChain getClientAfterFilterChain() {
        return clientAfterFilterChain;
    }
}
