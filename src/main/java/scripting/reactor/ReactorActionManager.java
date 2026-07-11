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
package scripting.reactor;

import client.Character;
import client.Client;
import client.inventory.Equip;
import client.inventory.InventoryType;
import client.inventory.Item;
import constants.inventory.ItemConstants;
import scripting.AbstractPlayerInteraction;
import server.ItemInformationProvider;
import server.TimerManager;
import server.life.LifeFactory;
import server.life.Monster;
import server.maps.MapMonitor;
import server.maps.Reactor;
import server.maps.ReactorDropEntry;
import server.partyquest.CarnivalFactory;
import server.partyquest.CarnivalFactory.MCSkill;

import javax.script.Invocable;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

/**
 * @author Lerk
 * @author Ronan
 */
public class ReactorActionManager extends AbstractPlayerInteraction {
    private final Reactor reactor;
    private final Invocable iv;
    private ScheduledFuture<?> sprayTask = null;

    public ReactorActionManager(Client c, Reactor reactor, Invocable iv) {
        super(c);
        this.reactor = reactor;
        this.iv = iv;
    }

    public void hitReactor() {
        reactor.hitReactor(c);
    }

    public void destroyNpc(int npcId) {
        reactor.getMap().destroyNPC(npcId);
    }

    private static void sortDropEntries(List<ReactorDropEntry> from, List<ReactorDropEntry> item, List<ReactorDropEntry> visibleQuest, List<ReactorDropEntry> otherQuest, Character chr) {
        ItemInformationProvider ii = ItemInformationProvider.getInstance();

        for (ReactorDropEntry mde : from) {
            if (!ii.isQuestItem(mde.itemId)) {
                item.add(mde);
            } else {
                if (chr.needQuestItem(mde.questid, mde.itemId)) {
                    visibleQuest.add(mde);
                } else {
                    otherQuest.add(mde);
                }
            }
        }
    }

    private static List<ReactorDropEntry> assembleReactorDropEntries(Character chr, List<ReactorDropEntry> items) {
        final List<ReactorDropEntry> dropEntry = new ArrayList<>();
        final List<ReactorDropEntry> visibleQuestEntry = new ArrayList<>();
        final List<ReactorDropEntry> otherQuestEntry = new ArrayList<>();
        sortDropEntries(items, dropEntry, visibleQuestEntry, otherQuestEntry, chr);

        Collections.shuffle(dropEntry);
        Collections.shuffle(visibleQuestEntry);
        Collections.shuffle(otherQuestEntry);

        items.clear();
        items.addAll(dropEntry);
        items.addAll(visibleQuestEntry);
        items.addAll(otherQuestEntry);

        List<ReactorDropEntry> items1 = new ArrayList<>(items.size());
        List<ReactorDropEntry> items2 = new ArrayList<>(items.size() / 2);

        for (int i = 0; i < items.size(); i++) {
            if (i % 2 == 0) {
                items1.add(items.get(i));
            } else {
                items2.add(items.get(i));
            }
        }

        Collections.reverse(items1);
        items1.addAll(items2);

        return items1;
    }

    public void sprayItems() {
        sprayItems(false, 0, 0, 0, 0);
    }

    public void sprayItems(boolean meso, int mesoChance, int minMeso, int maxMeso) {
        sprayItems(meso, mesoChance, minMeso, maxMeso, 0);
    }

    public void sprayItems(boolean meso, int mesoChance, int minMeso, int maxMeso, int minItems) {
        sprayItems((int) reactor.getPosition().getX(), (int) reactor.getPosition().getY(), meso, mesoChance, minMeso, maxMeso, minItems);
    }

    public void sprayItems(int posX, int posY, boolean meso, int mesoChance, int minMeso, int maxMeso, int minItems) {
        dropItems(true, posX, posY, meso, mesoChance, minMeso, maxMeso, minItems);
    }

    public void dropItems() {
        dropItems(false, 0, 0, 0, 0);
    }

    public void dropItems(boolean meso, int mesoChance, int minMeso, int maxMeso) {
        dropItems(meso, mesoChance, minMeso, maxMeso, 0);
    }

    public void dropItems(boolean meso, int mesoChance, int minMeso, int maxMeso, int minItems) {
        dropItems((int) reactor.getPosition().getX(), (int) reactor.getPosition().getY(), meso, mesoChance, minMeso, maxMeso, minItems);
    }

    public void dropItems(int posX, int posY, boolean meso, int mesoChance, int minMeso, int maxMeso, int minItems) {
        dropItems(true, posX, posY, meso, mesoChance, minMeso, maxMeso, minItems);  // all reactors actually drop items sequentially... thanks inhyuk for pointing this out!
    }

    public void dropItems(boolean delayed, int posX, int posY, boolean meso, int mesoChance, final int minMeso, final int maxMeso, int minItems) {
        Character chr = c.getPlayer();
        if (chr == null) {
            return;
        }

        List<ReactorDropEntry> items = assembleReactorDropEntries(chr, generateDropList(getDropChances(), chr.getDropRate(), meso, mesoChance, minItems));
        if (items.size() % 2 == 0) {
            posX -= 12;
        }
        final Point dropPos = new Point(posX, posY);

        if (!delayed) {
            ItemInformationProvider ii = ItemInformationProvider.getInstance();

            byte p = 1;
            for (ReactorDropEntry d : items) {
                dropPos.x = posX + ((p % 2 == 0) ? (25 * ((p + 1) / 2)) : -(25 * (p / 2)));
                p++;

                if (d.itemId == 0) {
                    int range = maxMeso - minMeso;
                    int displayDrop = (int) (Math.random() * range) + minMeso;
                    int mesoDrop = (displayDrop * c.getWorldServer().getMesoRate());
                    reactor.getMap().spawnMesoDrop(mesoDrop, reactor.getMap().calcDropPos(dropPos, reactor.getPosition()), reactor, c.getPlayer(), false, (byte) 2);
                } else {
                    Item drop;

                    if (ItemConstants.getInventoryType(d.itemId) != InventoryType.EQUIP) {
                        drop = new Item(d.itemId, (short) 0, (short) 1);
                    } else {
                        drop = ii.randomizeStats((Equip) ii.getEquipById(d.itemId));
                    }

                    reactor.getMap().dropFromReactor(getPlayer(), reactor, drop, dropPos, (short) d.questid);
                }
            }
        } else {
            final Reactor r = reactor;
            final List<ReactorDropEntry> dropItems = items;
            final int worldMesoRate = c.getWorldServer().getMesoRate();

            dropPos.x -= (12 * items.size());

            sprayTask = TimerManager.getInstance().register(() -> {
                if (dropItems.isEmpty()) {
                    sprayTask.cancel(false);
                    return;
                }

                ReactorDropEntry d = dropItems.remove(0);
                if (d.itemId == 0) {
                    int range = maxMeso - minMeso;
                    int displayDrop = (int) (Math.random() * range) + minMeso;
                    int mesoDrop = (displayDrop * worldMesoRate);
                    r.getMap().spawnMesoDrop(mesoDrop, r.getMap().calcDropPos(dropPos, r.getPosition()), r, chr, false, (byte) 2);
                } else {
                    Item drop;

                    if (ItemConstants.getInventoryType(d.itemId) != InventoryType.EQUIP) {
                        drop = new Item(d.itemId, (short) 0, (short) 1);
                    } else {
                        ItemInformationProvider ii = ItemInformationProvider.getInstance();
                        drop = ii.randomizeStats((Equip) ii.getEquipById(d.itemId));
                    }

                    r.getMap().dropFromReactor(getPlayer(), r, drop, dropPos, (short) d.questid);
                }

                dropPos.x += 25;
            }, 200);
        }
    }

    private List<ReactorDropEntry> getDropChances() {
        return ReactorScriptManager.getInstance().getDrops(reactor.getId());
    }

    private List<ReactorDropEntry> generateDropList(List<ReactorDropEntry> drops, int dropRate, boolean meso, int mesoChance, int minItems) {
        List<ReactorDropEntry> items = new ArrayList<>();
        if (meso && Math.random() < (1 / (double) mesoChance)) {
            items.add(new ReactorDropEntry(0, mesoChance, -1));
        }

        for (ReactorDropEntry mde : drops) {
            if (Math.random() < (dropRate / (double) mde.chance)) {
                items.add(mde);
            }
        }

        while (items.size() < minItems) {
            items.add(new ReactorDropEntry(0, mesoChance, -1));
        }

        return items;
    }

    public void spawnMonster(int id) {
        spawnMonster(id, 1, getPosition());
    }

    public void createMapMonitor(int mapId, String portal) {
        new MapMonitor(c.getChannelServer().getMapFactory().getMap(mapId), portal);
    }

    public void spawnMonster(int id, int qty) {
        spawnMonster(id, qty, getPosition());
    }

    public void spawnMonster(int id, int qty, int x, int y) {
        spawnMonster(id, qty, new Point(x, y));
    }

    public void spawnMonster(int id, int qty, Point pos) {
        for (int i = 0; i < qty; i++) {
            reactor.getMap().spawnMonsterOnGroundBelow(LifeFactory.getMonster(id), pos);
        }
    }

    public void killMonster(int id) {
        killMonster(id, false);
    }

    public void killMonster(int id, boolean withDrops) {
        if (withDrops) {
            getMap().killMonsterWithDrops(id);
        }
        else {
            getMap().killMonster(id);
        }
    }

    public Point getPosition() {
        Point pos = reactor.getPosition();
        pos.y -= 10;
        return pos;
    }

    public void spawnNpc(int npcId) {
        spawnNpc(npcId, getPosition());
    }

    public void spawnNpc(int npcId, Point pos) {
        spawnNpc(npcId, pos, reactor.getMap());
    }

    public Reactor getReactor() {
        return reactor;
    }

    public void spawnFakeMonster(int id) {
        reactor.getMap().spawnFakeMonsterOnGroundBelow(LifeFactory.getMonster(id), getPosition());
    }

    /**
     * Used for Targa and Scarlion
     */
    public void summonBossDelayed(final int mobId, final int delayMs, final int x, final int y, final String bgm,
                                  final String summonMessage) {
        TimerManager.getInstance().schedule(() -> {
            summonBoss(mobId, x, y, bgm, summonMessage);
        }, delayMs);
    }

    private void summonBoss(int mobId, int x, int y, String bgmName, String summonMessage) {
        spawnMonster(mobId, x, y);
        changeMusic(bgmName);
        mapMessage(6, summonMessage);
    }

    public void dispelAllMonsters(int num, int team) { //dispels all mobs, cpq
        final MCSkill skil = CarnivalFactory.getInstance().getGuardian(num);
        if (skil != null) {
            for (Monster mons : getMap().getAllMonsters()) {
                if (mons.getTeam() == team) {
                    mons.dispelSkill(skil.getSkill());
                }
            }
        }
        if (team == 0) {
            getPlayer().getMap().getRedTeamBuffs().remove(skil);
        } else {
            getPlayer().getMap().getBlueTeamBuffs().remove(skil);
        }
    }
}