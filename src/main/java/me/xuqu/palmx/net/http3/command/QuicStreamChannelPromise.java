package me.xuqu.palmx.net.http3.command;


import io.netty.channel.Channel;
import io.netty.util.concurrent.DefaultPromise;

public class QuicStreamChannelPromise extends DefaultPromise<Channel> {

    private Throwable cause;

    private final Channel parentChannel;

    public QuicStreamChannelPromise(Channel parentChannel) {
        this.parentChannel = parentChannel;
    }

    public Channel getParentChannel() {
        return parentChannel;
    }

    public Throwable cause() {
        return cause;
    }

    public boolean isSuccess() {
        return isDone() && cause() == null;
    }
}
