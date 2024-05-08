package org.zy.rpc.common;

/**
 * @package: com.zy.rpc.common
 * @author: zyakmd
 * @description: 公共方法
 * @date: 2024/5/7 11:40
 */
public class CommonMethod {

    // key: 服务名 value: 服务提供方
    public static String getServiceKey(String serviceName, String serviceVersion){
        return String.join("$", serviceName, serviceVersion);
    }


}
