package me.xuqu.palmx.rule;

import lombok.Data;

@Data
public class QoS {
    /**
     * QoS 等级，体现的是服务质量的分数
     * 值域 = 1-100
     * 默认值 = 100
     */
    private int qoSLevel = 100;
}
