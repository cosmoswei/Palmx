package me.xuqu.palmx.net.http3.command;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.incubator.codec.http3.DefaultHttp3HeadersFrame;

import java.util.concurrent.ExecutionException;

public class Http3HeaderQueueCommand extends Http3FrameQueueCommand {

    private final DefaultHttp3HeadersFrame headers;

    private Http3HeaderQueueCommand(QuicStreamChannelFuture streamChannelFuture, DefaultHttp3HeadersFrame headers) {
        super(streamChannelFuture);
        this.headers = headers;
        super.channel = streamChannelFuture.getParentChannel();
        this.setPromise(streamChannelFuture.getParentChannel().newPromise());
    }

    public static Http3HeaderQueueCommand createHeader(
            QuicStreamChannelFuture streamChannelFuture, DefaultHttp3HeadersFrame headers) {
        return new Http3HeaderQueueCommand(streamChannelFuture, headers);
    }

    public DefaultHttp3HeadersFrame getHeaders() {
        return headers;
    }

    @Override
    public void doSend(ChannelHandlerContext ctx, ChannelPromise promise) {
        System.out.println("执行到了 HeaderQueueCommand.run" + channel.getClass());
        try {
            Channel channel = quicStreamChannelFuture.get();
            channel.write(headers, promise);
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run(Channel channel) {
        if (channel == null) {
           return;
        }
        ChannelPromise promise = this.getPromise();
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
}
