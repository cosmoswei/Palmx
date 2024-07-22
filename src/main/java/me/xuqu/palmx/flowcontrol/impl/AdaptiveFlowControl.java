package me.xuqu.palmx.flowcontrol.impl;

import me.xuqu.palmx.flowcontrol.AbstractFlowControl;
import me.xuqu.palmx.flowcontrol.FlowControlReq;

public class AdaptiveFlowControl extends AbstractFlowControl {

    public AdaptiveFlowControl(int qps) {
        super.qps = qps;
    }

    @Override
    public boolean doControl(FlowControlReq flowControlReq) {
        return false;
    }
}

