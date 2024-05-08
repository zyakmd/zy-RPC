package org.zy.rpc.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zy.rpc.annotation.RpcReference;
import org.zy.rpc.common.constants.FaultTolerantRules;
import org.zy.rpc.common.constants.LoadBalancerRules;
import org.zy.rpc.middlemethod.TestInterface1;
import org.zy.rpc.middlemethod.TestInterface2;

/**
 * @package: org.zy.rpc.controller
 * @author: zyakmd
 * @description: 调用方测试接口
 * @date: 2024/5/7 15:10
 */
@RestController
@RequestMapping("/Consumer")
@Slf4j
public class Test {

    @RpcReference(timeout = 10000L, faultTolerant = FaultTolerantRules.Failover, loadBalancer = LoadBalancerRules.RoundRobin)
    TestInterface1 interface1;

    @RpcReference()
    TestInterface2 interface2;

    /**
     * 轮询 http://localhost:8888/Consumer/test/1
     * 会触发故障转移,提供方模拟异常
     * @param word
     * @return
     */
    @RequestMapping("test/{word}")
    public String test(@PathVariable String word){
        interface1.test1(word);
        return "interface1.test1 ok";
    }

    /**
     * 轮询,无如何异常
     * @param word
     * @return
     */
    @RequestMapping("test2/{word}")
    public String test2(@PathVariable String word){
        interface1.test2(word);
        return "interface1.test2 ok";
    }

    /**
     * 一致性哈希
     * @param word
     * @return
     */
    @RequestMapping("test3/{word}")
    public String test3(@PathVariable String word){
        return interface2.test("interface2 ok" + word);
    }

}
