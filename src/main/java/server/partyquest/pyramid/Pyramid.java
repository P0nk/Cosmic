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

package server.partyquest.pyramid;

import client.Character;
import client.QuestStatus;
import net.server.Server;
import net.server.channel.Channel;
import net.server.world.Party;
import net.server.world.PartyCharacter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.ItemInformationProvider;
import server.TimerManager;
import server.life.LifeFactory;
import server.life.Monster;
import server.maps.MapleMap;
import server.quest.Quest;
import tools.PacketCreator;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.ScheduledFuture;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * @author kevintjuh93
 * @author Silwhoon
 */
public class Pyramid {

    private static final Logger log = LoggerFactory.getLogger(Pyramid.class);
    private static final int PROTECTOR_OF_PHARAOH_QUEST = 29932;
    private static final int ENTRANCE_MAP_ID = 926010000;
    private static final int END_MAP_ID = 926010001;

    private int world = -1;
    private int channel = -1;
    private final PyramidDifficulty difficulty;
    private final boolean solo;
    private final List<Character> characters = new ArrayList<>();
    private final Map<Integer, MapleMap> maps = new HashMap<>();
    private int gauge;
    private int counter = 0;
    private int currentStage = 0;
    private int coolAdd;
    private int decrease;
    private int hitAdd;
    private int missSub;
    private int total;
    private ScheduledFuture<?> stageTimer = null;
    private ScheduledFuture<?> coreTimer = null;
    private ScheduledFuture<?> respawnTimer = null;

    protected Pyramid(Character soloCharacter, PyramidDifficulty difficulty) {
        this.difficulty = difficulty;
        this.solo = true;

        addCharacter(soloCharacter);
    }

    public Pyramid(Party party, PyramidDifficulty difficulty) {
        this.difficulty = difficulty;
        this.solo = false;

        for (PartyCharacter partyCharacter : party.getPartyMembersOnline()) {
            Character character = partyCharacter.getPlayer();
            addCharacter(character);
        }
    }

    private void warpToEnd(Character character) {
        // Remove the character from the PQ, and remove their instance reference to the PQ
        removeCharacter(character);

        // If they are dead, respawn them in the main map
        if (!character.isAlive()) {
            character.respawn(ENTRANCE_MAP_ID);
            return;
        }

        // Otherwise move them to the end map
        character.changeMap(END_MAP_ID);
    }

    private List<Character> getCharacters() {
        synchronized (this.characters) {
            return Collections.unmodifiableList(this.characters);
        }
    }

    private void addCharacter(Character character) {
        // If the world and channel aren't set, set it to whatever the first player's world/channel is
        // The ONLY reason we need this is because of how MapManager works..
        if (world == -1) world = character.getWorld();
        if (channel == -1) channel = character.getClient().getChannel();

        synchronized (this.characters) {
            this.characters.add(character);
        }

        character.setPyramidCharacterStats(new PyramidCharacterStats(this.difficulty));
    }

    private void removeCharacter(Character character) {
        cancelPharaohBuffs(character);

        PyramidProcessor.closePyramid(character.getId());

        synchronized (this.characters) {
            this.characters.remove(character);
        }
    }

    public void start() {
        for (Character character : getCharacters()) {
            PyramidProcessor.registerPyramid(character.getId(), this);

            broadcastInfo(character, "party", solo ? 0 : 1);
            broadcastInfo(character, "hit", 0);
            broadcastInfo(character, "miss", 0);
            broadcastInfo(character, "cool", 0);
            broadcastInfo(character, "skill", 0);
            broadcastInfo(character, "laststage", 1);
        }

        startNextStage();
    }

    private void complete() {
        if (stageTimer != null) {
            stageTimer.cancel(true);
            stageTimer = null;
        }
        if (coreTimer != null) {
            coreTimer.cancel(true);
            coreTimer = null;
        }
        if (respawnTimer != null) {
            respawnTimer.cancel(true);
            respawnTimer = null;
        }

        List<Character> charactersToRemove = new ArrayList<>(getCharacters());
        for (Character character : charactersToRemove) {
            warpToEnd(character);
        }
    }

    public void fail(Character character) {
        // Set the characters rank to D
        character.getPyramidCharacterStats().setRank(PyramidRank.D);

        // Warp them to the end map
        warpToEnd(character);
    }

    public void failAllCharacters() {
        List<Character> charactersToRemove = new ArrayList<>(getCharacters());
        for (Character character : charactersToRemove) {
            fail(character);
        }
    }

    private void startNextStage() {
        // Increment stage counter
        currentStage++;

        // If the stage is above 5, then the PQ has been successfully completed. Warp them all!
        if (currentStage > 5) {
            complete();
            return;
        }

        // Set all the relevant map stats ready for the next stage
        coolAdd = getMap(getCurrentMapId()).getPyramidInfo().getCoolAdd();
        decrease = getMap(getCurrentMapId()).getPyramidInfo().getDecrease();
        hitAdd = getMap(getCurrentMapId()).getPyramidInfo().getHitAdd();
        missSub = getMap(getCurrentMapId()).getPyramidInfo().getMissSub();
        total = getMap(getCurrentMapId()).getPyramidInfo().getTotal();
        gauge = total;
        counter = 0;

        spawnYetiMonsters();

        List<Character> charactersToRemove = new ArrayList<>();

        // Move players into the map
        for (Character character : getCharacters()) {
            // If the character died during the stage, we warp them out
            if (!character.isAlive()) {
                charactersToRemove.add(character);
            } else {
                character.changeMap(getCurrentMapId());
                character.sendPacket(PacketCreator.getClock(getStageTimeInSeconds()));
                broadCastStageInfo(character);
            }
        }

        // Remove the dead players
        for (Character character : charactersToRemove) {
            warpToEnd(character);
        }

        // Start the relevant timers for the stage
        startStageTimer();
        startCoreTimer();
        startRespawnTimer();
    }

    private void startStageTimer() {
        if (stageTimer != null) {
            stageTimer.cancel(false);
            stageTimer = null;
        }

        stageTimer = TimerManager.getInstance().schedule(() -> {
            // Stop the core timer, startNextStage() will restart it again
            if (coreTimer != null) {
                coreTimer.cancel(true);
                coreTimer = null;
            }

            // Also stop the respawn timer, startNextStage() will restart it again
            if (respawnTimer != null) {
                respawnTimer.cancel(true);
                respawnTimer = null;
            }

            // Warp all players to new stage
            startNextStage();
        }, SECONDS.toMillis(getStageTimeInSeconds()));
    }

    private void startCoreTimer() {
        // Core timer loop - Runs every second
        coreTimer = TimerManager.getInstance().register(() -> {
            gauge -= decrease;
            if (gauge <= 0) {
                failAllCharacters();
            }
        }, SECONDS.toMillis(1));
    }

    private void startRespawnTimer() {
        // TODO: Find GMS-like respawn rates.
        //  Every video I watch the mobs begin spawning around 9-10 secs from when the stage starts and then spawn every
        //  3-5 ish seconds. But in the WZ files there is a 'createMobInterval' node that is set to 3000 (I'm assuming milliseconds)
        //  but I feel like respawn every 3 secs is way too much.. especially for solo
        respawnTimer = TimerManager.getInstance().register(() -> {
            getMap(getCurrentMapId()).respawn();
        }, SECONDS.toMillis(5), SECONDS.toMillis(10));
    }

    private void spawnYetiMonsters() {
        int easyYetiToSpawn = 0; // Yeti that spawns after 90 seconds
        int hardYetiToSpawn = 0; // Yeti that spawns after 15 seconds

        // The number of Yeti's spawned is based on whether you are running solo or not

        // Solo:
        //  Stage 1: 0
        //  Stage 2: 1 after 90 sec - Spawn 9700023
        //  Stage 3: 1 after 15 sec - Spawn 9700022
        //  Stage 4: 1 after 15 sec, 1 after 90 sec - Spawn 9700022 and 2700023
        //  Stage 5: 2 after 15 sec - Spawn 2 9700022
        if (solo) {
            switch (currentStage) {
                case 2:
                    easyYetiToSpawn = 1;
                    break;
                case 3:
                    hardYetiToSpawn = 1;
                    break;
                case 4:
                    easyYetiToSpawn = 1;
                    hardYetiToSpawn = 1;
                    break;
                case 5:
                    hardYetiToSpawn = 2;
                    break;
                default:
                    return;
            }
        }

        // Party:
        //  Stage 1: 1 after 90 sec - Spawn 9700023
        //  Stage 2: 1 after 15 sec - Spawn 9700022
        //  Stage 3: 1 after 15 sec, 1 after 90 sec - Spawn 9700022 and 2700023
        //  Stage 4: 2 after 15 sec - Spawn 2 9700022
        //  Stage 5: 2 after 15 sec - Spawn 2 9700022
        if (!solo) {
            switch (currentStage) {
                case 1:
                    easyYetiToSpawn = 1;
                    break;
                case 2:
                    hardYetiToSpawn = 1;
                    break;
                case 3:
                    easyYetiToSpawn = 1;
                    hardYetiToSpawn = 1;
                    break;
                case 4, 5:
                    hardYetiToSpawn = 2;
                    break;
                default:
                    return;
            }
        }

        for (int i = 0; i < easyYetiToSpawn; i++) {
            getMap(getCurrentMapId()).spawnMonsterOnGroundBelow(LifeFactory.getMonster(9700023), new Point(0, 88));
        }
        for (int i = 0; i < hardYetiToSpawn; i++) {
            getMap(getCurrentMapId()).spawnMonsterOnGroundBelow(LifeFactory.getMonster(9700022), new Point(0, 88));
        }
    }

    private void cancelPharaohBuffs(Character character) {
        ItemInformationProvider ii = ItemInformationProvider.getInstance();
        character.cancelEffect(ii.getItemEffect(2022585), false, -1);
        character.cancelEffect(ii.getItemEffect(2022586), false, -1);
        character.cancelEffect(ii.getItemEffect(2022587), false, -1);
        character.cancelEffect(ii.getItemEffect(2022588), false, -1);
    }

    public void leaveParty(Character character) {
        // Leaving the party only matters if the PQ was setup as a party PQ and not a solo one
        if (!solo) {
            fail(character);
        }
    }

    public void disbandParty() {
        // Disbanding the party only matters if the PQ was setup as a party PQ and not a solo one
        if (!solo) {
            failAllCharacters();
        }
    }

    public void playerDead(Character character) {
        // When the player dies, their rank must be set to D
        character.getPyramidCharacterStats().setRank(PyramidRank.D);
    }

    public boolean revivePlayer(Character character) {
        // When the player revives after dying, remove them from the PQ and warp them to the end map
        warpToEnd(character);
        return false;
    }

    public void forfeitChallenge(Character character) {
        // This is called when the player manually selects that they wish to 'Forfeit the Challenge' while inside the PQ
        // Set their rank to D and warp to the end map
        fail(character);
    }

    public void playerDisconnected(Character character) {
        // If the player disconnects, remove from from the PQ and remove their reference to this instance
        removeCharacter(character);
    }

    private int getCurrentMapId() {
        int mapId = 926010000;
        if (!solo) {
            mapId += 10000;
        }

        mapId += (this.difficulty.getMode() * 1000);
        mapId += (this.currentStage * 100);

        return mapId;
    }

    public void hitMonster(Character character, Monster monster, int damage) {
        // If the damage is 0, then the player has missed
        if (damage == 0) {
            missMonster(character);
            return;
        }

        // Otherwise, the player killed the monsteer
        killMonster(character);

        // Check if the kill proc'd a 'Cool' effect
        if (damage >= monster.getStats().getCoolDamage()) {
            int rand = (new Random().nextInt(100) + 1);
            if (rand <= monster.getStats().getCoolDamageProb()) {
                coolProc(character);
            }
        }

        // Give the player a use of the Rage of Pharaoh skill if necessary
        broadcastInfo(character, "skill", character.getPyramidCharacterStats().getAvailableSkillUses());

        // Check if the player needs a new 'Pharaoh's Blessing' buff
        checkBlessingBuff(character);
    }

    private void killMonster(Character character) {
        character.getPyramidCharacterStats().addHits(1);
        character.getPyramidCharacterStats().calculateRank();
        addQuestProgress(character);
        if (gauge < total) {
            counter++;
        }
        gauge += hitAdd;

        broadcastInfo(character, "hit", character.getPyramidCharacterStats().getTotalHits());
        if (gauge >= total) {
            gauge = total;
        }
    }

    private void coolProc(Character character) {
        character.getPyramidCharacterStats().addCools(1);
        character.getPyramidCharacterStats().calculateRank();
        int plus = coolAdd;
        if ((gauge + coolAdd) > total) {
            plus -= ((gauge + coolAdd) - total);
        }
        gauge += plus;
        counter += plus;
        if (gauge >= total) {
            gauge = total;
        }
        broadcastInfo(character, "cool", character.getPyramidCharacterStats().getTotalCools());
    }

    private void missMonster(Character character) {
        character.getPyramidCharacterStats().addMisses(1);
        character.getPyramidCharacterStats().calculateRank();
        gauge -= missSub;
        counter -= missSub;

        broadcastInfo(character, "miss", character.getPyramidCharacterStats().getTotalMisses());
    }

    public void useSkill(Character character) {
        if (!character.getPyramidCharacterStats().canUseSkill()) {
            fail(character);
            return;
        }

        character.getPyramidCharacterStats().addSkillUses(1);
    }

    public void addQuestProgress(Character character) {
        QuestStatus questStatus = character.getQuest(PROTECTOR_OF_PHARAOH_QUEST);
        QuestStatus.Status currentStatus = questStatus.getStatus();

        // If the player has not yet started the quest, force start it
        if (currentStatus.equals(QuestStatus.Status.NOT_STARTED)) {
            Quest quest = Quest.getInstance(PROTECTOR_OF_PHARAOH_QUEST);
            quest.forceStart(character, quest.getNpcRequirement(false));
        }

        try {
            // Otherwise, if it is started then increment progress
            if (currentStatus.equals(QuestStatus.Status.STARTED)) {
                int currentProgress = 0;
                if (!questStatus.getProgress(7760).isEmpty()) {
                    currentProgress = Integer.parseInt(questStatus.getProgress(7760));
                }

                currentProgress++;

                character.setQuestProgress(PROTECTOR_OF_PHARAOH_QUEST, 7760, String.valueOf(currentProgress));
            }
        } catch (NumberFormatException nfe) {
            log.error("Trying to update characters pyramid quest, but getProgress(7760) return a non-integer", nfe);
        }
    }

    public void checkBlessingBuff(Character character) {
        int itemId = character.getPyramidCharacterStats().getBlessingBuff();
        if (itemId != 0 && !character.hasActiveBuff(itemId)) {
            ItemInformationProvider ii = ItemInformationProvider.getInstance();
            ii.getItemEffect(itemId).applyTo(character);
        }
    }

    public boolean checkCharactersArePresent() {
        for (Character character : getCharacters()) {
            if (character.getWorld() != world ||
                    character.getClient().getChannel() != channel ||
                    character.getMap().getId() != ENTRANCE_MAP_ID) {
                return false;
            }
        }

        return true;
    }

    public boolean checkCharacterLevels() {
        for (Character character : getCharacters()) {
            if (character.getLevel() < getMinLevel() || character.getLevel() > getMaxLevel()) {
                return false;
            }
        }

        return true;
    }

    public int getMinLevel() {
        return switch (this.difficulty) {
            case EASY -> 40;
            case NORMAL -> 46;
            case HARD -> 51;
            case HELL -> 61;
        };
    }

    public int getMaxLevel() {
        return switch (this.difficulty) {
            case EASY, NORMAL, HARD -> 60;
            case HELL -> 200;
        };
    }

    public int getStageTimeInSeconds() {
        return switch (this.currentStage) {
            case 0, 1 -> 120;
            default -> 180;
        };
    }

    public MapleMap getMap(int id) {
        if (this.maps.containsKey(id)) {
            return this.maps.get(id);
        }

        Channel cs = Server.getInstance().getWorld(world).getChannel(channel);

        // Get a fresh map
        MapleMap map = cs.getMapFactory().getDisposableMap(id);

        // Kill all monsters
        for (Monster monster : map.getAllMonsters()) {
            map.killMonster(monster, null, false);
        }

        this.maps.put(id, map);

        return map;
    }

    public void broadCastStageInfo(Character character) {
        character.sendPacket(PacketCreator.mapEffect("killing/first/stage"));
        character.sendPacket(PacketCreator.mapEffect("killing/first/number/" + currentStage));
        character.sendPacket(PacketCreator.mapEffect("killing/first/start"));
    }

    public void broadcastInfo(Character character, String info, int amount) {
        character.sendPacket(PacketCreator.getEnergy("massacre_" + info, amount));
        broadcastGauge();
    }

    private void broadcastGauge() {
        synchronized (this.characters) {
            for (Character character : this.characters) {
                character.sendPacket(PacketCreator.pyramidGauge(counter));
            }
        }
    }
}
