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
import client.processor.npc.DueyProcessor;
import config.YamlConfig;
import constants.id.NpcId;
import net.AbstractPacketHandler;
import net.packet.InPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scripting.npc.NPCScriptManager;
import server.life.NPC;
import server.life.PlayerNPC;
import server.maps.MapObject;
import server.shop.ShopFactory;
import tools.PacketCreator;

public final class NPCTalkHandler extends AbstractPacketHandler {
    private static final Logger log = LoggerFactory.getLogger(NPCTalkHandler.class);
    private final ShopFactory shopFactory;

    public NPCTalkHandler(ShopFactory shopFactory) {
        this.shopFactory = shopFactory;
    }

    @Override
    public void handlePacket(InPacket p, Client c) {
        if (!c.getPlayer().isAlive()) {
            c.sendPacket(PacketCreator.enableActions());
            return;
        }

        if (currentServerTime() - c.getPlayer().getNpcCooldown() < YamlConfig.config.server.BLOCK_NPC_RACE_CONDT) {
            c.sendPacket(PacketCreator.enableActions());
            return;
        }

        int oid = p.readInt();
        MapObject obj = c.getPlayer().getMap().getMapObject(oid);
        if (obj instanceof NPC npc) {
            if (YamlConfig.config.server.USE_DEBUG) {
                c.getPlayer().dropMessage(5, "Talking to NPC " + npc.getId());
            }

            if (npc.getId() == NpcId.DUEY) {
                DueyProcessor.dueySendTalk(c, false);
            } else {
                if (c.getCM() != null || c.getQM() != null) {
                    c.sendPacket(PacketCreator.enableActions());
                    return;
                }

                // Custom handling to reduce the amount of scripts needed.
                if (npc.getId() >= NpcId.GACHAPON_MIN && npc.getId() <= NpcId.GACHAPON_MAX) {
                    NPCScriptManager.getInstance().start(c, npc.getId(), "gachapon");
                } else if (npc.getName().endsWith("Maple TV")) {
                    NPCScriptManager.getInstance().start(c, npc.getId(), "mapleTV");
                } else {
                    boolean hasNpcScript = NPCScriptManager.getInstance().start(c, npc.getId(), oid, null);
                    if (!hasNpcScript) {
                        if (!hasShop(npc)) {
                            log.warn("NPC {} ({}) is not coded", npc.getName(), npc.getId());
                            return;
                        } else if (c.getPlayer().getShop() != null) {
                            c.sendPacket(PacketCreator.enableActions());
                            return;
                        }

                        sendShop(npc, c);
                    }
                }
            }
        } else if (obj instanceof PlayerNPC pnpc) {
            NPCScriptManager nsm = NPCScriptManager.getInstance();

            if (pnpc.getScriptId() < NpcId.CUSTOM_DEV && !nsm.isNpcScriptAvailable(c, "" + pnpc.getScriptId())) {
                nsm.start(c, pnpc.getScriptId(), "rank_user");
            } else {
                nsm.start(c, pnpc.getScriptId());
            }
        }
    }

    private boolean hasShop(NPC npc) {
        return shopFactory.getShop(npc.getId()).isPresent();
    }

    private void sendShop(NPC npc, Client c) {
        shopFactory.getShop(npc.getId()).ifPresent(shop -> shop.sendShop(c));
    }
}
