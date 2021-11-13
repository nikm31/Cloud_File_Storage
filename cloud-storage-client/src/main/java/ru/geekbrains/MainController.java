package ru.geekbrains;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import ru.geekbrains.models.Authentication;

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
    private String selectedHostFile;
    private String selectedServerFile;

    @FXML
    Label statusBar;
    @FXML
    ListView<String> serverFileList;
    @FXML
    ListView<String> hostFileList;
    @FXML
    TextField serverDir;
    @FXML
    TextField hostDir;
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

    private void setActiveWindows(boolean isAuthorized) {
        authPanel.setVisible(!isAuthorized);
        authPanel.setManaged(!isAuthorized);
        mainPanel.setVisible(isAuthorized);
        mainPanel.setManaged(isAuthorized);
    }

    @SneakyThrows
    public void connectToServer() {

        String[] connection = connectAddressField.getText()
                .trim()
                .split(":");

        if (connection.length != 2) {
            errorConnectionMessage();
            log.error("Error server address or port");
            connectAddressField.setText("127.0.0.1:8189");
            return;
        }
        String serverAddress = connection[0];
        short serverPort = Short.parseShort(connection[1]);

        /*
        - Передаем данные для авторизации
        - Делаем новый коннект
        - Запращиваем список файлов
        - Показываем основное окно
        */


        connectionManager = new ConnectionManager(serverAddress, serverPort, loginField.getText(), passwordField.getText(), this);
        connectionManager.start();

        setActiveWindows(true);


    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        mainPanel.setVisible(false);
        mainPanel.setManaged(false);

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

    public void downloadFromServer() {
        getSelectedServerItem();
        connectionManager.downloadFile(selectedServerFile);
    }

    public void uploadToServer() throws IOException {
        getSelectedHostItem();
        File fileToSend = Paths.get(clientDir.resolve(selectedHostFile).toString()).toFile();
        connectionManager.uploadFile(fileToSend);
    }

    @SneakyThrows
    private List<String> getFiles(Path path, String filemask) {
        if (filemask == null) {
            filemask = "";
        }
        String finalFilemask = filemask;
        return Files.list(path)
                .map(p -> p.getFileName().toString())
                .filter(fileName -> fileName.contains(finalFilemask))
                .collect(Collectors.toList());
    }

    public void serverBackToDir() {
    }

    public void hostBackToDir() {
    }

    @SneakyThrows
    public void refreshHostFiles(String filemask) {
        hostFileList.getItems().clear();
        hostFileList.getItems().addAll(getFiles(clientDir, filemask));
    }

    @SneakyThrows
    public void refreshHostFiles() {
        refreshHostFiles(null);
    }

    public void hostCopyFile() {
        try {
            getSelectedHostItem();
            Files.copy(clientDir.resolve(selectedHostFile), clientDir.resolve("copy_" + selectedHostFile), StandardCopyOption.REPLACE_EXISTING);
            refreshHostFiles(null);
            log.debug("File {} is copied", selectedHostFile);
        } catch (Exception e) {
            log.error("Cant copy file: {}", selectedHostFile);
        }
    }

    public void hostDeleteFile() {
        try {
            getSelectedHostItem();
            Files.deleteIfExists(Paths.get(clientDir.resolve(selectedHostFile).toString()));
            refreshHostFiles(null);
            log.debug("File {} is deleted", selectedHostFile);
        } catch (Exception e) {
            log.error("Cant delete file: {}", selectedHostFile);
        }
    }

    public void serverCopyFile() {
        getSelectedServerItem();
        connectionManager.serverCopyFile(selectedServerFile);
    }

    public void serverDeleteFile() {
        getSelectedServerItem();
        connectionManager.serverDeleteFile(selectedServerFile);
    }

    public void closeConnection() {
        setActiveWindows(false);
        connectionManager.stop();
    }

    private void getSelectedHostItem() {
        selectedHostFile = hostFileList.getSelectionModel().getSelectedItem();
    }

    private void getSelectedServerItem() {
        selectedServerFile = serverFileList.getSelectionModel().getSelectedItem();
    }

    private void errorConnectionMessage() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Connection Error");
        alert.setHeaderText("Not Connected");
        alert.setContentText("Please check address and port of Server");
        alert.showAndWait();
    }

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

    public void registerUser() {

    }

    public void searchOnHost(ActionEvent actionEvent) {
        refreshHostFiles(hostSearchFile.getText());

    }

    public void searchOnServer(ActionEvent actionEvent) {
    }

    public void setPathCaption(Path pathNew) {
        clientDir = pathNew;
        hostPath.setText(clientDir.toString());
    }

    public void enterToDir(MouseEvent mouseEvent) {
        if (mouseEvent.getClickCount() == 2) {
            getSelectedHostItem();
            File fileToSend = Paths.get(clientDir.resolve(selectedHostFile).toString()).toFile();


            if (fileToSend.isDirectory()) {
                setPathCaption(fileToSend.toPath());
                refreshHostFiles();
            }
        }
    }

    public void backHostDir(MouseEvent mouseEvent) {
        setPathCaption(clientDir.getParent());
        refreshHostFiles();
    }
}
