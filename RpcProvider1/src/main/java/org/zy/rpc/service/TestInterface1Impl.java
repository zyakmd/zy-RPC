package org.zy.rpc.service;

import org.zy.rpc.annotation.RpcService;
import org.zy.rpc.middlemethod.TestInterface1;

/**
 * @package: org.zy.rpc.service
 * @author: zyakmd
 * @description: TODO
 * @date: 2024/5/7 20:20
 */
@RpcService
public class TestInterface1Impl implements TestInterface1 {
    @Override
    public void test1(String word) {
        System.out.println("RpcProvider1 test1 succeed  :" + word);
    }

    @Override
    public void test2(String word) {
        System.out.println("RpcProvider1 test2 succeed  :" + word);
    }
}
