package me.xuqu.palmx.net.http3;

import io.netty.channel.*;
import io.netty.incubator.codec.http3.DefaultHttp3DataFrame;
import io.netty.incubator.codec.http3.Http3DataFrame;
import io.netty.incubator.codec.http3.Http3HeadersFrame;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import lombok.extern.slf4j.Slf4j;
import me.xuqu.palmx.net.RpcMessage;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class RpcRequestSender {

    private final Queue<Http3DataFrame> writeQueue = new ConcurrentLinkedQueue<>();
    private final Channel channel;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public RpcRequestSender(Channel channel) {
        this.channel = channel;
        // 每隔500ms检查一次队列并批量发送
        scheduler.scheduleAtFixedRate(this::flushQueue, 0, 500, TimeUnit.MILLISECONDS);
    }

    // 将请求放入队列
    public void addRequestToQueue(RpcMessage rpcMessage) {
        Http3HeadersFrame http3HeadersFrame = getHttp3HeadersFrame(rpcMessage);
        DefaultHttp3DataFrame dataFrame = new DefaultHttp3DataFrame(MessageCodecHelper.encode(rpcMessage));
        
        // 将请求添加到队列中
        writeQueue.offer(dataFrame);
    }

    // 定时触发队列刷新的方法
    private void flushQueue() {
        if (!writeQueue.isEmpty()) {
            try {
                // 获取写流通道（保证每次发送数据的 channel 是同一个）
                Future<QuicStreamChannel> quicStreamChannelFuture = getQuicStreamChannelFuture();
                
                quicStreamChannelFuture.addListener(new GenericFutureListener<Future<QuicStreamChannel>>() {
                    @Override
                    public void operationComplete(Future<QuicStreamChannel> future) throws Exception {
                        if (future.isSuccess()) {
                            QuicStreamChannel quicStreamChannel = future.getNow();

                            // 批量写入队列中的请求数据
                            while (!writeQueue.isEmpty()) {
                                Http3DataFrame dataFrame = writeQueue.poll();
                                quicStreamChannel.write(dataFrame);
                            }

                            // 一次性flush所有请求数据
                            quicStreamChannel.writeAndFlush(new DefaultHttp3DataFrame(writeQueue.peek().content()))
                                    .addListener(QuicStreamChannel.SHUTDOWN_OUTPUT);
                        } else {
                            // 处理失败情况
                            log.warn("Failed to get QuicStreamChannel, cause: {}", future.cause());
                        }
                    }
                });
            } catch (Exception e) {
                log.error("Error while flushing write queue", e);
            }
        }
    }

    // 获取一个新的 QuicStreamChannel
    private Future<QuicStreamChannel> getQuicStreamChannelFuture() {
        // 你可以根据你的需求，使用不同的方式获取一个新的QuicStreamChannel
        // 例如，通过负载均衡选择目标地址，并建立连接
        return null; // 伪代码，应该返回一个真正的 Future<QuicStreamChannel> 对象
    }

    // 获取 Http3 请求头
    private Http3HeadersFrame getHttp3HeadersFrame(RpcMessage rpcMessage) {
        // 根据 rpcMessage 创建请求头，具体实现由你定义
        return null; // 伪代码，应该返回一个真正的 Http3HeadersFrame 对象
    }
}
