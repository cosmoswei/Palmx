package me.xuqu.palmx.loadbalance.impl;

import me.xuqu.palmx.loadbalance.AbstractLoadBalance;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class RandomLoadBalance extends AbstractLoadBalance {

    @Override
    protected InetSocketAddress doChoose(List<InetSocketAddress> socketAddressList, String serviceName) {
        int size = socketAddressList.size();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        return socketAddressList.get(random.nextInt(size));
    }

}
