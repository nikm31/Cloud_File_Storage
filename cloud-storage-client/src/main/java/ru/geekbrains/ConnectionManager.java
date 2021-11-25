package ru.geekbrains;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import ru.geekbrains.models.Actions.Action;
import ru.geekbrains.models.Actions.Authentication;
import ru.geekbrains.models.Actions.FileList;
import ru.geekbrains.models.Commands;
import ru.geekbrains.models.File.CloudFile;
import ru.geekbrains.models.File.GenericFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ConnectionManager {

    private final String serverAddress;
    private final short serverPort;
    private Channel channel;
    private final MainController mainController;
    private final String login;
    private final String password;
    private EventLoopGroup workGroup;

    public ConnectionManager(String serverAddress, short serverPort, String login, String password, MainController mainController) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.mainController = mainController;
        this.login = login;
        this.password = password;
    }

    // создаем подключение и подключаемся к серверу при нажатии Login
    void start() {
        workGroup = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = new Bootstrap().group(workGroup)
                    .channel(NioSocketChannel.class)
                    .handler(new ConnectionInitializer(mainController));
            ChannelFuture f = bootstrap.connect(serverAddress, serverPort);
            channel = f.sync().channel();
        } catch (Exception e) {
            log.error("Cant start server", e);
        }
        log.debug("Connection with Server is active");
    }

    @SneakyThrows
    void stop() {
        workGroup.shutdownGracefully();
    }

    // отправка файла на сервер
    public void uploadFile(File file) throws IOException {
        channel.writeAndFlush(new CloudFile(new GenericFile(file.getName(), file.length(), Files.readAllBytes(file.toPath())), Commands.UPLOAD));
        fileListReq();
    }

    // отправка запроса на шаринг файла с пользователем
    public void sendReqToShareFile(String login, String fileName) {
        Authentication userInfoMessage = new Authentication(login, "", "", Commands.FIND_USER);
        userInfoMessage.setFileName(fileName);
        userInfoMessage.setRootDirectory(mainController.serverPath.getText());
        channel.writeAndFlush(userInfoMessage);
    }

    // Посылаем сообщение на аунтификацию польтзователя
    public void sendAuthMessage() {
        channel.writeAndFlush(new Authentication(login, password, "", Commands.LOGIN));
    }

    // регистрируем юзера
    public void sendRegistrationMessage() {
        channel.writeAndFlush(new Authentication(login, password, "", Commands.REGISTER));
    }

    // копирование файла на сервере
    public void serverCopyFile(String file) {
        channel.writeAndFlush(new Action(file, Commands.COPY));
    }

    // удаление файла на сервере
    public void serverDeleteFile(String file) {
        channel.writeAndFlush(new Action(file, Commands.DELETE));
    }

    // скачивание файла с сервера
    public void downloadFile(String file) {
        channel.writeAndFlush(new CloudFile(new GenericFile(file, 0, new byte[0]), Commands.DOWNLOAD));
    }

    // запрос стрцуктуры каталога сервера
    public void getServerPath() {
        channel.writeAndFlush(new Action("", Commands.GET_DIRECTORY));
    }

    // запрос списка файла сервера
    public void fileListReq() {
        try {
            List<String> empty = new ArrayList<>();
            channel.writeAndFlush(new FileList(empty));
        } catch (Exception e) {
            log.error("FileList Req Error");
        }
    }

    // запрос создание папки на серверу
    public void sendReqToCreateFolder() {
        channel.writeAndFlush(new Action("", Commands.CREATE_DIRECTORY));
    }

    // перейти в папку сервера
    public void enterToServerDir(String selectedItem) {
        channel.writeAndFlush(new Action(selectedItem, Commands.UP_TO_PATH));
    }

    // получить размер файла на сервере
    public void getServerFileSize(String selectedItem) {
        channel.writeAndFlush(new Action(selectedItem, Commands.FILE_SIZE));
    }

    // перейти в директорию родителя
    public void backToParentDir(String path) {
        channel.writeAndFlush(new Action(path, Commands.BACK_TO_PATH));
    }
}
