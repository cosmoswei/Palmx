package me.xuqu.palmx.net;

import lombok.Data;

import java.io.Serializable;

@Data
public class RpcResponse implements Serializable {
    private transient int sequenceId;
    private byte status;
    private Object data;
    private String message;
}
