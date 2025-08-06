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
package server.maps;

import client.Character;
import client.Client;
import net.packet.Packet;
import net.server.Server;
import tools.PacketCreator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static java.util.concurrent.TimeUnit.MINUTES;

/**
 * @author Matze
 * @author Ronan (HeavenMS)
 */
public class MiniGame extends AbstractMapObject {
    private Character owner;
    private Character visitor;
    private final String password;
    private MiniGameType GameType = MiniGameType.UNDEFINED;
    private int piecetype;
    private int inprogress = 0;
    private final int[] piece = new int[250];
    private final List<Integer> list4x3 = new ArrayList<>();
    private final List<Integer> list5x4 = new ArrayList<>();
    private final List<Integer> list6x5 = new ArrayList<>();
    private final String description;
    private int loser = 1;
    private int firstslot = 0;
    private int visitorpoints = 0, visitorscore = 0, visitorforfeits = 0, lastvisitor = -1;
    private int ownerpoints = 0, ownerscore = 0, ownerforfeits = 0;
    private boolean visitorquit, ownerquit;
    private long nextavailabletie = 0;
    private int matchestowin = 0;

    public enum MiniGameType {
        UNDEFINED(0), OMOK(1), MATCH_CARD(2);
        private int value = 0;

        MiniGameType(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    public enum MiniGameResult {
        WIN, LOSS, TIE
    }

    public MiniGame(Character owner, String description, String password) {
        this.owner = owner;
        this.description = description;
        this.password = password;
    }

    public String getPassword() {
        return this.password;
    }

    public boolean checkPassword(String sentPw) {
        return this.password.length() == 0 || sentPw.toLowerCase().contentEquals(this.password.toLowerCase());
    }

    public boolean hasFreeSlot() {
        return visitor == null;
    }

    public boolean isOwner(Character chr) {
        return owner.equals(chr);
    }

    public void addVisitor(Character challenger) {
        visitor = challenger;
        if (lastvisitor != challenger.getId()) {
            ownerscore = 0;
            ownerforfeits = 0;

            visitorscore = 0;
            visitorforfeits = 0;
            lastvisitor = challenger.getId();
        }

        Character owner = this.getOwner();
        if (GameType == MiniGameType.OMOK) {
            owner.sendPacket(PacketCreator.getMiniGameNewVisitor(this, challenger, 1));
            owner.getMap().broadcastMessage(PacketCreator.addOmokBox(owner, 2, 0));
        } else if (GameType == MiniGameType.MATCH_CARD) {
            owner.sendPacket(PacketCreator.getMatchCardNewVisitor(this, challenger, 1));
            owner.getMap().broadcastMessage(PacketCreator.addMatchCardBox(owner, 2, 0));
        }
    }

    public void closeRoom(boolean forceClose) {
        owner.getMap().broadcastMessage(PacketCreator.removeMinigameBox(owner));

        if (forceClose) {
            this.broadcastToOwner(PacketCreator.getMiniGameClose(false, 4));
        }
        this.broadcastToVisitor(PacketCreator.getMiniGameClose(true, 3));

        if (visitor != null) {
            visitor.setMiniGame(null);
            visitor = null;
        }

        owner.setMiniGame(null);
        owner = null;
    }

    public void removeVisitor(boolean forceClose, Character challenger) {
        if (visitor == challenger) {
            if (isMatchInProgress()) { // owner is winner if visitor leave in progress
                minigameMatchOwnerWins(true);
            }
            if (forceClose) {
                visitor.sendPacket(PacketCreator.getMiniGameClose(true, 4));
            }

            challenger.setMiniGame(null);
            visitor = null;

            this.getOwner().sendPacket(PacketCreator.getMiniGameRemoveVisitor());
            if (GameType == MiniGameType.OMOK) {
                this.getOwner().getMap().broadcastMessage(PacketCreator.addOmokBox(owner, 1, 0));
            } else if (GameType == MiniGameType.MATCH_CARD) {
                this.getOwner().getMap().broadcastMessage(PacketCreator.addMatchCardBox(owner, 1, 0));
            }
        }
    }

    public boolean isVisitor(Character challenger) {
        return visitor == challenger;
    }

    public void broadcastToOwner(Packet packet) {
        Client c = owner.getClient();
        if (c != null) {
            c.sendPacket(packet);
        }
    }

    public void broadcastToVisitor(Packet packet) {
        if (visitor != null) {
            visitor.sendPacket(packet);
        }
    }

    public void setFirstSlot(int type) {
        firstslot = type;
    }

    public int getFirstSlot() {
        return firstslot;
    }

    private void updateMiniGameBox() {
        this.getOwner().getMap().broadcastMessage(PacketCreator.addOmokBox(owner, visitor != null ? 2 : 1, inprogress));
    }

    private synchronized boolean minigameMatchFinish() {
        if (isMatchInProgress()) {
            inprogress = 0;
            return true;
        } else {
            return false;
        }
    }

    private void minigameMatchFinished() {
        updateMiniGameBox();
        Arrays.fill(piece, 0);
        if (ownerquit) {
            owner.closeMiniGame(true);
        } else if (visitorquit) {
            visitor.closeMiniGame(true);
        }
    }

    public void minigameMatchStarted() {
        inprogress = 1;
        ownerquit = false;
        visitorquit = false;
    }

    public void setQuitAfterGame(Character player, boolean quit) {
        if (isOwner(player)) {
            ownerquit = quit;
        } else {
            visitorquit = quit;
        }
    }

    public boolean isMatchInProgress() {
        return inprogress != 0;
    }

    public void denyTie(Character chr) {
        if (this.isOwner(chr)) {
            inprogress |= (1 << 1);
        } else {
            inprogress |= (1 << 2);
        }
    }

    public boolean isTieDenied(Character chr) {
        if (this.isOwner(chr)) {
            return ((inprogress >> 2) % 2) == 1;
        } else {
            return ((inprogress >> 1) % 2) == 1;
        }
    }

    public void minigameMatchOwnerWins(boolean forfeit) {
        if (!minigameMatchFinish()) {
            return;
        }

        owner.setMiniGamePoints(visitor, 1, this.isOmok());

        if (visitorforfeits < 4 || !forfeit) {
            ownerscore += 50;
        }
        visitorscore += (15 * (forfeit ? -1 : 1));
        if (forfeit) {
            visitorforfeits++;
        }

        this.broadcast(PacketCreator.getMiniGameOwnerWin(this, forfeit));

        minigameMatchFinished();
    }

    public void minigameMatchVisitorWins(boolean forfeit) {
        if (!minigameMatchFinish()) {
            return;
        }

        owner.setMiniGamePoints(visitor, 2, this.isOmok());

        if (ownerforfeits < 4 || !forfeit) {
            visitorscore += 50;
        }
        ownerscore += (15 * (forfeit ? -1 : 1));
        if (forfeit) {
            ownerforfeits++;
        }

        this.broadcast(PacketCreator.getMiniGameVisitorWin(this, forfeit));

        minigameMatchFinished();
    }

    public void minigameMatchDraw() {
        if (!minigameMatchFinish()) {
            return;
        }

        owner.setMiniGamePoints(visitor, 3, this.isOmok());

        long timeNow = Server.getInstance().getCurrentTime();
        if (nextavailabletie <= timeNow) {
            visitorscore += 10;
            ownerscore += 10;

            nextavailabletie = timeNow + MINUTES.toMillis(5);
        }

        this.broadcast(PacketCreator.getMiniGameTie(this));

        minigameMatchFinished();
    }

    public void setOwnerPoints() {
        ownerpoints++;
        if (ownerpoints + visitorpoints == matchestowin) {
            if (ownerpoints == visitorpoints) {
                minigameMatchDraw();
            } else if (ownerpoints > visitorpoints) {
                minigameMatchOwnerWins(false);
            } else {
                minigameMatchVisitorWins(false);
            }
            ownerpoints = 0;
            visitorpoints = 0;
        }
    }

    public void setVisitorPoints() {
        visitorpoints++;
        if (ownerpoints + visitorpoints == matchestowin) {
            if (ownerpoints > visitorpoints) {
                minigameMatchOwnerWins(false);
            } else if (visitorpoints > ownerpoints) {
                minigameMatchVisitorWins(false);
            } else {
                minigameMatchDraw();
            }
            ownerpoints = 0;
            visitorpoints = 0;
        }
    }

    public void setMatchesToWin(int type) {
        matchestowin = type;
    }

    public void setPieceType(int type) {
        piecetype = type;
    }

    public int getPieceType() {
        return piecetype;
    }

    public void setGameType(MiniGameType game) {
        GameType = game;
        if (GameType == MiniGameType.MATCH_CARD) {
            if (matchestowin == 6) {
                for (int i = 0; i < 6; i++) {
                    list4x3.add(i);
                    list4x3.add(i);
                }
            } else if (matchestowin == 10) {
                for (int i = 0; i < 10; i++) {
                    list5x4.add(i);
                    list5x4.add(i);
                }
            } else {
                for (int i = 0; i < 15; i++) {
                    list6x5.add(i);
                    list6x5.add(i);
                }
            }
        }
    }

    public MiniGameType getGameType() {
        return GameType;
    }

    public boolean isOmok() {
        return GameType.equals(MiniGameType.OMOK);
    }

    public void shuffleList() {
        if (matchestowin == 6) {
            Collections.shuffle(list4x3);
        } else if (matchestowin == 10) {
            Collections.shuffle(list5x4);
        } else {
            Collections.shuffle(list6x5);
        }
    }

    public int getCardId(int slot) {
        int cardid;
        if (matchestowin == 6) {
            cardid = list4x3.get(slot);
        } else if (matchestowin == 10) {
            cardid = list5x4.get(slot);
        } else {
            cardid = list6x5.get(slot);
        }
        return cardid;
    }

    public int getMatchesToWin() {
        return matchestowin;
    }

    public void setLoser(int type) {
        loser = type;
    }

    public int getLoser() {
        return loser;
    }

    public void broadcast(Packet packet) {
        broadcastToOwner(packet);
        broadcastToVisitor(packet);
    }

    public void chat(Client c, String chat) {
        broadcast(PacketCreator.getPlayerShopChat(c.getPlayer(), chat, isOwner(c.getPlayer())));
    }

    public void sendOmok(Client c, int type) {
        c.sendPacket(PacketCreator.getMiniGame(c, this, isOwner(c.getPlayer()), type));
    }

    public void sendMatchCard(Client c, int type) {
        c.sendPacket(PacketCreator.getMatchCard(c, this, isOwner(c.getPlayer()), type));
    }

    public Character getOwner() {
        return owner;
    }

    public Character getVisitor() {
        return visitor;
    }

    public void setPiece(int move1, int move2, int type, Character chr) {
        int slot = move2 * 15 + move1 + 1;
        if (piece[slot] == 0) {
            piece[slot] = type;
            this.broadcast(PacketCreator.getMiniGameMoveOmok(this, move1, move2, type));
            boolean gameWon = false;
            for (int y = 0; y < 15 && !gameWon; y++) {
                for (int x = 0; x < 15 && !gameWon; x++) {
                    for (int d = 0; d < 4; d++) {
                        if (searchCombo(x, y, type, d)) {
                            gameWon = true;
                            break;
                        }
                    }
                }
            }
            if (gameWon) {
                if (this.isOwner(chr)) {
                    this.minigameMatchOwnerWins(false);
                    this.setLoser(0);
                } else {
                    this.minigameMatchVisitorWins(false);
                    this.setLoser(1);
                }
            }
        }
    }

    private boolean searchCombo(int x, int y, int type, int direction) {
        int slot = y * 15 + x + 1;
        int dx, dy;
        switch (direction) {
            case 0: dx = 1; dy = 0; break;
            case 1: dx = 0; dy = 1; break;
            case 2: dx = 1; dy = 1; break;
            case 3: dx = -1; dy = 1; break;
            default: return false;
        }
        int nx = x + (4 * dx);
        int ny = y + (4 * dy);
        if (nx < 0 || nx >= 15 || ny < 0 || ny >= 15) {
            return false;
        }
        for (int i = 0; i < 5; i++) {
            int checkSlot = slot + (i * dx) + (i * dy * 15);
            if (piece[checkSlot] != type) {
                return false;
            }
        }
        return true;
    }

    public String getDescription() {
        return description;
    }

    public int getOwnerScore() {
        return ownerscore;
    }

    public int getVisitorScore() {
        return visitorscore;
    }

    @Override
    public void sendDestroyData(Client client) {}

    @Override
    public void sendSpawnData(Client client) {}

    @Override
    public MapObjectType getType() {
        return MapObjectType.MINI_GAME;
    }
}
