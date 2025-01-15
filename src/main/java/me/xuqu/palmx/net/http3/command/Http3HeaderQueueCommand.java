package me.xuqu.palmx.net.http3.command;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.incubator.codec.http3.DefaultHttp3HeadersFrame;
import io.netty.util.concurrent.DefaultPromise;

public class Http3HeaderQueueCommand extends Http3FrameQueueCommand {

    private final DefaultHttp3HeadersFrame headers;

    private final DefaultPromise<Channel> channelDefaultPromise;

    private Http3HeaderQueueCommand(QuicStreamChannelPromise streamChannelFuture, DefaultHttp3HeadersFrame headers,
                                    DefaultPromise<Channel> channelDefaultPromise) {
        super(streamChannelFuture);
        this.headers = headers;
        this.channelDefaultPromise = channelDefaultPromise;
        super.channel = streamChannelFuture.getParentChannel();
        this.setPromise(streamChannelFuture.getParentChannel().newPromise());
    }

    public static Http3HeaderQueueCommand createHeader(
            QuicStreamChannelPromise streamChannelFuture, DefaultHttp3HeadersFrame headers, DefaultPromise<Channel> channelDefaultPromise) {
        return new Http3HeaderQueueCommand(streamChannelFuture, headers, channelDefaultPromise);
    }

    @Override
    public void doSend(ChannelHandlerContext ctx, ChannelPromise promise) {
        channelDefaultPromise.addListener(future -> {
            Channel now = (Channel) future.getNow();
            now.write(headers, promise);
        });

    }

    @Override
    public Channel getChannel() {
        return super.channel;
    }

    @Override
    public void run(Channel channel) {
        channelDefaultPromise.addListener(future -> {
                    Channel now = channelDefaultPromise.getNow();
                    now.write(this).addListener(future1 -> {
                        if (future.isSuccess()) {
                            this.getPromise().setSuccess();
                        } else {
                            this.getPromise().setFailure(future.cause());
                        }
                    });
                }
        );
    }
}
