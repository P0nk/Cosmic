/*
    This file is part of the OdinMS Maple Story Server
    Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
              Matthias Butz <matze@odinms.de>
              Jan Christian Meyer <vimes@odinms.de>
*/
package net.server.channel.handlers;

import client.Character;
import client.Client;
import client.inventory.InventoryType;
import client.inventory.Item;
import client.inventory.manipulator.InventoryManipulator;
import net.AbstractPacketHandler;
import net.packet.InPacket;
import scripting.npc.NPCScriptManager;
import scripting.quest.QuestScriptManager;
import tools.PacketCreator;

/**
 * @author Matze
 */
public final class NPCMoreTalkHandler extends AbstractPacketHandler {
    @Override
    public final void handlePacket(InPacket p, Client c) {
        byte lastMsg = p.readByte(); // The type of the message we just responded to
        byte action = p.readByte(); // 00 = End/No, 01 = Next/Yes

        // ========================================================================
        // START: TELEPORT CONSENT CHECK
        // Check if this interaction is a response to a VIP Teleport Rock request
        // ========================================================================
        if (c.getPlayer().getTeleportRequesterId() > -1) {
            // Retrieve stored data
            int requesterId = c.getPlayer().getTeleportRequesterId();
            int rockId = c.getPlayer().getTeleportRockId();

            // Reset the request ID immediately so they don't get stuck in this state
            c.getPlayer().setTeleportRequest(-1, -1);

            if (action == 1) { // User clicked 'Yes'
                Character requester = c.getChannelServer().getPlayerStorage().getCharacterById(requesterId);

                if (requester != null) {
                    // Check if requester still has the item (to prevent dropping/trading it while waiting)
                    Item item = requester.getInventory(InventoryType.CASH).findById(rockId);

                    if (item != null && item.getQuantity() > 0) {
                        // 1. Consume the rock from the requester
                        InventoryManipulator.removeById(requester.getClient(), InventoryType.CASH, rockId, 1, true, false);

                        // 2. Perform the Teleport
                        requester.forceChangeMap(c.getPlayer().getMap(), c.getPlayer().getMap().findClosestPlayerSpawnpoint(c.getPlayer().getPosition()));

                        // 3. Notify both players
                        requester.dropMessage(5, "Teleport successful.");
                        c.getPlayer().dropMessage(5, "You have accepted the teleport request.");
                    } else {
                        c.getPlayer().dropMessage(1, "The requester no longer has the teleport rock.");
                        requester.dropMessage(1, "Teleport failed: You are missing the item.");
                    }
                } else {
                    c.getPlayer().dropMessage(1, "The player is no longer online.");
                }
            } else { // User clicked 'No' or closed the window
                c.getPlayer().dropMessage(5, "You denied the teleport request.");
                Character requester = c.getChannelServer().getPlayerStorage().getCharacterById(requesterId);
                if (requester != null) {
                    requester.dropMessage(1, c.getPlayer().getName() + " denied your request.");
                }
            }
            // Return here to prevent this packet from going to NPCScriptManager (which would crash since no script is active)
            return;
        }
        // ========================================================================
        // END: TELEPORT CONSENT CHECK
        // ========================================================================

        if (lastMsg == 2) {
            if (action != 0) {
                String returnText = p.readString();
                if (c.getQM() != null) {
                    c.getQM().setGetText(returnText);
                    if (c.getQM().isStart()) {
                        QuestScriptManager.getInstance().start(c, action, lastMsg, -1);
                    } else {
                        QuestScriptManager.getInstance().end(c, action, lastMsg, -1);
                    }
                } else {
                    c.getCM().setGetText(returnText);
                    NPCScriptManager.getInstance().action(c, action, lastMsg, -1);
                }
            } else if (c.getQM() != null) {
                c.getQM().dispose();
            } else {
                c.getCM().dispose();
            }
        } else {
            int selection = -1;
            if (p.available() >= 4) {
                selection = p.readInt();
            } else if (p.available() > 0) {
                selection = p.readUnsignedByte();
            }
            if (c.getQM() != null) {
                if (c.getQM().isStart()) {
                    QuestScriptManager.getInstance().start(c, action, lastMsg, selection);
                } else {
                    QuestScriptManager.getInstance().end(c, action, lastMsg, selection);
                }
            } else if (c.getCM() != null) {
                NPCScriptManager.getInstance().action(c, action, lastMsg, selection);
            }
        }
    }
}