package me.xuqu.palmx.metric.pojo;

import lombok.Data;

@Data
public class NetworkMetrics {

    // Network metrics
    private double networkLatency;
    private int networkErrors;
    private double networkBandwidth;
}
