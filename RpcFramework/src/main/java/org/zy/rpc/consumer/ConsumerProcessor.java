package org.zy.rpc.consumer;

import org.zy.rpc.annotation.RpcReference;
import org.zy.rpc.config.RpcProperties;
import org.zy.rpc.filter.ClientLogFilter;
import org.zy.rpc.filter.FilterConfig;
import org.zy.rpc.protocol.serialization.SerializationFactory;
import org.zy.rpc.registry.RegistryFactory;
import org.zy.rpc.router.LoadBalancerFactory;
import org.zy.rpc.utils.PropertiesUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;


/**
 * @package: com.zy.rpc.consumer
 * @author: zyakmd
 * @description: 服务调用方的处理器，@EnableConsumerRpc注解调用
 * @date: 2024/4/26 16:44
 */
public class ConsumerProcessor implements BeanPostProcessor, EnvironmentAware, InitializingBean {

    private Logger logger = LoggerFactory.getLogger("调用方处理器-ConsumerProcessor");

    RpcProperties rpcProperties;

    /**
     * 初始化环境，从配置文件读取
     * @param environment
     */
    @Override
    public void setEnvironment(Environment environment) {
        RpcProperties properties = RpcProperties.getInstance();
        PropertiesUtils.init(properties, environment);
        rpcProperties = properties;
        logger.info("配置文件读取成功");
    }

    /**
     * 初始化bean
     * @throws Exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        SerializationFactory.init();
        RegistryFactory.init();
        LoadBalancerFactory.init();
        FilterConfig.initClientFilter();
    }

    /**
     * bean实例化之后的后置处理器。
     * 当spring完成对象的创建之后，遍历所有的InstantiationAwareBeanPostProcessor，并执行里面实现的所有postProcessAfterInitialization方法。
     * @param bean
     * @param beanName
     * @return
     * @throws BeansException
     */
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        // 获取所有字段
        final Field[] fields = bean.getClass().getDeclaredFields();
        // 遍历所有字段找到 RpcReference 注解的字段
        for (Field field : fields) {
            if(field.isAnnotationPresent(RpcReference.class)){
                final RpcReference rpcReference = field.getAnnotation(RpcReference.class);
                final Class<?> aClass = field.getType();
                field.setAccessible(true);
                Object object = null;
                try {
                    // 创建代理对象
                    object = Proxy.newProxyInstance(
                            aClass.getClassLoader(),
                            new Class<?>[]{aClass},
                            new RpcInvokerProxy(rpcReference.serviceVersion(),rpcReference.timeout(),rpcReference.faultTolerant(),
                                    rpcReference.loadBalancer(),rpcReference.retryCount()));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                try {
                    // 将代理对象设置给字段
                    field.set(bean,object);
                    field.setAccessible(false);
                    logger.info(beanName + " field:" + field.getName() + "注入成功");
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                    logger.info(beanName + " field:" + field.getName() + "注入失败");
                }
            }
        }
        return bean;
    }
}
