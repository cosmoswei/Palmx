package me.xuqu.palmx.registry;

import me.xuqu.palmx.loadbalance.PalmxSocketAddress;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * 服务注册中心
 */
public interface ServiceRegistry {

    void register(String serviceName, PalmxSocketAddress inetSocketAddress);

    void unregister(String serviceName, PalmxSocketAddress inetSocketAddress);

    List<PalmxSocketAddress> lookup(String serviceName);

}
