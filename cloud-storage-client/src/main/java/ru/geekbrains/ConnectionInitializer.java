package ru.geekbrains;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ConnectionInitializer extends ChannelInitializer<SocketChannel> {
    private final MainController mainController;

    @Override
    protected void initChannel(SocketChannel socketChannel) {
        ChannelPipeline pipeline = socketChannel.pipeline();
        pipeline.addLast(new ObjectDecoder(1073741824, ClassResolvers.cacheDisabled(null)));
        pipeline.addLast("encoder", new ObjectEncoder());
        pipeline.addLast(new ClientChannelHandler(mainController));
    }
}
