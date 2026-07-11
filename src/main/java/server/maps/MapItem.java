/*
	This file is part of the OdinMS Maple Story Server
    Copyright (C) 2008 ~ 2010 Patrick Huy <patrick.huy@frz.cc>
                       Matthias Butz <matze@odinms.de>
                       Jan Christian Meyer <vimes@odinms.de>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License version 3
    as published by the Free Software Foundation. You may not use, modify
    or distribute this program under any other version of the
    GNU Affero General Public License.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package server.maps;

import client.Character;
import client.Client;
import client.inventory.Item;
import client.inventory.manipulator.InventoryManipulator;
import server.ItemInformationProvider;
import tools.PacketCreator;

import java.awt.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.util.concurrent.TimeUnit.SECONDS;

public class MapItem extends AbstractMapObject {
    protected Client ownerClient;
    protected Item item;
    protected MapObject dropper;
    protected int character_ownerid, party_ownerid, meso, questid = -1;
    protected byte type;
    protected boolean pickedUp = false, playerDrop, partyDrop;
    protected long dropTime;
    private final Lock itemLock = new ReentrantLock();

    public MapItem(Item item, Point position, MapObject dropper, Character owner, Client ownerClient, byte type, boolean playerDrop) {
        setPosition(position);
        this.item = item;
        this.dropper = dropper;
        this.character_ownerid = owner.getId();
        this.party_ownerid = owner.getPartyId();
        this.partyDrop = this.party_ownerid != -1;
        this.ownerClient = owner.getClient();
        this.meso = 0;
        this.type = type;
        this.playerDrop = playerDrop;
    }

    public MapItem(Item item, Point position, MapObject dropper, Character owner, Client ownerClient, byte type, boolean playerDrop, int questid) {
        setPosition(position);
        this.item = item;
        this.dropper = dropper;
        this.character_ownerid = owner.getId();
        this.party_ownerid = owner.getPartyId();
        this.partyDrop = this.party_ownerid != -1;
        this.ownerClient = owner.getClient();
        this.meso = 0;
        this.type = type;
        this.playerDrop = playerDrop;
        this.questid = questid;
    }

    public MapItem(int meso, Point position, MapObject dropper, Character owner, Client ownerClient, byte type, boolean playerDrop) {
        setPosition(position);
        this.item = null;
        this.dropper = dropper;
        this.character_ownerid = owner.getId();
        this.party_ownerid = owner.getPartyId();
        this.partyDrop = this.party_ownerid != -1;
        this.ownerClient = owner.getClient();
        this.meso = meso;
        this.type = type;
        this.playerDrop = playerDrop;
    }

    public final Item getItem() {
        return item;
    }

    public final int getQuest() {
        return questid;
    }

    public final int getItemId() {
        if (meso > 0) {
            return meso;
        }
        return item.getItemId();
    }

    public final MapObject getDropper() {
        return dropper;
    }

    public final int getOwnerId() {
        return character_ownerid;
    }

    public final int getPartyOwnerId() {
        return party_ownerid;
    }

    public final void setPartyOwnerId(int partyid) {
        party_ownerid = partyid;
    }

    public final int getClientsideOwnerId() {   // thanks nozphex (RedHat) for noting an issue with collecting party items
        if (this.party_ownerid == -1) {
            return this.character_ownerid;
        } else {
            return this.party_ownerid;
        }
    }

    public final boolean hasClientsideOwnership(Character player) {
        return this.character_ownerid == player.getId() || this.party_ownerid == player.getPartyId() || hasExpiredOwnershipTime();
    }

    public final boolean isFFADrop() {
        return type == 2 || type == 3 || hasExpiredOwnershipTime();
    }

    public final boolean hasExpiredOwnershipTime() {
        return System.currentTimeMillis() - dropTime >= SECONDS.toMillis(15);
    }

    public final boolean canBePickedBy(Character chr) {
        return canBePickedBy(chr, false);
    }
    public final boolean canBePickedBy(Character chr, boolean byPet) {
        if (!chr.needQuestItem(questid, getItemId())) {
            return false;
        }

        if (this.meso == 0
                && !InventoryManipulator.checkSpace(chr.getClient(), getItemId(), getItem().getQuantity(), getItem().getOwner())
                && !ItemInformationProvider.getInstance().isConsumeOnPickup(getItemId())
        ){
            if (!byPet) {
                chr.getClient().sendPacket(PacketCreator.getInventoryFull());
                chr.getClient().sendPacket(PacketCreator.getShowInventoryFull());
            }

            return false;
        }
        if (character_ownerid <= 0 || isFFADrop()) {
            return true;
        }

        if (party_ownerid == -1) {
            if (chr.getId() == character_ownerid) {
                return true;
            } else if (chr.isPartyMember(character_ownerid)) {
                party_ownerid = chr.getPartyId();
                return true;
            }
        } else {
            if (chr.getPartyId() == party_ownerid) {
                return true;
            } else if (chr.getId() == character_ownerid) {
                party_ownerid = chr.getPartyId();
                return true;
            }
        }

        return hasExpiredOwnershipTime();
    }

    public final Client getOwnerClient() {
        return (ownerClient.isLoggedIn() && !ownerClient.getPlayer().isAwayFromWorld()) ? ownerClient : null;
    }

    public final int getMeso() {
        return meso;
    }

    public final boolean isPlayerDrop() {
        return playerDrop;
    }

    public final boolean isAlreadyPickedUp() {
        return pickedUp;
    }

    public void setPickedUp(final boolean pickedUp) {
        this.pickedUp = pickedUp;
    }

    public long getDropTime() {
        return dropTime;
    }

    public void setDropTime(long time) {
        this.dropTime = time;
    }

    public byte getDropType() {
        return type;
    }

    public void lockItem() {
        itemLock.lock();
    }

    public void unlockItem() {
        itemLock.unlock();
    }

    @Override
    public final MapObjectType getType() {
        return MapObjectType.ITEM;
    }

    @Override
    public void sendSpawnData(final Client client) {
        Character chr = client.getPlayer();

        if (chr.needQuestItem(questid, getItemId())) {
            this.lockItem();
            try {
                client.sendPacket(PacketCreator.dropItemFromMapObject(chr, this, null, getPosition(), (byte) 2));
            } finally {
                this.unlockItem();
            }
        }
    }

    @Override
    public void sendDestroyData(final Client client) {
        client.sendPacket(PacketCreator.removeItemFromMap(getObjectId(), 1, 0));
    }
}