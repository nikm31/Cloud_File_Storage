package ru.geekbrains;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import ru.geekbrains.authentication.DbProvider;
import ru.geekbrains.models.Actions.*;
import ru.geekbrains.models.Commands;
import ru.geekbrains.models.File.PartFile;
import ru.geekbrains.models.Message;
import ru.geekbrains.utils.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Collectors;


@Slf4j
public class ServerHandler extends SimpleChannelInboundHandler<Message> {
    private final DbProvider dbProvider;
    private File userRootDir;
    private File currentDir;
    private Authentication authInfoReceived;
    private Authentication authInfoToSend;

    public ServerHandler(DbProvider dbProvider) {
        this.dbProvider = dbProvider;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channel, Message message) throws IOException {
        log.info("Получена команда {}", message.getType());
        switch (message.getType()) {
            case LOGIN:
                userLogin(channel, message);
                break;
            case REGISTER:
                registerUser(channel, message);
                break;
            case FIND_USER:
                shareFile(channel, message);
                break;
            case DOWNLOAD:
                uploadFileToUser(channel, message);
                break;
            case COPY:
                copyFile(channel, message);
                break;
            case DELETE:
                deleteFile(channel, message);
                break;
            case GET_DIRECTORY:
                sendDirectory(channel, currentDir);
                break;
            case CREATE_DIRECTORY:
                createUserNewFolder(channel);
                break;
            case UP_TO_PATH:
                enterToFolder(message, channel);
                break;
            case BACK_TO_PATH:
                getParentPath(message, channel);
                break;
            case FILE_SIZE:
                sendFileSize(message, channel);
                break;
            case FILTER_FILE_LIST:
                sendFileList(channel, currentDir, message);
                break;
            case PART_FILE:
                receivePartOfFile((PartFile) message, channel);
                break;
            case PART_FILE_INFO:
                sendNextPart((PartFileInfo) message, channel);
                break;
        }
    }

    // отправляет следующую часть файла
    private void sendNextPart(PartFileInfo partFileInfo, ChannelHandlerContext channel) throws IOException {
        FileUtils.getInstance().sendFileByParts(new File(currentDir, partFileInfo.getFilename()).toPath(), channel.channel(), (long) partFileInfo.getMessage());
    }

    // подготавливаем файл к отправке
    private void receivePartOfFile(PartFile partFile, ChannelHandlerContext channel) {
        Path filePath = new File(currentDir, partFile.getFilename()).toPath();
        try {
            FileUtils.getInstance().prepareAndSavePart(filePath, partFile.getStartPos(), partFile.getMessage());
            if (partFile.isLast()) {
                channel.writeAndFlush(new Status("Файл передан на сервер " + partFile.getFilename()));
            } else {
                PartFileInfo partFileInfo = new PartFileInfo(partFile.getEndPos(), partFile.getFilename());
                channel.writeAndFlush(partFileInfo);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    // получаем текущую директорию Path сервера из формы клиента и возвращаем файлы родителя и путь
    private void getParentPath(Message message, ChannelHandlerContext channel) {
        File current = Paths.get(message.getMessage().toString()).toFile();
        File parent = !current.equals(userRootDir) ? Paths.get(current.getParent()).toFile() : current;
        currentDir = parent;
        sendFileList(channel, parent);
        sendDirectory(channel, parent);
    }

    // отправляем размер выбранного файла в листе на сервере
    private void sendFileSize(Message message, ChannelHandlerContext channel) {
        try {
            String dir = message.getMessage().toString();
            File fileToSend = Paths.get(String.valueOf(currentDir.toPath().resolve(dir))).toFile();
            long size = Files.size(fileToSend.toPath());
            channel.writeAndFlush(new Action(String.valueOf(size), Commands.FILE_SIZE));
        } catch (IOException e) {
            log.error("Прозошла ошибка", e);
        }
    }

    // Переходим в новую директорию на сервере и возвращаем новый путь
    private void enterToFolder(Message message, ChannelHandlerContext channel) {
        String dir = message.getMessage().toString();
        File fileToSend = Paths.get(String.valueOf(currentDir.toPath().resolve(dir))).toFile();

        if (fileToSend.isDirectory()) {
            currentDir = fileToSend;
            sendFileList(channel, currentDir);
            channel.writeAndFlush(new Action(currentDir.toString(), Commands.CURRENT_PATH));
        }
    }

    // создание новых папок
    private void createUserNewFolder(ChannelHandlerContext channel) {
        try {
            if (Paths.get(currentDir + "\\Новая папка").toFile().exists()) {
                int i = 1;
                while (Paths.get(currentDir + "\\Новая папка" + " (" + i + ")").toFile().exists()) {
                    i++;
                }
                Files.createDirectory(Paths.get(currentDir + "\\Новая папка" + " (" + i + ")"));
            }
            Files.createDirectory(Paths.get(currentDir + "\\Новая папка"));
            channel.writeAndFlush(new Status("Папка на сервере создана"));
        } catch (IOException e) {
            log.error("Cant create folder ", e);
        }
        sendFileList(channel, currentDir);
    }

    // аунтефицируем пользователя
    private void userLogin(ChannelHandlerContext channel, Message message) {
        authInfoReceived = (Authentication) message;
        authInfoToSend = dbProvider.userAuthentication(authInfoReceived.getLogin(), authInfoReceived.getPassword());
        log.info("User login status {}", authInfoToSend.getType());
        if (authInfoToSend.getCommand() == Commands.AUTHENTICATED) {
            userRootDir = Paths.get("server-file-storage", authInfoToSend.getRootDirectory()).toFile();
            currentDir = new File(userRootDir.getPath());
        }
        channel.writeAndFlush(authInfoToSend);
    }

    @SneakyThrows
    // регистрируем пользователя
    private void registerUser(ChannelHandlerContext channel, Message message) {
        authInfoReceived = (Authentication) message;
        authInfoToSend = dbProvider.userRegistration(authInfoReceived.getLogin(), authInfoReceived.getPassword());
        log.info("User registration status {}", authInfoToSend.getType());
        if (authInfoToSend.getCommand() == Commands.REGISTERED) {
            Files.createDirectory(Paths.get("server-file-storage" + File.separator + authInfoToSend.getRootDirectory()));
        }
        channel.writeAndFlush(authInfoToSend);
    }

    // шаринг файла другому юзеру
    private void shareFile(ChannelHandlerContext channel, Message message) {
        authInfoReceived = (Authentication) message;
        String target = authInfoReceived.getRootDirectory();
        String file = authInfoReceived.getFileName();
        authInfoToSend = dbProvider.getUserRootFolderByLogin(authInfoReceived.getLogin());
        String link = authInfoToSend.getRootDirectory();
        log.info("Ready to create link");

        Path targetLink = Paths.get(target, file).toAbsolutePath();
        Path linkOut = Paths.get("server-file-storage", link, file).toAbsolutePath();

        try {
            Files.createSymbolicLink(linkOut, targetLink);
        } catch (IOException e) {
            log.error("Create link Error", e);
        }

        log.info("Link is created: {}", linkOut);
        channel.writeAndFlush(new Status("Файл " + file + " расшарен пользователю " + authInfoReceived.getLogin()));
    }

    // обновляем директорию
    private void sendDirectory(ChannelHandlerContext channel, File curDir) {
        channel.writeAndFlush(new Action(curDir.toString(), Commands.SEND_CURRENT_PATH));
    }

    // отправляем файл юзеру
    private void uploadFileToUser(ChannelHandlerContext channel, Message message) {
        File fileToSend = Paths.get(String.valueOf(currentDir.toPath().resolve((String) message.getMessage()))).toFile();
        try {
            FileUtils.getInstance().sendFileByParts(fileToSend.toPath(), channel.channel(), 0L);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // копируем файл
    private void copyFile(ChannelHandlerContext channel, Message message) {
        try {
            Files.copy(currentDir.toPath().resolve((String) message.getMessage()), currentDir.toPath().resolve("copy_" + message.getMessage()), StandardCopyOption.REPLACE_EXISTING);
            sendFileList(channel, currentDir);
            channel.writeAndFlush(new Status("Файл скопирован на сервере"));
            log.debug("File is copied");
        } catch (Exception e) {
            channel.writeAndFlush(new Status("Ошибка копирования файла"));
            log.error("Cant copy file:");
        }
    }

    // удаляем файл
    private void deleteFile(ChannelHandlerContext channel, Message message) {
        try {
            File file = new File(currentDir.toString(), message.getMessage().toString());
            String[] files = file.list();
            if (file.isDirectory() & files != null) {
                for (String s : files) {
                    File currentFile = new File(file.getPath(), s);
                    Files.deleteIfExists(currentFile.toPath());
                }
            }
            Files.deleteIfExists(file.toPath());
            sendFileList(channel, currentDir);
            channel.writeAndFlush(new Status("Файл удален на сервере " + file));
            log.info("File is deleted " + file);
        } catch (Exception e) {
            channel.writeAndFlush(new Status("Ошибка удаления файла"));
            log.error("Cant delete file:");
        }
    }

    // обновляем список файлов
    private void sendFileList(ChannelHandlerContext channel, File currentDir) {
        try {
            List<String> files;
            files = Files.list(currentDir.toPath()).map(p -> p.getFileName().toString())
                    .collect(Collectors.toList());
            FileList fileLists = new FileList(files);
            channel.writeAndFlush(fileLists);
        } catch (IOException e) {
            log.error("Cant send file list ", e);
        }
    }

    // перегрузка метода sendFileList
    private void sendFileList(ChannelHandlerContext channel, File currentDir, Message message) {
        try {
            String finalFileMask = String.valueOf(message.getMessage()).trim();
            List<String> lists = Files.list(currentDir.toPath())
                    .map(p -> p.getFileName().toString())
                    .filter(fileName -> fileName.contains(finalFileMask))
                    .collect(Collectors.toList());
            FileList fileLists = new FileList(lists);
            channel.writeAndFlush(fileLists);
        } catch (IOException e) {
            log.error("Cant send file list ", e);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        log.info("Client is connected");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Channel exception", cause);
        ctx.close();
    }

}