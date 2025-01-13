package me.xuqu.palmx.net.http3.command;


import java.util.concurrent.CompletableFuture;

import io.netty.channel.Channel;
import io.netty.incubator.codec.quic.QuicStreamChannel;

public class QuicStreamChannelFuture extends CompletableFuture<Channel> {

    private final Channel parentChannel;

    private Throwable cause;

    public QuicStreamChannelFuture(Channel parentChannel) {
        this.parentChannel = parentChannel;
    }

    public Channel getParentChannel() {
        return parentChannel;
    }

    @Override
    public boolean completeExceptionally(Throwable cause) {
        boolean result = super.completeExceptionally(cause);
        if (result) {
            this.cause = cause;
        }
        return result;
    }

    public Throwable cause() {
        return cause;
    }

    public boolean isSuccess() {
        return isDone() && cause() == null;
    }

    public Channel getNow() {
        return getNow(null);
    }
}
