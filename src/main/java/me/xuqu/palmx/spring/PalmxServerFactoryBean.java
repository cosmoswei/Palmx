package me.xuqu.palmx.spring;

import lombok.extern.slf4j.Slf4j;
import me.xuqu.palmx.common.PalmxConfig;
import me.xuqu.palmx.net.PalmxServer;
import me.xuqu.palmx.net.netty.NettyHttp3Server;
import me.xuqu.palmx.net.netty.NettyServer;
import me.xuqu.palmx.registry.ZookeeperUpdater;
import org.springframework.beans.factory.FactoryBean;

@Slf4j
public class PalmxServerFactoryBean implements FactoryBean<PalmxServer> {

    @Override
    public PalmxServer getObject() {
        PalmxServer server;
        boolean enableQuic = PalmxConfig.getQuicEnable();
        if (!enableQuic) {
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
