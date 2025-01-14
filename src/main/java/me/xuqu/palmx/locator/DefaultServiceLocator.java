package me.xuqu.palmx.locator;

import lombok.extern.slf4j.Slf4j;
import me.xuqu.palmx.common.PalmxConfig;
import me.xuqu.palmx.exception.RpcInvocationException;
import me.xuqu.palmx.net.PalmxClient;
import me.xuqu.palmx.net.RpcRequest;
import me.xuqu.palmx.net.http3.command.Http3NewClient;
import me.xuqu.palmx.net.netty.NettyClient;
import me.xuqu.palmx.net.http3.NettyHttp3Client;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.TimeUnit;

/**
 * 使用动态代理技术创建代理对象来远程调用相关的服务
 */
@Slf4j
public class DefaultServiceLocator implements ServiceLocator {

    private static PalmxClient CLIENT = null;

    // 重试退避策略数组
    static final int[] retryBackoffStrategy = {1, 2, 4, 8, 16, 32};

    @Override
    @SuppressWarnings("unchecked")
    public <T> T lookup(Class<T> clazz) {
        ClassLoader classLoader = clazz.getClassLoader();
        if (CLIENT == null) {
            synchronized (DefaultServiceLocator.class) {
                if (PalmxConfig.getQuicEnable()) {
                    CLIENT = new Http3NewClient();
                } else {
                    CLIENT = new NettyClient();
                }
            }
        }
        T proxyObject = (T) Proxy.newProxyInstance(classLoader, new Class[]{clazz}, (proxy, method, args) -> {
            RpcRequest rpcRequest = buildRpcInvocation(clazz, method, args);
            for (int i = 0; i < 3; i++) {
                try {
                    return CLIENT.sendAndExpect(rpcRequest);
                } catch (Exception e) {
                    log.info("Remote call exception, err msg = {}", e.getMessage());
                    TimeUnit.SECONDS.sleep(retryBackoffStrategy[i]);
                }
            }
            throw new RpcInvocationException("Remote call exception");
        });

        log.info("client create success, serializer is {}, load balancer is {}",
                PalmxConfig.getSerializationType(), PalmxConfig.getLoadBalanceType());

        return proxyObject;
    }

    private <T> RpcRequest buildRpcInvocation(Class<T> clazz, Method method, Object[] args) {
        RpcRequest rpcRequest = new RpcRequest();
        rpcRequest.setInterfaceName(clazz.getName());
        rpcRequest.setMethodName(method.getName());
        rpcRequest.setParameterTypes(method.getParameterTypes());
        rpcRequest.setArguments(args);
        return rpcRequest;
    }

    public void shutdown() {
        CLIENT.shutdown();
    }
}
