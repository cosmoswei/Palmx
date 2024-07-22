package me.xuqu.palmx.flowcontrol;

import lombok.Data;

/**
 * 用于封装不同限流方案中的参数
 */
@Data
public class FlowControlReq {

    /**
     * 流控键
     */
    public String flowControlKey;
}
