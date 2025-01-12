
package me.xuqu.palmx.net.http3.command;


import io.netty.channel.Channel;

public abstract class QuicStreamChannelCommand extends QueuedCommand {

    protected final QuicStreamChannelFuture quicStreamChannelFuture;

    protected QuicStreamChannelCommand(QuicStreamChannelFuture quicStreamChannelFuture) {
        this.quicStreamChannelFuture = quicStreamChannelFuture;
        this.promise(quicStreamChannelFuture.getParentChannel().newPromise());
    }

    @Override
    public void run(Channel channel) {
        if (quicStreamChannelFuture.isSuccess()) {
            super.run(channel);
            return;
        }
        promise().setFailure(quicStreamChannelFuture.cause());
    }

    @Override
    public Channel channel() {
        return this.quicStreamChannelFuture.getNow();
    }
}
