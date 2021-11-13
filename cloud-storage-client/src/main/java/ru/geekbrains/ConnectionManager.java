package ru.geekbrains;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;
import javafx.scene.control.TextField;
import lombok.extern.slf4j.Slf4j;
import ru.geekbrains.models.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    public ConnectionManager(String serverAddress, short serverPort, String login, String password, MainController mainController) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.mainController = mainController;
        this.login = login;
        this.password = password;
    }

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

        fileListReq();
        getServerPath();
        sendAuthInfo();
    }

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

    public void uploadFile(File file) throws IOException {
        CloudFile cloudFile = new CloudFile(new GenericFile(file.getName(), Files.readAllBytes(file.toPath())), "upload");
        channel.writeAndFlush(cloudFile);
        fileListReq();
    }

    public void serverCopyFile(String file) {
        Command copyCommand = new Command(file, "copyFile");
        channel.writeAndFlush(copyCommand);
    }

    public void serverDeleteFile(String file) {
        Command copyCommand = new Command(file, "deleteFile");
        channel.writeAndFlush(copyCommand);
    }

    public void downloadFile(String file) {
        CloudFile cloudFile = new CloudFile(new GenericFile(file, new byte[0]), "download");
        channel.writeAndFlush(cloudFile);
    }

    public void sendAuthInfo() {
        Authentication auth = new Authentication(login,
                password,
          //      generateRootDir(login),
                false,
                AuthAction.REGISTER);
        channel.writeAndFlush(auth);
        log.info("Информация о пользователе передана");

    }

    public void getServerPath() {
        channel.writeAndFlush(new Command("", "getDirectory"));
    }

    public Path generateRootDir(String loginField) {
        return Paths.get(login + "_rootDir");
    }

    public void fileListReq() {
        try {
            List<String> empty = new ArrayList<>();
            channel.writeAndFlush(new FileList(empty));
        } catch (Exception e) {
            log.error("FileList Req Error");
        }
    }

}
