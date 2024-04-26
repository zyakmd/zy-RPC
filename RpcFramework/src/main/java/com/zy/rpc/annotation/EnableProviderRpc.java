package com.zy.rpc.annotation;

import com.zy.rpc.provider.ProviderProcessor;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 配置在Provider模块的Application，即可自动装配ProviderProcessor
 * Target: 作用范围，TYPE:用于描述类、接口(包括注解类型) 或enum声明
 * RetentionPolicy.RUNTIME策略不仅被保存到class文件中，jvm加载class文件之后，仍然存在
 * 此时可以在运行时通过反射获取到。这样的注解可以用来在运行时进行一些特殊的操作，例如动态生成代码、动态代理等
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(ProviderProcessor.class)
public @interface EnableProviderRpc {
}
