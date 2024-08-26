package me.xuqu.palmx.qos;

import lombok.extern.slf4j.Slf4j;
import me.xuqu.palmx.metric.MetricsHolder;
import me.xuqu.palmx.metric.pojo.Metrics;
import me.xuqu.palmx.rule.Metric2QoSRule;
import me.xuqu.palmx.rule.QoS;
import me.xuqu.palmx.util.JsonUtils;

@Slf4j
public class QoSHandler {

    private static final Metric2QoSRule metric2QoSRule = new Metric2QoSRule();

    public static int getQoSLevel(Metrics metrics) {
        // 通过规则引擎给出QoS等级
        QoS qoS = metric2QoSRule.getQoS(metrics);
        return qoS.getQoSLevel();
    }

    public static int getLocalQoSLevel() {
        Metrics localeMetrics = MetricsHolder.getLocaleMetrics();
        log.info("localeMetrics is [{}]", localeMetrics);
        int qoSLevel = getQoSLevel(localeMetrics);
        log.info("get local qos level is {}", qoSLevel);
        return qoSLevel;
    }
}
