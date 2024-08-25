package me.xuqu.palmx.net;

import me.xuqu.palmx.loadbalance.PalmxSocketAddress;

public interface PalmxServer {

    void start();

    void shutdown();

    int getPort();

    PalmxSocketAddress getAddress();
}
