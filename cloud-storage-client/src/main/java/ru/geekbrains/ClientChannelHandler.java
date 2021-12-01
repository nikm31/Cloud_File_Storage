package ru.geekbrains;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import javafx.application.Platform;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.geekbrains.models.Actions.Authentication;
import ru.geekbrains.models.Actions.PartFileInfo;
import ru.geekbrains.models.File.PartFile;
import ru.geekbrains.models.Message;
import ru.geekbrains.utils.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class ClientChannelHandler extends SimpleChannelInboundHandler<Message> {
    private final MainController mainController;

    @Override
    // проверяем статусы сообщений от сервера
    protected void channelRead0(ChannelHandlerContext channel, Message message) throws IOException {
        log.info("Получена команда {}", message.getType());
        switch (message.getType()) {
            case AUTHENTICATED:
                authenticated();
                break;
            case NOT_AUTHENTICATED:
                notAuthenticated();
                break;
            case REGISTERED:
                registered(message);
                break;
            case NOT_REGISTERED:
                notRegistered(message);
                break;
            case USER_FOUND:
                userFound(message);
                break;
            case USER_NOT_FOUND:
                userNotFound(message);
                break;
            case STATUS:
                refreshStatusBar(message);
                break;
            case FILE_SIZE:
                refreshStatusBarFilesSize(message);
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
            case PART_FILE_INFO:
                sendNextPart((PartFileInfo) message, channel);
                break;
            case PART_FILE:
                receivePartOfFile((PartFile) message, channel);
                break;
        }
    }

    // получение файла с сервера
    private void receivePartOfFile(PartFile partFile, ChannelHandlerContext channel) {
        Path filePath = new File(mainController.hostPath.getText(), partFile.getFilename()).toPath();
        try {
            FileUtils.getInstance().prepareAndSavePart(filePath, partFile.getStartPos(), partFile.getMessage());
            if (!partFile.isLast()) {
                PartFileInfo partFileInfo = new PartFileInfo(partFile.getEndPos(), partFile.getFilename());
                channel.writeAndFlush(partFileInfo);
            }
            refreshHostList();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // запрос следующей части файла
    private void sendNextPart(PartFileInfo partFileInfo, ChannelHandlerContext channel) throws IOException {
        FileUtils.getInstance().sendFileByParts(new File(mainController.hostPath.getText(), partFileInfo.getFilename()).toPath(), channel.channel(), (long) partFileInfo.getMessage());
    }

    // обновление размера файла на статус баре
    private void refreshStatusBarFilesSize(Message message) {
        Platform.runLater(() -> mainController.setSizeStatusBar(Long.parseLong((String) message.getMessage())));
    }

    // обновляем путь на форме
    private void updateServerPath(Message message) {
        Platform.runLater(() -> {
            mainController.serverPath.clear();
            mainController.serverPath.setText(message.getMessage().toString());
        });
    }

    private void authenticated() {
        Platform.runLater(() -> {
            mainController.setActiveWindows(true);
            mainController.enterClientDir();
            mainController.statusBar.setText("Успешная авторизация");
        });
        log.info("Authentication successful");
    }

    private void notAuthenticated() {
        Platform.runLater(() -> {
            mainController.authInfoBar.setText("Не верный логин / пароль");
            mainController.errorConnectionMessage();
        });
        log.debug("Authentication failed");
    }

    private void registered(Message message) {
        Authentication authInfo = (Authentication) message.getMessage();
        Platform.runLater(() -> mainController.authInfoBar.setText("Успешная регистрация под логином: " + authInfo.getLogin())); // getLogin()
        log.info("Registration is success");
    }

    private void notRegistered(Message message) {
        Authentication authInfo = (Authentication) message.getMessage();
        Platform.runLater(() -> mainController.authInfoBar.setText("Данный логин: " + authInfo.getLogin() + " уже используется в системе. В регистрации отказано."));
        log.debug("Registration failed");
    }

    private void userFound(Message message) {
        Platform.runLater(() -> mainController.statusBar.setText("Файл успешно расшарен юзеру: " + message.getMessage()));//.getLogin()
        log.info("Link is shared");
    }

    private void userNotFound(Message message) {
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

}