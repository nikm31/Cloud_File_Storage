package ru.geekbrains;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import javafx.application.Platform;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Message message) {
        log.info("Получено {}", message);

        if (message.getType().equals("status")) {
            refreshStatusBar(message);
        } else if (message.getType().equals("upload")) {
            downloadFile(message, channelHandlerContext);
        } else if (message.getType().equals("fileList")) {
            refreshServerList(message);
        }
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


    public void downloadFile(Message message, ChannelHandlerContext channelHandlerContext) {
        try {
        File dir = new File("C:\\Users\\Nikolay\\Desktop\\FileCloudStorage\\cloud-storage-client\\client");
        GenericFile fileSource = (GenericFile) message.getMessage();
        File fileToCreate = new File(dir, fileSource.getFilename());
        FileOutputStream fos = new FileOutputStream(fileToCreate);
        fos.write(fileSource.getContent());
        fos.close();
        refreshHostList();
        channelHandlerContext.writeAndFlush(new Status("ok"));
        } catch (Exception e) {
            log.error("Write error file to host", e) ;
        }
    }

}
