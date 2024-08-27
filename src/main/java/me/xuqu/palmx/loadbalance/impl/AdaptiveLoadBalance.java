package me.xuqu.palmx.loadbalance.impl;

import com.google.common.collect.Lists;
import me.xuqu.palmx.loadbalance.AbstractLoadBalance;
import me.xuqu.palmx.loadbalance.PalmxSocketAddress;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 自适应负载均衡算法
 */
public class AdaptiveLoadBalance extends AbstractLoadBalance {

    @Override
    protected PalmxSocketAddress doChoose(List<PalmxSocketAddress> socketAddressList, String serviceName) {
        // 先去获取服务列表

        // 做一次P2C
        List<PalmxSocketAddress> palmxSocketAddresses = selectP2C(socketAddressList);

        // 获取列表的机器配置、性能指标，没有的话去远程获取

        // 根据规则获取自适应配置（数据指标、聚合算法）
        Optional<PalmxSocketAddress> palmxSocketAddress = palmxSocketAddresses.stream().max(Comparator.comparingInt(PalmxSocketAddress::getQoSLevel));

        // 根据自适应配置做负载均衡
        return palmxSocketAddress.orElse(socketAddressList.get(0));
    }

    private List<PalmxSocketAddress> selectP2C(List<PalmxSocketAddress> socketAddressList) {
        int length = socketAddressList.size();
        if (length == 2) {
            return socketAddressList;
        }
        int pos1 = ThreadLocalRandom.current().nextInt(length);
        int pos2 = ThreadLocalRandom.current().nextInt(length - 1);
        if (pos2 >= pos1) {
            pos2 = pos2 + 1;
        }
        PalmxSocketAddress palmxSocketAddress1 = socketAddressList.get(pos1);
        PalmxSocketAddress palmxSocketAddress2 = socketAddressList.get(pos2);
        return Lists.newArrayList(palmxSocketAddress1, palmxSocketAddress2);
    }
}
