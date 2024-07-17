package me.xuqu.palmx.loadbalance.impl;

import me.xuqu.palmx.loadbalance.AbstractLoadBalance;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * 自适应负载均衡算法
 */
public class AdaptiveLoadBalance extends AbstractLoadBalance {
    @Override
    protected InetSocketAddress doChoose(List<InetSocketAddress> socketAddressList, String serviceName) {
        return null;
    }
}
