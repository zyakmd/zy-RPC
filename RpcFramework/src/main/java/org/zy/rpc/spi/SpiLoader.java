package org.zy.rpc.spi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @package: com.zy.rpc.spi
 * @author: SPI机制，管理统一加载的bean、
 * @description: TODO
 * @date: 2024/4/28 20:20
 */
public class SpiLoader {

    private Logger logger = LoggerFactory.getLogger(SpiLoader.class);

    // 系统SPI
    private static String SYS_EXTENSION_LOADER_DIR_PREFIX = "META-INF/admin/";

    // 用户SPI
    private static String DIY_EXTENSION_LOADER_DIR_PREFIX = "META-INF/user/";

    private static String[] prefixs = {SYS_EXTENSION_LOADER_DIR_PREFIX, DIY_EXTENSION_LOADER_DIR_PREFIX};

    // bean定义信息 key: 定义的key value：具体类
    private static Map<String, Class> spiClassCache = new ConcurrentHashMap<>();

    // bean 定义信息 key：接口className value：接口子类s
    private static Map<String, Map<String,Class>> spiClassCaches = new ConcurrentHashMap<>();

    // 实例化的bean
    private static Map<String, Object> singletonsObject = new ConcurrentHashMap<>();

    private static SpiLoader spiLoader;

    static {
        spiLoader = new SpiLoader();
    }

    public static SpiLoader getInstance(){
        return spiLoader;
    }

    private SpiLoader(){}

    /**
     * 获取bean
     * @param name
     * @return
     * @param <V>
     */
    public <V> V get(String name) {
        if (!singletonsObject.containsKey(name)) {
            try {
                singletonsObject.put(name, spiClassCache.get(name).newInstance());
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return (V) singletonsObject.get(name);
    }

    /**
     * 获取接口下所有的类
     * @param clazz
     * @return
     */
    public List<Object> gets(Class clazz) {

        final String name = clazz.getName();
        if (!spiClassCaches.containsKey(name)) {
            try {
                throw new ClassNotFoundException(clazz + "未找到");
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        final Map<String, Class> stringClassMap = spiClassCaches.get(name);
        List<Object> objects = new ArrayList<>();
        if (stringClassMap.size() > 0){
            stringClassMap.forEach((k,v)->{
                try {
                    objects.add(singletonsObject.getOrDefault(k,v.newInstance()));
                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            });
        }
        return objects;
    }

    /**
     * spi加载prefixs中指定的bean，并放入extensionClassCache map中
     * @param clazz
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public void loadSpi(Class clazz) throws IOException, ClassNotFoundException {
        if (clazz == null) {
            throw new IllegalArgumentException("class 不能指定为空");
        }
        ClassLoader classLoader = this.getClass().getClassLoader();
        Map<String, Class> classMap = new HashMap<>();
        // 从系统SPI以及用户SPI中找bean
        for (String prefix : prefixs) {
            String spiFilePath = prefix + clazz.getName();
            Enumeration<URL> enumeration = classLoader.getResources(spiFilePath);
            while (enumeration.hasMoreElements()) {
                URL url = enumeration.nextElement();
                InputStreamReader inputStreamReader = null;
                inputStreamReader = new InputStreamReader(url.openStream());
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    String[] lineArr = line.split("=");
                    String key = lineArr[0];
                    String name = lineArr[1];
                    final Class<?> aClass = Class.forName(name);
                    spiClassCache.put(key, aClass);
                    classMap.put(key, aClass);
                    logger.info("加载bean key:{} , value:{}",key,name);
                }
            }
        }
        spiClassCaches.put(clazz.getName(),classMap);
    }

}
