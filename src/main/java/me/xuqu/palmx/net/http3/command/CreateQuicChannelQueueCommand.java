package me.xuqu.palmx.net.http3.command;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.incubator.codec.http3.Http3ClientConnectionHandler;
import io.netty.incubator.codec.quic.QuicChannel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class CreateQuicChannelQueueCommand extends QueueCommand {

    public static final ConcurrentHashMap<String, QuicChannel> connectionCache = new ConcurrentHashMap<>();

    private final Channel parentChannel;

    private final InetSocketAddress socketAddress;

    private CreateQuicChannelQueueCommand(
            InetSocketAddress socketAddress, QuicStreamChannelPromise future) {
        this.socketAddress = socketAddress;
        this.parentChannel = future.getParentChannel();
        super.setPromise(future.getParentChannel().newPromise());
        this.setChannel(future.getParentChannel());
    }

    public static CreateQuicChannelQueueCommand create(
            InetSocketAddress socketAddress, QuicStreamChannelPromise future) {
        return new CreateQuicChannelQueueCommand(socketAddress, future);
    }

    @Override
    public void doSend(ChannelHandlerContext ctx, ChannelPromise promise) {
    }

    @Override
    public void run(Channel channel) {
        log.info("第一步，创建 QuicChannel");
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
                            System.out.println("QuicChannel 创建成功！" + now);
                            connectionCache.put(hostName, now);
                            ChannelPromise channelPromise = this.getPromise();
                            channelPromise.setSuccess();
                        } else {
                            this.getPromise().setFailure(future.cause());
                        }
                    });
        } else {
            this.getPromise().setSuccess();
        }
    }
}
