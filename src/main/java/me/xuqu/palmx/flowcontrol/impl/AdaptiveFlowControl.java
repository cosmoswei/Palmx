package me.xuqu.palmx.flowcontrol.impl;

import me.xuqu.palmx.flowcontrol.AbstractFlowControl;
import me.xuqu.palmx.flowcontrol.FlowControlMetadata;

public class AdaptiveFlowControl extends AbstractFlowControl {

    @Override
    public boolean doControl(FlowControlMetadata limiterMataData) {
        return false;
    }
}

