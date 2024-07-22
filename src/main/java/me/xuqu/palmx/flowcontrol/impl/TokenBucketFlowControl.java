package me.xuqu.palmx.flowcontrol.impl;


import lombok.extern.slf4j.Slf4j;
import me.xuqu.palmx.flowcontrol.AbstractFlowControl;
import me.xuqu.palmx.flowcontrol.FlowControlReq;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 漏桶限流
 */
@Slf4j
public class TokenBucketFlowControl extends AbstractFlowControl {

    // 上一次令牌发放时间
    public long lastTime;
    // 桶的容量
    public int capacity;
    // 令牌生成速度 /s
    public int rate;
    // 当前令牌数量
    public AtomicInteger tokens = new AtomicInteger(0);

    public TokenBucketFlowControl(int qps) {
        this.lastTime = qps;
        this.capacity = qps;
        this.rate = qps;
        super.qps = qps;
    }

    @Override
    public boolean doControl(FlowControlReq flowControlReq) {
        long now = System.currentTimeMillis();
        //时间间隔,单位为 ms
        long gap = now - lastTime;
        //计算时间段内的令牌数
        int reverse_permits = (int) (gap * rate / 1000);
        int all_permits = tokens.get() + reverse_permits;
        // 当前令牌数
        tokens.set(Math.min(capacity, all_permits));
        log.info("tokens {} capacity {} gap {} ", tokens, capacity, gap);
        if (tokens.get() < 1) {
            // 若拿不到令牌,则拒绝
            log.info("被限流了..");
            return true;
        } else {
            // 还有令牌，领取令牌
            tokens.getAndAdd(-1);
            lastTime = now;
            log.info("剩余令牌..{}", tokens);
            return false;
        }
    }
}
