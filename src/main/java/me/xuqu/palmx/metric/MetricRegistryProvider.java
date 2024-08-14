package me.xuqu.palmx.metric;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

public class MetricRegistryProvider {
    private static final MeterRegistry registry = new SimpleMeterRegistry();

    public static MeterRegistry getRegistry() {
        return registry;
    }
}
