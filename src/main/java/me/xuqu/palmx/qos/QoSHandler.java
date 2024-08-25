package me.xuqu.palmx.qos;

import lombok.extern.slf4j.Slf4j;
import me.xuqu.palmx.metric.MetricsHolder;
import me.xuqu.palmx.metric.pojo.Metrics;
import me.xuqu.palmx.rule.Metric2QoSRule;
import me.xuqu.palmx.rule.QoS;

import javax.annotation.Resource;

@Slf4j
public class QoSHandler {

    @Resource
    private static Metric2QoSRule metric2QoSRule;

    public static int getQoSLevel(Metrics metrics) {
        // 通过规则引擎给出QoS等级
        QoS qoS = metric2QoSRule.getQoS(metrics);
        return qoS.getQoSLevel();
    }

    public static int getLocalQoSLevel() {
        Metrics localeMetrics = MetricsHolder.getLocaleMetrics();
        int qoSLevel = getQoSLevel(localeMetrics);
        log.info("get local qos level = {}", qoSLevel);
        return qoSLevel;
    }
}
