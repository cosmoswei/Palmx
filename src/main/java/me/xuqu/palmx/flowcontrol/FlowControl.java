package me.xuqu.palmx.flowcontrol;

/**
 * 限流接口
 */
public interface FlowControl {

    /**
     * 检查是否达到限制
     * true = 被限制；false = 没被限制
     */
    boolean control(FlowControlReq flowControlReq);

}
