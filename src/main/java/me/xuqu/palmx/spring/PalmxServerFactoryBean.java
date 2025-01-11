package me.xuqu.palmx.spring;

import lombok.extern.slf4j.Slf4j;
import me.xuqu.palmx.common.PalmxConfig;
import me.xuqu.palmx.net.PalmxServer;
import me.xuqu.palmx.net.http3.NettyHttp3Server;
import me.xuqu.palmx.net.netty.NettyServer;
import org.springframework.beans.factory.FactoryBean;

@Slf4j
public class PalmxServerFactoryBean implements FactoryBean<PalmxServer> {

    @Override
    public PalmxServer getObject() {
        PalmxServer server;
        if (!PalmxConfig.getQuicEnable()) {
            server = new NettyServer();
        } else {
            server = new NettyHttp3Server();
        }
        new Thread(server::start, "palmx-server").start();
        return server;
    }

    @Override
    public Class<?> getObjectType() {
        return PalmxServer.class;
    }
}
