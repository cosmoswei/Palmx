package me.xuqu.palmx.registry;

import lombok.Data;

@Data
public class RegistryDTO {
    private short qoSLevel;
    private String protocol;
    private String host;
    private int port;
    private String serviceName;
}
