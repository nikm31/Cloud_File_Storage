package ru.geekbrains.authentication;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.mindrot.jbcrypt.BCrypt;
import ru.geekbrains.models.Actions.Authentication;
import ru.geekbrains.models.Commands;

import java.sql.*;

@Slf4j
public class DbHandler implements DbProvider {
    private Connection connection;
    private Statement statement;
    private Authentication authentication;

    public DbHandler() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:fileStorageDB.db");
            statement = connection.createStatement();
            authentication = new Authentication();
            log.info("Connected to DB");
            createDB();
        } catch (Exception e) {
            log.error("SQL connect Error ", e);
        }
    }

    private String encrypt(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt(12));
    }

    @SneakyThrows
    private void checkPass(String inputPass, String hashPass, ResultSet rs) {
        if (BCrypt.checkpw(inputPass, hashPass)) {
            authentication.setRootDirectory(rs.getString("rootFolder"));
            authentication.setCommand(Commands.AUTHENTICATED);
            log.info("Login or password is accepted");
        } else {
            authentication.setCommand(Commands.NOT_AUTHENTICATED);
        }
    }

    public void disconnect() {
        try {
            connection.close();
            log.info("Closed connection with DB");
        } catch (SQLException e) {
            log.error("Error closing connection with DB ", e);
        }
    }

    @Override
    public Authentication userAuthentication(String login, String inputPass) {
        try (PreparedStatement preparedStatement = connection.prepareStatement("select password, rootFolder from users where login = ?")) {
            preparedStatement.setString(1, login);
            ResultSet rs = preparedStatement.executeQuery();
            if (rs.next()) {
                checkPass(inputPass, rs.getString("password"), rs);
            } else {
                authentication.setCommand(Commands.NOT_AUTHENTICATED);
            }
        } catch (Exception e) {
            log.error("Error statement", e);
        }
        authentication.setLogin(login);
        return authentication;
    }

    @Override
    public Authentication userRegistration(String login, String password) {
        try (PreparedStatement psCheckUser = connection.prepareStatement("select id from users where login = ?")) {
            psCheckUser.setString(1, login);
            ResultSet rsCheck = psCheckUser.executeQuery();
            if (rsCheck.next()) {
                log.debug("Login is used by other user");
                authentication.setCommand(Commands.NOT_REGISTERED);
                authentication.setLogin(login);
                return authentication;
            }
        } catch (Exception e) {
            log.error("Error statement", e);
        }

        log.debug("Trying to create entity in DB");

        try (PreparedStatement psRegister = connection.prepareStatement("insert into users (login, password, rootFolder) values (?,?,?)")) {
            String rootDir = login + "_rootDir";
            psRegister.setString(1, login);
            psRegister.setString(2, encrypt(password));
            psRegister.setString(3, rootDir);
            psRegister.execute();
            authentication.setRootDirectory(rootDir);
            authentication.setCommand(Commands.REGISTERED);
            authentication.setLogin(login);
            return authentication;
        } catch (Exception e) {
            log.error("Error statement", e);
        }
        log.info("User {} added to BD", login);
        return authentication;
    }

    @Override
    public Authentication getUserRootFolderByLogin(String login) {
        try (PreparedStatement preparedStatement = connection.prepareStatement("select rootFolder from users where login = ?")) {
            preparedStatement.setString(1, login);
            ResultSet rs = preparedStatement.executeQuery();
            if (rs.next()) {
                authentication.setRootDirectory(rs.getString("rootFolder"));
                authentication.setCommand(Commands.USER_FOUND);
                authentication.setLogin(login);
                log.info("Login is found in DB. Sending rootFolder");
            } else {
                authentication.setCommand(Commands.USER_NOT_FOUND);
                authentication.setLogin(login);
                log.debug("Login is not found in DB. Sending status");
            }
            return authentication;
        } catch (Exception e) {
            log.error("Error statement", e);
        }
        log.info("Login or password is accepted");
        return authentication;
    }

    public void createDB() {
        try {
            String sql = "CREATE TABLE IF NOT EXISTS users (\n" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,\n" +
                    "login VARCHAR (30) NOT NULL UNIQUE,\n" +
                    "password STRING NOT NULL,\n" +
                    "rootFolder VARCHAR (30) NOT NULL\n" +
                    ");";
            statement.executeUpdate(sql);
        } catch (SQLException e) {
            log.error("Statement of creation BD error: ", e);
        }
    }
}
