package ru.geekbrains;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import ru.geekbrains.models.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Collectors;


@Slf4j
public class ServerHandler extends SimpleChannelInboundHandler<Message> {

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Message message) {
        log.info("Получен {}", message);

        File serverDir = new File("C:\\Users\\Nikolay\\Desktop\\FileCloudStorage\\server-file-storage\\server");

        if (message.getType().equals("upload")) {
            downloadFileToServer(channelHandlerContext, serverDir, message);
        } else if (message.getType().equals("download")) {
            uploadFileToUser(channelHandlerContext, serverDir, message);
        } else if (message.getType().equals("copyFile")) {
            copyFile(channelHandlerContext, serverDir, message);
        } else if (message.getType().equals("deleteFile")) {
            deleteFile(channelHandlerContext, serverDir, message);
        } else if (message.getType().equals("fileList")) {
            refreshServerFilesList(channelHandlerContext, serverDir);
        }
    }

    private void downloadFileToServer(ChannelHandlerContext channelHandlerContext, File serverDir, Message message) {
        try {
            GenericFile fileSource = (GenericFile) message.getMessage();
            File fileToCreate = new File(serverDir, fileSource.getFilename());
            FileOutputStream fos = new FileOutputStream(fileToCreate);
            fos.write(fileSource.getContent());
            fos.close();
            channelHandlerContext.writeAndFlush(new Status("ok"));
        } catch (Exception e) {
            log.error("Error file transfer to server");
        }
    }

    private void uploadFileToUser(ChannelHandlerContext channelHandlerContext, File serverDir, Message message) {
        try {
            GenericFile fileSource = (GenericFile) message.getMessage();
            File fileToUpload = new File(serverDir, fileSource.getFilename());
            CloudFile cloudFile = new CloudFile(new GenericFile(fileSource.getFilename(), Files.readAllBytes(fileToUpload.toPath())), "upload");
            channelHandlerContext.writeAndFlush(cloudFile);
            channelHandlerContext.writeAndFlush(new Status("ok"));
        } catch (Exception e) {
            log.error("Error file transfer to user");
        }
    }

    private void copyFile(ChannelHandlerContext channelHandlerContext, File serverDir, Message message) {
        try {
            Files.copy(serverDir.toPath().resolve((String) message.getMessage()), serverDir.toPath().resolve("copy_" + message.getMessage()), StandardCopyOption.REPLACE_EXISTING);
            refreshServerFilesList(channelHandlerContext, serverDir);
            log.debug("File is copied");
        } catch (Exception e) {
            log.error("Cant copy file:");
        }
    }

    private void deleteFile(ChannelHandlerContext channelHandlerContext, File serverDir, Message message) {
        try {
            Files.delete(Paths.get(serverDir.toPath().resolve(message.getMessage().toString()).toString()));
            refreshServerFilesList(channelHandlerContext, serverDir);
            log.debug("File is deleted");
        } catch (Exception e) {
            log.error("Cant delete file:");
        }
    }

    private void refreshServerFilesList(ChannelHandlerContext channelHandlerContext, File serverDir) {
        try {
            List<String> files;
            files = Files.list(serverDir.toPath()).map(p -> p.getFileName().toString())
                    .collect(Collectors.toList());
            FileList fileLists = new FileList(files);
            channelHandlerContext.writeAndFlush(fileLists);
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
