package me.xuqu.palmx.net.demo;

@FunctionalInterface
public interface AsyncCommand {
    void execute(AsyncCallback callback);
}

