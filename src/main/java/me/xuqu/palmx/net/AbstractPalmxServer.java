package me.xuqu.palmx.net;

import lombok.extern.slf4j.Slf4j;
import me.xuqu.palmx.common.PalmxConfig;
import me.xuqu.palmx.loadbalance.PalmxSocketAddress;
import me.xuqu.palmx.registry.ZookeeperUpdater;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Slf4j
public abstract class AbstractPalmxServer implements PalmxServer {

    protected PalmxSocketAddress inetSocketAddress;
    protected String host = "127.0.0.1";
    protected int port = PalmxConfig.getPalmxServerPort();

    public AbstractPalmxServer() {
        try {
            host = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public AbstractPalmxServer(int port) {
        this();
        this.port = port;
    }

    @Override
    public void start() {
        inetSocketAddress = new PalmxSocketAddress(host, port);
        doStart();
        // 启动更新ZK线程
        ZookeeperUpdater.startUpdating();
        log.info("server start success");
    }

    @Override
    public void shutdown() {
        doShutdown();
        log.info("Palmx server has been shutdown");
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public PalmxSocketAddress getAddress() {
        return inetSocketAddress;
    }

    protected abstract void doStart();

    protected abstract void doShutdown();
}
