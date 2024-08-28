package me.xuqu.palmx.flowcontrol.impl;


import lombok.extern.slf4j.Slf4j;
import me.xuqu.palmx.flowcontrol.AbstractFlowControl;
import me.xuqu.palmx.flowcontrol.FlowControlReq;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 计数器限流
 */
@Slf4j
public class CounterFlowControl extends AbstractFlowControl {

    // 起始时间
    private static long startTime = System.currentTimeMillis();
    // 时间区间的时间间隔 ms
    private final long interval;
    //累加器
    private final AtomicLong accumulator = new AtomicLong(0);

    @Override
    public boolean doControl(FlowControlReq flowControlReq) {
        long nowTime = System.currentTimeMillis();
        //在时间区间之内
        if (nowTime < startTime + interval) {
            long count = accumulator.incrementAndGet();
            return count > qps;
        } else {
            //在时间区间之外
            synchronized (this) {
                // 再一次判断，防止重复初始化
                if (nowTime > startTime + interval) {
                    accumulator.set(0);
                    startTime = nowTime;
                }
            }
            return false;
        }
    }


    public CounterFlowControl(int qps) {
        this.interval = 1000;
        super.qps = qps;
    }
}
