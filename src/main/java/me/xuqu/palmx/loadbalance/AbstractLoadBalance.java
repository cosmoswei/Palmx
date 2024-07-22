package me.xuqu.palmx.loadbalance;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public abstract class AbstractLoadBalance implements LoadBalance {

    protected static final Map<String, List<PalmxSocketAddress>> serviceNodes = new ConcurrentHashMap<>();

    private final Map<String, Boolean> needRefresh = new ConcurrentHashMap<>();

    /**
     * 负载均衡器维护节点的状态
     * 当节点下线时，从状态列表中移除
     * 当节点上线时，新增到状态列表
     */
    public synchronized void refreshServiceNodes(List<PalmxSocketAddress> palmxSocketAddresses,
                                                 String service) {
        Boolean absent = needRefresh.putIfAbsent(service, false);
        if (Boolean.TRUE.equals(absent)) {
            return;
        }
        List<PalmxSocketAddress> fleshSocketAddress = serviceNodes.get(service);
        if (fleshSocketAddress == null) {
            serviceNodes.put(service, palmxSocketAddresses);
            return;
        }
        // 删除过期的节点
        fleshSocketAddress.removeIf(e -> !palmxSocketAddresses.contains(e));
        // 新增新节点
        palmxSocketAddresses.removeIf(fleshSocketAddress::contains);
        fleshSocketAddress.addAll(palmxSocketAddresses);
        serviceNodes.put(service, fleshSocketAddress);
    }

    public synchronized void notifyRefresh(String serviceName) {
        needRefresh.put(serviceName, true);
    }

    @Override
    public PalmxSocketAddress choose(List<PalmxSocketAddress> socketAddressList, String serviceName) {
        if (socketAddressList == null || socketAddressList.isEmpty()) {
            log.warn("No servers available for service: {}", serviceName);
            return null;
        }
        if (socketAddressList.size() == 1)
            return (socketAddressList.get(0).isAvailable()) ? socketAddressList.get(0) : null;

        PalmxSocketAddress socketAddress = doChoose(socketAddressList, serviceName);
        log.debug("Choose a server[{}] for service[name = {}] with services = {}", socketAddress, serviceName, socketAddressList);
        return socketAddress;
    }

    protected abstract PalmxSocketAddress doChoose(List<PalmxSocketAddress> socketAddressList, String serviceName);
}
