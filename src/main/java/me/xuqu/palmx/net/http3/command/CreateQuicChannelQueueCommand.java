package me.xuqu.palmx.net.http3.command;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.incubator.codec.http3.Http3ClientConnectionHandler;
import io.netty.incubator.codec.quic.QuicChannel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;

public class CreateQuicChannelQueueCommand extends QueuedCommand {

    public static final ConcurrentHashMap<String, QuicChannel> connectionCache = new ConcurrentHashMap<>();

    private final Channel parentChannel;

    private final InetSocketAddress socketAddress;

    private final QuicStreamChannelFuture streamChannelFuture;

    private CreateQuicChannelQueueCommand(
            InetSocketAddress socketAddress, QuicStreamChannelFuture future) {
        this.socketAddress = socketAddress;
        this.streamChannelFuture = future;
        this.parentChannel = future.getParentChannel();
        this.promise(future.getParentChannel().newPromise());
        this.channel(future.getParentChannel());
    }

    public static CreateQuicChannelQueueCommand create(
            InetSocketAddress socketAddress, QuicStreamChannelFuture future) {
        return new CreateQuicChannelQueueCommand(socketAddress, future);
    }

    @Override
    public void doSend(ChannelHandlerContext ctx, ChannelPromise promise) {
    }

    @Override
    public void run(Channel channel) {
        System.out.println("执行到了 CreateQuicChannelQueueCommand.run" + channel.getClass());
        String hostName = socketAddress.getHostName();
        QuicChannel quicChannel = connectionCache.get(hostName);
        // 连接缓存
        if (null == quicChannel || !quicChannel.isActive()) {
            QuicChannel.newBootstrap(parentChannel)
                    .handler(new Http3ClientConnectionHandler())
                    .remoteAddress(socketAddress)
                    .connect().addListener((GenericFutureListener<Future<QuicChannel>>) future -> {
                        if (future.isSuccess()) {
                            QuicChannel now = future.getNow();
                            connectionCache.putIfAbsent(hostName, now);
                            this.promise().setSuccess();
                        } else {
                            this.promise().setFailure(future.cause());
                        }
                    });

        } else {
            this.promise().setSuccess();
        }
    }
}
