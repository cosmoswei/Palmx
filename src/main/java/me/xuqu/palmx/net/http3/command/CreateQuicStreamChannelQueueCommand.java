package me.xuqu.palmx.net.http3.command;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.incubator.codec.http3.Http3;
import io.netty.incubator.codec.http3.Http3RequestStreamInitializer;
import io.netty.incubator.codec.quic.QuicChannel;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import lombok.extern.slf4j.Slf4j;
import me.xuqu.palmx.net.http3.Http3RpcResponseHandler;

import java.net.InetSocketAddress;

import static me.xuqu.palmx.net.http3.command.CreateQuicChannelQueueCommand.connectionCache;

@Slf4j
public class CreateQuicStreamChannelQueueCommand extends QueueCommand {

    private final InetSocketAddress address;
    private final ChannelFuture quicChannelFuture;
    private final QuicStreamChannelPromise streamPromise;
    private final DefaultPromise<Channel> channelDefaultPromise;

    private CreateQuicStreamChannelQueueCommand(InetSocketAddress address, QuicStreamChannelPromise streamPromise,
                                                ChannelFuture quicChannelFuture, DefaultPromise<Channel> channelDefaultPromise) {
        this.address = address;
        this.streamPromise = streamPromise;
        this.setPromise(streamPromise.getParentChannel().newPromise());
        this.setChannel(streamPromise.getParentChannel());
        this.quicChannelFuture = quicChannelFuture;
        this.channelDefaultPromise = channelDefaultPromise;
    }

    public static CreateQuicStreamChannelQueueCommand create(InetSocketAddress address,
                                                             QuicStreamChannelPromise streamCommand,
                                                             ChannelFuture channelFuture,
                                                             DefaultPromise<Channel> channelDefaultPromise) {
        return new CreateQuicStreamChannelQueueCommand(address, streamCommand, channelFuture,channelDefaultPromise);
    }

    @Override
    public void doSend(ChannelHandlerContext ctx, ChannelPromise promise) {
    }

    @Override
    public void run(Channel channel) {
        log.info("第二步，创建 QuicStreamChannel");
        quicChannelFuture.addListener(future -> {
            if (future.isSuccess()) {
                QuicChannel quicChannel = connectionCache.get(address.getHostName());
                Http3RequestStreamInitializer initializer = new Http3RequestStreamInitializer() {
                    @Override
                    protected void initRequestStream(QuicStreamChannel ch) {
                        ch.pipeline()
                                .addLast(new Http3RpcResponseHandler())
                                .addLast(new CommandOutBoundHandler());
                    }
                };
                System.out.println("quicChannel = " + quicChannel.isActive());
                Http3.newRequestStream(quicChannel, initializer).addListener((GenericFutureListener<Future<QuicStreamChannel>>) quicStreamChannelFuture -> {
                    if (quicStreamChannelFuture.isSuccess()) {
                        log.info("QuicStreamChannel 创建成功！");
                        QuicStreamChannel quicStreamChannel = quicStreamChannelFuture.getNow();
                        channelDefaultPromise.setSuccess(quicStreamChannel);
                    } else {
                        log.info("QuicStreamChannel 创建失败！cause = {}", quicStreamChannelFuture.cause().getMessage());
                        channelDefaultPromise.setFailure(quicStreamChannelFuture.cause());
                    }
                });
            } else {
                log.info("quicChannelFuture 创建失败！cause = {}", quicChannelFuture.cause().getMessage());
                channelDefaultPromise.setFailure(streamPromise.cause());
            }
        });
    }
}
