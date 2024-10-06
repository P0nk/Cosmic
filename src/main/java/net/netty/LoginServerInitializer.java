package net.netty;

import client.Client;
import io.netty.channel.socket.SocketChannel;
import net.PacketProcessor;
import net.server.coordinator.session.SessionCoordinator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.TransitionService;

public class LoginServerInitializer extends ServerChannelInitializer {
    private static final Logger log = LoggerFactory.getLogger(LoginServerInitializer.class);

    public LoginServerInitializer(TransitionService transitionService) {
        super(transitionService);
    }

    @Override
    public void initChannel(SocketChannel socketChannel) {
        final String clientIp = socketChannel.remoteAddress().getHostString();
        log.debug("Client connected to login server from {} ", clientIp);

        PacketProcessor packetProcessor = PacketProcessor.getLoginServerProcessor();
        final long clientSessionId = sessionId.getAndIncrement();
        final String remoteAddress = getRemoteAddress(socketChannel);
        final Client client = Client.createLoginClient(clientSessionId, remoteAddress, packetProcessor, LoginServer.WORLD_ID, LoginServer.CHANNEL_ID);

        if (!SessionCoordinator.getInstance().canStartLoginSession(client)) {
            socketChannel.close();
            return;
        }

        initPipeline(socketChannel, client);
    }
}
