package me.xuqu.palmx.common;

public enum LoadBalancerType {
    RANDOM,
    ROUND_ROBIN,
    CONSISTENT_HASH,
    WEIGHT_RANDOM,
    WEIGHT_ROUND_ROBIN,
    ADAPTIVE,
}
