package me.xuqu.palmx.flowcontrol.impl;


import lombok.extern.slf4j.Slf4j;
import me.xuqu.palmx.flowcontrol.AbstractFlowControl;
import me.xuqu.palmx.flowcontrol.FlowControlReq;

import java.util.LinkedList;
import java.util.Queue;

@Slf4j
public class SlidingWindowFlowControl extends AbstractFlowControl {

    private final int maxRequests;
    private final long windowSizeInMillis;
    private final Queue<Long> requestTimestamps;

    public SlidingWindowFlowControl(int qps) {
        this.maxRequests = qps;
        this.windowSizeInMillis = 1000;
        this.requestTimestamps = new LinkedList<>();
        super.qps = qps;
    }

    @Override
    public boolean doControl(FlowControlReq flowControlReq) {
        long currentTime = System.currentTimeMillis();
        long windowStart = currentTime - windowSizeInMillis;
        // Remove timestamps that are outside of the window
        while (!requestTimestamps.isEmpty() && requestTimestamps.peek() < windowStart) {
            requestTimestamps.poll();
        }
        if (requestTimestamps.size() < maxRequests) {
            requestTimestamps.add(currentTime);
            log.debug("Request allowed at {}", currentTime);
            return false;
        } else {
            log.debug("Request denied at {}", currentTime);
            return true;
        }
    }

}
