package ru.geekbrains.models;

import lombok.Data;

@Data
public class Authentication implements Message {

    public enum AuthAction {
        LOGIN,
        REGISTER
    }

    private String login;
    private String password;
    private String rootDirectory;
    private boolean isAuthenticated;
    private AuthAction authAction;

    public Authentication() {}

    public Authentication(String login, String password, String rootDirectory, boolean isAuthenticated, AuthAction authAction) {
        this.login = login;
        this.password = password;
        this.rootDirectory = rootDirectory;
        this.isAuthenticated = isAuthenticated;
        this.authAction = authAction;
    }

    @Override
    public String getType() {
        return "userInfo";
    }

    @Override
    public Object getMessage() {
        return this;
    }
}
