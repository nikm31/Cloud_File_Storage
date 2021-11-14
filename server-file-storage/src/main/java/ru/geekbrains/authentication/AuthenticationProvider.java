package ru.geekbrains.authentication;

import ru.geekbrains.models.Authentication;

public interface AuthenticationProvider {
    Authentication userAuthentication(String login, String password);
    Authentication userRegistration(String login, String password);
    void createDB();
}
