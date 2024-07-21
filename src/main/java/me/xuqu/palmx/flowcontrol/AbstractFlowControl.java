package me.xuqu.palmx.flowcontrol;

/**
 * 此处是为了让限流实现类必须实现 doControl(),其他方法按需索取
 */
public abstract class AbstractFlowControl implements FlowControl {

    @Override
    public boolean control(FlowControlMetadata limiterMataData) {
        return false;
    }

    protected abstract boolean doControl(FlowControlMetadata limiterMataData) ;
}
