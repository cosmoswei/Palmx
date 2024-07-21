package me.xuqu.palmx.flowcontrol.impl;


import lombok.extern.slf4j.Slf4j;
import me.xuqu.palmx.flowcontrol.AbstractFlowControl;
import me.xuqu.palmx.flowcontrol.FlowControlMetadata;

@Slf4j
public class LeakBucketFlowControl extends AbstractFlowControl {

    @Override
    public boolean doControl(FlowControlMetadata flowControlMetadata) {
        return false;
    }
}
