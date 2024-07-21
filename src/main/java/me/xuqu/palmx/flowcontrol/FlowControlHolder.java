package me.xuqu.palmx.flowcontrol;

import me.xuqu.palmx.common.FlowControlType;
import me.xuqu.palmx.common.PalmxConfig;
import me.xuqu.palmx.flowcontrol.impl.*;

public class FlowControlHolder {

    private static FlowControl flowControl;

    public static synchronized FlowControl get() {
        if (flowControl == null) {
            FlowControlType flowControlType = PalmxConfig.getFlowControlType();
            switch (flowControlType) {
                case COUNTER:
                    flowControl = new CounterFlowControl();
                    break;
                case LEAK_BUCKET:
                    flowControl = new LeakBucketFlowControl();
                    break;
                case TOKEN_BUCKET:
                    flowControl = new TokenBucketFlowControl();
                    break;
                case ADAPTIVE:
                    flowControl = new AdaptiveFlowControl();
                    break;
                case SLIDING_WINDOW:
                default:
                    flowControl = new SlidingWindowFlowControl();
            }
        }
        return flowControl;
    }
}
