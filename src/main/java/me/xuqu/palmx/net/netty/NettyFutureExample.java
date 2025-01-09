package me.xuqu.palmx.net.netty;

import io.netty.util.concurrent.DefaultEventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

public class NettyFutureExample {
    public static void main(String[] args) throws InterruptedException {
        DefaultEventExecutor executor = new DefaultEventExecutor();

        // 创建一个异步任务
        Future<String> future = executor.submit(() -> {
            try {
                Thread.sleep(1000);
                return "Hello from Future!";
            } catch (InterruptedException e) {
                return "Task interrupted!";
            }
        });

        // 非阻塞地获取结果，若未完成则返回 null
        String result = future.getNow();
        if (result != null) {
            System.out.println("Result: " + result);
        } else {
            System.out.println("Future not completed yet.");
        }

        // 添加一个监听器来获取结果
        future.addListener((GenericFutureListener<Future<? super String>>) f -> {
            if (f.isSuccess()) {
                System.out.println("Completed successfully: " + f.getNow());
            } else {
                System.out.println("Failed: " + f.cause());
            }
        });

        // 稍等一下，给任务完成的时间
        Thread.sleep(1500);
    }
}
