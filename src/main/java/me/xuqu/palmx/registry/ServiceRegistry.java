package me.xuqu.palmx.registry;

import com.google.common.collect.Lists;
import me.xuqu.palmx.loadbalance.PalmxSocketAddress;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 服务注册中心
 */
public interface ServiceRegistry {

    Set<String> services = new LinkedHashSet<>();

    void register(String serviceName, PalmxSocketAddress inetSocketAddress);

    void unregister(String serviceName, PalmxSocketAddress inetSocketAddress);

    List<PalmxSocketAddress> lookup(String serviceName);

    default List<PalmxSocketAddress> mock(String serviceName) {
        return Lists.newArrayList(new PalmxSocketAddress("127.0.0.1", 8081));
    }

}
