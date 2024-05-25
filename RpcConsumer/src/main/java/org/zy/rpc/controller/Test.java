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
import org.zy.rpc.router.LoadBalancer;

import javax.servlet.http.HttpSession;

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

    /**
     * 指定@RpcReference的参数即可测试，如loadBalancer负载均衡，faultTolerant容错机制
     */
    @RpcReference(timeout = 10000L, faultTolerant = FaultTolerantRules.Failover, loadBalancer = LoadBalancerRules.Random)
    TestInterface1 interface1;

    @RpcReference(loadBalancer = LoadBalancerRules.ConsistentHash)
    TestInterface2 interface2;

    @RequestMapping("test/{word}")
    public String test(@PathVariable String word){
        interface1.test1(word);
        HttpSession httpSession = null;
        return "interface1.test1 ok";
    }

    @RequestMapping("test2/{word}")
    public String test2(@PathVariable String word){
        interface1.test2(word);
        return "interface1.test2 ok";
    }


    @RequestMapping("test3/{word}")
    public String test3(@PathVariable String word){
        return interface2.test("interface2 ok" + word);
    }

}
