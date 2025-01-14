package me.xuqu.palmx.net.http3.command;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.incubator.codec.http3.Http3;
import io.netty.incubator.codec.http3.Http3RequestStreamInitializer;
import io.netty.incubator.codec.quic.QuicChannel;
import io.netty.incubator.codec.quic.QuicStreamChannel;
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
    private final QuicStreamChannelFuture streamChannelFuture;

    private CreateQuicStreamChannelQueueCommand(InetSocketAddress address, QuicStreamChannelFuture future,
                                                ChannelFuture quicChannelFuture) {
        this.address = address;
        this.streamChannelFuture = future;
        this.setPromise(future.getParentChannel().newPromise());
        this.setChannel(future.getParentChannel());
        this.quicChannelFuture = quicChannelFuture;
    }

    public static CreateQuicStreamChannelQueueCommand create(InetSocketAddress address,

                                                             QuicStreamChannelFuture future,
                                                             ChannelFuture enqueue) {
        return new CreateQuicStreamChannelQueueCommand(address, future, enqueue);
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
                Http3.newRequestStream(quicChannel, initializer).addListener((GenericFutureListener<Future<QuicStreamChannel>>) quicStreamChannelFuture -> {
                    if (quicStreamChannelFuture.isSuccess()) {
                        log.info("QuicStreamChannel 创建成功！");
                        QuicStreamChannel quicStreamChannel = quicStreamChannelFuture.getNow();
                        streamChannelFuture.complete(quicStreamChannel);
                    } else {
                        log.info("QuicStreamChannel 创建失败！cause = {}", quicStreamChannelFuture.cause().getMessage());
                        streamChannelFuture.completeExceptionally(quicStreamChannelFuture.cause());
                    }
                });
            } else {
                log.info("QuicStreamChannel 创建失败！cause = {}", streamChannelFuture.cause().getMessage());
                streamChannelFuture.completeExceptionally(streamChannelFuture.cause());
            }

        });
    }
}
