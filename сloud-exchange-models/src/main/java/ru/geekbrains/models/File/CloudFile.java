package ru.geekbrains.models.File;

import lombok.RequiredArgsConstructor;
import ru.geekbrains.models.Commands;
import ru.geekbrains.models.Message;

@RequiredArgsConstructor
public class CloudFile implements Message {

    private final GenericFile genericFile;
    private Commands command;



    public CloudFile(GenericFile genericFile, Commands command) {
        this.genericFile = genericFile;
        this.command = command;
    }

    @Override
    public Commands getType() {
        return command;
    }

    @Override
    public Object getMessage() {
        return genericFile;
    }
}
