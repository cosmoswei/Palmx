package me.xuqu.palmx.net.http3.command;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPromise;
import io.netty.incubator.codec.http3.Http3;
import io.netty.incubator.codec.quic.QuicChannel;
import io.netty.incubator.codec.quic.QuicStreamChannel;

public class CreateStreamQueueCommand extends QueuedCommand {

    private final ChannelInitializer<QuicStreamChannel> initializer;

    private final QuicStreamChannelFuture streamChannelFuture;

    private CreateStreamQueueCommand(
            ChannelInitializer<QuicStreamChannel> initializer, QuicStreamChannelFuture future) {
        this.initializer = initializer;
        this.streamChannelFuture = future;
        this.promise(future.getParentChannel().newPromise());
        this.channel(future.getParentChannel());
    }

    public static CreateStreamQueueCommand create(
            ChannelInitializer<QuicStreamChannel> initializer, QuicStreamChannelFuture future) {
        return new CreateStreamQueueCommand(initializer, future);
    }

    @Override
    public void doSend(ChannelHandlerContext ctx, ChannelPromise promise) {
    }

    @Override
    public void run(Channel channel) {
        Http3.newRequestStream((QuicChannel) channel, initializer).addListener(future -> {
            if (future.isSuccess()) {
                streamChannelFuture.complete((Channel) future.getNow());
            } else {
                streamChannelFuture.completeExceptionally(future.cause());
            }
        });
    }
}
