package me.xuqu.palmx.net.netty;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.incubator.codec.http3.*;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;
import me.xuqu.palmx.invoke.InvokeHandler;
import me.xuqu.palmx.net.RpcInvocation;
import me.xuqu.palmx.net.RpcMessage;
import me.xuqu.palmx.net.RpcResponse;

@Slf4j
public class Http3RpcInvocationHandler extends Http3RequestStreamInboundHandler {

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Caught a exception", cause);
        ctx.close().syncUninterruptibly();
    }


    @Override
    protected void channelRead(
            ChannelHandlerContext ctx, Http3HeadersFrame frame) {
        System.err.println("this is head " + ctx);
        ReferenceCountUtil.release(frame);
    }

    @Override
    protected void channelRead(
            ChannelHandlerContext ctx, Http3DataFrame frame) {
        String msg = frame.content().toString(CharsetUtil.UTF_8);
        RpcInvocation rpcInvocation = getRpcInvocation(msg);
        RpcResponse rpcResponse = InvokeHandler.doInvoke(rpcInvocation);
        RpcMessage rpcMessage = new RpcMessage(rpcResponse.getSequenceId(), rpcResponse);
        byte[] bytes = InvokeHandler.obj2Byte(rpcMessage);
        ctx.write(getDefaultHttp3HeadersFrame(bytes.length));
        ctx.writeAndFlush(new DefaultHttp3DataFrame(
                Unpooled.wrappedBuffer(bytes))).addListener(QuicStreamChannel.SHUTDOWN_OUTPUT);
        ReferenceCountUtil.release(frame);
    }

    private static RpcInvocation getRpcInvocation(String msg) {
        return new RpcInvocation();
    }

    private static Http3HeadersFrame getDefaultHttp3HeadersFrame(int length) {
        Http3HeadersFrame headersFrame = new DefaultHttp3HeadersFrame();
        headersFrame.headers().status("200");
        headersFrame.headers().add("server", "netty");
        headersFrame.headers().addInt("content-length", length);
        return headersFrame;
    }

    @Override
    protected void channelInputClosed(ChannelHandlerContext ctx) {
    }
}
