package me.xuqu.palmx.loadbalance.impl;

import me.xuqu.palmx.loadbalance.AbstractLoadBalance;
import me.xuqu.palmx.loadbalance.PalmxSocketAddress;

import java.util.List;

/**
 * 自适应负载均衡算法
 */
public class AdaptiveLoadBalance extends AbstractLoadBalance {
    @Override
    protected PalmxSocketAddress doChoose(List<PalmxSocketAddress> socketAddressList, String serviceName) {
        return null;
    }
}
