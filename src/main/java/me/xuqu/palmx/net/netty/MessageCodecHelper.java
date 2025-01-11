package me.xuqu.palmx.net.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;
import me.xuqu.palmx.common.PalmxConstants;
import me.xuqu.palmx.exception.PalmxException;
import me.xuqu.palmx.net.RpcMessage;
import me.xuqu.palmx.net.RpcRequest;
import me.xuqu.palmx.net.RpcResponse;

import static me.xuqu.palmx.common.PalmxConstants.NETTY_RPC_REQUEST_MESSAGE;
import static me.xuqu.palmx.common.PalmxConstants.NETTY_RPC_RESPONSE_MESSAGE;
import static me.xuqu.palmx.serialize.Serialization.deserialize;
import static me.xuqu.palmx.serialize.Serialization.serialize;

@Slf4j
public class MessageCodecHelper {

    /**
     * 编码
     */
    public static ByteBuf encode(RpcMessage rpcMessage) {
        ByteBuf byteBuf = Unpooled.buffer();
        byteBuf.writeInt(PalmxConstants.NETTY_MESSAGE_MAGIC_NUMBER);
        byteBuf.writeByte(PalmxConstants.NETTY_MESSAGE_VERSION);
        byteBuf.writeInt(rpcMessage.getSequenceId());
        byteBuf.writeByte(rpcMessage.getSerializationType());
        byteBuf.writeByte(rpcMessage.getMessageType());
        byteBuf.writeByte(0xff);
        byte[] data = serialize(rpcMessage.getSerializationType(), rpcMessage.getData());
        byteBuf.writeInt(data.length);
        byteBuf.writeBytes(data);
        return byteBuf;
    }

    /**
     * 解码
     */
    public static RpcMessage decode(ByteBuf byteBuf) {

        int originalReaderIndex = byteBuf.readerIndex();  // 保存原始 readerIndex
        // 转换为字符串
        String content = byteBuf.toString(CharsetUtil.US_ASCII);
        // 恢复 readerIndex
        byteBuf.readerIndex(originalReaderIndex);
        RpcMessage rpcMessage = new RpcMessage();
        int magicNumber = byteBuf.readInt();
        // 可以简单的校验消息的正确性，比如说魔数
        if (magicNumber != PalmxConstants.NETTY_MESSAGE_MAGIC_NUMBER) {
            throw new PalmxException("Magic number is wrong，magic number is " + magicNumber);
        }
        // version
        byteBuf.readByte();
        int sequenceId = byteBuf.readInt();
        rpcMessage.setSequenceId(sequenceId);
        byte serializedType = byteBuf.readByte();
        rpcMessage.setSerializationType(serializedType);
        byte messageType = byteBuf.readByte();
        rpcMessage.setMessageType(messageType);
        // padding: 0xff
        byteBuf.readByte();
        int length = byteBuf.readInt();
        byte[] bytes = new byte[length];
        byteBuf.readBytes(bytes, 0, length);
        if (messageType == NETTY_RPC_REQUEST_MESSAGE) {
            RpcRequest rpcRequest = deserialize(serializedType, RpcRequest.class, bytes);
            rpcRequest.setSequenceId(sequenceId);
            rpcMessage.setData(rpcRequest);
        } else if (messageType == NETTY_RPC_RESPONSE_MESSAGE) {
            RpcResponse rpcResponse = deserialize(serializedType, RpcResponse.class, bytes);
            rpcResponse.setSequenceId(sequenceId);
            rpcMessage.setData(rpcResponse);
        }
        return rpcMessage;
    }
}
