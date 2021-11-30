package ru.geekbrains.models.File;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import ru.geekbrains.models.Commands;
import ru.geekbrains.models.Message;

@Getter
@RequiredArgsConstructor
public class PartFile implements Message {

    private final byte[] message;
    private final long startPos;
    private final long endPos;
    private final boolean isLast;
    private final String filename;
    @Override
    public Commands getType() {
        return Commands.PART_FILE;
    }

}
