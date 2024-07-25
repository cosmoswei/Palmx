package me.xuqu.palmx.metric;

import lombok.Data;

@Data
public class MetricMetadata {

    /**
     * CPU 负载 %
     */
    private double cpuLoad;
    /**
     * CPU使用率 %
     */
    private double cpuUsage;
    /**
     * 内存使用率 %
     */
    private double memUsage;
    /**
     * 磁盘负载 %
     */
    private double diskLoad;
    /**
     * 网络延迟 ms
     */
    private double latency;
    /**
     * 带宽使用率 %
     */
    private double bandwidthUtilization;
    /**
     * 并发连接数
     */
    private double concurrentConnections;
    /**
     * 丢包率 %
     */
    private double packetLossRate;
}
