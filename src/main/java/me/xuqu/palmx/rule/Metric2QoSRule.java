package me.xuqu.palmx.rule;

import me.xuqu.palmx.common.PalmxConfig;
import me.xuqu.palmx.metric.pojo.Metrics;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.io.Resource;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;

public class Metric2QoSRule {

    private KieContainer kieContainer;

    public QoS getQoS(Metrics metrics) {
        QoS qoS = new QoS();

        String metricQoSRulePath = PalmxConfig.getMetricQoSRulePath();

        KieSession kieSession = getKieSession(metricQoSRulePath);

        kieSession.setGlobal("qoS", qoS);
        // 插入数据到 KieSession
        kieSession.insert(metrics);

        // 触发规则执行
        kieSession.fireAllRules();

        // 关闭 KieSession
        kieSession.dispose();
        return qoS;
    }

    public KieSession getKieSession(String customRulesFilePath) {

        if (kieContainer == null) {
            KieServices kieServices = KieServices.Factory.get();
            KieFileSystem kieFileSystem = kieServices.newKieFileSystem();

            // 加载默认规则文件
            Resource defaultResource = kieServices.getResources().newClassPathResource(customRulesFilePath);
            kieFileSystem.write("src/main/resources/rules.drl", defaultResource);

            // 构建 KieContainer
            KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem);
            kieBuilder.buildAll();
            kieContainer = kieServices.newKieContainer(kieServices.getRepository().getDefaultReleaseId());
        }
        // 创建 KieSession 并返回
        return kieContainer.newKieSession();
    }
}
