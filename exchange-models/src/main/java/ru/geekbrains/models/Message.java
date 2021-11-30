package ru.geekbrains.models;
import java.io.Serializable;

public interface Message extends Serializable {
    Commands getType();
    Object getMessage();
}
