package ru.geekbrains;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConnectionManager {

    private final String serverAddress;
    private final short serverPort;
    private Channel channel;
    private final MainController mainController;
    private EventLoopGroup workGroup;

    public ConnectionManager(String serverAddress, short serverPort, MainController mainController) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.mainController = mainController;
    }

    // создаем подключение и подключаемся к серверу
    public void start() {
        workGroup = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = new Bootstrap().group(workGroup)
                    .channel(NioSocketChannel.class)
                    .handler(new ConnectionInitializer(mainController));
            ChannelFuture future = bootstrap.connect(serverAddress, serverPort);
            channel = future.sync().channel();
        } catch (Exception e) {
            log.error("Cant start server", e);
        }
        log.debug("Connection with Server is active");
    }

    @SneakyThrows
    public void stop() {
        workGroup.shutdownGracefully();
        channel.closeFuture();
    }

    public Channel getChannel() {
        return channel;
    }
}