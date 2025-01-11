package me.xuqu.palmx.net.http3;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.incubator.codec.http3.*;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;
import me.xuqu.palmx.common.PalmxConstants;
import me.xuqu.palmx.exception.FlowControlException;
import me.xuqu.palmx.flowcontrol.FlowControlHolder;
import me.xuqu.palmx.flowcontrol.FlowControlReq;
import me.xuqu.palmx.invoke.InvokeHandler;
import me.xuqu.palmx.net.RpcMessage;
import me.xuqu.palmx.net.RpcRequest;
import me.xuqu.palmx.net.RpcResponse;

@Slf4j
public class Http3RpcRequestHandler extends Http3RequestStreamInboundHandler {

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Caught a exception", cause);
        ctx.close().syncUninterruptibly();
    }


    @Override
    protected void channelRead(
            ChannelHandlerContext ctx, Http3HeadersFrame frame) {
        ReferenceCountUtil.release(frame);
    }

    @Override
    protected void channelRead(
            ChannelHandlerContext ctx, Http3DataFrame frame) {
//        long start = System.currentTimeMillis();
        RpcMessage reqMessage = MessageCodecHelper.decode(frame.content());
        RpcRequest rpcRequest = (RpcRequest) reqMessage.getData();
        boolean control = FlowControlHolder.control(new FlowControlReq(rpcRequest.getInterfaceName()));
        if (control) {
            throw new FlowControlException("Flow control exception");
        }
        // 发起调用
        RpcResponse rpcResponse = InvokeHandler.doInvoke(rpcRequest);
        rpcResponse.setSequenceId(reqMessage.getSequenceId());
        RpcMessage resMessage = new RpcMessage(rpcResponse.getSequenceId(), rpcResponse);
        resMessage.setMessageType(PalmxConstants.NETTY_RPC_RESPONSE_MESSAGE);
        ByteBuf encode = MessageCodecHelper.encode(resMessage);
        int len = encode.readableBytes();
        ctx.write(getDefaultHttp3HeadersFrame(len));
        DefaultHttp3DataFrame defaultHttp3DataFrame = new DefaultHttp3DataFrame(encode);
        ctx.writeAndFlush(defaultHttp3DataFrame).addListener(QuicStreamChannel.SHUTDOWN_OUTPUT);
//        log.info("duration Time = {}", System.currentTimeMillis() - start);
        ReferenceCountUtil.release(frame);
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
