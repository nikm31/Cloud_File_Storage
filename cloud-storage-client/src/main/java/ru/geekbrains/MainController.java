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
import ru.geekbrains.models.Actions.Action;
import ru.geekbrains.models.Actions.Authentication;
import ru.geekbrains.models.Commands;
import ru.geekbrains.utils.FileUtils;

import java.io.File;
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
    private Network network;

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
    TextField serverSearchFile;
    @FXML
    Label authInfoBar;
    @FXML
    TextField userLoginToShare;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        mainPanel.setVisible(false);
        mainPanel.setManaged(false);
    }

    // отображение и скрытие форм приложения
    public void setActiveWindows(boolean isAuthorized) {
        authPanel.setVisible(!isAuthorized);
        authPanel.setManaged(!isAuthorized);
        mainPanel.setVisible(isAuthorized);
        mainPanel.setManaged(isAuthorized);
    }

    // кнопка Register - регистрация юзера
    public void registerUser() {
        createConnection();
        network.getChannel().writeAndFlush(new Authentication(loginField.getText(), passwordField.getText(), "", Commands.REGISTER));
    }

    // аунтефикация
    public void connectToServer() {
        createConnection();
        sendAuthMessage();
        fileListReq("");
        getServerPath();
    }

    // подключение и создание коннекта
    public void createConnection() {
        String serverAddress = getServerAddress()[0];
        short serverPort = Short.parseShort(getServerAddress()[1]);
        network = new Network(serverAddress, serverPort, this);
        network.start();
    }

    // сообщение на аунтификацию
    public void sendAuthMessage() {
        network.getChannel().writeAndFlush(new Authentication(loginField.getText(), passwordField.getText(), "", Commands.LOGIN));
    }

    // запрос списка файла сервера
    public void fileListReq(String mask) {
        network.getChannel().writeAndFlush(new Action(mask, Commands.FILTER_FILE_LIST));
    }

    // фильтрация списка файлов на сервере
    public void searchOnServer() {
        fileListReq(serverSearchFile.getText());
    }

    // запрос стрцуктуры каталога сервера
    public void getServerPath() {
        network.getChannel().writeAndFlush(new Action("", Commands.GET_DIRECTORY));
    }

    // скачиваем файл с сервера на клиент
    public void downloadFromServer() {
        //  connectionManager.getChannel().writeAndFlush(new CloudFile(new GenericFile(getSelectedServerItem(), 0, new byte[0]), Commands.DOWNLOAD));
        network.getChannel().writeAndFlush(new Action(getSelectedServerItem(), Commands.DOWNLOAD));
    }

    // закачиваем файл с клиента на сервер
    @SneakyThrows
    public void uploadToServer() {
        File fileToSend = Paths.get(clientDir.resolve(getSelectedHostItem()).toString()).toFile();
        FileUtils.getInstance().sendFileByParts(fileToSend.toPath(), network.getChannel(), 0L);
        fileListReq("");
    }

    // обновить список файлов на клиенте
    public void refreshHostFiles(String fileMask) {
        hostFileList.getItems().clear();
        hostFileList.getItems().addAll(getFiles(clientDir, fileMask));
    }

    // копирование файла на клиенте
    public void hostCopyFile() {
        try {
            Files.copy(clientDir.resolve(getSelectedHostItem()), clientDir.resolve("copy_" + getSelectedHostItem()), StandardCopyOption.REPLACE_EXISTING);
            refreshHostFiles(null);
            log.debug("File {} is copied", getSelectedHostItem());
        } catch (Exception e) {
            log.error("Cant copy file: {}", getSelectedHostItem());
        }
    }

    // удаление файла на клиенте
    @SneakyThrows
    public void hostDeleteFile() {
        File file = new File(clientDir.toString(), getSelectedHostItem());
        String[] files = file.list();
        if (file.isDirectory() & files != null) {
            for (String s : files) {
                File currentFile = new File(file.getPath(), s);
                Files.deleteIfExists(currentFile.toPath());
            }
        }
        Files.deleteIfExists(file.toPath());
        refreshHostFiles(null);
        log.debug("File {} is deleted", file);
    }

    // создание новой папки на клиенте
    @SneakyThrows
    public void hostCreateNewFolder() {
        if (Paths.get(clientDir + "\\Новая папка").toFile().exists()) {
            int i = 1;
            while (Paths.get(clientDir + "\\Новая папка" + " (" + i + ")").toFile().exists()) {
                i++;
            }
            Files.createDirectory(Paths.get(clientDir + "\\Новая папка" + " (" + i + ")"));
        }
        Files.createDirectory(Paths.get(clientDir + "\\Новая папка"));
        refreshHostFiles(null);
    }

    // фильтруем файлы на клиенте
    public void filterFilesOnHost() {
        refreshHostFiles(hostSearchFile.getText());
    }

    // возврат в папку на уровень ниже на клиенте
    public void backHostDir() {
        setPathCaption(clientDir.getParent());
        refreshHostFiles(null);
    }

    // копирование файла на сервере
    public void serverCopyFile() {
        network.getChannel().writeAndFlush(new Action(getSelectedServerItem(), Commands.COPY));
    }

    // удавление файла на сервере
    public void serverDeleteFile() {
        network.getChannel().writeAndFlush(new Action(getSelectedServerItem(), Commands.DELETE));
    }

    // закрыть соединение и выйти в окно аунтефикации
    public void closeConnection() {
        setActiveWindows(false);
        network.stop();
    }

    // назад в директорию на сервере
    public void backServerDir() {
        network.getChannel().writeAndFlush(new Action(serverPath.getText(), Commands.BACK_TO_PATH));
    }

    // создаем символическую ссылку другому юзеру на сервере на выбранный файл (шаринг)
    public void shareFileWithUser() {
        Authentication userInfoMessage = new Authentication(userLoginToShare.getText(), "", "", Commands.FIND_USER);
        userInfoMessage.setFileName(getSelectedServerItem());
        userInfoMessage.setRootDirectory(serverPath.getText());
        network.getChannel().writeAndFlush(userInfoMessage);
    }

    // переходим в папку на сервере
    public void enterToServerDir(MouseEvent mouseEvent) {
        if (mouseEvent.getClickCount() == 2) {
            network.getChannel().writeAndFlush(new Action(serverFileList.getSelectionModel().getSelectedItem(), Commands.UP_TO_PATH));
        } else if (mouseEvent.getClickCount() == 1) {
            network.getChannel().writeAndFlush(new Action(serverFileList.getSelectionModel().getSelectedItem(), Commands.FILE_SIZE));
        }
    }

    // создание новой директории на серере
    public void serverCreateNewFolder() {
        network.getChannel().writeAndFlush(new Action("", Commands.CREATE_DIRECTORY));
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

    // возвращает выбранный элемент в списке хоста
    private String getSelectedHostItem() {
        return hostFileList.getSelectionModel().getSelectedItem();
    }

    // возвращает выбранный элемент в списке сервера
    private String getSelectedServerItem() {
        return serverFileList.getSelectionModel().getSelectedItem();
    }

    // выводим размер файла(ов) в статус бар
    public void setSizeStatusBar(long size) {
        long MB = 1048576L;
        statusBar.setText("Размер файла " + ": " + (float) size / MB + " Мб");
    }

    // отображаем размер файла в статус баре при клике на файл списка клиента
    @SneakyThrows
    void setFileSize() {
        File fileToSend = Paths.get(clientDir.resolve(getSelectedHostItem()).toString()).toFile();
        long size = Files.size(fileToSend.toPath());
        long MB = 1048576L;
        statusBar.setText("Размер файла " + getSelectedHostItem() + ": " + (float) size / MB + " Мб");
    }

    // создаем клиентскую папку (на хосте не должно быть индивидуальной как на сервере)
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

    // фильтр для списка файлов
    @SneakyThrows
    private List<String> getFiles(Path path, String fileMask) {
        if (fileMask == null) {
            fileMask = "";
        }
        String finalFileMask = fileMask.trim();
        return Files.list(path)
                .map(p -> p.getFileName().toString())
                .filter(fileName -> fileName.contains(finalFileMask))
                .collect(Collectors.toList());
    }

    // формируем строку каталога
    public void setPathCaption(Path pathNew) {
        clientDir = pathNew;
        hostPath.setText(clientDir.toString());
    }

    // переходим в папку по двойному щелчку или отображение размера при одиночном
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

    // окно с ошибкой для окна авторизации
    public void errorConnectionMessage() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Connection Error");
        alert.setHeaderText("Not Connected");
        alert.setContentText("Please check address of Server or login / password");
        alert.showAndWait();
    }

}