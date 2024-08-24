package me.xuqu.palmx.qos;

import lombok.extern.slf4j.Slf4j;
import me.xuqu.palmx.metric.MetricsHolder;
import me.xuqu.palmx.metric.pojo.Metrics;

import java.util.Random;

@Slf4j
public class QoSHandler {
    public static int getQoSLevel(Metrics metrics) {
        // 通过规则引擎给出QoS等级
        return new Random().nextInt(100);
    }

    public static int getLocalQoSLevel() {
        Metrics localeMetrics = MetricsHolder.getLocaleMetrics();
        int qoSLevel = getQoSLevel(localeMetrics);
        log.info("get local qos level = {}", qoSLevel);
        return qoSLevel;
    }
}
