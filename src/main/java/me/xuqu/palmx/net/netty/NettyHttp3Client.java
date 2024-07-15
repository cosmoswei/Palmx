package me.xuqu.palmx.net.netty;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.incubator.codec.http3.*;
import io.netty.incubator.codec.quic.*;
import io.netty.util.concurrent.DefaultPromise;
import lombok.extern.slf4j.Slf4j;
import me.xuqu.palmx.common.PalmxConfig;
import me.xuqu.palmx.exception.RpcInvocationException;
import me.xuqu.palmx.loadbalancer.LoadBalancerHolder;
import me.xuqu.palmx.net.AbstractPalmxClient;
import me.xuqu.palmx.net.RpcInvocation;
import me.xuqu.palmx.net.RpcMessage;
import me.xuqu.palmx.registry.ServiceRegistry;
import me.xuqu.palmx.registry.impl.ZookeeperServiceRegistry;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
public class NettyHttp3Client extends AbstractPalmxClient {

    Cache<String, QuicChannel> connectionCache = Caffeine.newBuilder()
            //过期时间
            .expireAfterWrite(500000, TimeUnit.MILLISECONDS)
            //最大容量
            .maximumSize(20)
            .build();

    NioEventLoopGroup group = null;

    @Override
    protected Object doSend(RpcMessage rpcMessage) {
        String serviceName = ((RpcInvocation) rpcMessage.getData()).getInterfaceName();
        ServiceRegistry serviceRegistry = new ZookeeperServiceRegistry();
        List<InetSocketAddress> socketAddresses = serviceRegistry.lookup(serviceName);
        // load balance
        InetSocketAddress socketAddress = LoadBalancerHolder.get().choose(socketAddresses, serviceName);
        try {
            QuicStreamChannel quicStreamChannel = getQuicStreamChannel(socketAddress);
            Http3HeadersFrame frame = new DefaultHttp3HeadersFrame();
            frame.headers().method("POST").path("/")
                    .authority(socketAddress.toString())
                    .scheme("https");
            quicStreamChannel.write(frame);
            ByteBuf encode = MessageCodecHelper.encode(rpcMessage);
            DefaultHttp3DataFrame defaultHttp3DataFrame = new DefaultHttp3DataFrame(encode);
            quicStreamChannel.writeAndFlush(defaultHttp3DataFrame);
            // 准备一个 Promise，并将其加入到 RpcResponsePacketHandler 的集合中，以该请求的序列化为键
            DefaultPromise<Object> promise = new DefaultPromise<>(quicStreamChannel.eventLoop());
            Http3RpcResponseHandler.map.put(rpcMessage.getSequenceId(), promise);
            promise.await();
            // 取出结果
            if (promise.isSuccess()) {
                Object result = promise.getNow();
                // log.debug("Send a packet[{}], get result = {}", rpcMessage, result);
                return result;
            } else {
                log.warn("Method invocation failed, with exception");
                promise.cause().printStackTrace();
                throw ((RpcInvocationException) promise.cause());
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RpcInvocationException("invoke error msg = " + e.getMessage());
        }
    }

    private ChannelHandler getChannelHandler() {
        QuicSslContext context = QuicSslContextBuilder.forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .applicationProtocols(Http3.supportedApplicationProtocols()).build();
        return Http3.newQuicClientCodecBuilder()
                .sslContext(context)
                .maxIdleTimeout(500000, TimeUnit.MILLISECONDS)
                .initialMaxData(10000000)
                .initialMaxStreamDataBidirectionalLocal(1000000)
                .initialMaxStreamDataBidirectionalRemote(1000000)
                .initialMaxStreamsBidirectional(PalmxConfig.getInitialMaxStreamsBidirectional())  // 设置最大并发双向流数
                .initialMaxStreamsUnidirectional(2000) // 设置最大并发单向流数
                .build();
    }

    private QuicStreamChannel getQuicStreamChannel(InetSocketAddress socketAddress) {
        String socketAddressString = socketAddress.toString();
        QuicChannel quicChannel = connectionCache.getIfPresent(socketAddressString);
        if (quicChannel == null) {
            quicChannel = getNewQuicChannel(socketAddress);
        }
        QuicStreamChannel quicStreamChannel;
        try {
            if (quicChannel.peerAllowedStreams(QuicStreamType.BIDIRECTIONAL) < 0) {
                quicChannel = getNewQuicChannel(socketAddress);
            }
            quicStreamChannel = Http3.newRequestStream(quicChannel, new Http3RpcResponseHandler()).sync().getNow();
        } catch (Exception e) {
//            e.printStackTrace();
            connectionCache.invalidate(socketAddressString);
            return getQuicStreamChannel(socketAddress);
        }
        return quicStreamChannel;
    }

    private QuicChannel getNewQuicChannel(InetSocketAddress socketAddress) {
        String socketAddressString = socketAddress.toString();
        QuicChannel quicChannel;
        try {
            if (this.group == null) {
                group = new NioEventLoopGroup(PalmxConfig.ioThreads());
            }
            ChannelHandler channelHandler = getChannelHandler();
            Bootstrap bs = new Bootstrap();
            Channel channel = bs.group(group)
                    .channel(NioDatagramChannel.class)
                    .handler(channelHandler)
                    .bind(0).sync().channel();
            quicChannel = QuicChannel.newBootstrap(channel)
                    .handler(new Http3ClientConnectionHandler())
                    .remoteAddress(socketAddress)
                    .connect()
                    .get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        connectionCache.put(socketAddressString, quicChannel);
        return quicChannel;
    }
}
