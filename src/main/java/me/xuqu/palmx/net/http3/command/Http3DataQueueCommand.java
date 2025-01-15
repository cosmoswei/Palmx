package me.xuqu.palmx.net.http3.command;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.incubator.codec.http3.DefaultHttp3DataFrame;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import io.netty.util.concurrent.DefaultPromise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Http3DataQueueCommand extends Http3FrameQueueCommand {

    private static final Logger log = LoggerFactory.getLogger(Http3DataQueueCommand.class);
    private final DefaultHttp3DataFrame data;
    private final ChannelFuture headerFuture;
    private final DefaultPromise<Channel> channelDefaultPromise;

    private Http3DataQueueCommand(
            QuicStreamChannelPromise streamChannelFuture, DefaultHttp3DataFrame data, ChannelFuture headerFuture, DefaultPromise<Channel> channelDefaultPromise) {
        super(streamChannelFuture);
        this.data = data;
        this.headerFuture = headerFuture;
        this.channelDefaultPromise = channelDefaultPromise;
        super.channel = streamChannelFuture.getParentChannel();
    }

    public static Http3DataQueueCommand create(
            QuicStreamChannelPromise streamChannelFuture, DefaultHttp3DataFrame http3DataFrame, ChannelFuture headerFuture, DefaultPromise<Channel> channelDefaultPromise) {
        return new Http3DataQueueCommand(streamChannelFuture, http3DataFrame, headerFuture, channelDefaultPromise);
    }

    @Override
    public Channel getChannel() {
        return super.channel;
    }

    @Override
    public void doSend(ChannelHandlerContext ctx, ChannelPromise promise) {
        headerFuture.addListener(future -> {
            if (future.isSuccess()) {
                QuicStreamChannel quicStreamChannel = (QuicStreamChannel) channelDefaultPromise.getNow();
                quicStreamChannel.write(data, promise).addListener(QuicStreamChannel.SHUTDOWN_OUTPUT);
            }
        });
    }

    @Override
    public void run(Channel channel) {
        headerFuture.addListener(future -> {
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
