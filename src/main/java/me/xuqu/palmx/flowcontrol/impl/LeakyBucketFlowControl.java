package me.xuqu.palmx.flowcontrol.impl;


import me.xuqu.palmx.flowcontrol.FlowControlAbstract;
import me.xuqu.palmx.flowcontrol.FlowControlMetadata;

/**
 * 漏桶限流
 */
public class LeakyBucketFlowControl extends FlowControlAbstract {

    @Override
    public boolean doControl(FlowControlMetadata limiterMataData) {
        return false;
    }
}
