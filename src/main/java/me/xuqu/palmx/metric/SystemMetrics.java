package me.xuqu.palmx.metric;

import lombok.Data;

@Data
public class SystemMetrics {

    // CPU metrics
    private double cpuLoad;
    private double cpuUsage;

    // Memory metrics
    private double memoryLoad;
    private double memoryUsage;

    // Disk metrics
    private double diskLoad;
    private double diskIOLoad;

    // Network metrics
    private double networkLatency;
    private int networkErrors;
    private double networkBandwidth;

    // JVM GC metrics
    private double jvmGcThroughput;
    private double jvmGcLatency;
    private double jvmGcTime;
    private double jvmStwTime;
    private double jvmMemoryPoolUsage;
    private double jvmHeapMemoryAllocationRate;
}
