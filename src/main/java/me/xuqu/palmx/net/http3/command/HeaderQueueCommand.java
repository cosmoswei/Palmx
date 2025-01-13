package me.xuqu.palmx.net.http3.command;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.incubator.codec.http3.DefaultHttp3HeadersFrame;

public class HeaderQueueCommand extends QuicStreamChannelCommand {

    private final DefaultHttp3HeadersFrame headers;

    private final boolean endStream;

    private HeaderQueueCommand(QuicStreamChannelFuture streamChannelFuture, DefaultHttp3HeadersFrame headers, boolean endStream) {
        super(streamChannelFuture);
        this.headers = headers;
        this.endStream = endStream;
        super.channel = streamChannelFuture.getParentChannel();
    }

    public static HeaderQueueCommand createHeader(
            QuicStreamChannelFuture streamChannelFuture, DefaultHttp3HeadersFrame headers) {
        return new HeaderQueueCommand(streamChannelFuture, headers, false);
    }

    public DefaultHttp3HeadersFrame getHeaders() {
        return headers;
    }

    public boolean isEndStream() {
        return endStream;
    }

    @Override
    public void doSend(ChannelHandlerContext ctx, ChannelPromise promise) {
        System.out.println("执行到了 HeaderQueueCommand.run" + channel.getClass());
        ctx.write(headers, promise);
    }

    @Override
    public Channel channel() {
        return channel;
    }
}
