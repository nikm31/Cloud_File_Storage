package ru.geekbrains.models;

public class Command implements Message{

    public enum CommandAction {
        COPY,
        DELETE,
        GET_DIRECTORY,
        SEND_DIRECTORY,
        CREATE_DIRECTORY,
        ENTER_TO_DIRECTORY,
        BACK_TO_PARENT_SERVER_PATH,
        UPDATE_SERVER_PATH,
        FILE_SIZE
    }

    private final String filePath;
    private CommandAction commandAction;

    public Command(String filePath, CommandAction commandAction) {
        this.filePath = filePath;
        this.commandAction = commandAction;
    }

    @Override
    public String getType() {
        return commandAction.toString();
    }

    @Override
    public Object getMessage() {
        return filePath;
    }
}
