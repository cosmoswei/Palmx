package me.xuqu.palmx.command;


import java.util.concurrent.CompletableFuture;

import io.netty.channel.Channel;
import io.netty.handler.codec.http2.Http2StreamChannel;

public class StreamChannelFuture extends CompletableFuture<Channel> {

    private final Channel parentChannel;

    private Throwable cause;

    public StreamChannelFuture(Channel parentChannel) {
        this.parentChannel = parentChannel;
    }

    public StreamChannelFuture(Http2StreamChannel channel) {
        this.complete(channel);
        this.parentChannel = channel.parent();
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
