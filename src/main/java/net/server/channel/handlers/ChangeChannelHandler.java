/*
 This file is part of the OdinMS Maple Story Server
 Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
 Matthias Butz <matze@odinms.de>
 Jan Christian Meyer <vimes@odinms.de>

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as
 published by the Free Software Foundation version 3 as published by
 the Free Software Foundation. You may not use, modify or distribute
 this program under any other version of the GNU Affero General Public
 License.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.server.channel.handlers;

import client.Client;
import client.autoban.AutobanFactory;
import net.AbstractPacketHandler;
import net.netty.GameViolationException;
import net.packet.InPacket;
import net.server.Server;
import service.TransitionService;

/**
 * @author Matze
 */
public final class ChangeChannelHandler extends AbstractPacketHandler {
    private final TransitionService transitionService;

    public ChangeChannelHandler(TransitionService transitionService) {
        this.transitionService = transitionService;
    }

    @Override
    public final void handlePacket(InPacket p, Client c) {
        int channel = p.readByte() + 1;
        p.readInt();
        c.getPlayer().getAutobanManager().setTimestamp(6, Server.getInstance().getCurrentTimestamp(), 3);
        if (c.getChannel() == channel) {
            AutobanFactory.GENERAL.alert(c.getPlayer(), "CCing to same channel.");
            throw new GameViolationException("Change to same channel");
        } else if (c.getPlayer().getCashShop().isOpened() || c.getPlayer().getMiniGame() != null || c.getPlayer().getPlayerShop() != null) {
            return;
        }

        transitionService.changeChannel(c, channel);
    }
}
