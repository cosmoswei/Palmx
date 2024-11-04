# Palmx：一个次世代的 RPC 框架

基于 Netty 实现的一个的 RPC 框架，使用 Zookeeper 作为服务注册中心，手动实现了几个简单的负载均衡算，然后封装了多种序列算法，与 Spring Framework 集成，实现了基于注解驱动的服务自动注册和一键式启动服务器的功能。

## 基本功能

### 配置文件

在类路径下添加 `palmx.properties`，可以配置 Zookeeper 相关的连接信息、序列化方式、负载均衡算法、服务器端口号，具体如下所示：

```Properties
palmx.zookeeper.host=ubuntu.qaab8h9.wsl
palmx.zookeeper.port=2181
palmx.zookeeper.root-node=palmx
palmx.serialization.type=kryo
palmx.load-balancer=round-robin
palmx.server.port=8081
```



### 服务注册与发现

服务注册与发现是一个RPC框架的最基本能力，本项目使用 Zookeeper 作为服务注册中心，当使用 @EnablePalmx 注解启动服务时，使用 @PalmxService 注解类的接口，会被注册到 Zookeeper 注册中心，例如：

```Java
@PalmxService
public class DemoServiceImpl implements DemoService {

    @Override
    public String demoSleepSecond(long l) {
        try {
            TimeUnit.MILLISECONDS.sleep(l);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return "DemoServiceImpl success" + System.currentTimeMillis() % 10000;
    }
}
```

使用 @PalmxClient 注解会被 Palmx 代理，去调用远端的服务，例如：

```Java
@PalmxClient
private DemoService demoService;
```



### 序列化

序列化是在网络通信中的重要技术，程序中的类实例无法在网络中传输，所以需要将它序列化成二进制数据，才能在网络中传输。本项目封装了多种序列算法（Object Stream、Jackson、Kryo、protostuff）

供选择，在 `palmx.properties` 配置文件中指定 palmx.serialization.type 属性可以指定程序的序列化算法，比如

```Properties
palmx.serialization.type=kryo
```



### 负载均衡

负载均衡是 RPC 框架中一个常见且重要的技术，当服务的生产者有多个时，需要从这些服务提供者中，通过特定的算法，选择一个生产者进行消费，以达到实现更高的吞吐以及资源利用率的目的。其中，主要的技术细节在特定的算法中，常见的算法有随机算法，轮询算法，在这两个算法的基础上加上权重，就有了加权随机与加权轮询算法，此外，一致性Hash算法在一些特定的场景也发挥了巨大作用。本项目也支持了上述负载均衡算法，通过在 `palmx.properties` 配置文件中指定 palmx.load-balancer 属性可以指定程序的负载算法，比如：

```properties
palmx.load-balancer=round-robin
```

除了上面的经典算法之外，本项目还支持了一种新的负载均衡算法---自适应负载均衡，会在接下来的高级特征篇中介绍。



### 流量控制

流控是通过特定算法对服务端输入流量进行限制的一种手段，当入口流量超过系统的负载时，会对程序造成大的影响，所以需要对输入流量进行限制。流控的主要技术细节在流量控制算法中，经典的流量控制算法有计数器、滑动窗口、令牌桶、漏桶，本项目也支持了上述负载算法，通过在服务接口中指定服务算法与元数据可以对接口进行流量控制，比如：

```java
@PalmxService(flowControlLimitCount = 200000, flowControlLimitType = FlowControlType.SLIDING_WINDOW)
public class DemoServiceImpl implements DemoService {

    @Override
    public String hello(String str) {
        return "helll" + str;
    }
}
```

上面的配置会对对 DemoServiceImpl 接口进行流控，流控算法为滑动窗口，限流数值为每秒最多请求 200000 次。在此之上，本项目还支持了一种新的流控算法---自适应流量控制，会在接下来的高级特征篇中介绍。



## 快速开始

> 本项目并没有发布到maven 中央仓库，所以需要先在本地打包，之后再引入依赖。



代码拉取：

```shell
git clone https://github.com/cosmoswei/palmx
```

本地打包：

```Shell
 mvn clean install 
```

引入依赖：

```XML
<dependency>
    <groupId>me.xuqu</groupId>
    <artifactId>palmx</artifactId>
    <version>2.3-SNAPSHOT</version>
</dependency>
```



### 使用

在服务端启动类上加上 @EnablePalmx 注解

```Java
@SpringBootApplication
@EnablePalmx
public class ProviderRun {
    public static void main(String[] args) {
        SpringApplication.run(ProviderRun.class, args);
    }
}
```

服务提供者

```Java
@PalmxService
public class DemoServiceImpl implements DemoService {

    @Override
    public String demoSleepSecond(long l) {
        try {
            TimeUnit.MILLISECONDS.sleep(l);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return "success, currentTimeMillis IS " + System.currentTimeMillis() % 1000;
    }
}
```

服务消费者

```Java
@Component
public class PalmxService {

    @PalmxClient
    private DemoService demoService;

    public String invokeTest() {
        return "invokeTest = " + demoService.demoSleepSecond(1L);
    }
}
```



### 样例

本项目提供了参考案例可供使用：https://github.com/cosmoswei/palmx-samples，可以帮助你里快速启动本项目。



## 高级特性

除了 RPC 基础的能力之外，本项目的主要亮点是一些高级特性，这些高级特性在其它 RPC 框架稳定版本中是没有的，本项目对其进行的支持。



### 传输协议：quic

自从 2012 年 Google 前瞻性地提出 QUIC 传输协议，历经 10 年迭代，HTTP/3 凭借其对 QUIC 协议的深度整合以及从 TCP 向 UDP 的底层连接转型。截止到2022年11⽉，全球已有超过四分之⼀的网站部署了 HTTP/3 技术，如下图。⼀个崭新的⾼速、⾼效⽹络时代正逐步拉开帷幕。

![](https://raw.githubusercontent.com/cosmoswei/images/main/202411041618604.png)


在当前的开源网络通信领域中，诸如 Netty、NGINX 以及 gRPC 等主流框架正竞相投入资源研发各的 HTTP/3 实现方案，HTTP/3 协议本⾝所蕴含的诸多优势不容忽视，例如摒弃队头阻塞问题、实现 0RTT 握手延迟、连接迁移等特性，这些都极大地提升了通信效率，将 HTTP/3 入 RPC 有助于构建出性能更卓越的应用系统。通过在 `palmx.properties` 配置文件中指定 palmx.load-balancer 属性可以指定程序的负载算法，比如：

```properties
palmx.quic-enable=ture
```

关于这项技术的详细介绍，你可以参考这篇文章：[高效传输：以 quic 构建高效传输通道](https://cosmoswei.github.io/w442024-gao-xiao-chuan-shu-yi-quic-gou-jian-gao-xiao-chuan-shu-tong-dao/)，里面解释为什么要使用 quic 以及 quic  的最佳实践。



### 异步读写：io_uring

从 2019 年 Linux 在 5.1 版本发布 io_uring 开始，许多框架都使用了这一项技术，比如A、B、C，但是这项技术并没有在 Java 生态中大规模使用，幸运的是 Netty 社区，目前正在孵化使用 io_uring 的项目，详情访问： [netty/netty-incubator-transport-io_uring](https://github.com/netty/netty-incubator-transport-io_uring)，里面提供了新的读写模型 IOUringEventLoop，本项目也基于这项技术实现了 io_uring，通过在 `palmx.properties` 配置文件中指定 palmx.io_uring-enable 属性可以开启 io_uring 作为读写事件模型，比如：

```properties
palmx.io_uring-enable=ture
```

关于这项技术的详细介绍，你可以参考这篇文章：[异步读写：窥见未来的系统调用---iouring](https://cosmoswei.github.io/w442024-yi-bu-du-xie-kui-jian-wei-lai-de-xi-tong-diao-yong-iouring/)，里面解释为什么要使用 io_uring 以及 io_uring 的最佳实践。



### 自适应负载均衡

自适应负载均衡试图自适应的去衡量服务提供者机器的吞吐能力，然后将流量尽可能分配到吞吐能力高的机器上，以提高系统整体的性能。通过在 `palmx.properties` 配置文件中指定 palmx.load-balancer 属性可以指定程序使用自适应负载均衡算法，例如：

```properties
palmx.load-balancer=adaptive
```

关于这项技术的详细介绍，你可以参考这篇文章：[双端流控：自适应的负载均衡与流量控制](https://cosmoswei.github.io/w442024-shuang-duan-liu-kong-zi-gua-ying-de-fu-zai-jun-heng-yu-liu-liang-kong-zhi/)，里面解释为什么要使用自适应负载均衡。



### 自适应流量控制

从理论上讲，服务端机器的处理能力是存在上限的，对于一台服务端机器，当短时间内出现大量的请求调用时，会导致处理不及时的请求积压，使机器过载，在可能存在过载风险时，拒绝掉一部分请求反而是更好的选择。通常情况下，是通过在服务端设置静态的最大并发值实现的，但是在处理能力会动态变化的局面下，这个值难以计算。基于以上原因，需要一种自适应的算法，其可以动态调整服务端机器的最大并发值，使其可以在保证机器不过载的前提下，尽可能多的处理接收到的请求。在本项目中，通过在 `palmx.properties` 配置文件中指定 palmx.load-balancer 属性可以指定程序的负载算法，比如：

```properties
palmx.adaptive-flow-control-enable=true
```

关于这项技术的详细介绍，你可以参考这篇文章：[双端流控：自适应的负载均衡与流量控制](https://cosmoswei.github.io/w442024-shuang-duan-liu-kong-zi-gua-ying-de-fu-zai-jun-heng-yu-liu-liang-kong-zhi/)，里面解释为什么要使用自适应流量控制及自适应流量控制的使用。



### 配置化评分体系

上述的自适应负载均衡与流量控制，是通过服务服务端的实际负载，评估出一个值来控制机器的最大并发量，通常情况下是根据一个算法来确定这个并发量，但是在实际的应用中，这个算法的通用性不强，本项目的方案是通过规则引擎配置具体的机器最大并发量计算算法，得到最能体现当前机器最大并发量的分数，通过这个分数可以更好的实现自适应适应负债均衡与流量控制，在 `palmx.properties` 配置文件中指定 metric-qos-rule-path 属性可以指定规则规则路径，比如：

```properties
palmx.metric-qos-rule-path=drl/customMetric.drl
```

关于这项技术的详细介绍，你可以参考这篇文章：[配置驱动：灵活的配置指标与评分体系](https://cosmoswei.github.io/w442024-pei-zhi-qu-dong-ling-huo-de-pei-zhi-zhi-biao-yu-ping-fen-ti-xi/)，里面解释为什么要使用 配置化评分体系以及配置化评分体系的最佳实践。



### 虚拟线程*

Java 在 JDK21 中正式推出了虚拟线程，它是一种轻量级线程，开发者可以不使用内核线程的情况下，实现用户态线程。大大提高了 Java项目的并发能力。所以本项目也支持使用虚拟线程的方式提高系统性能。通过在 `palmx.properties` 配置文件中指定 palmx.load-balancer 属性可以指定程序的负载算法，比如：

```properties
palmx.virtual-thread-enable=true
```

关于这项技术的详细介绍，你可以参考这篇文章：[超高并发：虚拟线程在 IO 中的机遇](https://cosmoswei.github.io/w442024-chao-gao-bing-fa-xu-ni-xian-cheng-zai-io-zhong-de-ji-yu/)，里面解释为什么要使用虚拟线程以及虚拟线程的最佳实践。

