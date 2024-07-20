package me.xuqu.palmx.flowcontrol.impl;


import lombok.extern.slf4j.Slf4j;
import me.xuqu.palmx.flowcontrol.FlowControlAbstract;
import me.xuqu.palmx.flowcontrol.FlowControlMetadata;

@Slf4j
public class LeakBucketFlowControl extends FlowControlAbstract {

    @Override
    public boolean doControl(FlowControlMetadata limiterMataData) {
        return false;
    }
}
