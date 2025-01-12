package me.xuqu.palmx.net.http3.command;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import me.xuqu.palmx.net.RpcMessage;

public abstract class QueuedCommand {

    protected Channel channel;

    private RpcMessage rpcMessage;

    private ChannelPromise promise;

    public ChannelPromise promise() {
        return promise;
    }

    public void promise(ChannelPromise promise) {
        this.promise = promise;
    }

    public void cancel() {
        promise.tryFailure(new IllegalStateException("Canceled"));
    }

    public void run(Channel channel) {
        if (channel.isActive()) {
            channel.write(this).addListener(future -> {
                if (future.isSuccess()) {
                    promise.setSuccess();
                } else {
                    promise.setFailure(future.cause());
                }
            });
        } else {
            promise.trySuccess();
        }
    }

    public final void send(ChannelHandlerContext ctx, ChannelPromise promise) {
        if (ctx.channel().isActive()) {
            doSend(ctx, promise);
        }
    }

    public QueuedCommand channel(Channel channel) {
        this.channel = channel;
        return this;
    }

    public Channel channel() {
        return channel;
    }

    public abstract void doSend(ChannelHandlerContext ctx, ChannelPromise promise);
}
