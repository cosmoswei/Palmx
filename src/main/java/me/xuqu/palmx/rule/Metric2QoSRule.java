package me.xuqu.palmx.rule;

import me.xuqu.palmx.metric.pojo.Metrics;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class Metric2QoSRule {

    @Resource
    private KieContainer kieContainer;

    public QoS getQoS(Metrics metrics) {
        QoS qoS = new QoS();
        // 开启会话
        KieSession kieSession = kieContainer.newKieSession();
        // 设置QoS对象
        kieSession.setGlobal("qoS", qoS);
        // 设置指标对象
        kieSession.insert(metrics);
        // 触发规则
        kieSession.fireAllRules();
        // 中止会话
        kieSession.dispose();
        return qoS;
    }
}
