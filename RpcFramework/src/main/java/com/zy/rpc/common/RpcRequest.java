package com.zy.rpc.common;

import java.io.Serializable;
import java.util.Map;

/**
 * @package: com.zy.rpc.common
 * @author: zyakmd
 * @description: TODO
 * @date: 2024/4/27 17:44
 */
public class RpcRequest implements Serializable {

    private String serviceVersion;
    private String className;
    private String methodName;
    private Object data;
    private Class dataClass;
    private Class<?>[] parameterTypes;
    private Map<String,Object> serviceAttachments;
    private Map<String,Object> clientAttachments;

    public String getServiceVersion() {
        return serviceVersion;
    }

    public void setServiceVersion(String serviceVersion) {
        this.serviceVersion = serviceVersion;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public Class getDataClass() {
        return dataClass;
    }

    public void setDataClass(Class dataClass) {
        this.dataClass = dataClass;
    }

    public Class<?>[] getParameterTypes() {
        return parameterTypes;
    }

    public void setParameterTypes(Class<?>[] parameterTypes) {
        this.parameterTypes = parameterTypes;
    }

    public Map<String, Object> getServiceAttachments() {
        return serviceAttachments;
    }

    public void setServiceAttachments(Map<String, Object> serviceAttachments) {
        this.serviceAttachments = serviceAttachments;
    }

    public Map<String, Object> getClientAttachments() {
        return clientAttachments;
    }

    public void setClientAttachments(Map<String, Object> clientAttachments) {
        this.clientAttachments = clientAttachments;
    }
}
