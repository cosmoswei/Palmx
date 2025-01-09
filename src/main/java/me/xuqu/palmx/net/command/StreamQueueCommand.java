package me.xuqu.palmx.net.command;

import java.util.LinkedList;
import java.util.Queue;

public class StreamQueueCommand {

    private Queue<AsyncCommand> commandQueue = new LinkedList<>();
    
    // 添加命令到队列
    public void addCommand(AsyncCommand command) {
        commandQueue.offer(command);
    }
    
    // 开始执行队列中的命令
    public void execute() {
        if (!commandQueue.isEmpty()) {
            executeNextCommand();
        }
    }

    private void executeNextCommand() {
        AsyncCommand command = commandQueue.poll();
        if (command != null) {
            command.execute(result -> {
                // 完成当前命令后，执行下一个命令
                executeNextCommand();
            });
        }
    }
}
