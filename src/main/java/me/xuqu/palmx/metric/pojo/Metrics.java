package me.xuqu.palmx.metric.pojo;

import lombok.Data;

@Data
public class Metrics {

    private SystemMetrics systemMetrics;

    private NetworkMetrics networkMetrics;

    private JvmMetrics jvmMetrics;
}
