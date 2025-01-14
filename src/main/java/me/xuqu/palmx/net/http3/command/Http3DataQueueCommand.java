package me.xuqu.palmx.net.http3.command;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.incubator.codec.http3.DefaultHttp3DataFrame;

public class Http3DataQueueCommand extends Http3FrameQueueCommand {

    private final DefaultHttp3DataFrame data;

    private Http3DataQueueCommand(
            QuicStreamChannelFuture streamChannelFuture, DefaultHttp3DataFrame data) {
        super(streamChannelFuture);
        this.data = data;
        super.channel = streamChannelFuture.getParentChannel();
    }

    public static Http3DataQueueCommand create(
            QuicStreamChannelFuture streamChannelFuture, DefaultHttp3DataFrame http3DataFrame) {
        return new Http3DataQueueCommand(streamChannelFuture, http3DataFrame);
    }

    @Override
    public void doSend(ChannelHandlerContext ctx, ChannelPromise promise) {
        System.out.println("执行到了 DataQueueCommand.run" + channel.getClass());
        ctx.channel().write(data, promise);
    }

    public DefaultHttp3DataFrame getData() {
        return data;
    }
}
