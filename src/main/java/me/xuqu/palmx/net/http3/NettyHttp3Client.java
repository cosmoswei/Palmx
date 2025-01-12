package me.xuqu.palmx.net.http3;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.incubator.codec.http3.*;
import io.netty.incubator.codec.quic.QuicChannel;
import io.netty.incubator.codec.quic.QuicSslContext;
import io.netty.incubator.codec.quic.QuicSslContextBuilder;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import io.netty.util.concurrent.*;
import lombok.extern.slf4j.Slf4j;
import me.xuqu.palmx.common.PalmxConfig;
import me.xuqu.palmx.exception.RpcInvocationException;
import me.xuqu.palmx.loadbalance.LoadBalanceHolder;
import me.xuqu.palmx.loadbalance.PalmxSocketAddress;
import me.xuqu.palmx.net.AbstractPalmxClient;
import me.xuqu.palmx.net.DatagramChannelHandler;
import me.xuqu.palmx.net.RpcMessage;
import me.xuqu.palmx.net.RpcRequest;
import me.xuqu.palmx.registry.ServiceRegistry;
import me.xuqu.palmx.registry.impl.ZookeeperServiceRegistry;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class NettyHttp3Client extends AbstractPalmxClient {

    private final Cache<String, QuicChannel> connectionCache = Caffeine.newBuilder()
            //过期时间
            .expireAfterWrite(10, TimeUnit.MINUTES)
            //最大容量
            .maximumSize(20)
            .build();

    private final NioEventLoopGroup group = new NioEventLoopGroup(PalmxConfig.ioThreads());

    private Channel channel = null;

    @Override
    public void shutdown() {
        group.shutdownGracefully();
    }

    @Override
    protected Object doSend(RpcMessage rpcMessage) {
        CompletableFuture<Object> objectCompletableFuture = new CompletableFuture<>();

        String serviceName = ((RpcRequest) rpcMessage.getData()).getInterfaceName();
        ServiceRegistry serviceRegistry = new ZookeeperServiceRegistry();
        List<PalmxSocketAddress> socketAddresses = serviceRegistry.lookup(serviceName);

        // load balance
        PalmxSocketAddress socketAddress = LoadBalanceHolder.get().choose(socketAddresses, serviceName);
        log.debug("ip =  {}'s QoS is {}", socketAddress.getAddress(), socketAddress.getQoSLevel());

        // 获取请求流
        Future<QuicStreamChannel> quicStreamChannelFuture = getQuicStreamChannelFuture(socketAddress);
        DefaultPromise<Object> resPromise = new DefaultPromise<>(channel.eventLoop());

        // 在请求返回前设置 resPromise
        Http3RpcResponseHandler.map.put(rpcMessage.getSequenceId(), resPromise);

        // 准备HTTP3请求数据
        Http3HeadersFrame http3HeadersFrame = getHttp3HeadersFrame(socketAddress);
        DefaultHttp3DataFrame defaultHttp3DataFrame = new DefaultHttp3DataFrame(MessageCodecHelper.encode(rpcMessage));

        // 添加监听器，处理非阻塞行为
        quicStreamChannelFuture.addListener((GenericFutureListener<Future<QuicStreamChannel>>) future -> {
            if (future.isSuccess()) {
                QuicStreamChannel quicStreamChannel = future.getNow();

                // 写入HTTP3 headers
                quicStreamChannel.write(http3HeadersFrame);

                // 异步写入并刷新数据帧
                quicStreamChannel.writeAndFlush(defaultHttp3DataFrame)
                        .addListener(QuicStreamChannel.SHUTDOWN_OUTPUT)
                        .addListener((GenericFutureListener<Future<Void>>) writeFuture -> {
                            if (writeFuture.isSuccess()) {
                                // 等待响应并通过Promise处理结果
                                resPromise.addListener(resultFuture -> {
                                    if (resultFuture.isSuccess()) {
                                        Object result = resultFuture.getNow();
                                        log.debug("Send a packet[{}], get result = {}", rpcMessage, result);
                                        quicStreamChannel.closeFuture();

                                        // 将结果传递给 CompletableFuture
                                        objectCompletableFuture.complete(result);
                                    } else {
                                        // 处理失败情况
                                        log.warn("Remote invocation failed, cause: {}", resultFuture.cause());
                                        objectCompletableFuture.completeExceptionally(resultFuture.cause());
                                    }
                                });
                            } else {
                                log.warn("Write operation failed, cause: {}", writeFuture.cause());
                                objectCompletableFuture.completeExceptionally(writeFuture.cause());
                            }
                        });
            } else {
                log.warn("Failed to get QuicStreamChannel, cause: {}", future.cause());
                resPromise.setFailure(future.cause());
                objectCompletableFuture.completeExceptionally(future.cause());
            }
        });
        Object o;
        try {
            o = objectCompletableFuture.get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return o;
    }

    private void transportException(Throwable cause) {
        // todo 处理异常
    }

    private Http3HeadersFrame getHttp3HeadersFrame(PalmxSocketAddress socketAddress) {
        Http3HeadersFrame frame = new DefaultHttp3HeadersFrame();
        frame.headers().method("POST").path("/")
                .authority(socketAddress.toString())
                .scheme("https");
        return frame;
    }

    private Future<QuicStreamChannel> getQuicStreamChannelFuture(InetSocketAddress socketAddress) {
        QuicChannel quicChannel = getQuicChannel(socketAddress);
        try {
            return Http3.newRequestStream(quicChannel, new Http3RpcResponseHandler());
        } catch (Exception e) {
            log.error("get quic stream channel error, msg = {}", e.getMessage());
            connectionCache.invalidate(socketAddress.getHostString());
            return getQuicStreamChannelFuture(socketAddress);
        }
    }


    private QuicChannel newQuicChannel(InetSocketAddress socketAddress) {
        QuicChannel quicChannel;
        try {
            quicChannel = QuicChannel.newBootstrap(getChannel())
                    .handler(new Http3ClientConnectionHandler())
                    .remoteAddress(socketAddress)
                    .connect().get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
        return quicChannel;
    }

    private QuicChannel getQuicChannel(InetSocketAddress socketAddress) {
        String hostname = socketAddress.getHostString();
        QuicChannel quicChannel = connectionCache.getIfPresent(hostname);
        // 去缓冲里查找
        if (null == quicChannel) {
            quicChannel = newQuicChannel(socketAddress);
            connectionCache.put(hostname, quicChannel);
            return quicChannel;
        }

        // 无效连接
        if (!quicChannel.isActive()) {
            log.info("quicChannel isClosed");
            connectionCache.invalidate(hostname);
            quicChannel = newQuicChannel(socketAddress);
            connectionCache.put(hostname, quicChannel);
        }

        // 流数量限制，数量跟由服务端的 initialMaxStreamsBidirectional 配置
//        long allowedStreams = quicChannel.peerAllowedStreams(QuicStreamType.BIDIRECTIONAL);
//        if (allowedStreams <= 0) {
//            // 流释放时，不会恢复（增加）
//            connectionCache.invalidate(hostname);
//            quicChannel = newQuicChannel(socketAddress);
//            connectionCache.put(hostname, quicChannel);
//        }
        return quicChannel;
    }

    private Channel getChannel() {
        if (null != channel) {
            return channel;
        }
        QuicSslContext context = QuicSslContextBuilder.forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .applicationProtocols(Http3.supportedApplicationProtocols()).build();
        ChannelHandler channelHandler = Http3.newQuicClientCodecBuilder()
                .sslContext(context)
                .maxIdleTimeout(5000, TimeUnit.MILLISECONDS)
                .initialMaxData(10000000)
                .initialMaxStreamDataBidirectionalLocal(1000000)
                .initialMaxStreamDataBidirectionalRemote(1000000)

                .build();
        Bootstrap bs = new Bootstrap();
        try {
            channel = bs.group(group)
                    .channel(DatagramChannelHandler.getChannelClass())
                    .handler(channelHandler)
                    .bind(0).sync().channel();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return channel;

    }
}
