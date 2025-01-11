package me.xuqu.palmx.net;

import lombok.extern.slf4j.Slf4j;
import me.xuqu.palmx.common.PalmxConstants;
import me.xuqu.palmx.util.SequenceIdGenerator;

@Slf4j
public abstract class AbstractPalmxClient implements PalmxClient {
    @Override
    public Object sendAndExpect(RpcRequest rpcRequest) {
        RpcMessage rpcMessage = new RpcMessage(SequenceIdGenerator.nextId(), rpcRequest);
        rpcMessage.setMessageType(PalmxConstants.NETTY_RPC_REQUEST_MESSAGE);
        return doSend(rpcMessage);
    }

    protected abstract Object doSend(RpcMessage rpcMessage);

    @Override
    public void shutdown() {

    }
}
