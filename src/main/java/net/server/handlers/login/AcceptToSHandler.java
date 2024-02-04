package net.server.handlers.login;

import client.Client;
import net.AbstractPacketHandler;
import net.netty.GameViolationException;
import net.packet.InPacket;
import tools.PacketCreator;

/**
 * @author kevintjuh93
 */
public final class AcceptToSHandler extends AbstractPacketHandler {

    @Override
    public boolean validateState(Client c) {
        return !c.isLoggedIn();
    }

    @Override
    public void handlePacket(InPacket p, Client c) {
        if (p.available() == 0 || p.readByte() != 1 || c.acceptToS()) {
            throw new GameViolationException("ToS not accepted");
        }

        if (c.finishLogin()) {
            c.sendPacket(PacketCreator.getAuthSuccess(c));
        } else {
            c.sendPacket(PacketCreator.getLoginFailed(9));//shouldn't happen XD
        }
    }
}
