package net.server.handlers.login;

import client.Client;
import net.AbstractPacketHandler;
import net.netty.GameViolationException;
import net.packet.InPacket;
import service.AccountService;
import tools.PacketCreator;

/**
 * @author kevintjuh93
 * @author Ponk
 */
public final class AcceptToSHandler extends AbstractPacketHandler {
    private final AccountService accountService;

    public AcceptToSHandler(final AccountService accountService) {
        this.accountService = accountService;
    }

    @Override
    public boolean validateState(Client c) {
        return !c.isLoggedIn();
    }

    @Override
    public void handlePacket(InPacket p, Client c) {
        if (p.available() == 0 || p.readByte() != 1 || !accountService.acceptTos(c.getAccID())) {
            throw new GameViolationException("ToS not accepted");
        }

        if (!accountService.setLoggedIn(c)) {
            c.sendPacket(PacketCreator.getLoginFailed(7));
        }

        c.sendPacket(PacketCreator.getAuthSuccess(c));
    }
}
