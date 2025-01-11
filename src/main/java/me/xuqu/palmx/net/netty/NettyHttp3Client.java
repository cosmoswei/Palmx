package me.xuqu.palmx.net.netty;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.incubator.codec.http3.*;
import io.netty.incubator.codec.quic.QuicChannel;
import io.netty.incubator.codec.quic.QuicSslContext;
import io.netty.incubator.codec.quic.QuicSslContextBuilder;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Future;
import lombok.extern.slf4j.Slf4j;
import me.xuqu.palmx.command.WriteQueue;
import me.xuqu.palmx.common.PalmxConfig;
import me.xuqu.palmx.exception.RpcInvocationException;
import me.xuqu.palmx.loadbalance.LoadBalanceHolder;
import me.xuqu.palmx.loadbalance.PalmxSocketAddress;
import me.xuqu.palmx.net.AbstractPalmxClient;
import me.xuqu.palmx.net.RpcMessage;
import me.xuqu.palmx.net.RpcRequest;
import me.xuqu.palmx.registry.ServiceRegistry;
import me.xuqu.palmx.registry.impl.ZookeeperServiceRegistry;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

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

        String serviceName = ((RpcRequest) rpcMessage.getData()).getInterfaceName();
        ServiceRegistry serviceRegistry = new ZookeeperServiceRegistry();
        List<PalmxSocketAddress> socketAddresses = serviceRegistry.lookup(serviceName);
        // load balance
        PalmxSocketAddress socketAddress = LoadBalanceHolder.get().choose(socketAddresses, serviceName);
        log.debug("ip =  {}'s QoS is {}", socketAddress.getAddress(), socketAddress.getQoSLevel());
        QuicStreamChannel quicStreamChannel;
        // 创建请求流
        Future<QuicStreamChannel> quicStreamChannelFuture = getQuicStreamChannelFuture(socketAddress);
        DefaultPromise<Object> resPromise = new DefaultPromise<>(channel.eventLoop());
        // 一定要在请求返回写入 resPromise 前设置，不然会报错。
        Http3RpcResponseHandler.map.put(rpcMessage.getSequenceId(), resPromise);
        // 发送信息
        Http3HeadersFrame http3HeadersFrame = getHttp3HeadersFrame(socketAddress);
        DefaultHttp3DataFrame defaultHttp3DataFrame = new DefaultHttp3DataFrame(MessageCodecHelper.encode(rpcMessage));
        try {
            quicStreamChannel = quicStreamChannelFuture.sync().getNow();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        quicStreamChannel.write(http3HeadersFrame);
        try {
            quicStreamChannel.writeAndFlush(defaultHttp3DataFrame)
                    .addListener(QuicStreamChannel.SHUTDOWN_OUTPUT).sync();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        try {
            // 同步等待结果
            resPromise.sync().get();
            // 取出结果
            if (resPromise.isSuccess()) {
                Object result = resPromise.getNow();
                log.debug("Send a packet[{}], get result = {}", rpcMessage, result);
                quicStreamChannel.closeFuture();
                return result;
            } else {
                throw resPromise.cause();
            }
        } catch (Throwable e) {
            // 远程调用的过程出现了异常
            log.warn("Method invocation failed, with exception");
            throw new RpcInvocationException(e.getMessage());
        }


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
