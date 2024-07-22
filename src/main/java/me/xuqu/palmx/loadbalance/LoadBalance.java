package me.xuqu.palmx.loadbalance;

import java.util.List;

public interface LoadBalance {

    PalmxSocketAddress choose(List<PalmxSocketAddress> socketAddressList, String serviceName);

    void notifyRefresh(String serviceName);
}