package me.xuqu.palmx.exception;

public class RpcInvocationException extends PalmxException {

    public RpcInvocationException(String message) {
        super(message);
    }

    public RpcInvocationException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
