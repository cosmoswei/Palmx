package me.xuqu.palmx.net.netty;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;
import me.xuqu.palmx.common.PalmxConstants;
import me.xuqu.palmx.exception.PalmxException;
import me.xuqu.palmx.net.RpcInvocation;
import me.xuqu.palmx.net.RpcMessage;
import me.xuqu.palmx.net.RpcResponse;

import java.io.IOException;

import static me.xuqu.palmx.common.PalmxConstants.NETTY_RPC_INVOCATION_MESSAGE;
import static me.xuqu.palmx.common.PalmxConstants.NETTY_RPC_RESPONSE_MESSAGE;
import static me.xuqu.palmx.serialize.Serialization.deserialize;
import static me.xuqu.palmx.serialize.Serialization.serialize;

@Slf4j
public class MessageCodecHelper {

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

    public static String encode2String(RpcMessage rpcMessage) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            String jsonString = objectMapper.writeValueAsString(rpcMessage);
            return jsonString;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static RpcResponse decodeResponse(ByteBuf byteBuf) {
        int magicNumber = byteBuf.readInt();
        // 可以简单的校验消息的正确性，比如说魔数
        if (magicNumber != PalmxConstants.NETTY_MESSAGE_MAGIC_NUMBER) {
            log.error("Unknown message, magic number is {}", magicNumber);
            throw new PalmxException("Magic number is wrong");
        }
        // version
        byteBuf.readByte();
        int sequenceId = byteBuf.readInt();
        byte serializedType = byteBuf.readByte();
        byte messageType = byteBuf.readByte();
        // padding: 0xff
        byteBuf.readByte();
        int length = byteBuf.readInt();
        byte[] bytes = new byte[length];
        byteBuf.readBytes(bytes, 0, length);
        if (messageType == NETTY_RPC_RESPONSE_MESSAGE) {
            RpcResponse rpcResponse = deserialize(serializedType, RpcResponse.class, bytes);
            rpcResponse.setSequenceId(sequenceId);
            return rpcResponse;
        }
        return null;
    }


    public static RpcResponse decodeResponse2(String json) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            RpcMessage rpcMessage = objectMapper.readValue(json, RpcMessage.class);
            Object data = rpcMessage.getData();
            return objectMapper.readValue(objectMapper.writeValueAsBytes(data), RpcResponse.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static RpcMessage decodeRpcInvocation2(String json) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readValue(json, RpcMessage.class);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static RpcInvocation decodeRpcInvocation(ByteBuf byteBuf) {
        int magicNumber = byteBuf.readInt();
        // 可以简单的校验消息的正确性，比如说魔数
        if (magicNumber != PalmxConstants.NETTY_MESSAGE_MAGIC_NUMBER) {
            log.error("Unknown message, magic number is {}", magicNumber);
            throw new PalmxException("Magic number is wrong");
        }
        // version
        byteBuf.readByte();
        int sequenceId = byteBuf.readInt();
        byte serializedType = byteBuf.readByte();
        byte messageType = byteBuf.readByte();
        // padding: 0xff
        byteBuf.readByte();
        int length = byteBuf.readInt();
        byte[] bytes = new byte[length];
        byteBuf.readBytes(bytes, 0, length);
        if (messageType == NETTY_RPC_INVOCATION_MESSAGE) {
            RpcInvocation rpcInvocation = deserialize(serializedType, RpcInvocation.class, bytes);
            rpcInvocation.setSequenceId(sequenceId);
            return rpcInvocation;
        }
        return null;
    }
}
