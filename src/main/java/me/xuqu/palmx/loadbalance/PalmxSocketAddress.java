package me.xuqu.palmx.loadbalance;

import java.net.InetAddress;
import java.net.InetSocketAddress;

public class PalmxSocketAddress extends InetSocketAddress implements Comparable<PalmxSocketAddress> {
    private final Integer weight;
    private int effectiveWeight;
    private int currentWeight;
    private boolean isAvailable = true;

    public PalmxSocketAddress(String host, int port) {
        super(host, port);
        this.weight = 1;
    }

    public PalmxSocketAddress(String host, int port, int weight) {
        super(host, port);
        this.weight = weight;
        this.currentWeight = weight;
        this.effectiveWeight = weight;
    }

    public PalmxSocketAddress(InetAddress address,
                              int port,
                              int weight,
                              Integer effectiveWeight,
                              Integer currentWeight) {
        super(address.getHostAddress(), port);
        this.weight = weight;
        this.currentWeight = currentWeight;
        this.effectiveWeight = effectiveWeight;
    }

    public PalmxSocketAddress(InetAddress address, int port, int weight, Boolean available) {
        super(address.getHostAddress(), port);
        this.weight = weight;
        this.currentWeight = weight;
        this.effectiveWeight = weight;
        this.isAvailable = available;
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

    public Boolean isAvailable() {
        return isAvailable;
    }

    public void setIsAvailable(Boolean isAvailable) {
        this.isAvailable = isAvailable;
    }

    // 每成功一次，恢复有效权重1，不超过配置的起始权重
    public void onInvokeSuccess() {
        if (effectiveWeight < weight) effectiveWeight++;
    }

    // 每失败一次，有效权重减少1，无底线的减少
    public void onInvokeFault() {
        effectiveWeight--;
    }

    @Override
    public int compareTo(PalmxSocketAddress node) {
        return Integer.compare(currentWeight, node.currentWeight);
    }

    @Override
    public String toString() {
        return "{ip='" + getAddress() + "', weight=" + weight + ", effectiveWeight=" + effectiveWeight + ", currentWeight=" + currentWeight + "}";
    }
}
