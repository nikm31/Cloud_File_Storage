package ru.geekbrains.authentication;

public interface AuthenticationProvider {
    String authByLoginAndPassword(String userName, String password);
    void createDB();
}
