package ru.geekbrains.models.Actions;

import ru.geekbrains.models.Commands;
import ru.geekbrains.models.Message;

import java.util.List;

public class FileList implements Message {

    private List<String> fileList;
    private Commands command;

    public FileList(List<String> fileList) {
        this.fileList = fileList;
        this.command = Commands.FILE_LIST;
    }

    @Override
    public Commands getType() {
        return command;
    }

    @Override
    public Object getMessage() {
        return fileList;
    }
}
