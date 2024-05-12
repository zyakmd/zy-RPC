package org.zy.rpc.registry;

import com.alibaba.fastjson.JSON;
import org.springframework.util.ObjectUtils;
import org.zy.rpc.common.CommonMethod;
import org.zy.rpc.common.ServiceMeta;
import org.zy.rpc.config.RpcProperties;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.exceptions.JedisException;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @package: org.zy.rpc.registry
 * @author: zyakmd
 * @description: TODO
 * @date: 2024/5/10 19:39
 */
public class RedisRegistry implements RegistryService{

    private JedisPool jedisPool;

    // 该服务提供方的标识
    private String UUID;

    // 缓存时间
    private static final int ttl = 10 * 1000;

    // 执行定时任务，如：心跳监听，继承自 ExecutorService
    private ScheduledExecutorService executor;

    // 同步记录已注册的服务Name，免得每次从redis取
    Set<String> services = new HashSet<>();

    RedisRegistry(){
        RpcProperties properties = RpcProperties.getInstance();
        String[] split = properties.getRegisterAddr().split(":");
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(20); // 设置最大连接数为 20，超过阻塞
        poolConfig.setMaxIdle(5);   // 设置最大空闲连接数为 5，超过释放
        jedisPool = new JedisPool(poolConfig, split[0], Integer.parseInt(split[1]));

        // 开启心跳
        executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleWithFixedDelay(this::heartbeat, 5, 5, TimeUnit.SECONDS);
    }

    @Override
    public void register(ServiceMeta serviceMeta) throws Exception {
        String key = CommonMethod.getServiceKey(serviceMeta.getServiceName(), serviceMeta.getServiceVersion());
        if (!services.contains(key)) {
            services.add(key);
        }
        serviceMeta.setUUID(this.UUID);
        serviceMeta.setEndTime(new Date().getTime()+ttl);
        Jedis jedis = getJedis();
        String script = "redis.call('RPUSH', KEYS[1], ARGV[1])\n" +
                "redis.call('EXPIRE', KEYS[1], ARGV[2])";
        List<String> value = new ArrayList<>();
        value.add(JSON.toJSONString(serviceMeta));
        value.add(String.valueOf(10));
        jedis.eval(script,Collections.singletonList(key),value);
        jedis.close();
    }


    @Override
    public void unRegister(ServiceMeta serviceMeta) throws Exception {

    }

    @Override
    public List<ServiceMeta> getServices(String serviceName) {
        // 获取服务地址集合
        Jedis jedis = getJedis();
        List<String> list = jedis.lrange(serviceName, 0, -1);
        jedis.close();
        List<ServiceMeta> serviceMetas = list.stream().map(o -> JSON.parseObject(o, ServiceMeta.class)).collect(Collectors.toList());
        return serviceMetas;
    }

    @Override
    public void destroy() throws IOException {

    }

    private Jedis getJedis(){
        Jedis jedis = jedisPool.getResource();
        RpcProperties properties = RpcProperties.getInstance();
        if(!ObjectUtils.isEmpty(properties.getRegisterPsw())){
            jedis.auth(properties.getRegisterPsw());
        }
        return jedis;
    }

    // redis的lua脚本，晕乎乎
    private void loadService(String key, List<ServiceMeta> serviceMetas){
        String script = "redis.call('DEL', KEYS[1])\n" +
                "for i = 1, #ARGV do\n" +
                "   redis.call('RPUSH', KEYS[1], ARGV[i])\n" +
                "end \n"+
                "redis.call('EXPIRE', KEYS[1],KEYS[2])";
        List<String> keys = new ArrayList<>();
        keys.add(key);
        keys.add(String.valueOf(10));   //过期时间10秒
        List<String> values = serviceMetas.stream().map(o -> JSON.toJSONString(o)).collect(Collectors.toList());
        Jedis jedis = getJedis();
        jedis.eval(script,keys,values);
        jedis.close();
    }

    // 心跳检测
    private void heartbeat(){
        try {
            // 遍历每个服务名称，检查其心跳时间
            for (String serviceName : services) {
                // 获取服务地址集合
                Jedis jedis = getJedis();
                List<String> list = jedis.lrange(serviceName, 0, -1);
                jedis.close();
                List<ServiceMeta> serviceMetas = list.stream().map(o -> JSON.parseObject(o, ServiceMeta.class)).collect(Collectors.toList());
                Iterator<ServiceMeta> iterator = serviceMetas.iterator();

                while(iterator.hasNext()){
                    ServiceMeta meta = iterator.next();
                    // 1.删除过期服务
                    if (meta.getEndTime() < new Date().getTime()){
                        iterator.remove();
                    }
                    // 2.自身续签
                    if (meta.getUUID().equals(this.UUID)){
                        meta.setEndTime(meta.getEndTime() + ttl/2); // 每次增加一半的存活时间
                    }
                }

                // 刷新服务
                if (!ObjectUtils.isEmpty(serviceMetas)) {
                    loadService(serviceName, serviceMetas);
                }
            }
        } catch (JedisException e) {
            e.printStackTrace();
        }
    }
}
