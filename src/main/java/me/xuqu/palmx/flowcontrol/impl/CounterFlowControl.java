package me.xuqu.palmx.flowcontrol.impl;


import me.xuqu.palmx.flowcontrol.FlowControlAbstract;
import me.xuqu.palmx.flowcontrol.FlowControlMetadata;

/**
 * 计数器限流
 */
public class CounterFlowControl extends FlowControlAbstract {

    @Override
    public boolean doControl(FlowControlMetadata limiterMataData) {
        return false;
    }
}
