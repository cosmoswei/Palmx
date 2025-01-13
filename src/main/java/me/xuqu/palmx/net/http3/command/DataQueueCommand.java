package me.xuqu.palmx.net.http3.command;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.incubator.codec.http3.DefaultHttp3DataFrame;

public class DataQueueCommand extends QuicStreamChannelCommand {

    private final DefaultHttp3DataFrame data;

    private final boolean endStream;

    private DataQueueCommand(
            QuicStreamChannelFuture streamChannelFuture, DefaultHttp3DataFrame data, boolean endStream) {
        super(streamChannelFuture);
        this.data = data;
        super.channel = streamChannelFuture.getParentChannel();
        this.endStream = endStream;
    }

    public static DataQueueCommand create(
            QuicStreamChannelFuture streamChannelFuture, DefaultHttp3DataFrame http3DataFrame, boolean endStream) {
        return new DataQueueCommand(streamChannelFuture, http3DataFrame, endStream);
    }

    @Override
    public void doSend(ChannelHandlerContext ctx, ChannelPromise promise) {
        System.out.println("执行到了 DataQueueCommand.run" + channel.getClass());
        ctx.write(data, promise);
    }

    public DefaultHttp3DataFrame getData() {
        return data;
    }

    public boolean isEndStream() {
        return endStream;
    }

    @Override
    public Channel channel() {
        return channel;
    }
}
