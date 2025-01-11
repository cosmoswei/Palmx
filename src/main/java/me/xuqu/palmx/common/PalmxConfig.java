package me.xuqu.palmx.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static me.xuqu.palmx.common.PalmxConstants.*;

@Slf4j
public class PalmxConfig {

    private static final Properties properties;

    static {
        properties = new Properties();
        try (InputStream inputStream = new ClassPathResource("palmx.properties").getInputStream()) {
            properties.load(inputStream);
            log.info("Load config file success: classpath:/palmx.properties");
        } catch (IOException e) {
            if (e.getClass() == FileNotFoundException.class) {
                log.warn("Cannot found config file: classpath:/palmx.properties");
            } else {
                e.printStackTrace();
            }
        }
    }

    public static LoadBalancerType getLoadBalanceType() {
        String property = getProperty(PropertyKey.LOAD_BALANCE_TYPE);
        if ("round-robin".equals(property)) {
            return LoadBalancerType.ROUND_ROBIN;
        } else if ("consistent-hash".equals(property)) {
            return LoadBalancerType.CONSISTENT_HASH;
        } else if ("weight-random".equals(property)) {
            return LoadBalancerType.WEIGHT_RANDOM;
        } else if ("weight-round-robin".equals(property)) {
            return LoadBalancerType.WEIGHT_ROUND_ROBIN;
        } else if ("adaptive".equals(property)) {
            return LoadBalancerType.ADAPTIVE;
        } else {
            return LoadBalancerType.RANDOM;
        }
    }

    public static FlowControlType getFlowControlType() {
        String property = getProperty(PropertyKey.FLOW_CONTROL_TYPE);
        if ("counter".equals(property)) {
            return FlowControlType.COUNTER;
        } else if ("sliding_window".equals(property)) {
            return FlowControlType.SLIDING_WINDOW;
        } else if ("leak-bucket".equals(property)) {
            return FlowControlType.LEAK_BUCKET;
        } else if ("token-bucket".equals(property)) {
            return FlowControlType.TOKEN_BUCKET;
        } else {
            return FlowControlType.SLIDING_WINDOW;
        }
    }

    public static int getPalmxServerPort() {
        String property = getProperty(PropertyKey.PALMX_SERVER_PORT);
        return property == null ? DEFAULT_PALMX_SERVER_PORT : Integer.parseInt(property);
    }

    public static boolean getQuicEnable() {
        String property = getProperty(PropertyKey.QUIC_ENABLE);
        return "ture".equals(property) || "Ture".equals(property);
    }

    public static boolean getIoUringEnable() {
        String property = getProperty(PropertyKey.IO_URING_ENABLE);
        return "ture".equals(property) || "Ture".equals(property);
    }



    public static int getInitialMaxStreamsBidirectional() {
        String property = getProperty(PropertyKey.QUIC_INITIAL_MAX_STREAMS_BIDIRECTIONAL);
        return property == null ? DEFAULT_INITIAL_MAX_STREAMS_BIDIRECTIONAL : Integer.parseInt(property);
    }

    public static int ioThreads() {
        String property = getProperty(PropertyKey.IO_THREADS);
        return property == null ? DEFAULT_PALMX_IO_THREADS : Integer.parseInt(property);
    }


    public static String getMetricQoSRulePath() {
        String property = getProperty(PropertyKey.METRIC_QOS_RULE_PATH);
        return property == null ? DEFAULT_METRIC_QOS_RULE_PATH : property;
    }

    public static boolean getMetricQoSEnable() {
        String property = getProperty(PropertyKey.METRIC_QOS_ENABLE);
        return "ture".equals(property) || "Ture".equals(property);
    }

    public static boolean getAdaptiveFlowControlEnable() {
        String property = getProperty(PropertyKey.ADAPTIVE_FLOW_CONTROL_ENABLE);
        return "ture".equals(property) || "Ture".equals(property);
    }


    public static boolean getFlowControlEnable() {
        String property = getProperty(PropertyKey.FLOW_CONTROL_ENABLE);
        return "ture".equals(property) || "Ture".equals(property);
    }

    /**
     * 获取当前项目所使用的 Zookeeper 根节点
     *
     * @return 根节点，不以 / 开头，默认时 palmx
     */
    public static String getZookeeperRootNode() {
        String property = getProperty(PropertyKey.ZOOKEEPER_ROOT_NODE);
        return property == null ? DEFAULT_ZOOKEEPER_ROOT_NODE : property;
    }

    public static String getZookeeperAddress() {
        String zookeeperHost = getProperty(PropertyKey.ZOOKEEPER_HOST);
        String zookeeperPort = getProperty(PropertyKey.ZOOKEEPER_PORT);
        if (zookeeperHost != null && zookeeperPort != null) {
            return java.lang.String.format("%s:%s", zookeeperHost, zookeeperPort);
        } else {
            return DEFAULT_ZOOKEEPER_ADDRESS;
        }
    }

    /**
     * 获取配置的序列化类型，默认使用 Java 对象流实现
     *
     * @return 序列化类型
     */
    public static SerializationType getSerializationType() {
        String property = getProperty(PropertyKey.SERIALIZATION_TYPE);
        if (property != null) {
            if ("json".equalsIgnoreCase(property)) {
                return SerializationType.JSON;
            } else if ("kryo".equals(property)) {
                return SerializationType.KRYO;
            } else if ("protostuff".equals(property)) {
                return SerializationType.PROTOSTUFF;
            } else {
                return SerializationType.JAVA;
            }
        }

        return SerializationType.JAVA;
    }

    public static String getProperty(String key) {
        return properties.getProperty(key);
    }

    public static String list() {
        // 使用 StringBuilder 构建字符串
        StringBuilder sb = new StringBuilder();
        properties.forEach((key, value) -> sb.append(key).append(" = ").append(value).append("\n"));
        return sb.toString();
    }

}
