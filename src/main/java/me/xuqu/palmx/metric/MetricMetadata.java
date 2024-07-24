package me.xuqu.palmx.metric;

import lombok.Data;

@Data
public class MetricMetadata {

    /**
     * CPU 负载 %
     */
    private String cpuLoad;

    /**
     * CPU使用率 %
     */
    private String cpuUsage;

    /**
     * 内存使用率 %
     */
    private String memUsage;

    /**
     * 磁盘负载 %
     */
    private String diskLoad;

    /**
     * 网络延迟 ms
     */
    private String latency;

    /**
     * 带宽使用率 %
     */
    private String bandwidthUtilization;

    /**
     * 并发连接数
     */
    private String concurrentConnections;
    /**
     * 丢包率 %
     */
    private String packetLossRate;
}
