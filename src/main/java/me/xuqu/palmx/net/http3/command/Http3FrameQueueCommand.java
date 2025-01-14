
package me.xuqu.palmx.net.http3.command;


import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class Http3FrameQueueCommand extends QueueCommand {

    protected final QuicStreamChannelFuture quicStreamChannelFuture;

    protected Http3FrameQueueCommand(QuicStreamChannelFuture quicStreamChannelFuture) {
        this.quicStreamChannelFuture = quicStreamChannelFuture;
        this.setPromise(quicStreamChannelFuture.getParentChannel().newPromise());
    }

    @Override
    public Channel getChannel() {
        return this.quicStreamChannelFuture.getNow();
    }
}
