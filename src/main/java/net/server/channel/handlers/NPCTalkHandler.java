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
import tools.PacketCreator;

public final class NPCTalkHandler extends AbstractPacketHandler {
    private static final Logger log = LoggerFactory.getLogger(NPCTalkHandler.class);

    @Override
    public void handlePacket(InPacket p, Client c) {
        // 1. Validation Checks (Guard Clauses)
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

        // 2. Handle Standard NPC
        if (obj instanceof NPC npc) {
            handleStandardNPC(c, npc, oid);
        }
        // 3. Handle Player NPC
        else if (obj instanceof PlayerNPC pnpc) {
            handlePlayerNPC(c, pnpc);
        }
    }

    private void handleStandardNPC(Client c, NPC npc, int oid) {
        int npcId = npc.getId(); // Optimization: Cache ID to avoid repeated method calls

        if (YamlConfig.config.server.USE_DEBUG) {
            c.getPlayer().dropMessage(5, "Talking to NPC " + npcId);
        }

        // Special Case: Duey (Bypasses CM/QM checks)
        if (npcId == NpcId.DUEY) {
            DueyProcessor.dueySendTalk(c, false);
            return;
        }

        // Valid State Check
        if (c.getCM() != null || c.getQM() != null) {
            c.sendPacket(PacketCreator.enableActions());
            return;
        }

        NPCScriptManager nsm = NPCScriptManager.getInstance(); // Optimization: Cache Singleton

        // Special Scripts (Gachapon / MapleTV)
        if (npcId >= NpcId.GACHAPON_MIN && npcId <= NpcId.GACHAPON_MAX) {
            nsm.start(c, npcId, "gachapon", null);
            return;
        }

        if (npc.getName().endsWith("Maple TV")) {
            nsm.start(c, npcId, "mapleTV", null);
            return;
        }

        // Standard Script Execution
        if (nsm.start(c, npcId, oid, null)) {
            return; // Script started successfully
        }

        // Fallback: Shop or Uncoded
        if (npc.hasShop()) {
            if (c.getPlayer().getShop() == null) {
                npc.sendShop(c);
            } else {
                c.sendPacket(PacketCreator.enableActions());
            }
        } else {
            // No script AND no shop -> Uncoded
            nsm.start(c, npcId, "unidentifiedNpc", null);
            log.warn("NPC {} ({}) is not coded", npc.getName(), npcId);
        }
    }

    private void handlePlayerNPC(Client c, PlayerNPC pnpc) {
        NPCScriptManager nsm = NPCScriptManager.getInstance();
        int scriptId = pnpc.getScriptId(); // Optimization: Cache Script ID

        if (scriptId < NpcId.CUSTOM_DEV && !nsm.isNpcScriptAvailable(c, String.valueOf(scriptId))) {
            nsm.start(c, scriptId, "rank_user", null);
        } else {
            nsm.start(c, scriptId, null);
        }
    }
}