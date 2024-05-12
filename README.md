学习并自己写个RPC ing(X) end(?)

# 概述
RPC（Remote Procedure Call）远程过程调用协议，是一个协议，具体落地自己实现。

RPC和HTTP有什么区别呢？
- 场景：在微服务下，A服务调用B服务就是远程调用
- 意义：传统可以使用http，但会有很多没必要的数据（协议头臃肿？），增加传输负担，降低速率，在服务和服务间有的数据响应的需求就可以通过自定义协议。

**思考/演变过程：**
按照需求和一些设计思想（解耦），可以这样逐步思考
- 最简单就是服务A（调用方）直接写死拿服务B（提供方）
- 写死了修改麻烦，怎么自动呢？添加个注册中心后，先发现后调用
- 那万一提供方挂了，好比服务器宕机——自然想到分布式，提供方也可以有多个，那就需要规划路由层处理（负载均衡）
- 我们知道进程通讯底层都是以字节流/字符串的形式进行数据传输，使用二进制而非传递对象。因此在路由选择后又应该有序列化处理
- 提供方也不该谁都可以调用，因此需要安全校验（token），也就是加拦截器，对其统一处理，又可以规划一个连接器链
- 此时我们从原始的（调用方、提供方），扩展到了（注册中心、调用方、路由层、序列化、拦截器、提供方）
- 是一个高耦合的整体，我们希望除了提供方和调用方都能解耦（双方使用都依赖一个公共模块即可快速对接）
- 那就是：SPI（类似IOC和AOP），注册中心、路由层、序列化、拦截器都由SPI动态加载
- 某个提供方失败了也应该有处理方案，也就是要有容灾/容错机制来保证高可用
- 提供方不该同步提供服务吧？线程池
- 接口不能两个小组口头约定吧，再解耦，在公共依赖模块中还应该有一个接口包统一双方的使用。
- 但两者一个是针对RPC的模块，一个设计业务，而且业务可能变动频繁，RPC大致定了就定了，因此还是再拆个业务模块出来，乃至于不同业务不同模块？好像又会很多，不知道实际开放中是怎样处理的

# 技术栈

- Spring Boot （实现自定义注解等）
- Netty（处理异步事件——请求发送和结果接收），future、promise
- Zookeeper（Curator）和Redis实现注册中心
- SPI
- 自定义协议
- 重试机制
- 容错机制
- 线程池

# 各层设计



## 调用方

主要是`rpc.consumer` 下的`ConsumerProcessor` 初始化 和 `RpcInvokerProxy` 进行代理调用。

具体的消息发送在 `RpcConsumer` 的 `sendRequest` 中，通过 Netty 的 `NioEventLoopGroup` 进行消息的轮询

## 提供方

`rpc.provider` 下的 `ProviderProcessor` 跟 `ConsumerProcessor`一样进行了各种init 操作，但多开了个线程执行 `startRpcServer` 监听调用方的消息。

具体的执行放在了`serverBootstrap`的`childHandler`中的`SimpleChannelInboundHandler`（继承自`SimpleChannelInboundHandler`，有新的数据消息到达时 Netty 将会自动调用）

## 注册中心





## 协议与通信

见 protocol 包



## 协议

主要是消息头 + 消息体

```java
public class MsgHeader implements Serializable {

    private short magic; // 魔数
    private byte version; // 协议版本号
    private byte msgType; // 数据类型
    private byte status; // 状态
    private long requestId; // 请求 ID
    private int serializationLen;   // 序列化方式的长度
    private byte[] serializations;  // 序列化方式
    private int msgLen; // 数据长度
    ...
}
```

- 魔数：可以安全校验
- 版本：协议版本
- 消息类型：请求/响应（最基本的）
- 状态：成功与否
- 请求id：timeout、唯一性等处理
- 序列化方式长度：不像int、long等固定长
- 请求体的数据长度：处理粘包、半包问题
- 请求体：具体的请求内容

## 通讯处理

上面提到的 Netty 

- `ServerBootstrap` 用于服务端
- `Bootstrap` 用于客户端

本项目中的例子：

调用方：

```java
// 创建一个客户端的引导类
bootstrap = new Bootstrap();
// 创建反应器轮询组，指定轮询的线程数
eventLoopGroup = new NioEventLoopGroup(4);
// Bootstrap是Netty提供的一个便利的工厂类，可以通过它来完成客户端或服务端的Netty初始化
bootstrap.group(eventLoopGroup)     //并设置到Bootstrap引导类实例
    .channel(NioSocketChannel.class)    // 设置通道的IO类型。Netty不止支持Java NIO，也支持阻塞式的OIO。
    .option(ChannelOption.SO_KEEPALIVE, true)   // 设置传输通道的配置选项，第二个表示是否开启TCP底层心跳机制，true为开启，false为关闭。
    .handler(new ChannelInitializer<SocketChannel>() {
        // 有连接到达时会创建一个通道的子通道，并初始化
        @Override
        protected void initChannel(SocketChannel socketChannel) throws Exception {
            socketChannel.pipeline()
                .addLast(new encoder())
            	.addLast(new decoder())
                .addLast(new xxHandler());
        }
    });

// 连接
ChannelFuture future = bootstrap.connect(serviceMeta.getServiceAddr(), serviceMeta.getServicePort()).sync();

// 写入数据
future.channel().writeAndFlush(protocol);
```



提供方：

```java
// 创建一个服务端的引导类  
bootstrap = new Bootstrap();  
// 创建反应器轮询组，指定轮询的线程数  
eventLoopGroup = new NioEventLoopGroup(4);  
// Bootstrap是Netty提供的一个便利的工厂类，可以通过它来完成客户端或服务端的Netty初始化  
bootstrap.group(eventLoopGroup)     //并设置到Bootstrap引导类实例  
        .channel(NioSocketChannel.class)    // 设置通道的IO类型。Netty不止支持Java NIO，也支持阻塞式的OIO。  
        .option(ChannelOption.SO_KEEPALIVE, true)   // 设置传输通道的配置选项，第二个表示是否开启TCP底层心跳机制，true为开启，false为关闭。
        .handler(new ChannelInitializer<SocketChannel>() {  
            @Override  
            protected void initChannel(SocketChannel socketChannel) throws Exception {  
	            // 向子通道流水线添加一个Handler业务处理器
                socketChannel.pipeline()  
                     .addLast(new encoder())
            		.addLast(new decoder())
                     .addLast(new xxHandler());
            }  
        });

// 连接，通过调用sync()同步方法阻塞直到绑定成功
ChannelFuture future = bootstrap.connect(ip, port).sync();

// 提供方处理完请求记得写个回写handle，当时模板搭好了，方法也调用了，调用方却一致报超时，发现往回传了
@Override
protected void channelRead0(ChannelHandlerContext ctx, RpcProtocol<RpcResponse> protocol) throws Exception {
    RpcResponse response = new RpcResponse();
    MsgHeader header = protocol.getHeader();
    ctx.writeAndFlush(protocol);
}
```

简单的使用 Netty，具体在于`socketChannel.pipeline().addLast(new xxHandler) ` 往管道添加的`handle`方法，在里面处理数据。同时 EventLoopGroup 会自行在充当消息发送方时执行encoder(继承自MessageToByteEncoder)，接收方时执行decoder(继承自ByteToMessageDecoder)

## 路由/负载均衡

实现了三种，随机、轮询、哈希一致性，都实现 org.zy.rpc.router 的 LoadBalancer 接口统一管理

为了确保实时性每次服务列表`List<ServiceMeta> services `都是重新获取，本地备份可能会过期，不然又设计个同步策略，同步复同步...

```java
// 获得注册中心
RegistryService registryService = SpiLoader.getInstance().get(RpcProperties.getInstance().getRegisterType());
// 从注册中心获取可用服务列表
List<ServiceMeta> services = registryService.getServices(serviceName);
```

还有个结果管理类 `RouterRes` ，为了容错机制，其实返回了所有查询到的服务，但按轮询规则选择了一个作为当前服务，当发生故障时，还可以选择其他的。

## 拦截

拦截链也就是有个List 存放多个Filter然后都触发

```java
public class FilterChain {

    private List<Filter> filters = new ArrayList<>();

    public void addFilter(Filter filter){ filters.add(filter);}

    public void addFilter(List<Object> filters){
        for (Object filter:filters){
            addFilter((Filter) filter);
        }
    }

    public void  doFilter(FilterData data){
        for (Filter filter : filters){
            filter.doFilter(data);
        }
    }
}
```

具体的Filter看各自需求设计了，可以过滤数据、作token验证，我就做了个打印，具体调用可以放在socketChannel的Handle中，对数据进行过滤

## SPI

使用SPI后对于整个程序的拓展性有了极大的提升。  
- 制定好接口规范协议后，其他服务按照你制定好的服务协议。随后使用SPI进行可插拔方式来加载不同的类。  
- 例如：jdbcl驱动，我先制定好一个协议，我不管你是什么sql厂商，你都遵循我的协议来，然后我使用spi机制来进行加载类就行。
- java中ServiceLoader的懒加载、Spring MVC、Spring Boot的装配等等都是该思想

通过SPI机制（配置文件中也是key，value的格式）去按需加载bean，类名加载到Map，调用的时候若没有再初始化(初始化方式也可以自选，像jdk)

它通过在指定的路径下的 `META-INF/services` 目录中的配置文件中列出服务接口的实现类，然后通过 `ServiceLoader` 类在运行时动态加载这些实现类。这样的机制使得服务接口的实现类可以由不同的类加载器加载，而不受双亲委派机制的限制，是在双亲委派机制之上提供了一种动态加载服务实现类的方式。

具体的在 org.zy.rpc.spi.SpiLoader 的 loadSpi方法中：

```java
public void loadSpi(Class clazz) throws IOException, ClassNotFoundException {
    if (clazz == null) {
        throw new IllegalArgumentException("class 不能指定为空");
    }
    ClassLoader classLoader = this.getClass().getClassLoader();
    Map<String, Class> classMap = new HashMap<>();
    // 从系统SPI以及用户SPI中找bean
    for (String prefix : prefixs) {	// 扫描路径
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
```

将配置文件中指定的类名加载进Map缓存中，文件中key自定义，跟指定的符合即可，但value就是类名要跟实现的类完全一致。

```properties
roundRobin=org.zy.rpc.router.RoundRobinLoadBalancer
consistentHash=org.zy.rpc.router.ConsistentHashLoadBalancer
random=org.zy.rpc.router.RandomLoadBalancer
```

获取实例时，则从map得到该指定（也就是和上面配置文件中的key匹配）的类名，`newInstance()` 获取实例

```java
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
```

也可以是SPI经典的 `ServiceLoader`

1. 创建一个接口文件
2. 在resources资源目录下创建META-INF/services文件夹
3. 在services文件夹中创建文件，以接口全名命名
4. 创建接口实现类

```java
ServiceLoader<IMyServiceLoader> serviceLoader = ServiceLoader.load(接口.class);
for (IMyServiceLoader myServiceLoader : serviceLoader){
    System.out.println(myServiceLoader.getName() + myServiceLoader.sayHello());
}
```

但看到的都是全遍历的例子，遂用newInstance，从静态名创建（又不是那么SPI），但静态名也是从篇日志文件动态加载到Map中的，应该还是算SPI的吧。

后补充，发现用迭代器就行了...应该可以把上面的合起来，不没差嘛！还是用newInstance解决的，还是加载到本地Map方便

```java
public <V> V get(String name) {
    if (!singletonsObject.containsKey(name)) {
        try {
            // 以加载 A 接口的实现类为例，这里还需要再优化下，不能指定类加载
            ServiceLoader<A> loader = ServiceLoader.load(A.class);
            // 获取迭代器
            var iterator = loader.iterator();
            // 找到指定的实现类
            Class<?> clazz = name; // 这里替换成你要加载的实现类的类对象
            while (iterator.hasNext()) {
                Calculator calculator = iterator.next();
                if (calculator.getClass() == name) {  
                    // 找到指定的实现类，进行实例
                    singletonsObject.put(name, clazz.newInstance());
                    break;
                }
            }
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }
    return (V) singletonsObject.get(name);
}


```

## 容灾



```java
while (count <= retryCount){
    // Consumer 处理请求        
    // xxx
}catch (Throwable e){	// 容错机制
    String errorMsg = e.toString();
    switch (faultTolerantType){
            // 快速失败
        case FaultTolerantRules.FailFast:
            log.warn("rpc 调用失败, 触发 FailFast 策略,异常信息: {}",errorMsg);
            return rpcResponse.getException();
            // 故障转移：尝试别的节点
        case FaultTolerantRules.Failover:
            log.warn("rpc 调用失败,第{}次重试,异常信息:{}",count,errorMsg);
            count++;
            if (!ObjectUtils.isEmpty(otherServiceMata)){
                final ServiceMeta next = otherServiceMata.iterator().next();
                curServiceMeta = next;
                otherServiceMata.remove(next);
            }else {
                final String msg = String.format("rpc 调用失败, 当前已无服务可用serviceName: {%s}, 异常信息: {%s}", serviceName, errorMsg);
                log.warn(msg);
                throw new RuntimeException(msg);
            }
            break;
            // 忽视错误
        case FaultTolerantRules.Failsafe:
            return null;
    }
}
```

- FailFast：思想是在发生错误时立即停止程序的执行，直接抛出异常哈哈哈，但更完善的话应该是有日志记录，错误检测等
- Failover：更多时候使用的故障转移，访问别的服务
- Failsafe：一般日志记录等无所谓的时候，直接放过去

## 并发

测试就用的请求，因此调用方是要等待结果才执行后续的，实际按照微服务的场景来应该也是要做并发的。

调用方就用了，在 `org.zy.rpc.ThreadPool.ThreadPoolFactory` 中 有线程池`ThreadPoolExecutor` 变量 `poll` ，执行调用时就提交到线程池中：

```java
public static void submitRequest(ChannelHandlerContext ctx, RpcProtocol<RpcRequest> protocol){

        final RpcRequest request = protocol.getBody();
        String key = request.getClassName() + request.getMethodName() + request.getServiceVersion();
        ThreadPoolExecutor poll = fastPoll;
        if (slowTaskMap.containsKey(key) && slowTaskMap.get(key).intValue() >= 10){
            poll = slowPoll;
        }
        poll.submit(()->{
            // Todo xxx
            ctx.fireChannelRead(resProtocol);
        });
    }
```

因为是在 `channel` 的 `Handle` 中调用，因此参数来自` SimpleChannelInboundHandler`的 `channelRead0` 方法（见 `RpcRequestHandler`）

对请求体一系列解析后，通过反射执行对应的服务

```java
// 获取服务提供方信息并且创建
Class<?> serviceClass = serviceBean.getClass();
String methodName = request.getMethodName();
Class<?>[] parameterTypes = request.getParameterTypes();
Object[] parameters = {request.getData()};

FastClass fastClass = FastClass.create(serviceClass);
int methodIndex = fastClass.getIndex(methodName, parameterTypes);

// 也可以
//FastMethod fastMethod = fastClass.getMethod(methodName, parameterTypes);
//return fastMethod.invoke(serviceBean, parameters);

// 调用方法并返回结果
return fastClass.invoke(methodIndex, serviceBean, parameters);
```

## 中间接口层

见 `MiddleMethod`模块，就是抽离出公共接口。免得双方有依赖，调用方调用接口，提供方实现接口

# 具体使用

## 调用方

参考`RpcConsumer`模块

在`Application`注解上`@EnableConsumerRpc`

在具体方法注解`@RpcReference`，可以通过参数指定负载均衡，容错机制等

执行是通过代理，在 `ConsumerPostProcessor` 中的 `postProcessAfterInitialization`（加载完Bean后执行）

扫描有RpcReference注解的，生成代理对象。当调用代理对象的方法时，实际上会调用代理类中的方法，即 `InvocationHandler` 对象（`org.xhystudy.rpc.consume.RpcInvokerProxy`）的 `invoke` 方法来处理方法调用，即执行代理逻辑。

## 提供方

参考 RpcProvider1/2 模块

在`Application`注解上`@EnableProviderRpc`

中间接口层实现的接口上注解`@RpcService`

## tips

用的Spring，上述模块的 `application.properties` 别忘了改成自己的 zookeeper服务器ip，虽然你用来测试下也行（假如我的服务开着的话）

但假如两边注册的服务名一样不就乱套了嘛，实际应用还是得加验证才行



# Acknowledgment

[yu-rpc](https://github.com/liyupi/yu-rpc) 虽然后续要知识星球付费，但其中得简单案例很通俗易懂，入门看下理解RPC是则么回事

[Xhy-rpc](https://gitee.com/XhyQAQ/xhy-rpc) 主要是参考~~手敲cv~~这个，配套视频对各层得讲解很清晰，跟着做也感觉学到很多，Netty这些之前都没用过，包括从接口到实现类设计得很清晰。太卷了哎，也不知道对实习能不能起到帮助。个人感觉还是充实的、收获满满，很多技术细节还要深耕就是了

![](tucao.jpg)

（能不能给个实习再让我深耕啊，怎么现在都要带资进组的，太离谱咯）