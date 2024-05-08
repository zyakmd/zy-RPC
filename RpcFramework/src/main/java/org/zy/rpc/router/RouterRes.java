package org.zy.rpc.router;

import org.zy.rpc.common.ServiceMeta;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @package: com.zy.rpc.router
 * @author: zyakmd
 * @description: 从router获取的服务节点
 * @date: 2024/4/30 13:14
 */
public class RouterRes {

    // 当前服务节点
    private ServiceMeta curServiceMeta;
    // 剩余服务节点
    private Collection<ServiceMeta> otherServiceMeta;

    public ServiceMeta getCurServiceMeta() {
        return curServiceMeta;
    }

    public Collection<ServiceMeta> getOtherServiceMeta() {
        return otherServiceMeta;
    }

    /**
     * 参数为当前要用的服务，和整个服务列表
     * @param curServiceMeta
     * @param otherServiceMeta
     * @return
     */
    public static RouterRes buildRouter(ServiceMeta curServiceMeta, Collection<ServiceMeta> otherServiceMeta) {
        final RouterRes routerRes = new RouterRes();
        routerRes.curServiceMeta = curServiceMeta;
        // 如果只有一个服务
        if (otherServiceMeta.size() == 1){
            otherServiceMeta = new ArrayList<>();
        }else {
            otherServiceMeta.remove(curServiceMeta);
        }
        routerRes.otherServiceMeta = otherServiceMeta;
        return routerRes;
    }
}
