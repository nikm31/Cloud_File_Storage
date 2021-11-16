package ru.geekbrains;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import javafx.application.Platform;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.geekbrains.models.Authentication;
import ru.geekbrains.models.GenericFile;
import ru.geekbrains.models.Message;
import ru.geekbrains.models.Status;

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

        log.info("Получено {}", message);

        if (message.getType().equals("USER_INFO")) {
            authorizeClient(message);
        }
        if (message.getType().equals("STATUS")) {
            refreshStatusBar(message);
        }
        if (message.getType().equals("UPLOAD")) {
            downloadFile(message, channel);
        }
        if (message.getType().equals("FILE_LIST")) {
            refreshServerList(message);
        }
        if (message.getType().equals("SEND_DIRECTORY")) {
            refreshServerPath(message);
        }
    }

    // обрабатываем ответы сервера на авторизацию / регистрацию
    private void authorizeClient(Message message) {

        Authentication authInfoReceived = (Authentication) message;

        if (authInfoReceived.getAuthStatus() == Authentication.AuthStatus.AUTHENTICATED) {
            mainController.setActiveWindows(true);
            mainController.enterClientDir();
            log.debug("Authentication successful");
        } else if (authInfoReceived.getAuthStatus() == Authentication.AuthStatus.NOT_AUTHENTICATED) {
            Platform.runLater(() -> {
                mainController.authInfoBar.setText("Не верный логин / пароль");
                mainController.errorConnectionMessage();
            });
            log.debug("Authentication failed");
        }
        if (authInfoReceived.getAuthStatus() == Authentication.AuthStatus.REGISTERED) {
            Platform.runLater(() -> mainController.authInfoBar.setText("Успешная регистрация под ником: " + authInfoReceived.getLogin()));
            log.debug("Registration is success");
        } else if (authInfoReceived.getAuthStatus() == Authentication.AuthStatus.NOT_REGISTERED) {
            Platform.runLater(() -> mainController.authInfoBar.setText("Данный логин уже используется в системе. В регистрации отказано."));
            log.debug("Registration failed");
        }
        if (authInfoReceived.getAuthStatus() == Authentication.AuthStatus.USER_FOUND) {
            Platform.runLater(() -> mainController.statusBar.setText("Файл успешно расшарен юзеру: " + authInfoReceived.getLogin()));
            log.debug("Link is shared");
        } else if (authInfoReceived.getAuthStatus() == Authentication.AuthStatus.USER_NOT_FOUND) {
            Platform.runLater(() -> mainController.authInfoBar.setText("Такого юзера " + authInfoReceived.getLogin() + " нет в системе. Расшарить файл невозможно"));
            log.debug("Link is not shared");
        }
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
    public void downloadFile(Message message, ChannelHandlerContext channel) {
        try {
            File dir = new File(mainController.hostPath.getText());
            GenericFile fileSource = (GenericFile) message.getMessage();
            File fileToCreate = new File(dir, fileSource.getFilename());
            FileOutputStream fos = new FileOutputStream(fileToCreate);
            fos.write(fileSource.getContent());
            fos.close();
            refreshHostList();
            channel.writeAndFlush(new Status("ok"));
        } catch (Exception e) {
            log.error("Write error file to host", e);
        }
    }

}
