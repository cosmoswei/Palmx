package me.xuqu.palmx.flowcontrol;

import lombok.extern.slf4j.Slf4j;
import me.xuqu.palmx.common.FlowControlType;
import me.xuqu.palmx.flowcontrol.impl.CounterFlowControl;
import me.xuqu.palmx.flowcontrol.impl.LeakBucketFlowControl;
import me.xuqu.palmx.flowcontrol.impl.SlidingWindowFlowControl;
import me.xuqu.palmx.flowcontrol.impl.TokenBucketFlowControl;

import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class FlowControlHolder {

    private static final ConcurrentHashMap<String, FlowControl> flowControlMap = new ConcurrentHashMap<>();

    public static synchronized FlowControl get(int limitCount, FlowControlType flowControlType) {
        FlowControl flowControl;
        switch (flowControlType) {
            case COUNTER:
                flowControl = new CounterFlowControl(limitCount);
                break;
            case LEAK_BUCKET:
                flowControl = new LeakBucketFlowControl(limitCount);
                break;
            case TOKEN_BUCKET:
                flowControl = new TokenBucketFlowControl(limitCount);
                break;
            case SLIDING_WINDOW:
            default:
                flowControl = new SlidingWindowFlowControl(limitCount);
        }
        return flowControl;
    }

    public static synchronized void initFlowControl(FlowControlMetadata flowControlMetadata) {
        FlowControl flowControl = get(flowControlMetadata.limitCount,
                flowControlMetadata.flowControlType);
        flowControlMap.put(flowControlMetadata.flowControlKey, flowControl);
        log.info("initFlowControl flowControlMetadata = {},", flowControlMetadata);
    }

    public static boolean control(FlowControlReq flowControlReq) {
        FlowControl flowControl = flowControlMap.get(flowControlReq.getFlowControlKey());
        if (flowControl == null) {
            log.error("flowControl is null, pass this control, flowControlMetadata = {},", flowControlReq);
            return false;
        }
        return flowControl.control(flowControlReq);
    }
}
