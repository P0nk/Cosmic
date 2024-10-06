package client.creator;

import client.CharacterStats;
import client.Job;
import client.inventory.Item;
import constants.id.ItemId;
import constants.id.MapId;
import database.PgDatabaseConnection;
import database.character.CharacterRepository;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CharacterCreator {
    private final PgDatabaseConnection connection;
    private final CharacterRepository chrRepository;

    public CharacterCreator(PgDatabaseConnection connection, CharacterRepository chrRepository) {
        this.connection = connection;
        this.chrRepository = chrRepository;
    }

    public boolean createNew(NewCharacterSpec spec, int accountId, int worldId) {
        CharacterStats stats = getStarterStats(spec, accountId, worldId);

        try {
            connection.getHandle().useTransaction(h -> {
                chrRepository.insert(h, stats);
            });
        } catch (Exception e) {
            log.warn("Failed to create new character in PG", e);
        }
        Item guide = getStarterGuide(spec.type());
        // TODO, save:
        // - character
        // - skills
        // - equips
        // - starter item
        // - keymap
        // - quickslots
        return false;
    }

    private CharacterStats getStarterStats(NewCharacterSpec spec, int accountId, int worldId) {
        return CharacterStats.builder()
                .account(accountId)
                .world(worldId)
                .name(spec.name())
                .job(getJob(spec.type()).getId())
                .gender(spec.gender())
                .skin(spec.skin().getId())
                .hair(spec.hair() + spec.hairColor())
                .face(spec.face())
                .mapId(getStartingMap(spec.type()))
                .spawnPortal(0)
                .level(StarterStats.LEVEL)
                .exp(0)
                .str(StarterStats.STR)
                .dex(StarterStats.DEX)
                .int_(StarterStats.INT)
                .luk(StarterStats.LUK)
                .maxHp(StarterStats.HP)
                .hp(StarterStats.HP)
                .maxMp(StarterStats.MP)
                .mp(StarterStats.MP)
                .fame(0)
                .ap(0)
                .sp(0)
                .buddyCapacity(StarterStats.BUDDY_CAPACITY)
                .equipSlots(StarterStats.INVENTORY_SLOTS)
                .useSlots(StarterStats.INVENTORY_SLOTS)
                .setupSlots(StarterStats.INVENTORY_SLOTS)
                .etcSlots(StarterStats.INVENTORY_SLOTS)
                .gmLevel(StarterStats.GM_LEVEL)
                .gachaExp(0)
                .hpMpApUsed(0)
                .party(null)
                .build();
    }

    private Job getJob(JobType type) {
        return switch (type) {
            case ADVENTURER -> Job.BEGINNER;
            case KNIGHT_OF_CYGNUS -> Job.NOBLESSE;
            case ARAN -> Job.LEGEND;
        };
    }

    private int getStartingMap(JobType type) {
        return switch (type) {
            case ADVENTURER -> MapId.MUSHROOM_TOWN;
            case KNIGHT_OF_CYGNUS -> MapId.ARAN_TUTORIAL_START;
            case ARAN -> MapId.STARTING_MAP_NOBLESSE;
        };
    }

    private Item getStarterGuide(JobType type) {
        int itemId = switch (type) {
            case ADVENTURER -> ItemId.BEGINNERS_GUIDE;
            case KNIGHT_OF_CYGNUS -> ItemId.NOBLESSE_GUIDE;
            case ARAN -> ItemId.LEGENDS_GUIDE;
        };
        return new Item(itemId, (short) 0, (short) 1);
    }
}
