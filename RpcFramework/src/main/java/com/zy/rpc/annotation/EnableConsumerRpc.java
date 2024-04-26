package com.zy.rpc.annotation;

import com.zy.rpc.consumer.ConsumerProcessor;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 配置在Consumer模块的Application，即可自动装配ConsumerProcessor
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Import(ConsumerProcessor.class)
public @interface EnableConsumerRpc {
}
