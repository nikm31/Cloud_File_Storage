package ru.geekbrains;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import ru.geekbrains.authentication.AuthenticationProvider;
import ru.geekbrains.authentication.DatabaseAuthenticator;
import ru.geekbrains.models.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class ServerHandler extends SimpleChannelInboundHandler<Message> {
    private AuthenticationProvider authentication = new DatabaseAuthenticator();
    private Authentication authInfoToSend;
    private Authentication authInfoReceived;
    private Path serverDir;
    private String userRootDir;


    @Override
    protected void channelRead0(ChannelHandlerContext channel, Message message) {
        log.info("Получен {}", message);

        if (message.getType().equals("userInfo")) {
            processUserInfo(channel, message);
        }

        File serverDir = new File(setDirectory().toString());

        if (message.getType().equals("upload")) {
            downloadFileToServer(channel, serverDir, message);
        } else if (message.getType().equals("download")) {
            uploadFileToUser(channel, serverDir, message);
        } else if (message.getType().equals("copyFile")) {
            copyFile(channel, serverDir, message);
        } else if (message.getType().equals("deleteFile")) {
            deleteFile(channel, serverDir, message);
        } else if (message.getType().equals("fileList")) {
            refreshServerFilesList(channel, serverDir);
        } else if (message.getType().equals("getDirectory")) {
            refreshServerDirectory(channel, serverDir);
        }
    }

    private void processUserInfo(ChannelHandlerContext channel, Message message) {
        authInfoReceived = (Authentication) message;

        if (authInfoReceived.getAuthStatus() == Authentication.AuthStatus.LOGIN) {
            authInfoToSend = authentication.userAuthentication(authInfoReceived.getLogin(), authInfoReceived.getPassword());
            userRootDir = authInfoToSend.getRootDirectory();
            setDirectory();
            log.info("User registration status {}", authInfoToSend.getAuthStatus());
            channel.writeAndFlush(authInfoToSend);
        } else if (authInfoReceived.getAuthStatus() == Authentication.AuthStatus.REGISTER) {
            authInfoToSend = authentication.userRegistration(authInfoReceived.getLogin(), authInfoReceived.getPassword());
            log.info("User registration status {}", authInfoToSend.getAuthStatus());
            channel.writeAndFlush(authInfoToSend);
        } else if (authInfoReceived.getAuthStatus() == Authentication.AuthStatus.FIND_USER) {

            String targetDir = authInfoReceived.getRootDirectory();
            String targetFileName = authInfoReceived.getFileName();

            authInfoToSend = authentication.getUserRootFolderByLogin(authInfoReceived.getLogin());

            String linkDir = authInfoToSend.getRootDirectory();

            Path target = Paths.get("C:\\IMG_6167.jpg");
            Path link = Paths.get("C:\\IMG_6168.jpg");

            try {
                Files.createSymbolicLink(link, target);
            } catch (IOException e) {
                e.printStackTrace();
            }


            // channel.writeAndFlush(authInfoToSend);
        }
    }

    private Path setDirectory() {
        try {
            serverDir = Paths.get("server-file-storage", userRootDir);
            if (!Files.exists(serverDir)) {
                Files.createDirectory(serverDir);
            }
            return serverDir;
        } catch (Exception e) {
            log.debug("File create/read on server error ", e);
        }
        return null;
    }

    private void refreshServerDirectory(ChannelHandlerContext channel, File serverDir) {
        channel.writeAndFlush(new Command(serverDir.toString(), "getDirectory"));
    }

    private void downloadFileToServer(ChannelHandlerContext channel, File serverDir, Message message) {
        try {
            GenericFile fileSource = (GenericFile) message.getMessage();
            File fileToCreate = new File(serverDir, fileSource.getFilename());
            FileOutputStream fos = new FileOutputStream(fileToCreate);
            fos.write(fileSource.getContent());
            fos.close();
            channel.writeAndFlush(new Status("ok"));
        } catch (Exception e) {
            log.error("Error file transfer to server");
        }
    }

    private void uploadFileToUser(ChannelHandlerContext channel, File serverDir, Message message) {
        try {
            GenericFile fileSource = (GenericFile) message.getMessage();
            File fileToUpload = new File(serverDir, fileSource.getFilename());
            CloudFile cloudFile = new CloudFile(new GenericFile(fileSource.getFilename(), Files.readAllBytes(fileToUpload.toPath())), "upload");
            channel.writeAndFlush(cloudFile);
            channel.writeAndFlush(new Status("ok"));
        } catch (Exception e) {
            log.error("Error file transfer to user");
        }
    }

    private void copyFile(ChannelHandlerContext channel, File serverDir, Message message) {
        try {
            Files.copy(serverDir.toPath().resolve((String) message.getMessage()), serverDir.toPath().resolve("copy_" + message.getMessage()), StandardCopyOption.REPLACE_EXISTING);
            refreshServerFilesList(channel, serverDir);
            log.debug("File is copied");
        } catch (Exception e) {
            log.error("Cant copy file:");
        }
    }

    private void deleteFile(ChannelHandlerContext channel, File serverDir, Message message) {
        try {
            Files.delete(Paths.get(serverDir.toPath().resolve(message.getMessage().toString()).toString()));
            refreshServerFilesList(channel, serverDir);
            log.debug("File is deleted");
        } catch (Exception e) {
            log.error("Cant delete file:");
        }
    }

    private void refreshServerFilesList(ChannelHandlerContext channel, File serverDir) {
        try {
            List<String> files;
            files = Files.list(serverDir.toPath()).map(p -> p.getFileName().toString())
                    .collect(Collectors.toList());
            FileList fileLists = new FileList(files);
            channel.writeAndFlush(fileLists);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        log.debug("Client is connected");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Channel exception", cause);
        ctx.close();
    }

}
