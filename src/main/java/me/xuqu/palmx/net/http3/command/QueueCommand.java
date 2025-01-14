package me.xuqu.palmx.net.http3.command;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public abstract class QueueCommand {

    protected Channel channel;

    private ChannelPromise promise;

    public ChannelPromise getPromise() {
        return promise;
    }

    public void setPromise(ChannelPromise promise) {
        this.promise = promise;
    }

    public void cancel() {
        promise.tryFailure(new IllegalStateException("Canceled"));
    }

    public void run(Channel channel) {
        log.info("{},  run.run() = {}", this.getClass().getName(), channel);
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

    public QueueCommand setChannel(Channel channel) {
        this.channel = channel;
        return this;
    }

    public Channel getChannel() {
        return channel;
    }

    public abstract void doSend(ChannelHandlerContext ctx, ChannelPromise promise);
}
