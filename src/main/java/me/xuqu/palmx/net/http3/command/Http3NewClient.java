package me.xuqu.palmx.net.http3.command;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.incubator.codec.http3.DefaultHttp3DataFrame;
import io.netty.incubator.codec.http3.DefaultHttp3HeadersFrame;
import io.netty.incubator.codec.http3.Http3;
import io.netty.incubator.codec.quic.QuicSslContext;
import io.netty.incubator.codec.quic.QuicSslContextBuilder;
import io.netty.util.concurrent.DefaultPromise;
import lombok.extern.slf4j.Slf4j;
import me.xuqu.palmx.common.PalmxConfig;
import me.xuqu.palmx.exception.RpcInvocationException;
import me.xuqu.palmx.loadbalance.LoadBalanceHolder;
import me.xuqu.palmx.loadbalance.PalmxSocketAddress;
import me.xuqu.palmx.net.AbstractPalmxClient;
import me.xuqu.palmx.net.DatagramChannelHandler;
import me.xuqu.palmx.net.RpcMessage;
import me.xuqu.palmx.net.RpcRequest;
import me.xuqu.palmx.net.http3.Http3RpcResponseHandler;
import me.xuqu.palmx.net.http3.MessageCodecHelper;
import me.xuqu.palmx.net.http3.queue.PalmxWriteQueue;
import me.xuqu.palmx.net.netty.RpcResponseHandler;
import me.xuqu.palmx.registry.ServiceRegistry;
import me.xuqu.palmx.registry.impl.ZookeeperServiceRegistry;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Slf4j
public class Http3NewClient extends AbstractPalmxClient {

    private final PalmxWriteQueue palmxWriteQueue = new PalmxWriteQueue(256);

    private final NioEventLoopGroup nioEventLoopGroup = new NioEventLoopGroup(PalmxConfig.ioThreads());

    private Channel datagramChannel = null;

    private final ServiceRegistry serviceRegistry = new ZookeeperServiceRegistry();

    @Override
    public void shutdown() {
        nioEventLoopGroup.shutdownGracefully();
    }

    @Override
    protected Object doSend(RpcMessage rpcMessage) {
        // service name
        String serviceName = ((RpcRequest) rpcMessage.getData()).getInterfaceName();

        // service discover
        List<PalmxSocketAddress> socketAddresses = serviceRegistry.mock(serviceName);

        // Load balance
        PalmxSocketAddress socketAddress = LoadBalanceHolder.get().choose(socketAddresses, serviceName);

        // 获取 channel
        datagramChannel = getDatagramChannel();

        // 准备一个 Promise，并将其加入到 RpcResponsePacketHandler 的集合中，以该请求的序列化为键
        DefaultPromise<Object> promise = new DefaultPromise<>(datagramChannel.eventLoop());

        // 响应结果
        Http3RpcResponseHandler.map.put(rpcMessage.getSequenceId(), promise);

        // 获取 quicStreamChannelFuture
        QuicStreamChannelPromise streamPromise = new QuicStreamChannelPromise(datagramChannel);
        DefaultPromise<Channel> channelDefaultPromise = new DefaultPromise<>(datagramChannel.eventLoop());
        // 获取 quicChannel (连接)
        CreateQuicChannelQueueCommand quicChannel = CreateQuicChannelQueueCommand.create(socketAddress,
                streamPromise);
        ChannelFuture channelFuture = palmxWriteQueue.enqueue(quicChannel);

        // 获取 quicStreamChannel
        CreateQuicStreamChannelQueueCommand quicStreamChannel = CreateQuicStreamChannelQueueCommand.create(socketAddress,
                streamPromise, channelFuture,channelDefaultPromise);
        ChannelFuture streamFuture = palmxWriteQueue.enqueue(quicStreamChannel);

        // 写入头
        Http3HeaderQueueCommand header = Http3HeaderQueueCommand.createHeader(streamPromise, getHttp3HeadersFrame(socketAddress),channelDefaultPromise);
        ChannelFuture headerFuture = palmxWriteQueue.enqueueFuture(header, datagramChannel.eventLoop());

        // 写入body
        DefaultHttp3DataFrame defaultHttp3DataFrame = new DefaultHttp3DataFrame(MessageCodecHelper.encode(rpcMessage));
        Http3DataQueueCommand body = Http3DataQueueCommand.create(streamPromise, defaultHttp3DataFrame, headerFuture,channelDefaultPromise);
        palmxWriteQueue.enqueue(body, datagramChannel.eventLoop());

        try {
            System.out.println("================= = " + body);
            // 等待命令完成
            promise.sync().get();
            if (promise.isSuccess()) {
                System.out.println("================= = " + body);
                Object result = promise.getNow(); // 取出结果
                log.debug("Send a packet [{}], get result = {}", rpcMessage, result);
                return result;
            } else {
                throw promise.cause();
            }
        } catch (Throwable e) {
            log.warn("Method invocation failed, with exception", e);
            throw new RpcInvocationException(e.getMessage());
        }
    }


    public static void main(String[] args) {
        Http3NewClient http3NewClient = new Http3NewClient();
        RpcRequest rpcRequest = new RpcRequest();
        RpcMessage rpcMessage = new RpcMessage();
        rpcRequest.setInterfaceName("abc");
        rpcMessage.setData(rpcRequest);
        rpcMessage.setMessageType((byte) PalmxConfig.getSerializationType().ordinal());
        http3NewClient.doSend(rpcMessage);
    }

    private DefaultHttp3HeadersFrame getHttp3HeadersFrame(InetSocketAddress socketAddress) {
        DefaultHttp3HeadersFrame frame = new DefaultHttp3HeadersFrame();
        frame.headers().method("POST").path("/")
                .authority(socketAddress.toString())
                .scheme("https");
        return frame;
    }

    private Channel getDatagramChannel() {
        if (null != datagramChannel) {
            return datagramChannel;
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
            datagramChannel = bs.group(nioEventLoopGroup)
                    .channel(DatagramChannelHandler.getChannelClass())
                    .handler(channelHandler)
                    .bind(0).sync().channel();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return datagramChannel;
    }
}
