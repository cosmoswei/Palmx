package me.xuqu.palmx.flowcontrol;

/**
 * 限流接口
 */
public interface FlowControl {

    /**
     * 检查是否达到限制,子类必须强制实现该接口
     *
     * @param limiterMataData
     * @return
     */
    boolean control(FlowControlMetadata limiterMataData);

}
