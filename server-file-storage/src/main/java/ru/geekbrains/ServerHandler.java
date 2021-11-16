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
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
public class ServerHandler extends SimpleChannelInboundHandler<Message> {
    private final AuthenticationProvider authentication = new DatabaseAuthenticator();
    private String userRootDir;


    @Override
    protected void channelRead0(ChannelHandlerContext channel, Message message) {
        log.info("Получен {}", message);

        if (message.getType().equals("USER_INFO")) {
            processUserInfo(channel, message);
        }

        File serverDir = new File(Objects.requireNonNull(setDirectory()).toString());

        if (message.getType().equals("UPLOAD")) {
            downloadFileToServer(channel, serverDir, message);
        } else if (message.getType().equals("DOWNLOAD")) {
            uploadFileToUser(channel, serverDir, message);
        } else if (message.getType().equals("COPY")) {
            copyFile(channel, serverDir, message);
        } else if (message.getType().equals("DELETE")) {
            deleteFile(channel, serverDir, message);
        } else if (message.getType().equals("FILE_LIST")) {
            refreshServerFilesList(channel, serverDir);
        } else if (message.getType().equals("GET_DIRECTORY")) {
            refreshServerDirectory(channel, serverDir);
        }
    }

    private void processUserInfo(ChannelHandlerContext channel, Message message) {
        Authentication authInfoReceived = (Authentication) message;

        Authentication authInfoToSend;
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
            String target = authInfoReceived.getRootDirectory();
            String file = authInfoReceived.getFileName();
            authInfoToSend = authentication.getUserRootFolderByLogin(authInfoReceived.getLogin());
            String link = authInfoToSend.getRootDirectory();
            log.debug("ready to create link");

        //    Path target = Paths.get("server-file-storage" + target + file);
        //    Path link = Paths.get("C:\\IMG_6168.jpg");

//            try {
//                Files.createSymbolicLink(link, target);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }


            // channel.writeAndFlush(authInfoToSend);
        }
    }

    private Path setDirectory() {
        try {
            Path serverDir = Paths.get("server-file-storage", userRootDir);
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
        channel.writeAndFlush(new Command(serverDir.toString(), Command.CommandAction.SEND_DIRECTORY));
    }

    private void downloadFileToServer(ChannelHandlerContext channel, File serverDir, Message message) {
        try {
            GenericFile fileSource = (GenericFile) message.getMessage();
            File fileToCreate = new File(serverDir, fileSource.getFilename());
            FileOutputStream fos = new FileOutputStream(fileToCreate);
            fos.write(fileSource.getContent());
            fos.close();
            channel.writeAndFlush(new Status("Файл передан на сервер " + fileSource.getFilename()));
        } catch (Exception e) {
            channel.writeAndFlush(new Status("Ошибка записи файла на сервере"));
            log.error("Error file transfer to server");
        }
    }

    private void uploadFileToUser(ChannelHandlerContext channel, File serverDir, Message message) {
        try {
            GenericFile fileSource = (GenericFile) message.getMessage();
            File fileToUpload = new File(serverDir, fileSource.getFilename());
            CloudFile cloudFile = new CloudFile(new GenericFile(fileSource.getFilename(), Files.readAllBytes(fileToUpload.toPath())), CloudFile.SendFileAction.UPLOAD);
            channel.writeAndFlush(cloudFile);
            channel.writeAndFlush(new Status("Файл отправлен клиенту " + fileSource.getFilename()));
        } catch (Exception e) {
            channel.writeAndFlush(new Status("Ошибка отправки файла клиенту"));
            log.error("Error file transfer to user");
        }
    }

    private void copyFile(ChannelHandlerContext channel, File serverDir, Message message) {
        try {
            Files.copy(serverDir.toPath().resolve((String) message.getMessage()), serverDir.toPath().resolve("copy_" + message.getMessage()), StandardCopyOption.REPLACE_EXISTING);
            refreshServerFilesList(channel, serverDir);
            channel.writeAndFlush(new Status("Файл скопирован на сервере"));
            log.debug("File is copied");
        } catch (Exception e) {
            channel.writeAndFlush(new Status("Ошибка копирования файла"));
            log.error("Cant copy file:");
        }
    }

    private void deleteFile(ChannelHandlerContext channel, File serverDir, Message message) {
        try {
            Files.delete(Paths.get(serverDir.toPath().resolve(message.getMessage().toString()).toString()));
            refreshServerFilesList(channel, serverDir);
            channel.writeAndFlush(new Status("Файл удален на сервере"));
            log.debug("File is deleted");
        } catch (Exception e) {
            channel.writeAndFlush(new Status("Ошибка удаления файла"));
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
