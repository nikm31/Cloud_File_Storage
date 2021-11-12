package ru.geekbrains.models;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class GenericFile implements Serializable {
    private String filename;
    private byte[] content;
}
