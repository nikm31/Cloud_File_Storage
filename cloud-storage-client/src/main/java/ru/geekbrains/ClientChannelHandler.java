package ru.geekbrains;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import javafx.application.Platform;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.geekbrains.models.*;
import ru.geekbrains.models.Actions.Authentication;
import ru.geekbrains.models.File.GenericFile;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class ClientChannelHandler extends SimpleChannelInboundHandler<Message> {
    private final MainController mainController;

    @Override
    // проверяем статусы сообщений от сервера
    protected void channelRead0(ChannelHandlerContext channel, Message message) {
        log.info("Получена команда {}", message.getType());
        switch (message.getType()) {
            case AUTHENTICATED:
                isAuthTrue();
                break;
            case NOT_AUTHENTICATED:
                isAuthFalse();
                break;
            case REGISTERED:
                isRegisteredTrue(message);
                break;
            case NOT_REGISTERED:
                isRegisteredFalse(message);
                break;
            case USER_FOUND:
                isUserFoundTrue(message);
                break;
            case USER_NOT_FOUND:
                isUserFoundFalse(message);
                break;
            case STATUS:
                refreshStatusBar(message);
                break;
            case FILE_SIZE:
                refreshStatusBarFilesSize(message);
                break;
            case UPLOAD:
                downloadFile(message);
                channel.flush();
                break;
            case FILE_LIST:
                refreshServerList(message);
                break;
            case SEND_CURRENT_PATH:
                refreshServerPath(message);
                break;
            case CURRENT_PATH:
                updateServerPath(message);
                break;
        }
    }

    // обновление размера файла на статус баре
    private void refreshStatusBarFilesSize(Message message) {
        Platform.runLater(() -> mainController.setSizeStatusBar(Long.valueOf((String) message.getMessage())));
    }

    // обновляем путь на форме
    private void updateServerPath(Message message) {
        Platform.runLater(() -> {
            mainController.serverPath.clear();
            mainController.serverPath.setText(message.getMessage().toString());
        });
    }

    private void isAuthTrue() {
        mainController.setActiveWindows(true);
        mainController.enterClientDir();
        log.info("Authentication successful");
    }

    private void isAuthFalse() {
        Platform.runLater(() -> {
            mainController.authInfoBar.setText("Не верный логин / пароль");
            mainController.errorConnectionMessage();
        });
        log.debug("Authentication failed");
    }

    private void isRegisteredTrue(Message message) {
        Authentication authInfo = (Authentication) message.getMessage();
        Platform.runLater(() -> mainController.authInfoBar.setText("Успешная регистрация под логином: " + authInfo.getLogin())); // getLogin()
        log.info("Registration is success");
    }

    private void isRegisteredFalse(Message message) {
        Authentication authInfo = (Authentication) message.getMessage();
        Platform.runLater(() -> mainController.authInfoBar.setText("Данный логин: " + authInfo.getLogin() +  " уже используется в системе. В регистрации отказано."));
        log.debug("Registration failed");
    }

    private void isUserFoundTrue(Message message) {
        Platform.runLater(() -> mainController.statusBar.setText("Файл успешно расшарен юзеру: " + message.getMessage()));//.getLogin()
        log.info("Link is shared");
    }


    private void isUserFoundFalse(Message message) {
        Platform.runLater(() -> mainController.authInfoBar.setText("Такого юзера " + message.getMessage() + " нет в системе. Расшарить файл невозможно"));//.getLogin()
        log.debug("Link is not shared");
    }

    // обновляем путь дерриктории на сервере
    private void refreshServerPath(Message message) {
        mainController.serverPath.setText(message.getMessage().toString());
    }

    // обновляем список файлов на сервере
    public void refreshServerList(Message message) {
        Platform.runLater(() -> {
            mainController.serverFileList.getItems().clear();
            mainController.serverFileList.getItems().addAll((List<String>) message.getMessage());
        });
    }

    public void refreshHostList() {
        Platform.runLater(() -> mainController.refreshHostFiles(null));
    }

    // выводим сообщение в статус бар
    public void refreshStatusBar(Message message) {
        Platform.runLater(() -> mainController.statusBar.setText((String) message.getMessage()));
    }

    // логика скачивания файла с сервера
    public void downloadFile(Message message) {
        try {
            File dir = new File(mainController.hostPath.getText());
            GenericFile fileSource = (GenericFile) message.getMessage();
            File fileToCreate = new File(dir, fileSource.getFileName());
            FileOutputStream fos = new FileOutputStream(fileToCreate);
            fos.write(fileSource.getContent());
            fos.close();
            refreshHostList();
            log.info("File is downloaded");
        } catch (Exception e) {
            log.error("Write error file to host", e);
        }
    }

}
