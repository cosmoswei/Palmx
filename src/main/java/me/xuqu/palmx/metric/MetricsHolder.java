package me.xuqu.palmx.metric;

import com.google.common.collect.Lists;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.DiskSpaceMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import me.xuqu.palmx.metric.pojo.Metrics;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MetricsHolder {

    private static final Map<String, Metrics> stringMetricsMap = new ConcurrentHashMap<>();

    private static MeterRegistry registry;

    public static Metrics getMetrics0() {
        if (registry == null) {
            initMeterRegistry();
        }
        Metrics metrics = new Metrics();
        MetricsUtils metricsUtils = new MetricsUtils(registry);
        metrics.setSystemMetrics(metricsUtils.getSystemMetrics());
        metrics.setNetworkMetrics(metricsUtils.getNetworkMetrics());
        metrics.setJvmMetrics(metricsUtils.getJvmMetrics());
        return metrics;
    }

    private static void initMeterRegistry() {
        registry = MetricRegistryProvider.getRegistry();
        // Bind system metrics
        new ProcessorMetrics().bindTo(registry);
        new DiskSpaceMetrics(new File("/")).bindTo(registry);
        new ClassLoaderMetrics().bindTo(registry);
        new JvmMemoryMetrics().bindTo(registry);
        new JvmGcMetrics().bindTo(registry);
        new JvmThreadMetrics().bindTo(registry);
    }

    public Metrics getMetrics(String ipAddress) {
        if (null == ipAddress || ipAddress.isEmpty()) {
            return null;
        }
        Metrics metrics = stringMetricsMap.get(ipAddress);
        if (null == metrics) {
            metrics = getRemoteMetrics(ipAddress);
            stringMetricsMap.put(ipAddress, metrics);
        }
        return metrics;
    }

    // 还是按服务来吧，这样的
    public void loadMetrics(String serverName) {
        // 获取IP列表
        List<String> ipAddressList = getAllIpAddress(serverName);

        // 遍历
        for (String ipAddress : ipAddressList) {
            // 分别获取
            Metrics systemMetrics = getRemoteMetrics(ipAddress);
            if (null != systemMetrics) {
                stringMetricsMap.put(ipAddress, systemMetrics);
            }
        }
    }

    private List<String> getAllIpAddress(String serverName) {
        return Lists.newArrayList();
    }

    private Metrics getRemoteMetrics(String ipAddress) {
        return null;
    }

    public static Metrics getLocaleMetrics() {
        return getMetrics0();
    }
}
