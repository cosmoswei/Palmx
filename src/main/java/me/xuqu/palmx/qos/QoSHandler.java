package me.xuqu.palmx.qos;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import me.xuqu.palmx.metric.MetricsHolder;
import me.xuqu.palmx.metric.pojo.Metrics;
import me.xuqu.palmx.rule.Metric2QoSRule;
import me.xuqu.palmx.rule.QoS;
import me.xuqu.palmx.util.JsonUtils;

import java.util.concurrent.TimeUnit;

@Slf4j
public class QoSHandler {

    private static final Metric2QoSRule metric2QoSRule = new Metric2QoSRule();

    static Cache<String, Integer> qoSCache = Caffeine.newBuilder()
            //过期时间
            .expireAfterWrite(5, TimeUnit.SECONDS)
            //最大容量
            .maximumSize(20)
            .build();

    public static int getQoSLevel(Metrics metrics) {
        // 通过规则引擎给出QoS等级
        QoS qoS = metric2QoSRule.getQoS(metrics);
        return qoS.getQoSLevel();
    }

    public static int getLocalQoSLevel() {
        Metrics localeMetrics = MetricsHolder.getLocaleMetrics();
        log.info("localMetrics is [{}]", JsonUtils.toJson(localeMetrics));
        int qoSLevel = getQoSLevel(localeMetrics);
        log.info("get local qos level is {}", qoSLevel);
        return qoSLevel;
    }

    public static int getLocalQoSLevelFromCache(String cacheKey) {
        Integer qoSLevel = qoSCache.getIfPresent(cacheKey);
        if (qoSLevel == null) {
            qoSLevel = getLocalQoSLevel();
            qoSCache.put(cacheKey, qoSLevel);
        }
        return qoSLevel;
    }
}
