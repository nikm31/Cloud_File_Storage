package ru.geekbrains.models;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Authentication implements Message {

    public enum Action {LOIN, REGISTER};

    private String login;
    private String password;
    private String rootDirectory;
    private boolean isAuthenticated;
    private Action action;

    @Override
    public String getType() {
        return "authentication";
    }

    @Override
    public Object getMessage() {
        return rootDirectory;
    }
}
