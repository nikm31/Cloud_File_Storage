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
        }
        if (message.getType().equals("DOWNLOAD")) {
            uploadFileToUser(channel, serverDir, message);
        }
        if (message.getType().equals("COPY")) {
            copyFile(channel, serverDir, message);
        }
        if (message.getType().equals("DELETE")) {
            deleteFile(channel, serverDir, message);
        }
        if (message.getType().equals("FILE_LIST")) {
            refreshServerFilesList(channel, serverDir);
        }
        if (message.getType().equals("GET_DIRECTORY")) {
            refreshServerDirectory(channel, serverDir);
        }
        if (message.getType().equals("CREATE_DIRECTORY")) {
            createUserNewFolder(channel, serverDir);
        }
        if (message.getType().equals("ENTER_TO_DIRECTORY")) {
            enterToFolder(message, serverDir, channel);
        }
        if (message.getType().equals("FILE_SIZE")) {
            sendFileSize(message, serverDir, channel);
        }
        if (message.getType().equals("BACK_TO_PARENT_SERVER_PATH")) {
            getParentPath(message, channel);
        }

    }

    // получаем текущую директорию Path сервера из формы клиента и возвращаем файлы родителя и путь
    private void getParentPath(Message message, ChannelHandlerContext channel) {
        File current = Paths.get(message.getMessage().toString()).toFile();
        File parent = Paths.get(current.getParent()).toFile();
        refreshServerFilesList(channel, parent);
        refreshServerDirectory(channel, parent);
    }

    // отправляем размер выбранного файла в листе на сервере
    private void sendFileSize(Message message, File serverDir, ChannelHandlerContext channel) {
        try {
            String dir = message.getMessage().toString();
            File fileToSend = Paths.get(String.valueOf(serverDir.toPath().resolve(dir))).toFile();
            long size = Files.size(fileToSend.toPath());
            long MB = 1048576L;
            channel.writeAndFlush(new Command((float) size / MB + " Мб" , Command.CommandAction.FILE_SIZE));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Переходим в новую директорию на сервере и возвращаем новый путь
    private void enterToFolder(Message message, File serverDir, ChannelHandlerContext channel) {
        String dir = message.getMessage().toString();
        File fileToSend = Paths.get(String.valueOf(serverDir.toPath().resolve(dir))).toFile();
        if (fileToSend.isDirectory()) {
            serverDir = fileToSend;
            refreshServerFilesList(channel, serverDir);
            channel.writeAndFlush(new Command(serverDir.toString(), Command.CommandAction.UPDATE_SERVER_PATH));
        }
    }

    // создание новых папок
    private void createUserNewFolder(ChannelHandlerContext channel, File serverDir) {
        try {
            if (Paths.get(serverDir + "\\Новая папка").toFile().exists()) {
                int i = 1;
                while (Paths.get(serverDir + "\\Новая папка" + " (" + i + ")").toFile().exists()) {
                    i++;
                }
                Files.createDirectory(Paths.get(serverDir + "\\Новая папка" + " (" + i + ")"));
            }
            Files.createDirectory(Paths.get(serverDir + "\\Новая папка"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        refreshServerFilesList(channel, serverDir);
    }

    // обработчик информаций касающихся юзера
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

    // назначаем директорию юзеру при входе
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

    // обновляем директорию
    private void refreshServerDirectory(ChannelHandlerContext channel, File serverDir) {
        channel.writeAndFlush(new Command(serverDir.toString(), Command.CommandAction.SEND_DIRECTORY));
    }

    // записываем файл на сервер
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

    // отправляем файл юзеру
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

    // копируем файл
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

    // удаляем файл
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

    // обновляем список файлов
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
