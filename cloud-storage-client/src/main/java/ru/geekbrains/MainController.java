package ru.geekbrains;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

@Slf4j
public class MainController implements Initializable {

    private Path clientDir;
    private ConnectionManager connectionManager;

    @FXML
    Label statusBar;
    @FXML
    ListView<String> serverFileList;
    @FXML
    ListView<String> hostFileList;
    @FXML
    TextField connectAddressField;
    @FXML
    TextField loginField;
    @FXML
    TextField passwordField;
    @FXML
    VBox authPanel;
    @FXML
    VBox mainPanel;
    @FXML
    TextField hostPath;
    @FXML
    TextField serverPath;
    @FXML
    TextField hostSearchFile;
    @FXML
    Label authInfoBar;
    @FXML
    TextField userLoginToShare;

    // отображаем размер файла в статус баре при клике на файл списка клиента
    @SneakyThrows
    void setFileSize() {
        File fileToSend = Paths.get(clientDir.resolve(getSelectedHostItem()).toString()).toFile();
        long size = Files.size(fileToSend.toPath());
        long MB = 1048576L;
        statusBar.setText("Размер файла " + getSelectedHostItem() + ": " + (float) size / MB + " Мб");
    }

    // отображение и скрытие форм приложения
    public void setActiveWindows(boolean isAuthorized) {
        authPanel.setVisible(!isAuthorized);
        authPanel.setManaged(!isAuthorized);
        mainPanel.setVisible(isAuthorized);
        mainPanel.setManaged(isAuthorized);
    }

    // считываем информацию с поля адрес сервера на окне авторизации
    private String[] getServerAddress() {
        String[] connection = connectAddressField.getText()
                .trim()
                .split(":");
        if (connection.length != 2) {
            errorConnectionMessage();
            log.error("Error server address or port");
            connectAddressField.setText("127.0.0.1:8189");
        }
        return connection;
    }

    // кнопка Register - регистрация юзера
    public void registerUser() {
        createConnection();
        connectionManager.sendRegistrationMessage();
    }

    // кнопка - Login на окне аунтефикации
    public void connectToServer() {
        createConnection();
        connectionManager.sendAuthMessage();
        connectionManager.fileListReq();
        connectionManager.getServerPath();
    }

    // сбор информации для подключения и создание коннекта
    public void createConnection() {
        String serverAddress = getServerAddress()[0];
        short serverPort = Short.parseShort(getServerAddress()[1]);
        connectionManager = new ConnectionManager(serverAddress, serverPort, loginField.getText(), passwordField.getText(), this);
        connectionManager.start();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        mainPanel.setVisible(false);
        mainPanel.setManaged(false);
    }

    // создаем клиентскую папку (на хосте не должно быть индивидуальной папке как на сервере)
    public void enterClientDir() {
        try {
            clientDir = Paths.get("cloud-storage-client", "client");
            if (!Files.exists(clientDir)) {
                Files.createDirectory(clientDir);
            }
            refreshHostFiles(null);
        } catch (Exception e) {
            log.debug("File create/read on host error ", e);
        }
        hostPath.setText(clientDir.toString());
    }

    // скачиваем файл с сервера на хост
    public void downloadFromServer() {
        connectionManager.downloadFile(getSelectedServerItem());
    }

    // закачиваем файл с хоста на сервер
    public void uploadToServer() throws IOException {
        File fileToSend = Paths.get(clientDir.resolve(getSelectedHostItem()).toString()).toFile();
        connectionManager.uploadFile(fileToSend);
    }

    // фильтр для списка файлов
    @SneakyThrows
    private List<String> getFiles(Path path, String fileMask) {
        if (fileMask == null) {
            fileMask = "";
        }
        String finalFileMask = fileMask;
        return Files.list(path)
                .map(p -> p.getFileName().toString())
                .filter(fileName -> fileName.contains(finalFileMask))
                .collect(Collectors.toList());
    }

    // обновить список файлов
    @SneakyThrows
    public void refreshHostFiles(String fileMask) {
        hostFileList.getItems().clear();
        hostFileList.getItems().addAll(getFiles(clientDir, fileMask));
    }

    public void hostCopyFile() {
        try {
            Files.copy(clientDir.resolve(getSelectedHostItem()), clientDir.resolve("copy_" + getSelectedHostItem()), StandardCopyOption.REPLACE_EXISTING);
            refreshHostFiles(null);
            log.debug("File {} is copied", getSelectedHostItem());
        } catch (Exception e) {
            log.error("Cant copy file: {}", getSelectedHostItem());
        }
    }

    // удаление файла на хосте
    public void hostDeleteFile() {
        try {
            Files.deleteIfExists(Paths.get(clientDir.resolve(getSelectedHostItem()).toString()));
            refreshHostFiles(null);
            log.debug("File {} is deleted", getSelectedHostItem());
        } catch (Exception e) {
            log.error("Cant delete file: {}", getSelectedHostItem());
        }
    }

    // копирование файла на сервере
    public void serverCopyFile() {
        connectionManager.serverCopyFile(getSelectedServerItem());
    }

    // удавление файла на сервере
    public void serverDeleteFile() {
        connectionManager.serverDeleteFile(getSelectedServerItem());
    }

    public void closeConnection() {
        setActiveWindows(false);
        connectionManager.stop();
    }

    // возвращает выбранный элемент в списке хоста
    private String getSelectedHostItem() {
        return hostFileList.getSelectionModel().getSelectedItem();
    }

    // возвращает выбранный элемент в списке сервера
    private String getSelectedServerItem() {
        return serverFileList.getSelectionModel().getSelectedItem();
    }

    // окно с ошибкой для окна авторизации
    public void errorConnectionMessage() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Connection Error");
        alert.setHeaderText("Not Connected");
        alert.setContentText("Please check address of Server or login / password");
        alert.showAndWait();
    }

    // создание новой папки на хосте
    public void hostCreateNewFolder() {
        try {
            if (Paths.get(clientDir + "\\Новая папка").toFile().exists()) {
                int i = 1;
                while (Paths.get(clientDir + "\\Новая папка" + " (" + i + ")").toFile().exists()) {
                    i++;
                }
                Files.createDirectory(Paths.get(clientDir + "\\Новая папка" + " (" + i + ")"));
            }
            Files.createDirectory(Paths.get(clientDir + "\\Новая папка"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        refreshHostFiles(null);
    }


    public void searchOnHost() {
        refreshHostFiles(hostSearchFile.getText());
    }

    public void searchOnServer() {
    }

    // формируем строку каталога
    public void setPathCaption(Path pathNew) {
        clientDir = pathNew;
        hostPath.setText(clientDir.toString());
    }

    // переходим в папку по двойному щелчку
    public void enterToDir(MouseEvent mouseEvent) {
        if (mouseEvent.getClickCount() == 2) {
            File fileToSend = Paths.get(clientDir.resolve(getSelectedHostItem()).toString()).toFile();
            if (fileToSend.isDirectory()) {
                setPathCaption(fileToSend.toPath());
                refreshHostFiles(null);
            }
        } else if (mouseEvent.getClickCount() == 1) {
            setFileSize();
        }
    }

    // возврат в папку на уровень ниже
    public void backHostDir() {
        setPathCaption(clientDir.getParent());
        refreshHostFiles(null);
    }

    public void backServerDir() {
    }

    // создаем символическую ссылку другому юзеру на выбранный файл
    public void shareFileWithUser() {
        connectionManager.sendReqToShareFile(userLoginToShare.getText(), getSelectedHostItem());
    }
}
