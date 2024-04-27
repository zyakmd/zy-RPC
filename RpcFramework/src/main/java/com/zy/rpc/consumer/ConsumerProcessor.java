package com.zy.rpc.consumer;

import com.zy.rpc.config.RpcProperties;
import com.zy.rpc.filter.ClientLogFilter;
import com.zy.rpc.utils.PropertiesUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;


public class ConsumerProcessor implements BeanPostProcessor, EnvironmentAware, InitializingBean {

    private Logger logger = LoggerFactory.getLogger(ClientLogFilter.class);

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

    }

    /**
     * 代理层注入
     * @param bean
     * @param beanName
     * @return
     * @throws BeansException
     */
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return BeanPostProcessor.super.postProcessAfterInitialization(bean, beanName);
    }
}
