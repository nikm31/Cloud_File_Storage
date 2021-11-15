package ru.geekbrains.models;

import lombok.Data;

@Data
public class Authentication implements Message {

    public enum AuthStatus {
        LOGIN,
        REGISTER,
        FIND_USER,
        USER_FOUND,
        USER_NOTFOUND,
    }

    private String login;
    private String password;
    private String rootDirectory;
    private boolean isAuthenticated;
    private AuthStatus authStatus;
    private String fileName;

    public Authentication() {}



    public Authentication(String login, String password, String rootDirectory, boolean isAuthenticated, AuthStatus authStatus) {
        this.login = login;
        this.password = password;
        this.rootDirectory = rootDirectory;
        this.isAuthenticated = isAuthenticated;
        this.authStatus = authStatus;
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
