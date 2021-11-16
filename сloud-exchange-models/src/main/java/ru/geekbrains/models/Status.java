package ru.geekbrains.models;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class Status implements Message{
private final String message;

    @Override
    public String getType() {
        return "STATUS";
    }

}
