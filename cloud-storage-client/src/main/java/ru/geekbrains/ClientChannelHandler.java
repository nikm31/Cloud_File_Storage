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
    protected void channelRead0(ChannelHandlerContext channel, Message message) {
        log.info("Получено {}", message);

        if (message.getType().equals("status")) {
            refreshStatusBar(message);
        } else if (message.getType().equals("upload")) {
            downloadFile(message, channel);
        } else if (message.getType().equals("fileList")) {
            refreshServerList(message);
        } else if (message.getType().equals("getDirectory")) {
            refreshServerPath(message);
        } else if (message.getType().equals("userInfo")) {
            authorizeClient(message);
        }
    }

    private void authorizeClient(Message message) {
        Authentication authInfoReceived = (Authentication) message;

        if (authInfoReceived.getAuthStatus() == Authentication.AuthStatus.LOGIN & authInfoReceived.isAuthenticated()) {
            // если статус запроса Login и сервер вернул, что авторизация пройдена показываем основную форму
            mainController.setActiveWindows(true);
            mainController.enterClientDir();
        } else if (authInfoReceived.getAuthStatus() == Authentication.AuthStatus.LOGIN & !authInfoReceived.isAuthenticated()) {
            Platform.runLater(() -> {
                mainController.authInfoBar.setText("Не верный логин / пароль");
                mainController.errorConnectionMessage();
            });
        }
    }

    private void refreshServerPath(Message message) {
        mainController.serverPath.setText(message.getMessage().toString());
    }

    public void refreshServerList(Message message) {
        Platform.runLater(() -> {
            mainController.serverFileList.getItems().clear();
            mainController.serverFileList.getItems().addAll((List<String>) message.getMessage());
        });
    }

    public void refreshHostList() {
        Platform.runLater(() -> mainController.refreshHostFiles(null));
    }

    public void refreshStatusBar(Message message) {
        Platform.runLater(() -> mainController.statusBar.setText((String) message.getMessage()));
    }

    // огика скачивания файла с сервера
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
