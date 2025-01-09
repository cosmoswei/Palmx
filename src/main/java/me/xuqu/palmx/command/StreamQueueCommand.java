
package me.xuqu.palmx.command;


import io.netty.channel.Channel;

public abstract class StreamQueueCommand extends QueuedCommand {

    protected final StreamChannelFuture streamChannelFuture;

    protected StreamQueueCommand(StreamChannelFuture streamChannelFuture) {
        this.streamChannelFuture = streamChannelFuture;
        this.promise(streamChannelFuture.getParentChannel().newPromise());
    }

    @Override
    public void run(Channel channel) {
        if (streamChannelFuture.isSuccess()) {
            super.run(channel);
            return;
        }
        promise().setFailure(streamChannelFuture.cause());
    }

    @Override
    public Channel channel() {
        return this.streamChannelFuture.getNow();
    }
}
