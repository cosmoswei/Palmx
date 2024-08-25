package me.xuqu.palmx.registry;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class RegistryDTO implements Serializable {
    /**
     * 服务质量等级
     */
    private int qoSLevel;
    /**
     * 协议
     */
    private String protocol;
    /**
     * 主机地址
     */
    private String host;
    /**
     * 主机端口
     */
    private int port;
    /**
     * 服务名
     */
    private String serviceName;
    /**
     * 注册时间
     */
    private Date tmRegistry;
    /**
     * 更新时间
     */
    private Date tmRefresh;
}
