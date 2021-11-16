package ru.geekbrains.models;

import java.util.List;

public class FileList implements Message {

    private List<String> fileList;

    public FileList(List<String> fileList) {
        this.fileList = fileList;
    }

    @Override
    public String getType() {
        return "FILE_LIST";
    }

    @Override
    public Object getMessage() {
        return fileList;
    }
}
