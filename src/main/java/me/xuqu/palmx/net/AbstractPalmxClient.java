package me.xuqu.palmx.net;

import lombok.extern.slf4j.Slf4j;
import me.xuqu.palmx.common.PalmxConstants;
import me.xuqu.palmx.util.SequenceIdGenerator;

@Slf4j
public abstract class AbstractPalmxClient implements PalmxClient {
    @Override
    public Object sendAndExpect(RpcInvocation rpcInvocation) {
        RpcMessage rpcMessage = new RpcMessage(SequenceIdGenerator.nextId(), rpcInvocation);
        rpcMessage.setMessageType(PalmxConstants.NETTY_RPC_INVOCATION_MESSAGE);
//        long start = System.currentTimeMillis();
        Object res = doSend(rpcMessage);
//        log.info("invoke Time = {}", System.currentTimeMillis() - start);
        return res;
    }

    protected abstract Object doSend(RpcMessage rpcMessage);
}
