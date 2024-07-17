package me.xuqu.palmx.loadbalance;

import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public abstract class AbstractLoadBalance implements LoadBalance {


    protected static final Map<String, List<LBSocketAddress>> serviceNodes = new ConcurrentHashMap<>();

    private boolean needRefresh;

    public synchronized void refreshServiceNodes(List<LBSocketAddress> lbSocketAddresses,
                                                 String service) {
        if (!needRefresh) {
            return;
        }

        List<LBSocketAddress> fleshSocketAddress = serviceNodes.get(service);
        if (fleshSocketAddress == null) {
            serviceNodes.put(service, lbSocketAddresses);
            return;
        }
        // 删除过期的节点
        fleshSocketAddress.removeIf(e -> !lbSocketAddresses.contains(e));
        // 新增新节点
        lbSocketAddresses.removeIf(fleshSocketAddress::contains);
        fleshSocketAddress.addAll(lbSocketAddresses);
        serviceNodes.put(service, fleshSocketAddress);
    }

    public void notifyRefresh() {
        needRefresh = true;
    }

    @Override
    public InetSocketAddress choose(List<InetSocketAddress> socketAddressList, String serviceName) {
        if (socketAddressList == null || socketAddressList.isEmpty()) {
            log.warn("No servers available for service: {}", serviceName);
            return null;
        }

        if (socketAddressList.size() == 1) {
            return socketAddressList.get(0);
        }

        InetSocketAddress inetSocketAddress = doChoose(socketAddressList, serviceName);
        log.debug("Choose a server[{}] for service[name = {}] with services = {}", inetSocketAddress, serviceName, socketAddressList);
        return inetSocketAddress;
    }

    protected abstract InetSocketAddress doChoose(List<InetSocketAddress> socketAddressList, String serviceName);
}
