package com.zy.rpc.consumer;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;


public class ConsumerProcessor implements BeanPostProcessor, EnvironmentAware, InitializingBean {

    /**
     * 初始化环境，从配置文件读取
     * @param environment
     */
    @Override
    public void setEnvironment(Environment environment) {

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
