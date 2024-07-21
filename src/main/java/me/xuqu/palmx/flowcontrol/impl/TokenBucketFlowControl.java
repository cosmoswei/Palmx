package me.xuqu.palmx.flowcontrol.impl;


import me.xuqu.palmx.flowcontrol.AbstractFlowControl;
import me.xuqu.palmx.flowcontrol.FlowControlMetadata;

/**
 * 漏桶限流
 */
public class TokenBucketFlowControl extends AbstractFlowControl {

    @Override
    public boolean doControl(FlowControlMetadata limiterMataData) {
        return false;
    }
}
