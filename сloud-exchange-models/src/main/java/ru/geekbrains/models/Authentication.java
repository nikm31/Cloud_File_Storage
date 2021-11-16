package ru.geekbrains.models;

import lombok.Data;

@Data
public class Authentication implements Message {

    public enum AuthStatus {
        LOGIN,
        REGISTER,
        FIND_USER,
        AUTHENTICATED,
        NOT_AUTHENTICATED,
        REGISTERED,
        NOT_REGISTERED,
        USER_FOUND,
        USER_NOT_FOUND,
    }

    private String login;
    private String password;
    private String rootDirectory;
    private AuthStatus authStatus;
    private String fileName;

    public Authentication() {}

    public Authentication(String login, String password, String rootDirectory, boolean isAuthenticated, AuthStatus authStatus) {
        this.login = login;
        this.password = password;
        this.rootDirectory = rootDirectory;
        this.authStatus = authStatus;
    }

    @Override
    public String getType() {
        return "USER_INFO";
    }

    @Override
    public Object getMessage() {
        return this;
    }
}
