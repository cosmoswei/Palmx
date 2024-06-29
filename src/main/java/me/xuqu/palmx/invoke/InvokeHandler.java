package me.xuqu.palmx.invoke;

import io.netty.buffer.ByteBuf;
import me.xuqu.palmx.common.PalmxConstants;
import me.xuqu.palmx.net.RpcInvocation;
import me.xuqu.palmx.net.RpcMessage;
import me.xuqu.palmx.net.RpcResponse;
import me.xuqu.palmx.provider.DefaultServiceProvider;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static me.xuqu.palmx.serialize.Serialization.deserialize;
import static me.xuqu.palmx.serialize.Serialization.serialize;

public class InvokeHandler {

    private InvokeHandler() {
    }

    public static RpcResponse doInvoke(RpcInvocation rpcInvocation) {
        RpcResponse rpcResponse = new RpcResponse();
        rpcResponse.setSequenceId(rpcInvocation.getSequenceId());
        String serviceName = rpcInvocation.getInterfaceName();
        Object service = DefaultServiceProvider.getInstance().getService(serviceName);
        // 如果未找到直接返回空
        if (service == null) {
            rpcResponse.setStatus(PalmxConstants.NETTY_RPC_RESPONSE_STATUS_ERROR);
            rpcResponse.setMessage(String.format("Service[%s] instance not found", serviceName));
            return rpcResponse;
        }
        // 获取方法执行的信息
        String methodName = rpcInvocation.getMethodName();
        Class<?>[] paramTypes = rpcInvocation.getParameterTypes();
        Object[] arguments = rpcInvocation.getArguments();
        try {
            // 反射执行具体的方法
            Method method = service.getClass().getMethod(methodName, paramTypes);
            Object result = method.invoke(service, arguments);
            // 正常情况，将方法执行的结果封装到响应结果中
            rpcResponse.setStatus(PalmxConstants.NETTY_RPC_RESPONSE_STATUS_OK);
            rpcResponse.setData(result);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            // 异常情况，将异常对象封装到响应结果中
            rpcResponse.setStatus(PalmxConstants.NETTY_RPC_RESPONSE_STATUS_ERROR);
            rpcResponse.setMessage(e.getCause().getMessage());
        }
        return rpcResponse;
    }


    public static Object byte2Obj(ByteBuf var) {
        byte x = var.readByte();
        return deserialize(x, RpcInvocation.class, var.array());
    }

    public static byte[] obj2Byte(RpcMessage rpcMessage) {
        return serialize(rpcMessage.getSerializationType(), rpcMessage.getData());
    }
}
