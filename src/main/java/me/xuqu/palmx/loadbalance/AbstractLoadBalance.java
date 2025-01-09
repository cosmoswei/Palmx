package me.xuqu.palmx.loadbalance;

import lombok.extern.slf4j.Slf4j;
import me.xuqu.palmx.util.JsonUtils;

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
        // 更新节点
        serviceNodes.put(service, palmxSocketAddresses);
    }

    public synchronized void notifyRefresh(String serviceName) {
        needRefresh.put(serviceName, true);
    }

    @Override
    public PalmxSocketAddress choose(List<PalmxSocketAddress> socketAddressList, String serviceName) {
        long start = System.currentTimeMillis();
        if (socketAddressList == null || socketAddressList.isEmpty()) {
            log.warn("No servers available for service: {}", serviceName);
            return null;
        }
        if (socketAddressList.size() == 1)
            return (socketAddressList.get(0).isAvailable()) ? socketAddressList.get(0) : null;

        PalmxSocketAddress socketAddress = doChoose(socketAddressList, serviceName);
        long end = System.currentTimeMillis();
        log.debug("Choose a server[{}] for service[name = {}] with services = {} ,time = {}", JsonUtils.toJson(socketAddress), serviceName, socketAddressList, end - start);
        return socketAddress;
    }

    protected abstract PalmxSocketAddress doChoose(List<PalmxSocketAddress> socketAddressList, String serviceName);
}
