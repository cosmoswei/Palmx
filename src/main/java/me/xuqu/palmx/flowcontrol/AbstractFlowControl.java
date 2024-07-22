package me.xuqu.palmx.flowcontrol;

/**
 * 此处是为了让限流实现类必须实现 doControl(),其他方法按需索取
 */
public abstract class AbstractFlowControl implements FlowControl {

    protected int qps;

    @Override
    public boolean control(FlowControlReq flowControlReq) {
        return doControl(flowControlReq);
    }

    protected abstract boolean doControl(FlowControlReq flowControlReq);

}
