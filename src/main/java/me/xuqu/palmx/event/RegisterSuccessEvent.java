package me.xuqu.palmx.event;

import lombok.Data;

@Data
public class RegisterSuccessEvent {

    private String serviceName;

    private String serviceVersion;

    public RegisterSuccessEvent(String serviceName) {
        this.serviceName = serviceName;
    }

}