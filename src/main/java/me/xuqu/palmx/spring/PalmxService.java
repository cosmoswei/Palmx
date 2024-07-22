package me.xuqu.palmx.spring;

import me.xuqu.palmx.common.FlowControlType;
import org.springframework.stereotype.Service;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Service
public @interface PalmxService {


    String name() default "";

    /**
     * 流控类型 qps 默认为滑动窗口
     */
    FlowControlType flowControlLimitType() default FlowControlType.SLIDING_WINDOW;

    /**
     * qps 默认-1 为不限制
     */
    int flowControlLimitCount() default -1;


}
