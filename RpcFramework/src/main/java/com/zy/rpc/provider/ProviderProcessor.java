package com.zy.rpc.provider;

import com.zy.rpc.filter.ClientLogFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;


/**
 * @package: com.zy.rpc.provider
 * @author: zyakmd
 * @description: 服务提供方的处理器
 * @date: 2024/4/26 16:44
 */
public class ProviderProcessor implements EnvironmentAware, InitializingBean, BeanPostProcessor {

    @Override
    public void afterPropertiesSet() throws Exception {

    }

    @Override
    public void setEnvironment(Environment environment) {

    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return BeanPostProcessor.super.postProcessAfterInitialization(bean, beanName);
    }
}
