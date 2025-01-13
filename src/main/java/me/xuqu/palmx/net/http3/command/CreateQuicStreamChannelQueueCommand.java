package me.xuqu.palmx.net.http3.command;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.incubator.codec.http3.Http3;
import io.netty.incubator.codec.http3.Http3RequestStreamInboundHandler;
import io.netty.incubator.codec.quic.QuicChannel;

import java.net.InetSocketAddress;

import static me.xuqu.palmx.net.http3.command.CreateQuicChannelQueueCommand.connectionCache;

public class CreateQuicStreamChannelQueueCommand extends QueuedCommand {

    private final Http3RequestStreamInboundHandler handler;

    private final InetSocketAddress address;

    private final QuicStreamChannelFuture streamChannelFuture;

    private CreateQuicStreamChannelQueueCommand(InetSocketAddress address,
                                                Http3RequestStreamInboundHandler handler, QuicStreamChannelFuture future,
                                                ChannelFuture enqueue) {
        this.address = address;
        this.handler = handler;
        this.streamChannelFuture = future;
        this.promise(future.getParentChannel().newPromise());
        this.channel(future.getParentChannel());
    }

    public static CreateQuicStreamChannelQueueCommand create(InetSocketAddress address,
                                                             Http3RequestStreamInboundHandler handler,
                                                             QuicStreamChannelFuture future,
                                                             ChannelFuture enqueue) {
        return new CreateQuicStreamChannelQueueCommand(address, handler, future, enqueue);
    }

    @Override
    public void doSend(ChannelHandlerContext ctx, ChannelPromise promise) {
    }

    @Override
    public void run(Channel channel) {
        System.out.println("执行到了 CreateQuicStreamChannelQueueCommand.run" + channel.getClass());
        // add CommandOutBoundHandler
        streamChannelFuture.thenRun(() -> {
            QuicChannel quicChannel = connectionCache.get(address.getHostName());
            Http3.newRequestStream(quicChannel, handler).addListener(future -> {
                if (future.isSuccess()) {
                    streamChannelFuture.complete((Channel) future.getNow());
                } else {
                    streamChannelFuture.completeExceptionally(future.cause());
                }
            });
        });

    }
}
