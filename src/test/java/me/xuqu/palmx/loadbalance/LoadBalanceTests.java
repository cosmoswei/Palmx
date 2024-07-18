package me.xuqu.palmx.loadbalance;

import com.google.common.collect.Lists;
import me.xuqu.palmx.loadbalance.impl.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LoadBalanceTests {

    private static Map<String, List<PalmxSocketAddress>> serviceMap;
    private static LoadBalance randomLoadBalance;
    private static LoadBalance roundRobinLoadBalance;
    private static LoadBalance consistentHasLoadBalance;
    private static LoadBalance weightRandomLoadBalance;
    private static LoadBalance weightRoundRobinLoadBalance;


    @BeforeAll
    static void init() {
        serviceMap = new HashMap<>();

        serviceMap.put("service1", Lists.newArrayList(
                new PalmxSocketAddress("10.10.10.10", 8080),
                new PalmxSocketAddress("10.10.10.11", 8080),
                new PalmxSocketAddress("10.10.10.12", 8080),
                new PalmxSocketAddress("10.10.10.13", 8080),
                new PalmxSocketAddress("10.10.10.14", 8080)
        ));

        serviceMap.put("service2", Lists.newArrayList(
                new PalmxSocketAddress("20.10.10.10", 9999),
                new PalmxSocketAddress("20.10.10.11", 9999),
                new PalmxSocketAddress("20.10.10.12", 9999)
        ));

        serviceMap.put("service3", Lists.newArrayList(
                new PalmxSocketAddress("20.10.10.10", 9999, 1),
                new PalmxSocketAddress("20.10.10.11", 9999, 2),
                new PalmxSocketAddress("20.10.10.12", 9999, 3)
        ));

        randomLoadBalance = new RandomLoadBalance();
        roundRobinLoadBalance = new RoundRobinLoadBalance();
        consistentHasLoadBalance = new ConsistentHashLoadBalance();
        weightRandomLoadBalance = new WeightRandomLoadBalance();
        weightRoundRobinLoadBalance = new WeightRoundRobinLoadBalance();
    }

    @Test
    public void consistentHashLoadBalance() {
        for (int i = 0; i < 10; i++) {
            consistentHasLoadBalance.choose(serviceMap.get("service1"), "service1");
        }
    }

    @Test
    public void randomLoadBalance() {
        for (int i = 0; i < 10; i++) {
            randomLoadBalance.choose(serviceMap.get("service1"), "service1");
        }
    }

    @Test
    public void roundRobinBalance() {
        for (int i = 0; i < 6; i++) {
            roundRobinLoadBalance.choose(serviceMap.get("service1"), "service1");
            roundRobinLoadBalance.choose(serviceMap.get("service2"), "service2");
        }
    }

    @Test
    public void weightRoundRobinLoadBalance() {
        for (int i = 0; i < 10000; i++) {
            weightRoundRobinLoadBalance.choose(serviceMap.get("service1"), "service1");
            weightRoundRobinLoadBalance.choose(serviceMap.get("service3"), "service3");
        }
    }

    @Test
    public void weightRandomLoadBalance() {
        for (int i = 0; i < 10000; i++) {
            weightRandomLoadBalance.choose(serviceMap.get("service1"), "service1");
            weightRandomLoadBalance.choose(serviceMap.get("service3"), "service3");
        }
    }
}
