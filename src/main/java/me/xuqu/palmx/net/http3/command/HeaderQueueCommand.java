package me.xuqu.palmx.net.http3.command;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.incubator.codec.http3.DefaultHttp3HeadersFrame;
import io.netty.incubator.codec.http3.Http3Headers;

public class HeaderQueueCommand extends QuicStreamChannelCommand {

    private final Http3Headers headers;

    private final boolean endStream;

    private HeaderQueueCommand(QuicStreamChannelFuture streamChannelFuture, Http3Headers headers, boolean endStream) {
        super(streamChannelFuture);
        this.headers = headers;
        this.endStream = endStream;
    }

    public static HeaderQueueCommand createHeaders(
            QuicStreamChannelFuture streamChannelFuture, Http3Headers headers) {
        return new HeaderQueueCommand(streamChannelFuture, headers, false);
    }

    public static HeaderQueueCommand createHeaders(
            QuicStreamChannelFuture streamChannelFuture, Http3Headers headers, boolean endStream) {
        return new HeaderQueueCommand(streamChannelFuture, headers, endStream);
    }

    public Http3Headers getHeaders() {
        return headers;
    }

    public boolean isEndStream() {
        return endStream;
    }

    @Override
    public void doSend(ChannelHandlerContext ctx, ChannelPromise promise) {
        ctx.write(new DefaultHttp3HeadersFrame(headers), promise);
    }
}
