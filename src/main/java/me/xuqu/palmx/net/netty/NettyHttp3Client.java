package me.xuqu.palmx.net.netty;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.incubator.codec.http3.*;
import io.netty.incubator.codec.quic.*;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import lombok.extern.slf4j.Slf4j;
import me.xuqu.palmx.command.ClientChannelFuture;
import me.xuqu.palmx.command.WriteQueue;
import me.xuqu.palmx.common.PalmxConfig;
import me.xuqu.palmx.exception.RpcInvocationException;
import me.xuqu.palmx.loadbalance.LoadBalanceHolder;
import me.xuqu.palmx.loadbalance.PalmxSocketAddress;
import me.xuqu.palmx.net.AbstractPalmxClient;
import me.xuqu.palmx.net.RpcInvocation;
import me.xuqu.palmx.net.RpcMessage;
import me.xuqu.palmx.registry.ServiceRegistry;
import me.xuqu.palmx.registry.impl.ZookeeperServiceRegistry;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class NettyHttp3Client extends AbstractPalmxClient {

    private final WriteQueue writeQueue = new WriteQueue(256);

    private final Cache<String, QuicChannel> connectionCache = Caffeine.newBuilder()
            //过期时间
            .expireAfterWrite(10, TimeUnit.MINUTES)
            //最大容量
            .maximumSize(20)
            .build();

    private NioEventLoopGroup group = null;

    private Channel channel = null;

    @Override
    public void shutdown() {
        group.shutdownGracefully();
    }

    @Override
    protected ClientChannelFuture doSend(RpcMessage rpcMessage) {
        String serviceName = ((RpcInvocation) rpcMessage.getData()).getInterfaceName();
        ServiceRegistry serviceRegistry = new ZookeeperServiceRegistry();
        List<PalmxSocketAddress> socketAddresses = serviceRegistry.lookup(serviceName);
        // load balance
        PalmxSocketAddress socketAddress = LoadBalanceHolder.get().choose(socketAddresses, serviceName);
        log.debug("ip =  {}'s QoS is {}", socketAddress.getAddress(), socketAddress.getQoSLevel());
        try {
            DefaultPromise<Object> promise = new DefaultPromise<>(channel.eventLoop());
            Future<QuicStreamChannel> future = getQuicStreamChannelFuture(socketAddress);
            AtomicReference<ClientChannelFuture> res = new AtomicReference<>();
            // 第一个阻塞
            future.addListener((GenericFutureListener<Future<QuicStreamChannel>>) future1 -> {
                if (!future1.isSuccess()) {
                    transportException(future1.cause());
                }
                // 发送信息
                Http3HeadersFrame http3HeadersFrame = gethttp3HeadersFrame(socketAddress);
                DefaultHttp3DataFrame defaultHttp3DataFrame = new DefaultHttp3DataFrame(MessageCodecHelper.encode(rpcMessage));
                QuicStreamChannel quicStreamChannel = future1.getNow();
                quicStreamChannel.write(http3HeadersFrame);
                quicStreamChannel.writeAndFlush(defaultHttp3DataFrame);

                // 返回的信息
                Http3RpcResponseHandler.map.put(rpcMessage.getSequenceId(), promise);
                // 第二个阻塞
                res.set(new ClientChannelFuture(promise, future1));
            });
            return res.get();
        } catch (Exception e) {
            log.error("send error, msg = {}", e.getMessage());
            throw new RpcInvocationException("send error, msg = " + e.getMessage());
        }
    }

    private void transportException(Throwable cause) {
        // todo 处理异常
    }

    private Http3HeadersFrame gethttp3HeadersFrame(PalmxSocketAddress socketAddress) {
        Http3HeadersFrame frame = new DefaultHttp3HeadersFrame();
        frame.headers().method("POST").path("/")
                .authority(socketAddress.toString())
                .scheme("https");
        return frame;
    }

    private Future<QuicStreamChannel> getQuicStreamChannelFuture(InetSocketAddress socketAddress) {
        String hostname = socketAddress.getHostString();
        QuicChannel quicChannel = connectionCache.getIfPresent(hostname);
        if (quicChannel == null || !quicChannel.isActive()) {
            quicChannel = getNewQuicChannel(socketAddress);
        }
        // 数量跟由服务端的 initialMaxStreamsBidirectional 配置
        long allowedStreams = quicChannel.peerAllowedStreams(QuicStreamType.BIDIRECTIONAL);
        if (allowedStreams <= 0) {
            log.info("allowed Stream is depleted，curr = {} ", allowedStreams);
            quicChannel = getNewQuicChannel(socketAddress);
        }
        try {
            return Http3.newRequestStream(quicChannel, new Http3RpcResponseHandler());
        } catch (Exception e) {
            log.error("get quic stream channel error, msg = {}", e.getMessage());
            connectionCache.invalidate(hostname);
            return getQuicStreamChannelFuture(socketAddress);
        }
    }


    private QuicChannel getNewQuicChannel(InetSocketAddress socketAddress) {
        String hostname = socketAddress.getHostString();
        QuicChannel quicChannel;
        try {
            if (this.group == null) {
                group = new NioEventLoopGroup(PalmxConfig.ioThreads());
            }
            quicChannel = QuicChannel.newBootstrap(getChannel())
                    .handler(new Http3ClientConnectionHandler())
                    .remoteAddress(socketAddress)
                    .connect()
                    .get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        connectionCache.put(hostname, quicChannel);
        return quicChannel;
    }

    private Channel getChannel() throws InterruptedException {
        if (null != channel) {
            return channel;
        }
        QuicSslContext context = QuicSslContextBuilder.forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .applicationProtocols(Http3.supportedApplicationProtocols()).build();
        ChannelHandler channelHandler = Http3.newQuicClientCodecBuilder()
                .sslContext(context)
                .maxIdleTimeout(500000, TimeUnit.MILLISECONDS)
                .initialMaxData(10000000)
                .initialMaxStreamDataBidirectionalLocal(1000000)
                .initialMaxStreamDataBidirectionalRemote(1000000)
                .initialMaxStreamsBidirectional(PalmxConfig.getInitialMaxStreamsBidirectional())  // 设置最大并发双向流数
                .initialMaxStreamsUnidirectional(200) // 设置最大并发单向流数
                .build();
        Bootstrap bs = new Bootstrap();
        channel = bs.group(group)
                .channel(DatagramChannelHandler.getChannelClass())
                .handler(channelHandler)
                .bind(0).sync().channel();
        return channel;

    }
}
