package ru.geekbrains.models;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CloudFile implements Message {

    public enum SendFileAction {
        UPLOAD,
        DOWNLOAD
    }

    private final GenericFile genericFile;
    private SendFileAction sendFileAction;



    public CloudFile(GenericFile genericFile, SendFileAction sendFileAction) {
        this.genericFile = genericFile;
        this.sendFileAction = sendFileAction;
    }

    @Override
    public String getType() {
        return sendFileAction.toString();
    }

    @Override
    public Object getMessage() {
        return genericFile;
    }
}
