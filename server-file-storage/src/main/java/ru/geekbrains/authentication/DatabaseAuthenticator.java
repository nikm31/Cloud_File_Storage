package ru.geekbrains.authentication;

import lombok.extern.slf4j.Slf4j;
import ru.geekbrains.models.Authentication;

import java.sql.*;

@Slf4j
public class DatabaseAuthenticator implements AuthenticationProvider {
    private Connection connection;
    private Statement statement;
    private Authentication authentication;

    public DatabaseAuthenticator() {
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:fileStorageDB.db");
            statement = connection.createStatement();
            authentication = new Authentication();
            createDB();
        } catch (Exception e) {
            log.error("SQL connect Error");
        }
    }

    @Override
    public Authentication userAuthentication(String login, String password) {
        try (PreparedStatement preparedStatement = connection.prepareStatement("select rootFolder from users where login = ? and password = ?")) {
            preparedStatement.setString(1, login);
            preparedStatement.setString(2, password);
            ResultSet rs = preparedStatement.executeQuery();
            if (rs.next()) {
                authentication.setRootDirectory(rs.getString("rootFolder"));
                authentication.setAuthenticated(true);
            } else {
                authentication.setAuthenticated(false);
            }
            authentication.setAuthAction(Authentication.AuthAction.LOGIN);
            return authentication;
        } catch (Exception e) {
            log.error("Error statement", e);
        }
        log.debug("Login or password is accepted");
        return authentication;
    }

    @Override
    public Authentication userRegistration(String login, String password) {
        try (PreparedStatement psCheckUser = connection.prepareStatement("select id from users where login = ?")) {
            psCheckUser.setString(1, login);
            ResultSet rsCheck = psCheckUser.executeQuery();
            if (rsCheck.next()) {
                log.error("Login is used by other user");
                return authentication;
            }
        } catch (Exception e) {
            log.error("Error statement", e);
        }

        log.debug("Trying to create entity in DB");
        try (PreparedStatement psRegister = connection.prepareStatement("insert into users (login, password, rootFolder) values (?,?,?)")) {
            String rootDir = login + "_rootDir";
            psRegister.setString(1, login);
            psRegister.setString(2, password);
            psRegister.setString(3, rootDir);
            psRegister.execute();
            authentication.setRootDirectory(rootDir);
            authentication.setAuthenticated(true);
            authentication.setAuthAction(Authentication.AuthAction.REGISTER);
            return authentication;
        } catch (Exception e) {
            log.error("Error statement", e);
        }
        log.debug("User {} added to BD", login);
        return authentication;
    }

    @Override
    public void createDB() {
        try {
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
