package me.xuqu.palmx.flowcontrol.impl;


import lombok.extern.slf4j.Slf4j;
import me.xuqu.palmx.flowcontrol.AbstractFlowControl;
import me.xuqu.palmx.flowcontrol.FlowControlReq;

@Slf4j
public class SlidingWindowFlowControl extends AbstractFlowControl {

    public SlidingWindowFlowControl(int qps) {
        super.qps = qps;
    }

    @Override
    public boolean doControl(FlowControlReq flowControlReq) {
        return false;
    }
}
