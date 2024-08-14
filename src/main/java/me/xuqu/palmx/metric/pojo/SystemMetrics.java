package me.xuqu.palmx.metric.pojo;

import lombok.Data;

@Data
public class SystemMetrics {

    // CPU metrics
    private double cpuLoad;
    private double cpuUsage;

    // Memory metrics
    // todo
    private double memoryLoad;
    private double memoryUsage;

    // Disk metrics
    private double diskLoad;
    private double diskIOLoad;
}
