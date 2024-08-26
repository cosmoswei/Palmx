package me.xuqu.palmx.registry;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import me.xuqu.palmx.exception.ServiceNotFoundException;
import me.xuqu.palmx.loadbalance.PalmxSocketAddress;
import org.springframework.context.ApplicationContext;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public abstract class AbstractServiceRegistry implements ServiceRegistry {

    @Resource
    private ApplicationContext applicationContext;

    Cache<String, List<PalmxSocketAddress>> registryCache = Caffeine.newBuilder()
            //过期时间
            .expireAfterWrite(1, TimeUnit.MINUTES)
            //最大容量
            .maximumSize(20)
            .build();

    @Override
    public void register(String serviceName, PalmxSocketAddress inetSocketAddress) {
        String serviceAddress = String.format("%s:%d", inetSocketAddress.getHostString(), inetSocketAddress.getPort());
        doRegister(serviceName, serviceAddress);
        //todo
//        applicationContext.publishEvent(new RegisterSuccessEvent(serviceName));
        services.add(serviceName);
    }

    @Override
    public List<PalmxSocketAddress> lookup(String serviceName) {
        List<PalmxSocketAddress> ifPresent = registryCache.getIfPresent(serviceName);
        if (!CollectionUtils.isEmpty(ifPresent)) {
            return ifPresent;
        } else {
            List<RegistryDTO> serviceAddresses = doLookup(serviceName);
            List<PalmxSocketAddress> inetSocketAddresses = serviceAddresses.stream().map(s -> {
                String host = s.getHost();
                int port = s.getPort();
                PalmxSocketAddress palmxSocketAddress = new PalmxSocketAddress(host, port);
                palmxSocketAddress.setQoSLevel(s.getQoSLevel());
                return palmxSocketAddress;
            }).collect(Collectors.toList());
            if (inetSocketAddresses.isEmpty()) {
                throw new ServiceNotFoundException(serviceName);
            }
            registryCache.put(serviceName, inetSocketAddresses);
            return inetSocketAddresses;
        }
    }

    protected abstract void doRegister(String serviceName, String serviceAddress);

    protected abstract List<RegistryDTO> doLookup(String serviceName);
}
