package ru.geekbrains.models;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.nio.file.Path;

@AllArgsConstructor
@Getter
public class PartFileWriteWrapper {
    private final Path path;
    private final byte[] data;
}
