package me.xuqu.palmx.net;

import lombok.extern.slf4j.Slf4j;
import me.xuqu.palmx.common.PalmxConfig;
import me.xuqu.palmx.loadbalance.PalmxSocketAddress;
import me.xuqu.palmx.util.HostUtils;

@Slf4j
public abstract class AbstractPalmxServer implements PalmxServer {

    protected PalmxSocketAddress inetSocketAddress;
    protected String host = HostUtils.getLocalAddr();
    protected int port = HostUtils.getLocalPort();

    public AbstractPalmxServer() {
    }

    public AbstractPalmxServer(int port) {
        this();
        this.port = port;
    }

    @Override
    public void start() {
        inetSocketAddress = new PalmxSocketAddress(host, port);
        log.info("config = \n{}", PalmxConfig.list());
        doStart();
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
