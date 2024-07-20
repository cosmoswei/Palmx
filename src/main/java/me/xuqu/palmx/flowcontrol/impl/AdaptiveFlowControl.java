package me.xuqu.palmx.flowcontrol.impl;

import me.xuqu.palmx.flowcontrol.FlowControlAbstract;
import me.xuqu.palmx.flowcontrol.FlowControlMetadata;

public class AdaptiveFlowControl extends FlowControlAbstract {

    @Override
    public boolean doControl(FlowControlMetadata limiterMataData) {
        return false;
    }
}

