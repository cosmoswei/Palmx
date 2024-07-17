package me.xuqu.palmx.loadbalance;

import me.xuqu.palmx.common.LoadBalancerType;
import me.xuqu.palmx.common.PalmxConfig;
import me.xuqu.palmx.loadbalance.impl.*;

public class LoadBalanceHolder {

    private static LoadBalance loadBalance;

    public static synchronized LoadBalance get() {
        if (loadBalance == null) {
            LoadBalancerType loadBalanceType = PalmxConfig.getLoadBalanceType();
            switch (loadBalanceType) {
                case RANDOM:
                    loadBalance = new RandomLoadBalance();
                    break;
                case ROUND_ROBIN:
                    loadBalance = new RoundRobinLoadBalance();
                    break;
                case CONSISTENT_HASH:
                    loadBalance = new ConsistentHashLoadBalance();
                    break;
                case WEIGHT_RANDOM:
                    loadBalance = new WeightRandomLoadBalance();
                    break;
                case ADAPTIVE:
                    loadBalance = new AdaptiveLoadBalance();
                    break;
                case WEIGHT_ROUND_ROBIN:
                default:
                    loadBalance = new WeightRoundRobinLoadBalance();
            }
        }
        return loadBalance;
    }
}
