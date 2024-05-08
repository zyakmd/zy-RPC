package org.zy.rpc.service;

import org.zy.rpc.annotation.RpcService;
import org.zy.rpc.middlemethod.TestInterface2;

/**
 * @package: org.zy.rpc.service
 * @author: zyakmd
 * @description: TODO
 * @date: 2024/5/7 20:23
 */
@RpcService
public class TestInterface2Impl implements TestInterface2 {
    @Override
    public String test(String word) {
        System.out.println("RpcProvider2 test succeed:" + word);
        return "This is RpcProvider2's TestInterface2Impl";
    }
}
