package me.xuqu.palmx.event;


import lombok.extern.slf4j.Slf4j;
import me.xuqu.palmx.loadbalance.LoadBalanceHolder;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RegisterEventListener {

    @EventListener
    public void handleNotifyEvent(RegisterSuccessEvent event) {
        log.info("监听到用户注册成功事件 event = {}", event.toString());
        LoadBalanceHolder.notifyRefresh(event.getServiceName());
    }
}