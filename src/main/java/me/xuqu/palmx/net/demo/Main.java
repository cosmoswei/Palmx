package me.xuqu.palmx.net.demo;

public class Main {
    public static void main(String[] args) {
        StreamQueueCommand queue = new StreamQueueCommand();

        // 向队列添加命令
        queue.addCommand(callback -> {
            // 模拟异步操作（如网络请求）
            System.out.println("Executing Task 1");
            // 假设任务完成后，通过回调返回结果
            callback.onComplete("Task 1 Completed");
        });

        queue.addCommand(callback -> {
            System.out.println("Executing Task 2");
            callback.onComplete("Task 2 Completed");
        });

        queue.addCommand(callback -> {
            System.out.println("Executing Task 3");
            callback.onComplete("Task 3 Completed");
        });

        // 开始执行队列中的命令
        queue.execute();
    }
}
