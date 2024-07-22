package me.xuqu.palmx.flowcontrol;

import lombok.Data;
import me.xuqu.palmx.common.FlowControlType;

/**
 * 用于封装不同限流方案中的参数
 */
@Data
public class FlowControlMetadata {

    /**
     * 流控键
     */
    public String flowControlKey;

    /**
     * 流控类型
     */
    public FlowControlType flowControlType;

    /**
     * 限制次数（QPS）
     */
    public int limitCount;

    public FlowControlMetadata(String flowControlKey, FlowControlType flowControlType, int limitCount) {
        this.flowControlKey = flowControlKey;
        this.flowControlType = flowControlType;
        this.limitCount = limitCount;
    }
}
