package ru.geekbrains.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.nio.file.Path;

@Data
@AllArgsConstructor
public class Authentication implements Message {

    private String login;
    private String password;
   // private Path rootDirectory;
    private boolean isAuthenticated;
    private AuthAction authAction;

    @Override
    public String getType() {
        return "userInfo";
    }

    @Override
    public Object getMessage() {
        return this;
    }
}
