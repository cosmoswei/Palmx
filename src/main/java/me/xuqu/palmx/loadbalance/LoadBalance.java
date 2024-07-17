package me.xuqu.palmx.loadbalance;

import java.net.InetSocketAddress;
import java.util.List;

public interface LoadBalance {

    InetSocketAddress choose(List<InetSocketAddress> socketAddressList, String serviceName);

}