package ru.geekbrains.models.Actions;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import ru.geekbrains.models.Commands;
import ru.geekbrains.models.Message;

@RequiredArgsConstructor
@Getter
public class PartFileInfo implements Message {

    private final long receivedOffset;
    private final String filename;
    @Override
    public Commands getType() {
        return Commands.PART_FILE_INFO;
    }

    @Override
    public Object getMessage() {
        return receivedOffset;
    }


}
