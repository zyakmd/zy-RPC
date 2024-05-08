package org.zy.rpc.common;

import java.io.Serializable;
import java.util.Objects;

/**
 * @package: com.zy.rpc.common
 * @author: zyakmd
 * @description: 注册中心的服务的元数据
 * @date: 2024/4/30 11:48
 */
public class ServiceMeta implements Serializable {

    private String serviceName;

    private String serviceVersion;

    private String serviceAddr;

    private int servicePort;

    /**
     * 关于redis注册中心的属性
     */
    private long endTime;

    private String UUID;

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        // 逐一比对属性确认是否equals
        ServiceMeta other = (ServiceMeta) obj;
        return servicePort == other.servicePort &&
                Objects.equals(serviceName, other.serviceName) &&
                Objects.equals(serviceVersion, other.serviceVersion) &&
                Objects.equals(serviceAddr, other.serviceAddr) &&
                Objects.equals(UUID, other.UUID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serviceName, serviceVersion, serviceAddr, servicePort, UUID);
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getServiceVersion() {
        return serviceVersion;
    }

    public void setServiceVersion(String serviceVersion) {
        this.serviceVersion = serviceVersion;
    }

    public String getServiceAddr() {
        return serviceAddr;
    }

    public void setServiceAddr(String serviceAddr) {
        this.serviceAddr = serviceAddr;
    }

    public int getServicePort() {
        return servicePort;
    }

    public void setServicePort(int servicePort) {
        this.servicePort = servicePort;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public String getUUID() {
        return UUID;
    }

    public void setUUID(String UUID) {
        this.UUID = UUID;
    }
}
