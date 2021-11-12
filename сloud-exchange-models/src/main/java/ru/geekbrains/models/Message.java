package ru.geekbrains.models;
import java.io.Serializable;

public interface Message extends Serializable {
    public String getType();
    public Object getMessage();
}
