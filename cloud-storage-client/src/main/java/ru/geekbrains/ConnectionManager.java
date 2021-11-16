package ru.geekbrains;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;
import lombok.extern.slf4j.Slf4j;
import ru.geekbrains.models.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ConnectionManager {

    static ObjectEncoderOutputStream out;
    static ObjectDecoderInputStream in;
    private final String serverAddress;
    private final short serverPort;
    private Channel channel;
    private final MainController mainController;
    private final String login;
    private final String password;
    private Authentication userInfoMessage;

    public ConnectionManager(String serverAddress, short serverPort, String login, String password, MainController mainController) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.mainController = mainController;
        this.login = login;
        this.password = password;
    }

    // создаем подключение и подключаемся к серверу при нажатии Login
    void start() {
        EventLoopGroup group = new NioEventLoopGroup();
        try {

            Bootstrap bootstrap = new Bootstrap().group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ConnectionInitializer(mainController));
            ChannelFuture f = bootstrap.connect(serverAddress, serverPort);
            channel = f.sync().channel();

        } catch (Exception e) {
            log.error("Cant start server", e);
        }
        log.debug("Connection with Server is active");
    }

    // закрываем соединение и скрываем основную форму
    void stop() {
        try {
            out.close();
        } catch (Exception e) {
            log.error("Close OUT connection error", e);
        }
        try {
            in.close();
        } catch (Exception e) {
            log.error("Close IN connection error", e);
        }
        log.debug("Connection with Server is closed");
    }

    // отправка файла на сервер
    public void uploadFile(File file) throws IOException {
        channel.writeAndFlush(new CloudFile(new GenericFile(file.getName(), Files.readAllBytes(file.toPath())), CloudFile.SendFileAction.UPLOAD));
        fileListReq();
    }

    // отправка запроса на шаринг файла с пользователем
    public void sendReqToShareFile(String login, String fileName){
        userInfoMessage = new Authentication(login, "", "", false, Authentication.AuthStatus.FIND_USER);
        userInfoMessage.setFileName(fileName);
        userInfoMessage.setRootDirectory(mainController.serverPath.getText());
        channel.writeAndFlush(userInfoMessage);
    }

    // Посылаем сообщение на аунтификацию польтзователя
    public void sendAuthMessage() {
        channel.writeAndFlush(new Authentication(login, password, "", false, Authentication.AuthStatus.LOGIN));
    }

    // регистрируем юзера
    public void sendRegistrationMessage() {
        channel.writeAndFlush(new Authentication(login, password, "", false, Authentication.AuthStatus.REGISTER));
    }

    // копирование файла на сервере
    public void serverCopyFile(String file) {
        channel.writeAndFlush(new Command(file, Command.CommandAction.COPY));
    }

    // удаление файла на сервере
    public void serverDeleteFile(String file) {
        channel.writeAndFlush(new Command(file, Command.CommandAction.DELETE));
    }

    // скачивание файла с сервера
    public void downloadFile(String file) {
        channel.writeAndFlush(new CloudFile(new GenericFile(file, new byte[0]), CloudFile.SendFileAction.DOWNLOAD));
    }

    // запрос на авторизацию
    public void sendAuthInfo() {
        channel.writeAndFlush(new Authentication(login, password, "", false, Authentication.AuthStatus.LOGIN));
        log.info("Информация авторизации о пользователе передана");
    }

    // запрос стрцуктуры каталога сервера
    public void getServerPath() {
        channel.writeAndFlush(new Command("", Command.CommandAction.GET_DIRECTORY));
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
        channel.writeAndFlush(new Command("", Command.CommandAction.CREATE_DIRECTORY));
    }

    // перейти в папку сервера
    public void enterToServerDir(String selectedItem) {
        channel.writeAndFlush(new Command(selectedItem, Command.CommandAction.ENTER_TO_DIRECTORY));
    }

    // получить размер файла на сервере
    public void getServerFileSize(String selectedItem) {
        channel.writeAndFlush(new Command(selectedItem, Command.CommandAction.FILE_SIZE));
    }

    // перейти в директорию родителя
    public void backToParentDir(String path) {
        channel.writeAndFlush(new Command(path, Command.CommandAction.BACK_TO_PARENT_SERVER_PATH));
    }
}
