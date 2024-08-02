package me.xuqu.palmx.metric;

import com.google.common.collect.Lists;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SystemMetricsHolder {

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

    public double getMemoryUsage() {
        return memoryUsage;
    }

    public double getCpuLoad() {
        return cpuLoad;
    }

    public double getCpuUsage() {
        return cpuUsage;
    }

    public double getMemoryLoad() {
        return memoryLoad;
    }


    public double getDiskLoad() {
        return diskLoad;
    }

    public double getDiskIOLoad() {
        return diskIOLoad;
    }

    public double getNetworkLatency() {
        return networkLatency;
    }

    public int getNetworkErrors() {
        return networkErrors;
    }

    public double getNetworkBandwidth() {
        return networkBandwidth;
    }


    public double getJvmGcThroughput() {
        return jvmGcThroughput;
    }

    public double getJvmGcLatency() {
        return jvmGcLatency;
    }

    public double getJvmGcTime() {
        return jvmGcTime;
    }

    public double getJvmStwTime() {
        return jvmStwTime;
    }

    public double getJvmMemoryPoolUsage() {
        return jvmMemoryPoolUsage;
    }

    public double getJvmHeapMemoryAllocationRate() {
        return jvmHeapMemoryAllocationRate;
    }

    public SystemMetrics getLocalSystemMetrics() {
        SystemMetrics systemMetrics = new SystemMetrics();
        // todo 并行获取
        systemMetrics.setCpuLoad(getCpuLoad());
        systemMetrics.setCpuUsage(getCpuUsage());
        systemMetrics.setMemoryLoad(getMemoryLoad());
        systemMetrics.setMemoryUsage(getMemoryUsage());
        systemMetrics.setDiskLoad(getDiskLoad());
        systemMetrics.setDiskIOLoad(getDiskIOLoad());
        systemMetrics.setNetworkLatency(getNetworkLatency());
        systemMetrics.setNetworkErrors(getNetworkErrors());
        systemMetrics.setNetworkBandwidth(getNetworkBandwidth());
        systemMetrics.setJvmGcThroughput(getJvmGcThroughput());
        systemMetrics.setJvmGcLatency(getJvmGcLatency());
        systemMetrics.setJvmGcTime(getJvmGcTime());
        systemMetrics.setJvmStwTime(getJvmStwTime());
        systemMetrics.setJvmMemoryPoolUsage(getJvmMemoryPoolUsage());
        systemMetrics.setJvmHeapMemoryAllocationRate(getJvmHeapMemoryAllocationRate());
        return systemMetrics;
    }

    Map<String, SystemMetrics> stringSystemMetricsMap = new ConcurrentHashMap<>();

    public SystemMetrics getRomoteSystemMetrics(String ipAddress) {
        if (null == ipAddress || ipAddress.isEmpty()) {
            return null;
        }
        return stringSystemMetricsMap.get(ipAddress);
    }

    // 还是按服务来吧，这样的
    public void loadSystemMetrics(String serverName) {

        // 获取IP列表
        List<String> ipAddressList = getAllIpAddress(serverName);

        // 遍历
        for (String ipAddress : ipAddressList) {
            // 分别获取
            SystemMetrics systemMetrics = getSystemMetricsByIp(ipAddress);
            if (null != systemMetrics) {
                stringSystemMetricsMap.put(ipAddress, systemMetrics);
            }
        }
    }

    private List<String> getAllIpAddress(String serverName) {
        return Lists.newArrayList();
    }

    private SystemMetrics getSystemMetricsByIp(String ipAddress) {
        return null;
    }

    private void loadBalance(){
        // 先去获取服务列表

        // 做一次P2C？

        // 获取列表的机器配置、性能指标，没有的话去远程获取

        // 根据规则获取自适应配置（数据指标、聚合算法）

        // 根据自适应配置做负载均衡
    }
}
