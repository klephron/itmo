package ru.itmo.zavar.task.exithandler;

public class DefaultExitHandler implements ExitHandler {
    @Override
    public void exit(int code) {
        System.exit(code);
    }
}

