package org.zy.rpc.filter;

import org.zy.rpc.config.RpcProperties;

import java.util.Map;

/**
 * @package: org.zy.rpc.filter
 * @author: zyakmd
 * @description: TODO
 * @date: 2024/5/11 17:04
 */
public class ServiceTokenFilter implements ServiceBeforeFilter{
    @Override
    public void doFilter(FilterData filterData) {
        final Map<String, Object> attachments = filterData.getClientAttachments();
        final Map<String, Object> serviceAttachments = RpcProperties.getInstance().getServiceAttachments();
        if (!attachments.getOrDefault("token","").equals(serviceAttachments.getOrDefault("token",""))){
            throw new IllegalArgumentException("token不正确");
        }
    }
}
