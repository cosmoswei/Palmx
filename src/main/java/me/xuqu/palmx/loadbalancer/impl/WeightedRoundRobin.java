package me.xuqu.palmx.loadbalancer.impl;


import me.xuqu.palmx.loadbalancer.AbstractLoadBalancer;
import me.xuqu.palmx.loadbalancer.LBSocketAddress;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * 加权轮询算法
 */
public class WeightedRoundRobin  extends AbstractLoadBalancer {

    private static List<LBSocketAddress> nodes = new ArrayList<>();
    // 权重之和
    private static Integer totalWeight = 0;

    // 准备模拟数据
    static {
        nodes.add(new LBSocketAddress("192.168.1.101", 8080, 1));
        nodes.add(new LBSocketAddress("192.168.1.102", 8080, 3));
        nodes.add(new LBSocketAddress("192.168.1.103", 8080, 2));
        nodes.forEach(node -> totalWeight += node.getWeight());
    }

    /**
     * 按照当前权重（currentWeight）最大值获取IP
     *
     * @return Node
     */
    public LBSocketAddress selectNode() {
        if (nodes == null || nodes.size() <= 0) return null;
        if (nodes.size() == 1) return nodes.get(0);

        LBSocketAddress nodeOfMaxWeight; // 保存轮询选中的节点信息
        synchronized (WeightedRoundRobin.class) {
            // 打印信息对象：避免并发时打印出来的信息太乱，不利于观看结果
            StringBuffer sb = new StringBuffer();
            sb.append(Thread.currentThread().getName() + "==加权轮询--[当前权重]值的变化：" + printCurrentWeight(nodes));
            // 选出当前权重最大的节点
            LBSocketAddress tempNodeOfMaxWeight = null;
            for (LBSocketAddress node : nodes) {
                if (tempNodeOfMaxWeight == null)
                    tempNodeOfMaxWeight = node;
                else
                    tempNodeOfMaxWeight = tempNodeOfMaxWeight.compareTo(node) > 0 ? tempNodeOfMaxWeight : node;
            }
            // 必须new个新的节点实例来保存信息，否则引用指向同一个堆实例，后面的set操作将会修改节点信息
            nodeOfMaxWeight = new LBSocketAddress(tempNodeOfMaxWeight.getAddress(),
                    tempNodeOfMaxWeight.getPort(),
                    tempNodeOfMaxWeight.getWeight(),
                    tempNodeOfMaxWeight.getEffectiveWeight(),
                    tempNodeOfMaxWeight.getCurrentWeight());

            // 调整当前权重比：按权重（effectiveWeight）的比例进行调整，确保请求分发合理。
            tempNodeOfMaxWeight.setCurrentWeight(tempNodeOfMaxWeight.getCurrentWeight() - totalWeight);
            sb.append(" -> " + printCurrentWeight(nodes));
            nodes.forEach(node -> node.setCurrentWeight(node.getCurrentWeight() + node.getEffectiveWeight()));
            sb.append(" -> " + printCurrentWeight(nodes));
            System.out.println(sb); //打印权重变化过程
        }
        return nodeOfMaxWeight;
    }

    // 格式化打印信息
    private String printCurrentWeight(List<LBSocketAddress> nodes) {
        StringBuffer stringBuffer = new StringBuffer("[");
        nodes.forEach(node -> stringBuffer.append(node.getCurrentWeight() + ","));
        return stringBuffer.substring(0, stringBuffer.length() - 1) + "]";
    }

    // 并发测试：两个线程循环获取节点
    public static void main(String[] args) {
        Thread thread = new Thread(() -> {
            WeightedRoundRobin weightedRoundRobin1 = new WeightedRoundRobin();
            for (int i = 1; i <= totalWeight; i++) {
                LBSocketAddress node = weightedRoundRobin1.selectNode();
                System.out.println(Thread.currentThread().getName() + "==第" + i + "次轮询选中[当前权重最大]的节点：" + node + "\n");
            }
        });
        thread.start();
        //
        WeightedRoundRobin weightedRoundRobin2 = new WeightedRoundRobin();
        for (int i = 1; i <= totalWeight; i++) {
            LBSocketAddress node = weightedRoundRobin2.selectNode();
            System.out.println(Thread.currentThread().getName() + "==第" + i + "次轮询选中[当前权重最大]的节点：" + node + "\n");
        }
    }

    @Override
    protected InetSocketAddress doChoose(List<InetSocketAddress> socketAddressList, String serviceName) {
        return null;
    }
}
