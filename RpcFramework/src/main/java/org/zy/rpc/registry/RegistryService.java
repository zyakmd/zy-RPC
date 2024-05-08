package org.zy.rpc.registry;

import org.zy.rpc.common.ServiceMeta;

import java.io.IOException;
import java.util.List;

/**
 * @package: com.zy.rpc.registry
 * @author: zyakmd
 * @description: 注册中心接口
 * @date: 2024/4/30 11:46
 */
public interface RegistryService {

    /**
     * 服务注册
     * @param serviceMeta
     * @throws Exception
     */
    void register(ServiceMeta serviceMeta) throws Exception;

    /**
     * 服务注销
     * @param serviceMeta
     * @throws Exception
     */
    void unRegister(ServiceMeta serviceMeta) throws Exception;

    /**
     *  获取ServiceName下对应的所有服务
     * @param serviceName
     * @return
     */
    List<ServiceMeta> getServices(String serviceName);

    /**
     * 销毁
     * @throws IOException
     */
    void destroy() throws IOException;
}
