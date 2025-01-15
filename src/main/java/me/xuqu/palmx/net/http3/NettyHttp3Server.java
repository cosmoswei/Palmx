package me.xuqu.palmx.net.http3;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.incubator.codec.http3.Http3;
import io.netty.incubator.codec.http3.Http3ServerConnectionHandler;
import io.netty.incubator.codec.quic.*;
import lombok.extern.slf4j.Slf4j;
import me.xuqu.palmx.common.PalmxConfig;
import me.xuqu.palmx.net.AbstractPalmxServer;
import me.xuqu.palmx.net.DatagramChannelHandler;
import me.xuqu.palmx.registry.ZookeeperUpdater;

import java.net.InetSocketAddress;
import java.security.cert.CertificateException;
import java.util.concurrent.TimeUnit;

@Slf4j
public class NettyHttp3Server extends AbstractPalmxServer {

    public NettyHttp3Server() {
    }

    @Override
    protected void doStart() {
        ZookeeperUpdater.startUpdating();
        NioEventLoopGroup group = new NioEventLoopGroup(PalmxConfig.ioThreads());
        SelfSignedCertificate cert;
        try {
            cert = new SelfSignedCertificate();
        } catch (CertificateException e) {
            throw new RuntimeException(e);
        }
        QuicSslContext sslContext = QuicSslContextBuilder.forServer(cert.key(), null, cert.cert())
                .applicationProtocols(Http3.supportedApplicationProtocols()).build();

        ChannelHandler channelHandler = Http3.newQuicServerCodecBuilder()
                .sslContext(sslContext)
                .maxIdleTimeout(5000, TimeUnit.MILLISECONDS)
                .initialMaxData(10000000)
                .initialMaxStreamDataBidirectionalLocal(1000000)
                .initialMaxStreamDataBidirectionalRemote(1000000)
                .initialMaxStreamsBidirectional(PalmxConfig.getInitialMaxStreamsBidirectional())  // 设置最大并发双向流数
                .tokenHandler(InsecureQuicTokenHandler.INSTANCE)
                .handler(new ChannelInitializer<QuicChannel>() {
                    @Override
                    protected void initChannel(QuicChannel ch) {
                        // Called for each connection
                        ch.pipeline().addLast(new Http3ServerConnectionHandler(
                                new ChannelInitializer<QuicStreamChannel>() {
                                    @Override
                                    protected void initChannel(QuicStreamChannel ch) {
                                        ch.pipeline().addLast(new Http3RpcRequestHandler() {
                                        });
                                    }
                                }));
                    }
                }).build();
        try {
            Bootstrap bs = new Bootstrap();
            Channel channel = bs.group(group)
                    .channel(DatagramChannelHandler.getChannelClass())
                    .handler(channelHandler)
                    .bind(new InetSocketAddress(port)).sync().channel();
            channel.closeFuture().sync();
        } catch (Exception e) {
            throw new RuntimeException("服务端启动失败 cause = {}", e.getCause());
        } finally {
            group.shutdownGracefully();
        }
        log.info("NettyHttp3Server started on port {}", port);
    }


    @Override
    protected void doShutdown() {

    }
}
