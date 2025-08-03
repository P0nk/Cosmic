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

import client.Character;
import client.Client;
import client.Skill;
import client.SkillFactory;
import client.SkillMacro;
import client.creator.veteran.BowmanCreator;
import client.creator.veteran.MagicianCreator;
import client.creator.veteran.PirateCreator;
import client.creator.veteran.ThiefCreator;
import client.creator.veteran.WarriorCreator;
import client.inventory.Equip;
import client.inventory.Equip.ScrollResult;
import client.inventory.Inventory;
import client.inventory.InventoryType;
import client.inventory.Item;
import client.inventory.ModifyInventory;
import client.inventory.Pet;
import client.inventory.manipulator.InventoryManipulator;
import client.inventory.manipulator.KarmaManipulator;
import client.processor.npc.DueyProcessor;
import client.processor.stat.AssignAPProcessor;
import client.processor.stat.AssignSPProcessor;
import config.YamlConfig;
import constants.game.GameConstants;
import constants.id.ItemId;
import constants.id.MapId;
import constants.inventory.ItemConstants;
import net.AbstractPacketHandler;
import net.packet.InPacket;
import net.packet.out.SendNoteSuccessPacket;
import net.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.ItemInformationProvider;
import server.TimerManager;
import server.maps.AbstractMapObject;
import server.maps.FieldLimit;
import server.maps.Kite;
import server.maps.MapleMap;
import server.maps.MapleTVEffect;
import server.maps.PlayerShopItem;
import server.shop.Shop;
import server.shop.ShopFactory;
import service.AccountService;
import service.NoteService;
import tools.PacketCreator;
import tools.Pair;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.SECONDS;

public final class UseCashItemHandler extends AbstractPacketHandler {
    private static final Logger log = LoggerFactory.getLogger(UseCashItemHandler.class);
    private static final int MIU_MIU_SHOP_ID = 1338;

    private final NoteService noteService;
    private final ShopFactory shopFactory;
    private final AccountService accountService;

    public UseCashItemHandler(NoteService noteService, ShopFactory shopFactory, AccountService accountService) {
        this.noteService = noteService;
        this.shopFactory = shopFactory;
        this.accountService = accountService;
    }

    @Override
    public void handlePacket(InPacket p, Client c) {
        final Character player = c.getPlayer();

        long timeNow = currentServerTime();
        if (timeNow - player.getLastUsedCashItem() < 3000) {
            player.dropMessage(1, "You have used a cash item recently. Wait a moment, then try again.");
            c.sendPacket(PacketCreator.enableActions());
            return;
        }
        player.setLastUsedCashItem(timeNow);

        ItemInformationProvider ii = ItemInformationProvider.getInstance();
        short position = p.readShort();
        int itemId = p.readInt();
        int itemType = itemId / 10000;

        Inventory cashInv = player.getInventory(InventoryType.CASH);
        Item toUse = cashInv.getItem(position);
        if (toUse == null || toUse.getItemId() != itemId) {
            toUse = cashInv.findById(itemId);

            if (toUse == null) {
                c.sendPacket(PacketCreator.enableActions());
                return;
            }

            position = toUse.getPosition();
        }

        if (toUse.getQuantity() < 1) {
            c.sendPacket(PacketCreator.enableActions());
            return;
        }

        String medal = "";
        Item medalItem = player.getInventory(InventoryType.EQUIPPED).getItem((short) -49);
        if (medalItem != null) {
            medal = "<" + ii.getName(medalItem.getItemId()) + "> ";
        }

        if (itemType == 504) { // vip teleport rock
            String error1 = "Either the player could not be found or you were trying to teleport to an illegal location.";
            boolean vip = p.readByte() == 1 && itemId / 1000 >= 5041;
            remove(c, position, itemId);
            boolean success = false;
            if (!vip) {
                int mapId = p.readInt();
                if (itemId / 1000 >= 5041 || mapId / 100000000 == player.getMapId() / 100000000) { //check vip or same continent
                    MapleMap targetMap = c.getChannelServer().getMapFactory().getMap(mapId);
                    if (!FieldLimit.CANNOTVIPROCK.check(targetMap.getFieldLimit()) && (targetMap.getForcedReturnId() == MapId.NONE || MapId.isMapleIsland(mapId))) {
                        player.forceChangeMap(targetMap, targetMap.getRandomPlayerSpawnpoint());
                        success = true;
                    } else {
                        player.dropMessage(1, error1);
                    }
                } else {
                    player.dropMessage(1, "You cannot teleport between continents with this teleport rock.");
                }
            } else {
                String name = p.readString();
                Character victim = c.getChannelServer().getPlayerStorage().getCharacterByName(name);

                if (victim != null) {
                    MapleMap targetMap = victim.getMap();
                    if (!FieldLimit.CANNOTVIPROCK.check(targetMap.getFieldLimit()) && (targetMap.getForcedReturnId() == MapId.NONE || MapId.isMapleIsland(targetMap.getId()))) {
                        if (!victim.isGM() || victim.gmLevel() <= player.gmLevel()) {   // thanks Yoboes for noticing non-GM's being unreachable through rocks
                            player.forceChangeMap(targetMap, targetMap.findClosestPlayerSpawnpoint(victim.getPosition()));
                            success = true;
                        } else {
                            player.dropMessage(1, error1);
                        }
                    } else {
                        player.dropMessage(1, "You cannot teleport to this map.");
                    }
                } else {
                    player.dropMessage(1, "Player could not be found in this channel.");
                }
            }

            if (!success) {
                InventoryManipulator.addById(c, itemId, (short) 1);
                c.sendPacket(PacketCreator.enableActions());
            }
        } else if (itemType == 505) { // AP/SP reset
            if (!player.isAlive()) {
                c.sendPacket(PacketCreator.enableActions());
                return;
            }

            if (itemId > ItemId.AP_RESET) {
                int SPTo = p.readInt();
                if (!AssignSPProcessor.canSPAssign(c, SPTo)) {  // exploit found thanks to Arnah
                    return;
                }

                int SPFrom = p.readInt();
                Skill skillSPTo = SkillFactory.getSkill(SPTo);
                Skill skillSPFrom = SkillFactory.getSkill(SPFrom);
                byte curLevel = player.getSkillLevel(skillSPTo);
                byte curLevelSPFrom = player.getSkillLevel(skillSPFrom);
                if ((curLevel < skillSPTo.getMaxLevel()) && curLevelSPFrom > 0) {
                    player.changeSkillLevel(skillSPFrom, (byte) (curLevelSPFrom - 1), player.getMasterLevel(skillSPFrom), -1);
                    player.changeSkillLevel(skillSPTo, (byte) (curLevel + 1), player.getMasterLevel(skillSPTo), -1);

                    // update macros, thanks to Arnah
                    if ((curLevelSPFrom - 1) == 0) {
                        boolean updated = false;
                        for (SkillMacro macro : player.getMacros()) {
                            if (macro == null) {
                                continue;
                            }

                            boolean update = false;// cleaner?
                            if (macro.getSkill1() == SPFrom) {
                                update = true;
                                macro.setSkill1(0);
                            }
                            if (macro.getSkill2() == SPFrom) {
                                update = true;
                                macro.setSkill2(0);
                            }
                            if (macro.getSkill3() == SPFrom) {
                                update = true;
                                macro.setSkill3(0);
                            }
                            if (update) {
                                updated = true;
                                player.updateMacros(macro.getPosition(), macro);
                            }
                        }
                        if (updated) {
                            player.sendMacros();
                        }
                    }
                }
            } else {
                int APTo = p.readInt();
                int APFrom = p.readInt();

                if (!AssignAPProcessor.APResetAction(c, APFrom, APTo)) {
                    return;
                }
            }
            remove(c, position, itemId);
        } else if (itemType == 506) {
            Item eq = null;
            if (itemId == 5060000) { // Item tag.
                int equipSlot = p.readShort();
                if (equipSlot == 0) {
                    return;
                }
                eq = player.getInventory(InventoryType.EQUIPPED).getItem((short) equipSlot);
                eq.setOwner(player.getName());
            } else if (itemId == 5060001 || itemId == 5061000 || itemId == 5061001 || itemId == 5061002 || itemId == 5061003) { // Sealing lock
                InventoryType type = InventoryType.getByType((byte) p.readInt());
                eq = player.getInventory(type).getItem((short) p.readInt());
                if (eq == null) { //Check if the type is EQUIPMENT?
                    return;
                }
                short flag = eq.getFlag();
                if (eq.getExpiration() > -1 && (eq.getFlag() & ItemConstants.LOCK) != ItemConstants.LOCK) {
                    return; //No perma items pls
                }
                flag |= ItemConstants.LOCK;
                eq.setFlag(flag);

                long period = 0;
                if (itemId == 5061000) {
                    period = 7;
                } else if (itemId == 5061001) {
                    period = 30;
                } else if (itemId == 5061002) {
                    period = 90;
                } else if (itemId == 5061003) {
                    period = 365;
                }

                if (period > 0) {
                    long expiration = eq.getExpiration() > -1 ? eq.getExpiration() : currentServerTime();
                    eq.setExpiration(expiration + DAYS.toMillis(period));
                }

                // double-remove found thanks to BHB
            } else if (itemId == 5060002) { // Incubator
                byte inventory2 = (byte) p.readInt();
                short slot2 = (short) p.readInt();
                Item item2 = player.getInventory(InventoryType.getByType(inventory2)).getItem(slot2);
                if (item2 == null) // hacking
                {
                    return;
                }
                if (getIncubatedItem(c, itemId)) {
                    InventoryManipulator.removeFromSlot(c, InventoryType.getByType(inventory2), slot2, (short) 1, false);
                    remove(c, position, itemId);
                }
                return;
            }
            p.readInt(); // time stamp
            if (eq != null) {
                player.forceUpdateItem(eq);
                remove(c, position, itemId);
            }
        } else if (itemType == 507) {
            boolean whisper;
            switch ((itemId / 1000) % 10) {
                case 1: // Megaphone
                    if (player.getLevel() > 9) {
                        player.getClient().getChannelServer().broadcastPacket(PacketCreator.serverNotice(2, medal + player.getName() + " : " + p.readString()));
                    } else {
                        player.dropMessage(1, "You may not use this until you're level 10.");
                        return;
                    }
                    break;
                case 2: // Super megaphone
                    Server.getInstance().broadcastMessage(c.getWorld(), PacketCreator.serverNotice(3, c.getChannel(), medal + player.getName() + " : " + p.readString(), (p.readByte() != 0)));
                    break;
                case 5: // Maple TV
                    int tvType = itemId % 10;
                    boolean megassenger = false;
                    boolean ear = false;
                    Character victim = null;
                    if (tvType != 1) {
                        if (tvType >= 3) {
                            megassenger = true;
                            if (tvType == 3) {
                                p.readByte();
                            }
                            ear = 1 == p.readByte();
                        } else if (tvType != 2) {
                            p.readByte();
                        }
                        if (tvType != 4) {
                            victim = c.getChannelServer().getPlayerStorage().getCharacterByName(p.readString());
                        }
                    }
                    List<String> messages = new LinkedList<>();
                    StringBuilder builder = new StringBuilder();
                    for (int i = 0; i < 5; i++) {
                        String message = p.readString();
                        if (megassenger) {
                            builder.append(" ").append(message);
                        }
                        messages.add(message);
                    }
                    p.readInt();

                    if (!MapleTVEffect.broadcastMapleTVIfNotActive(player, victim, messages, tvType)) {
                        player.dropMessage(1, "MapleTV is already in use.");
                        return;
                    }

                    if (megassenger) {
                        Server.getInstance().broadcastMessage(c.getWorld(), PacketCreator.serverNotice(3, c.getChannel(), medal + player.getName() + " : " + builder, ear));
                    }

                    break;
                case 6: //item megaphone
                    String msg = medal + player.getName() + " : " + p.readString();
                    whisper = p.readByte() == 1;
                    Item item = null;
                    if (p.readByte() == 1) { //item
                        item = player.getInventory(InventoryType.getByType((byte) p.readInt())).getItem((short) p.readInt());
                        if (item == null) //hack
                        {
                            return;
                        }

                        // thanks Conrad for noticing that untradeable items should be allowed in megas
                    }
                    Server.getInstance().broadcastMessage(c.getWorld(), PacketCreator.itemMegaphone(msg, whisper, c.getChannel(), item));
                    break;
                case 7: //triple megaphone
                    int lines = p.readByte();
                    if (lines < 1 || lines > 3) //hack
                    {
                        return;
                    }
                    String[] msg2 = new String[lines];
                    for (int i = 0; i < lines; i++) {
                        msg2[i] = medal + player.getName() + " : " + p.readString();
                    }
                    whisper = p.readByte() == 1;
                    Server.getInstance().broadcastMessage(c.getWorld(), PacketCreator.getMultiMegaphone(msg2, c.getChannel(), whisper));
                    break;
            }
            remove(c, position, itemId);
        } else if (itemType == 508) {   // thanks tmskdl12 for graduation banner; thanks ratency for first pointing lack of Kite handling
            Kite kite = new Kite(player, p.readString(), itemId);

            if (!GameConstants.isFreeMarketRoom(player.getMapId())) {
                player.getMap().spawnKite(kite);
                remove(c, position, itemId);
            } else {
                c.sendPacket(PacketCreator.sendCannotSpawnKite());
            }
        } else if (itemType == 509) {
            String sendTo = p.readString();
            String msg = p.readString();
            boolean sendSuccess = noteService.sendNormal(msg, player.getName(), sendTo);
            if (sendSuccess) {
                remove(c, position, itemId);
                c.sendPacket(new SendNoteSuccessPacket());
            }
        } else if (itemType == 510) {
            player.getMap().broadcastMessage(PacketCreator.musicChange("Jukebox/Congratulation"));
            remove(c, position, itemId);
        } else if (itemType == 512) {
            if (ii.getStateChangeItem(itemId) != 0) {
                for (Character mChar : player.getMap().getCharacters()) {
                    ii.getItemEffect(ii.getStateChangeItem(itemId)).applyTo(mChar);
                }
            }
            player.getMap().startMapEffect(ii.getMsg(itemId).replaceFirst("%s", player.getName()).replaceFirst("%s", p.readString()), itemId);
            remove(c, position, itemId);
        } else if (itemType == 517) {
            Pet pet = player.getPet(0);
            if (pet == null) {
                c.sendPacket(PacketCreator.enableActions());
                return;
            }
            String newName = p.readString();
            pet.setName(newName);
            pet.saveToDb();

            Item item = player.getInventory(InventoryType.CASH).getItem(pet.getPosition());
            if (item != null) {
                player.forceUpdateItem(item);
            }

            player.getMap().broadcastMessage(player, PacketCreator.changePetName(player, newName, 1), true);
            c.sendPacket(PacketCreator.enableActions());
            remove(c, position, itemId);
        } else if (itemType == 520) {
            player.gainMeso(ii.getMeso(itemId), true, false, true);
            remove(c, position, itemId);
            c.sendPacket(PacketCreator.enableActions());
        } else if (itemType == 523) {
            int itemid = p.readInt();

            if (!YamlConfig.config.server.USE_ENFORCE_ITEM_SUGGESTION) {
                c.getWorldServer().addOwlItemSearch(itemid);
            }
            player.setOwlSearch(itemid);
            List<Pair<PlayerShopItem, AbstractMapObject>> hmsAvailable = c.getWorldServer().getAvailableItemBundles(itemid);
            if (!hmsAvailable.isEmpty()) {
                remove(c, position, itemId);
            }

            c.sendPacket(PacketCreator.owlOfMinerva(c, itemid, hmsAvailable));
            c.sendPacket(PacketCreator.enableActions());

        } else if (itemType == 524) {
            for (byte i = 0; i < 3; i++) {
                Pet pet = player.getPet(i);
                if (pet != null) {
                    Pair<Integer, Boolean> pair = pet.canConsume(itemId);

                    if (pair.getRight()) {
                        pet.gainTamenessFullness(player, pair.getLeft(), 100, 1, true);
                        remove(c, position, itemId);
                        break;
                    }
                } else {
                    break;
                }
            }
            c.sendPacket(PacketCreator.enableActions());
        } else if (itemType == 530) {
            ii.getItemEffect(itemId).applyTo(player);
            remove(c, position, itemId);
        } else if (itemType == 533) {
            DueyProcessor.dueySendTalk(c, true);
        } else if (itemType == 537) {
            if (GameConstants.isFreeMarketRoom(player.getMapId())) {
                player.dropMessage(5, "You cannot use the chalkboard here.");
                player.sendPacket(PacketCreator.enableActions());
                return;
            }

            player.setChalkboard(p.readString());
            player.getMap().broadcastMessage(PacketCreator.useChalkboard(player, false));
            player.sendPacket(PacketCreator.enableActions());
            //remove(c, position, itemId);  thanks Conrad for noticing chalkboards shouldn't be depleted upon use
        } else if (itemType == 539) {
            List<String> strLines = new LinkedList<>();
            for (int i = 0; i < 4; i++) {
                strLines.add(p.readString());
            }

            final int world = c.getWorld();
            Server.getInstance().broadcastMessage(world, PacketCreator.getAvatarMega(player, medal, c.getChannel(), itemId, strLines, (p.readByte() != 0)));
            TimerManager.getInstance().schedule(() -> Server.getInstance().broadcastMessage(world, PacketCreator.byeAvatarMega()), SECONDS.toMillis(10));
            remove(c, position, itemId);
        } else if (itemType == 540) {
            p.readByte();
            p.readInt();
            if (itemId == ItemId.NAME_CHANGE) {
                c.sendPacket(PacketCreator.showNameChangeCancel(player.cancelPendingNameChange()));
            } else if (itemId == ItemId.WORLD_TRANSFER) {
                c.sendPacket(PacketCreator.showWorldTransferCancel(player.cancelPendingWorldTranfer()));
            }
            remove(c, position, itemId);
            c.sendPacket(PacketCreator.enableActions());
        } else if (itemType == 543) {
            if (itemId == ItemId.MAPLE_LIFE_B && !accountService.addChrSlot(c)) {
                player.dropMessage(1, "You have already used up all 12 extra character slots.");
                c.sendPacket(PacketCreator.enableActions());
                return;
            }

            String name = p.readString();
            int face = p.readInt();
            int hair = p.readInt();
            int haircolor = p.readInt();
            int skin = p.readInt();
            int gender = p.readInt();
            int jobid = p.readInt();
            int improveSp = p.readInt();

            int createStatus = switch (jobid) {
                case 0 -> WarriorCreator.createCharacter(c, name, face, hair + haircolor, skin, gender, improveSp);
                case 1 -> MagicianCreator.createCharacter(c, name, face, hair + haircolor, skin, gender, improveSp);
                case 2 -> BowmanCreator.createCharacter(c, name, face, hair + haircolor, skin, gender, improveSp);
                case 3 -> ThiefCreator.createCharacter(c, name, face, hair + haircolor, skin, gender, improveSp);
                default -> PirateCreator.createCharacter(c, name, face, hair + haircolor, skin, gender, improveSp);
            };

            if (createStatus == 0) {
                c.sendPacket(PacketCreator.sendMapleLifeError(0));   // success!

                player.showHint("#bSuccess#k on creation of the new character through the Maple Life card.");
                remove(c, position, itemId);
            } else {
                if (createStatus == -1) {    // check name
                    c.sendPacket(PacketCreator.sendMapleLifeNameError());
                } else {
                    c.sendPacket(PacketCreator.sendMapleLifeError(-1 * createStatus));
                }
            }
        } else if (itemType == 545) { // MiuMiu's travel store
            if (player.getShop() == null) {
                Optional<Shop> shop = shopFactory.getShop(MIU_MIU_SHOP_ID);
                if (shop.isPresent()) {
                    shop.get().sendShop(c);
                    remove(c, position, itemId);
                }
            } else {
                c.sendPacket(PacketCreator.enableActions());
            }
        } else if (itemType == 550) { //Extend item expiration
            c.sendPacket(PacketCreator.enableActions());
        } else if (itemType == 552) {
            InventoryType type = InventoryType.getByType((byte) p.readInt());
            short slot = (short) p.readInt();
            Item item = player.getInventory(type).getItem(slot);
            if (item == null || item.getQuantity() <= 0 || KarmaManipulator.hasKarmaFlag(item) || !ii.isKarmaAble(item.getItemId())) {
                c.sendPacket(PacketCreator.enableActions());
                return;
            }

            KarmaManipulator.setKarmaFlag(item);
            player.forceUpdateItem(item);
            remove(c, position, itemId);
            c.sendPacket(PacketCreator.enableActions());
        } else if (itemType == 552) { //DS EGG THING
            c.sendPacket(PacketCreator.enableActions());
        } else if (itemType == 557) {
            p.readInt();
            int itemSlot = p.readInt();
            p.readInt();
            final Equip equip = (Equip) player.getInventory(InventoryType.EQUIP).getItem((short) itemSlot);
            if (equip.getVicious() >= 2 || player.getInventory(InventoryType.CASH).findById(ItemId.VICIOUS_HAMMER) == null) {
                return;
            }
            equip.setVicious(equip.getVicious() + 1);
            equip.setUpgradeSlots(equip.getUpgradeSlots() + 1);
            remove(c, position, itemId);
            c.sendPacket(PacketCreator.enableActions());
            c.sendPacket(PacketCreator.sendHammerData(equip.getVicious()));
            player.forceUpdateItem(equip);
        } else if (itemType == 561) { //VEGA'S SPELL
            if (p.readInt() != 1) {
                return;
            }

            final byte eSlot = (byte) p.readInt();
            final Item eitem = player.getInventory(InventoryType.EQUIP).getItem(eSlot);

            if (p.readInt() != 2) {
                return;
            }

            final byte uSlot = (byte) p.readInt();
            final Item uitem = player.getInventory(InventoryType.USE).getItem(uSlot);
            if (eitem == null || uitem == null) {
                return;
            }

            Equip toScroll = (Equip) eitem;
            if (toScroll.getUpgradeSlots() < 1) {
                c.sendPacket(PacketCreator.getInventoryFull());
                return;
            }

            //should have a check here against PE hacks
            if (itemId / 1000000 != 5) {
                itemId = 0;
            }

            player.toggleBlockCashShop();

            final int curlevel = toScroll.getLevel();
            c.sendPacket(PacketCreator.sendVegaScroll(0x40));

            final Equip scrolled = (Equip) ii.scrollEquipWithId(toScroll, uitem.getItemId(), false, itemId, player.isGM());
            c.sendPacket(PacketCreator.sendVegaScroll(scrolled.getLevel() > curlevel ? 0x41 : 0x43));
            //opcodes 0x42, 0x44: "this item cannot be used"; 0x39, 0x45: crashes

            InventoryManipulator.removeFromSlot(c, InventoryType.USE, uSlot, (short) 1, false);
            remove(c, position, itemId);

            final Client client = c;
            TimerManager.getInstance().schedule(() -> {
                if (!player.isLoggedin()) {
                    return;
                }

                player.toggleBlockCashShop();

                final List<ModifyInventory> mods = new ArrayList<>();
                mods.add(new ModifyInventory(3, scrolled));
                mods.add(new ModifyInventory(0, scrolled));
                client.sendPacket(PacketCreator.modifyInventory(true, mods));

                ScrollResult scrollResult = scrolled.getLevel() > curlevel ? ScrollResult.SUCCESS : ScrollResult.FAIL;
                player.getMap().broadcastMessage(PacketCreator.getScrollEffect(player.getId(), scrollResult, false, false));
                if (eSlot < 0 && (scrollResult == ScrollResult.SUCCESS)) {
                    player.equipChanged();
                }

                client.sendPacket(PacketCreator.enableActions());
            }, SECONDS.toMillis(3));
        } else {
            log.warn("NEW CASH ITEM TYPE: {}, packet: {}", itemType, p);
            c.sendPacket(PacketCreator.enableActions());
        }
    }

    private static void remove(Client c, short position, int itemid) {
        Inventory cashInv = c.getPlayer().getInventory(InventoryType.CASH);
        cashInv.lockInventory();
        try {
            Item it = cashInv.getItem(position);
            if (it == null || it.getItemId() != itemid) {
                it = cashInv.findById(itemid);
                if (it != null) {
                    position = it.getPosition();
                }
            }

            InventoryManipulator.removeFromSlot(c, InventoryType.CASH, position, (short) 1, true, false);
        } finally {
            cashInv.unlockInventory();
        }
    }

    private static boolean getIncubatedItem(Client c, int id) {
        final int[] ids = {1012070, 1302049, 1302063, 1322027, 2000004, 2000005, 2020013, 2020015, 2040307, 2040509, 2040519, 2040521, 2040533, 2040715, 2040717, 2040810, 2040811, 2070005, 2070006, 4020009,};
        final int[] quantitys = {1, 1, 1, 1, 240, 200, 200, 200, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 3};
        int amount = 0;
        for (int i = 0; i < ids.length; i++) {
            if (i == id) {
                amount = quantitys[i];
            }
        }
        if (c.getPlayer().getInventory(InventoryType.getByType((byte) (id / 1000000))).isFull()) {
            return false;
        }
        InventoryManipulator.addById(c, id, (short) amount);
        return true;
    }
}
