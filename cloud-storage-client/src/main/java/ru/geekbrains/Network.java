package ru.geekbrains;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Network {

    private final String serverAddress;
    private final short serverPort;
    private final MainController mainController;
    private final int MAX_MESSAGE_SIZE = 1024 * 1024 * 1024;
    private Channel channel;
    private EventLoopGroup workGroup;

    public Network(String serverAddress, short serverPort, MainController mainController) {
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
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(io.netty.channel.socket.SocketChannel socketChannel) {
                            ChannelPipeline pipeline = socketChannel.pipeline();
                            pipeline.addLast(new ObjectDecoder(MAX_MESSAGE_SIZE, ClassResolvers.cacheDisabled(null)));
                            pipeline.addLast("encoder", new ObjectEncoder());
                            pipeline.addLast(new ClientChannelHandler(mainController));
                        }
                    });
            ChannelFuture future = bootstrap.connect(serverAddress, serverPort);
            channel = future.sync().channel();
            log.info("Connection with Server is active");
        } catch (Exception e) {
            stop();
            log.error("Cant start server", e);
        }
    }

    public void stop() {
        channel.close();
        workGroup.shutdownGracefully();
    }

    public Channel getChannel() {
        return channel;
    }
}