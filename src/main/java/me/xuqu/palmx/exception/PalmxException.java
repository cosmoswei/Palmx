package me.xuqu.palmx.exception;

public class PalmxException extends RuntimeException {

    public PalmxException(String message) {
        super(message);
    }

    public PalmxException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
