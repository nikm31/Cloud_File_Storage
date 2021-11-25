package ru.geekbrains.authentication;

import ru.geekbrains.models.Actions.Authentication;

public interface DbProvider {
    Authentication userAuthentication(String login, String password);
    Authentication userRegistration(String login, String password);
    Authentication getUserRootFolderByLogin(String login);
    void disconnect();
}
