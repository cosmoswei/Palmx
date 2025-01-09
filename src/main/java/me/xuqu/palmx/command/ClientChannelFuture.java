package me.xuqu.palmx.command;


import io.netty.incubator.codec.quic.QuicStreamChannel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;

public class ClientChannelFuture {

    private Promise<Object> promise;

    private Future<QuicStreamChannel> streamChannelFuture;

    private Throwable cause;


    public ClientChannelFuture(Promise<Object> promise, Future future) {
        this.streamChannelFuture = future;
        this.promise = promise;
    }

    public ClientChannelFuture(Promise<Object> promise) {
        this.promise = promise;
    }

    public Future<QuicStreamChannel> streamChannelFuture() {
            return streamChannelFuture;
    }

    public Promise<Object> promise() {
        return promise;
    }
}
