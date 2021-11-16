package ru.geekbrains.models;
import java.io.Serializable;

public interface Message extends Serializable {
    String getType();
    Object getMessage();
}
