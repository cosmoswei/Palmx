package me.xuqu.palmx.registry;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import me.xuqu.palmx.exception.ServiceNotFoundException;
import org.springframework.util.CollectionUtils;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public abstract class AbstractServiceRegistry implements ServiceRegistry {

    Cache<String, List<InetSocketAddress>> registryCache = Caffeine.newBuilder()
            //过期时间
            .expireAfterWrite(1, TimeUnit.MINUTES)
            //最大容量
            .maximumSize(20)
            .build();

    @Override
    public void register(String serviceName, InetSocketAddress inetSocketAddress) {
        String serviceAddress = String.format("%s:%d", inetSocketAddress.getHostString(), inetSocketAddress.getPort());
        doRegister(serviceName, serviceAddress);
    }

    @Override
    public List<InetSocketAddress> lookup(String serviceName) {
        List<InetSocketAddress> ifPresent = registryCache.getIfPresent(serviceName);
        if (!CollectionUtils.isEmpty(ifPresent)) {
            return ifPresent;
        } else {
            List<String> serviceAddresses = doLookup(serviceName);
            List<InetSocketAddress> inetSocketAddresses = serviceAddresses.stream().map(s -> {
                String[] strings = s.split(":");
                return new InetSocketAddress(strings[0], Integer.parseInt(strings[1]));
            }).collect(Collectors.toList());
            if (inetSocketAddresses.isEmpty()) {
                throw new ServiceNotFoundException(serviceName);
            }
            registryCache.put(serviceName, inetSocketAddresses);
            return inetSocketAddresses;
        }
    }

    protected abstract void doRegister(String serviceName, String serviceAddress);

    protected abstract List<String> doLookup(String serviceName);
}
