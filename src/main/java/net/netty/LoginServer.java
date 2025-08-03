package net.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import service.TransitionService;

public class LoginServer extends AbstractServer {
    public static final int WORLD_ID = -1;
    public static final int CHANNEL_ID = -1;

    private final TransitionService transitionService;
    private Channel channel;

    public LoginServer(int port, TransitionService transitionService) {
        super(port);
        this.transitionService = transitionService;
    }

    @Override
    public void start() {
        EventLoopGroup parentGroup = new NioEventLoopGroup();
        EventLoopGroup childGroup = new NioEventLoopGroup();
        ServerBootstrap bootstrap = new ServerBootstrap()
                .group(parentGroup, childGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new LoginServerInitializer(transitionService));

        this.channel = bootstrap.bind(port).syncUninterruptibly().channel();
    }

    @Override
    public void stop() {
        if (channel == null) {
            throw new IllegalStateException("Must start LoginServer before stopping it");
        }

        channel.close().syncUninterruptibly();
    }
}
