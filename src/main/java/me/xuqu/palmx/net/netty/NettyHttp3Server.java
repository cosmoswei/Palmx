package me.xuqu.palmx.net.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.incubator.codec.http3.Http3;
import io.netty.incubator.codec.http3.Http3ServerConnectionHandler;
import io.netty.incubator.codec.quic.*;
import lombok.extern.slf4j.Slf4j;
import me.xuqu.palmx.net.AbstractPalmxServer;

import java.net.InetSocketAddress;
import java.security.cert.CertificateException;
import java.util.concurrent.TimeUnit;

@Slf4j
public class NettyHttp3Server extends AbstractPalmxServer {

    public NettyHttp3Server() {
    }

    public NettyHttp3Server(int port) {
        super(port);
    }

    @Override
    protected void doStart() {
        NioEventLoopGroup group = new NioEventLoopGroup(1);
        SelfSignedCertificate cert = null;
        try {
            cert = new SelfSignedCertificate();
        } catch (CertificateException e) {
            throw new RuntimeException(e);
        }
        QuicSslContext sslContext = QuicSslContextBuilder.forServer(cert.key(), null, cert.cert())
                .applicationProtocols(Http3.supportedApplicationProtocols()).build();
        ChannelHandler codec = Http3.newQuicServerCodecBuilder()
                .sslContext(sslContext)
                .maxIdleTimeout(500000, TimeUnit.MILLISECONDS)
                .initialMaxData(10000000)
                .initialMaxData(10000000)
                .initialMaxStreamDataBidirectionalLocal(1000000)
                .initialMaxStreamDataBidirectionalRemote(1000000)
                .initialMaxStreamsBidirectional(2000)  // 设置最大并发双向流数
                .initialMaxStreamsUnidirectional(2000) // 设置最大并发单向流数
                .tokenHandler(InsecureQuicTokenHandler.INSTANCE)
                .handler(new ChannelInitializer<QuicChannel>() {
                    @Override
                    protected void initChannel(QuicChannel ch) {
                        // Called for each connection
                        ch.pipeline().addLast(new Http3ServerConnectionHandler(
                                new ChannelInitializer<QuicStreamChannel>() {
                                    @Override
                                    protected void initChannel(QuicStreamChannel ch) {
                                        ch.pipeline().addLast(new Http3RpcInvocationHandler() {
                                        });
                                    }
                                }));
                    }
                }).build();
        try {
            Bootstrap bs = new Bootstrap();
            Channel channel = bs.group(group)
                    .channel(NioDatagramChannel.class)
                    .handler(codec)
                    .bind(new InetSocketAddress(port)).sync().channel();
            channel.closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            group.shutdownGracefully();
        }

    }

    @Override
    protected void doShutdown() {

    }
}
