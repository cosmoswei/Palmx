package me.xuqu.palmx.net.command;

import java.util.concurrent.CompletableFuture;

public class AsyncRpcClient {
    public CompletableFuture<String> makeAsyncRpcCall(String request) {
        return CompletableFuture.supplyAsync(() -> {
            // 模拟RPC调用
            try {
                Thread.sleep(2000);  // 假设网络延迟2秒
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "Response from server: " + request;
        });
    }

    public static void main(String[] args) throws Exception {
        AsyncRpcClient client = new AsyncRpcClient();
        CompletableFuture<String> response = client.makeAsyncRpcCall("Hello, server!");
        
        // 在等待响应的同时，可以执行其他任务
        System.out.println("Client continues working...");

        // 阻塞等待结果返回
        System.out.println(response.get()); // "Response from server: Hello, server!"
    }
}
