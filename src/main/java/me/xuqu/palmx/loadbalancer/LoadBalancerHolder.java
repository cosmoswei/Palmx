package me.xuqu.palmx.loadbalancer;

import me.xuqu.palmx.common.LoadBalancerType;
import me.xuqu.palmx.common.PalmxConfig;
import me.xuqu.palmx.loadbalancer.impl.ConsistentHashLoadBalancer;
import me.xuqu.palmx.loadbalancer.impl.RandomLoadBalancer;
import me.xuqu.palmx.loadbalancer.impl.RoundRobinLoadBalancer;

public class LoadBalancerHolder {

    private static LoadBalancer loadBalancer;

    public static synchronized LoadBalancer get() {
        if (loadBalancer == null) {
            LoadBalancerType loadBalanceType = PalmxConfig.getLoadBalanceType();
            switch (loadBalanceType) {
                case RANDOM:
                    loadBalancer = new RandomLoadBalancer();
                    break;
                case ROUND_ROBIN:
                    loadBalancer = new RoundRobinLoadBalancer();
                    break;
                case CONSISTENT_HASH:
                    loadBalancer = new ConsistentHashLoadBalancer();
                    break;
                default:
                    throw new IllegalArgumentException("Unknown load balance type: " + loadBalanceType);
            }
        }
        return loadBalancer;
    }
}
