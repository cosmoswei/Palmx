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
        // 先去获取服务列表

        // 做一次P2C？

        // 获取列表的机器配置、性能指标，没有的话去远程获取

        // 根据规则获取自适应配置（数据指标、聚合算法）

        // 根据自适应配置做负载均衡
        return null;
    }
}
