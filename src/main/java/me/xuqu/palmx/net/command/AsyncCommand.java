package me.xuqu.palmx.net.command;

@FunctionalInterface
public interface AsyncCommand {
    void execute(AsyncCallback callback);
}

