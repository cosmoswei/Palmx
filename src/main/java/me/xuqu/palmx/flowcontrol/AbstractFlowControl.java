package me.xuqu.palmx.flowcontrol;

import me.xuqu.palmx.common.PalmxConfig;
import me.xuqu.palmx.qos.QoSHandler;

/**
 * 此处是为了让限流实现类必须实现 doControl(),其他方法按需索取
 */
public abstract class AbstractFlowControl implements FlowControl {

    protected int qps;

    @Override
    public boolean control(FlowControlReq flowControlReq) {
        this.adaptiveControl();
        return doControl(flowControlReq);
    }

    protected abstract boolean doControl(FlowControlReq flowControlReq);


    /**
     * 自适应控制，通过获取服务质量等级 QoS 来限制流量的百分比
     */
    private void adaptiveControl() {
        if (PalmxConfig.getAdaptiveFlowControlEnable()) {
            int localQoSLevel = QoSHandler.getLocalQoSLevelFromCache("FLOW_CONTROL");
            if (localQoSLevel <= 0) {
                this.qps = 0;
            }
            this.qps = (this.qps / localQoSLevel) * 100;
        }
    }
}
