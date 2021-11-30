package ru.geekbrains.models.Actions;

import ru.geekbrains.models.Commands;
import ru.geekbrains.models.Message;

public class Action implements Message {

    private final String filePath;
    private Commands command;

    public Action(String filePath, Commands command) {
        this.filePath = filePath;
        this.command = command;
    }

    @Override
    public Commands getType() {
        return command;
    }

    @Override
    public Object getMessage() {
        return filePath;
    }
}
