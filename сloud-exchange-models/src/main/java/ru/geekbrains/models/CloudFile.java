package ru.geekbrains.models;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CloudFile implements Message {
private final GenericFile genericFile;
private String typeOfTransfer;

    public CloudFile(GenericFile genericFile, String typeOfTransfer) {
        this.genericFile = genericFile;
        this.typeOfTransfer = typeOfTransfer;
    }

    @Override
    public String getType() {
        return typeOfTransfer;
    }

    @Override
    public Object getMessage() {
        return genericFile;
    }
}
