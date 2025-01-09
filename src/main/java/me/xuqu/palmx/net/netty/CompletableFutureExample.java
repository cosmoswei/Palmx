package me.xuqu.palmx.net.netty;

import java.util.concurrent.CompletableFuture;

public class CompletableFutureExample {

    public static void main(String[] args) {
        // 创建一个 CompletableFuture
        CompletableFuture<Void> future = new CompletableFuture<>();

        // 异步操作
        asyncOperation(future);
        
        // 注册一个监听器，当 future 完成时，触发监听器
        future.thenRun(() -> {
            System.out.println("Async operation completed, now returning.");
        });
        
        // 阻塞，直到 future 完成
        future.join();
        
        // 执行到此处，表示监听器已经触发，方法可以返回
        System.out.println("Operation finished, method returning.");
    }

    // 模拟异步操作
    public static void asyncOperation(CompletableFuture<Void> future) {
        new Thread(() -> {
            try {
                // 模拟异步操作，睡眠 2 秒
                Thread.sleep(2000);
                
                // 操作完成，完成 future
                System.out.println("Async operation finished.");
                
                // 完成操作
                future.complete(null);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }
}
