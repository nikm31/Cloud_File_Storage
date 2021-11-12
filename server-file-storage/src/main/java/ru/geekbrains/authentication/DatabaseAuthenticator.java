package ru.geekbrains.authentication;

import lombok.extern.slf4j.Slf4j;

import java.sql.*;

@Slf4j
public class DatabaseAuthenticator implements AuthenticationProvider {
    private Connection connection;
    private Statement statement;
    private String userRootDirectory;

    @Override
    public String authByLoginAndPassword(String login, String password) {
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:fileStorageDB.db");
            statement = connection.createStatement();

            try (PreparedStatement preparedStatement = connection.prepareStatement("select rootFolder from users where login = ? and password = ?")) {
                preparedStatement.setString(1, login);
                preparedStatement.setString(2, password);
                ResultSet rs = preparedStatement.executeQuery();

                if (!rs.next()) {
                    log.debug("Login or password Enter error");
                } else {
                    userRootDirectory = rs.getString("rootFolder");
                }

            } catch (Exception e) {
                log.error("Error statement {}", e);
            }
        } catch (Exception e) {
            log.error("Error connecting to DB {}", e);
        }
        return userRootDirectory;
    }

    public void createDB() {
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:fileStorageDB.db");
            statement = connection.createStatement();
            String sql = "create table if not exists users (\n" +
                    "id integer primary key autoincrement not null,\n" +
                    "login VARCHAR (30) not null UNIQUE,\n" +
                    "password VARCHAR (30) not null,\n" +
                    "rootFolder VARCHAR (30) not null\n" +
                    ");";
            statement.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
