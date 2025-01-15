package me.xuqu.palmx.net.http3.command;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.incubator.codec.http3.DefaultHttp3DataFrame;
import io.netty.util.concurrent.DefaultPromise;
import lombok.extern.slf4j.Slf4j;
import me.xuqu.palmx.common.PalmxConfig;
import me.xuqu.palmx.exception.RpcInvocationException;
import me.xuqu.palmx.loadbalance.LoadBalanceHolder;
import me.xuqu.palmx.loadbalance.PalmxSocketAddress;
import me.xuqu.palmx.net.AbstractPalmxClient;
import me.xuqu.palmx.net.RpcMessage;
import me.xuqu.palmx.net.RpcRequest;
import me.xuqu.palmx.net.http3.ChannelBuilder;
import me.xuqu.palmx.net.http3.Http3RpcResponseHandler;
import me.xuqu.palmx.net.http3.MessageCodecHelper;
import me.xuqu.palmx.net.http3.queue.PalmxWriteQueue;
import me.xuqu.palmx.registry.ServiceRegistry;
import me.xuqu.palmx.registry.impl.ZookeeperServiceRegistry;

import java.util.List;

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
                streamPromise, channelFuture, channelDefaultPromise);
        ChannelFuture streamFuture = palmxWriteQueue.enqueue(quicStreamChannel);

        // 写入头
        Http3HeaderQueueCommand header = Http3HeaderQueueCommand.createHeader(streamPromise, ChannelBuilder.buildHttp3Headers(socketAddress), channelDefaultPromise);
        ChannelFuture headerFuture = palmxWriteQueue.enqueueFuture(header, datagramChannel.eventLoop());

        // 写入body
        DefaultHttp3DataFrame defaultHttp3DataFrame = new DefaultHttp3DataFrame(MessageCodecHelper.encode(rpcMessage));
        Http3DataQueueCommand body = Http3DataQueueCommand.create(streamPromise, defaultHttp3DataFrame, headerFuture, channelDefaultPromise);
        palmxWriteQueue.enqueue(body, datagramChannel.eventLoop());

        try {
            // 等待命令完成
            promise.await(2000);
            if (promise.isSuccess()) {
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

    private Channel getDatagramChannel() {
        if (null != datagramChannel) {
            return datagramChannel;
        }
        datagramChannel = ChannelBuilder.buildDatagramchannel(nioEventLoopGroup);
        return datagramChannel;
    }
}
