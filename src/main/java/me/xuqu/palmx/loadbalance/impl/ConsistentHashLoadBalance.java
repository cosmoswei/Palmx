package me.xuqu.palmx.loadbalance.impl;

import me.xuqu.palmx.loadbalance.AbstractLoadBalance;
import me.xuqu.palmx.loadbalance.PalmxSocketAddress;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ConsistentHashLoadBalance extends AbstractLoadBalance {

    private final ConcurrentHashMap<String, ConsistentHashSelector> selectors = new ConcurrentHashMap<>();

    @Override
    protected PalmxSocketAddress doChoose(List<PalmxSocketAddress> socketAddressList, String serviceName) {
        List<String> serviceAddresses = socketAddressList.stream().map(InetSocketAddress::toString).collect(Collectors.toList());

        int identityHashCode = System.identityHashCode(socketAddressList);
        // build rpc service name by rpcRequest
        ConsistentHashSelector selector = selectors.get(serviceName);
        // check for updates
        if (selector == null || selector.identityHashCode != identityHashCode) {
            selectors.put(serviceName, new ConsistentHashSelector(serviceAddresses, 160, identityHashCode));
            selector = selectors.get(serviceName);
        }

        return parseStringToServer(selector.select(serviceName));
    }

    public static PalmxSocketAddress parseStringToServer(String input) {

        // 使用正则匹配键值对
        Pattern pattern = Pattern.compile("(\\w+)='?(.*?)'?([,}])");
        Matcher matcher = pattern.matcher(input);
        String addr = "";
        int port = 0;
        int weight = 0;
        int effectiveWeight = 0;
        int currentWeight = 0;
        while (matcher.find()) {
            String key = matcher.group(1);  // 键
            String value = matcher.group(2); // 值
            // 根据键值设置对象属性
            switch (key) {
                case "ip":
                    addr = value;
                    break;
                case "weight":
                    weight = Integer.parseInt(value);
                    break;
                case "effectiveWeight":
                    effectiveWeight = Integer.parseInt(value);
                    break;
                case "currentWeight":
                    currentWeight = Integer.parseInt(value);
                    break;
            }
        }
        PalmxSocketAddress server = new PalmxSocketAddress(addr, port, weight);
        server.setCurrentWeight(currentWeight);
        server.setCurrentWeight(effectiveWeight);
        return server;
    }

    static class ConsistentHashSelector {
        private final TreeMap<Long, String> virtualInvokers;

        private final int identityHashCode;

        ConsistentHashSelector(List<String> invokers, int replicaNumber, int identityHashCode) {
            this.virtualInvokers = new TreeMap<>();
            this.identityHashCode = identityHashCode;

            for (String invoker : invokers) {
                for (int i = 0; i < replicaNumber / 4; i++) {
                    byte[] digest = md5(invoker + i);
                    for (int h = 0; h < 4; h++) {
                        long m = hash(digest, h);
                        virtualInvokers.put(m, invoker);
                    }
                }
            }
        }

        static byte[] md5(String key) {
            MessageDigest md;
            try {
                md = MessageDigest.getInstance("MD5");
                byte[] bytes = key.getBytes(StandardCharsets.UTF_8);
                md.update(bytes);
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }

            return md.digest();
        }

        static long hash(byte[] digest, int idx) {
            return ((long) (digest[3 + idx * 4] & 255) << 24 | (long) (digest[2 + idx * 4] & 255) << 16 | (long) (digest[1 + idx * 4] & 255) << 8 | (long) (digest[idx * 4] & 255)) & 4294967295L;
        }

        public String select(String rpcServiceKey) {
            byte[] digest = md5(rpcServiceKey);
            return selectForKey(hash(digest, 0));
        }

        public String selectForKey(long hashCode) {
            Map.Entry<Long, String> entry = virtualInvokers.tailMap(hashCode, true).firstEntry();

            if (entry == null) {
                entry = virtualInvokers.firstEntry();
            }

            return entry.getValue();
        }
    }
}
