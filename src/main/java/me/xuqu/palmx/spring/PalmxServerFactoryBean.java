package me.xuqu.palmx.spring;

import me.xuqu.palmx.common.PalmxConfig;
import me.xuqu.palmx.net.PalmxServer;
import me.xuqu.palmx.net.netty.NettyHttp3Server;
import me.xuqu.palmx.net.netty.NettyServer;
import org.springframework.beans.factory.FactoryBean;


public class PalmxServerFactoryBean implements FactoryBean<PalmxServer> {

    @Override
    public PalmxServer getObject() {
        PalmxServer server;
        boolean enableQuic = PalmxConfig.getEnableQuic();
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
