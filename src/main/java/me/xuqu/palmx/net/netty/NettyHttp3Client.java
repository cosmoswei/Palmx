package me.xuqu.palmx.net.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.incubator.codec.http3.*;
import io.netty.incubator.codec.quic.QuicChannel;
import io.netty.incubator.codec.quic.QuicSslContext;
import io.netty.incubator.codec.quic.QuicSslContextBuilder;
import io.netty.incubator.codec.quic.QuicStreamChannel;
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
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Slf4j
public class NettyHttp3Client extends AbstractPalmxClient {

    private final Map<String, QuicChannel> connections = new ConcurrentHashMap<>();

    NioEventLoopGroup group = null;

    @Override
    protected Object doSend(RpcMessage rpcMessage) {

        String serviceName = ((RpcInvocation) rpcMessage.getData()).getInterfaceName();
        ServiceRegistry serviceRegistry = new ZookeeperServiceRegistry();
        List<InetSocketAddress> socketAddresses = serviceRegistry.lookup(serviceName);
        // load balance
        InetSocketAddress socketAddress = LoadBalancerHolder.get().choose(socketAddresses, serviceName);

        try {
            QuicChannel quicChannel = getQuicChannel(socketAddress);
            QuicStreamChannel streamChannel = Http3.newRequestStream(quicChannel, new Http3RpcResponseHandler()).sync().getNow();
            Http3HeadersFrame frame = new DefaultHttp3HeadersFrame();
            frame.headers().method("POST").path("/")
                    .authority(socketAddress.toString())
                    .scheme("https");
            streamChannel.write(frame);
            String str = MessageCodecHelper.encode2String(rpcMessage);
            DefaultHttp3DataFrame defaultHttp3DataFrame = new DefaultHttp3DataFrame(Unpooled.wrappedBuffer((str)
                    .getBytes(StandardCharsets.UTF_8)));
            streamChannel.writeAndFlush(defaultHttp3DataFrame);
            // 准备一个 Promise，并将其加入到 RpcResponsePacketHandler 的集合中，以该请求的序列化为键
            DefaultPromise<Object> promise = new DefaultPromise<>(quicChannel.eventLoop());
            Http3RpcResponseHandler.map.put(rpcMessage.getSequenceId(), promise);
            promise.await();
            // 取出结果
            if (promise.isSuccess()) {
                Object result = promise.getNow();
                log.debug("Send a packet[{}], get result = {}", rpcMessage, result);
                quicChannel.close().sync();
                return result;
            } else {
                log.warn("Method invocation failed, with exception");
                promise.cause().printStackTrace();
                throw ((RpcInvocationException) promise.cause());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private Channel getChannel(NioEventLoopGroup group) throws InterruptedException {
        QuicSslContext context = QuicSslContextBuilder.forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .applicationProtocols(Http3.supportedApplicationProtocols()).build();
        ChannelHandler codec = Http3.newQuicClientCodecBuilder()
                .sslContext(context)
                .maxIdleTimeout(500000, TimeUnit.MILLISECONDS)
                .initialMaxData(10000000)
                .initialMaxStreamDataBidirectionalLocal(1000000)
                .build();

        Bootstrap bs = new Bootstrap();
        return bs.group(group)
                .channel(NioDatagramChannel.class)
                .handler(codec)
                .bind(0).sync().channel();
    }

    private QuicChannel getQuicChannel(InetSocketAddress socketAddress) throws InterruptedException {
        String socketAddressString = socketAddress.toString();
        QuicChannel quicChannel = connections.get(socketAddressString);
        if (quicChannel != null && quicChannel.isActive()) {
            return quicChannel;
        } else {
            if (quicChannel != null) {
                if (!quicChannel.isActive()) {
                    quicChannel.close();
                }
            }
            try {
                if (group == null) {
                    group = new NioEventLoopGroup(PalmxConfig.ioThreads());
                }
                Channel channel = getChannel(group);
                quicChannel = QuicChannel.newBootstrap(channel)
                        .handler(new Http3ClientConnectionHandler())
                        .remoteAddress(socketAddress)
                        .connect()
                        .get();
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
            connections.put(socketAddressString, quicChannel);
        }
        return quicChannel;
    }
}
