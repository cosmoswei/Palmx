package me.xuqu.palmx.net.http3.command;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;

public class EndStreamQueueCommand extends QuicStreamChannelCommand {

    public EndStreamQueueCommand(QuicStreamChannelFuture streamChannelFuture) {
        super(streamChannelFuture);
    }

    public static EndStreamQueueCommand create(QuicStreamChannelFuture streamChannelFuture) {
        return new EndStreamQueueCommand(streamChannelFuture);
    }

    @Override
    public void doSend(ChannelHandlerContext ctx, ChannelPromise promise) {
        ctx.write(new DefaultHttp2DataFrame(true), promise);
    }
}
