package ru.geekbrains.models.File;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class GenericFile implements Serializable {
    private String fileName;
    private long fileSize;
    private byte[] content;
}
