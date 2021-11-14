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
    private Authentication authInfoReceived;

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
        authInfoReceived = (Authentication) message;

        if (authInfoReceived.getAuthAction() == Authentication.AuthAction.LOGIN & authInfoReceived.isAuthenticated() == true) {
            mainController.setActiveWindows(true);
        } else if (authInfoReceived.getAuthAction() == Authentication.AuthAction.LOGIN & authInfoReceived.isAuthenticated() == false) {
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
        Platform.runLater(mainController::refreshHostFiles);
    }

    public void refreshStatusBar(Message message) {
        Platform.runLater(() -> mainController.statusBar.setText((String) message.getMessage()));
    }


    public void downloadFile(Message message, ChannelHandlerContext channel) {
        try {
            File dir = new File("C:\\Users\\Nikolay\\Desktop\\FileCloudStorage\\cloud-storage-client\\client");
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
