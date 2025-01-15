package me.xuqu.palmx.net.http3;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.incubator.codec.http3.DefaultHttp3HeadersFrame;
import io.netty.incubator.codec.http3.Http3;
import io.netty.incubator.codec.http3.Http3HeadersFrame;
import io.netty.incubator.codec.quic.QuicSslContext;
import io.netty.incubator.codec.quic.QuicSslContextBuilder;
import me.xuqu.palmx.loadbalance.PalmxSocketAddress;
import me.xuqu.palmx.net.DatagramChannelHandler;

import java.util.concurrent.TimeUnit;

public class ChannelBuilder {


    public static Channel buildDatagramchannel(EventLoopGroup group) {
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
        Channel channel;
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

    public static DefaultHttp3HeadersFrame buildHttp3Headers(PalmxSocketAddress socketAddress) {
        DefaultHttp3HeadersFrame frame = new DefaultHttp3HeadersFrame();
        frame.headers().method("POST").path("/")
                .authority(socketAddress.toString())
                .scheme("https");
        return frame;
    }
}
