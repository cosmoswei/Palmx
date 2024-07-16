package me.xuqu.palmx.loadbalancer;

import java.net.InetAddress;
import java.net.InetSocketAddress;

public class LBSocketAddress extends InetSocketAddress implements Comparable<LBSocketAddress> {
    private Integer weight = 1;
    private Integer effectiveWeight;
    private Integer currentWeight;

    public LBSocketAddress(String host, int port) {
        super(host, port);
    }

    public LBSocketAddress(String host, int port, int weight) {
        super(host, port);
        this.weight = weight;
        this.currentWeight = weight;
        this.effectiveWeight = weight;
    }

    public LBSocketAddress(InetAddress address,
                           int port,
                           int weight,
                           Integer effectiveWeight,
                           Integer currentWeight) {
        super(address.getHostAddress(), port);
        this.weight = weight;
        this.currentWeight = currentWeight;
        this.effectiveWeight = effectiveWeight;
    }

    public int getWeight() {
        return weight;
    }

    public Integer getEffectiveWeight() {
        return effectiveWeight;
    }

    public void setEffectiveWeight(Integer effectiveWeight) {
        this.effectiveWeight = effectiveWeight;
    }

    public Integer getCurrentWeight() {
        return currentWeight;
    }

    public void setCurrentWeight(Integer currentWeight) {
        this.currentWeight = currentWeight;
    }

    @Override
    public int compareTo(LBSocketAddress node) {
        return currentWeight > node.currentWeight ? 1 : (currentWeight.equals(node.currentWeight) ? 0 : -1);
    }

    @Override
    public String toString() {
        return "{ip='" + getAddress() + "', weight=" + weight + ", effectiveWeight=" + effectiveWeight + ", currentWeight=" + currentWeight + "}";
    }
}
