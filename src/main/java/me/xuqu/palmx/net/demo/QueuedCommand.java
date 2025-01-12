package me.xuqu.palmx.net.demo;

import io.netty.channel.Channel;
import io.netty.channel.ChannelPromise;
import io.netty.incubator.codec.http3.DefaultHttp3DataFrame;
import io.netty.incubator.codec.http3.Http3HeadersFrame;
import me.xuqu.palmx.net.RpcMessage;
import me.xuqu.palmx.net.http3.MessageCodecHelper;

public class QueuedCommand {
    private final RpcMessage rpcMessage;
    private final Channel channel;
    private ChannelPromise promise;

    public QueuedCommand(RpcMessage rpcMessage, Channel channel) {
        this.rpcMessage = rpcMessage;
        this.channel = channel;
    }

    public RpcMessage getRpcMessage() {
        return rpcMessage;
    }

    public Channel channel() {
        return channel;
    }

    public ChannelPromise promise() {
        return promise;
    }

    public void promise(ChannelPromise promise) {
        this.promise = promise;
    }

    // 执行命令
    public void run(Channel channel) {
        // 发送 RpcMessage
        Http3HeadersFrame http3HeadersFrame = getHttp3HeadersFrame(rpcMessage);
        DefaultHttp3DataFrame defaultHttp3DataFrame = new DefaultHttp3DataFrame(MessageCodecHelper.encode(rpcMessage));
        channel.writeAndFlush(http3HeadersFrame);
        channel.writeAndFlush(defaultHttp3DataFrame);
    }

    private Http3HeadersFrame getHttp3HeadersFrame(RpcMessage rpcMessage) {
        return null;
    }
}
