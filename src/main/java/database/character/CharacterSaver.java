package database.character;

import client.BuddylistEntry;
import client.Character;
import client.CharacterStats;
import client.Disease;
import client.FamilyEntry;
import client.QuestStatus;
import client.Skill;
import client.SkillMacro;
import client.inventory.Inventory;
import client.inventory.InventoryType;
import client.inventory.Item;
import client.inventory.ItemFactory;
import client.inventory.Pet;
import client.keybind.KeyBinding;
import client.keybind.QuickslotBinding;
import constants.id.MapId;
import database.PgDatabaseConnection;
import database.monsterbook.MonsterCardRepository;
import lombok.extern.slf4j.Slf4j;
import net.server.PlayerCoolDownValueHolder;
import net.server.Server;
import org.jdbi.v3.core.Handle;
import server.CashShop;
import server.Storage;
import server.events.Events;
import server.life.MobSkill;
import server.life.MobSkillId;
import server.maps.SavedLocation;
import server.maps.SavedLocationType;
import tools.DatabaseConnection;
import tools.LongTool;
import tools.Pair;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Slf4j
public class CharacterSaver {
    private static final Set<InventoryType> INVENTORIES_TO_SAVE = Set.of(InventoryType.EQUIP, InventoryType.USE,
            InventoryType.SETUP, InventoryType.ETC, InventoryType.CASH, InventoryType.EQUIPPED);

    private final Server server;
    private final PgDatabaseConnection pgConnection;
    private final CharacterRepository characterRepository;
    private final MonsterCardRepository monsterCardRepository;

    public CharacterSaver(Server server, PgDatabaseConnection pgConnection,
                          CharacterRepository characterRepository,
                          MonsterCardRepository monsterCardRepository) {
        this.server = server;
        this.pgConnection = pgConnection;
        this.characterRepository = characterRepository;
        this.monsterCardRepository = monsterCardRepository;
    }

    public void save(Character chr) {
        if (!chr.isLoggedin()) {
            log.debug("Not saving chr {} - not logged in", chr.getName());
            return;
        }

        log.debug("Saving chr {}", chr.getName());
        server.updateCharacterEntry(chr);
        saveToMysql(chr);
        saveToPostgres(chr);
    }

    private void saveToMysql(Character chr) {
        Instant before = Instant.now();
        try (Connection con = DatabaseConnection.getConnection()) {
            con.setAutoCommit(false);
            con.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
            try {
                saveChrMysql(con, chr);
                con.commit();
            } catch (Exception e) {
                con.rollback();
                throw e;
            } finally {
                con.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
                con.setAutoCommit(true);
            }
        } catch (SQLException e) {
            log.error("Error saving chr {}, level: {}, job: {}", chr.getName(), chr.getLevel(), chr.getJob().getId(), e);
        }
        Duration saveDuration = Duration.between(before, Instant.now());
        log.debug("Saved {} to MySQL in {} ms", chr.getName(), saveDuration.toMillis());
    }

    private void saveChrMysql(Connection con, Character chr) throws SQLException {
        saveCharacter(con, chr);
        saveInventory(con, chr);
        saveCashShop(con, chr);
        saveStorage(con, chr);
        savePets(con, chr);
        saveBuddies(con, chr);
        saveFamily(con, chr);
        saveDiseases(con, chr);
        saveKeymap(con, chr);
        saveQuickmap(con, chr);
        saveSkills(con, chr);
        saveCooldowns(con, chr);
        saveSkillMacros(con, chr);
        saveQuests(con, chr);
        saveTeleportRockLocations(con, chr);
        savePetIgnoredItems(con, chr);
        saveSavedLocations(con, chr);
        saveAreaInfo(con, chr);
        saveMonsterBook(con, chr);
        saveEventStats(con, chr);
    }

    private void saveCharacter(Connection con, Character chr) throws SQLException {
        CharacterStats stats = chr.getCharacterStats();
        try (PreparedStatement ps = con.prepareStatement("UPDATE characters SET level = ?, fame = ?, str = ?, dex = ?, luk = ?, `int` = ?, exp = ?, gachaexp = ?, hp = ?, mp = ?, maxhp = ?, maxmp = ?, sp = ?, ap = ?, gm = ?, skincolor = ?, gender = ?, job = ?, hair = ?, face = ?, map = ?, meso = ?, hpMpUsed = ?, spawnpoint = ?, party = ?, buddyCapacity = ?, messengerid = ?, messengerposition = ?, mountlevel = ?, mountexp = ?, mounttiredness= ?, equipslots = ?, useslots = ?, setupslots = ?, etcslots = ?,  monsterbookcover = ?, vanquisherStage = ?, dojoPoints = ?, lastDojoStage = ?, finishedDojoTutorial = ?, vanquisherKills = ?, matchcardwins = ?, matchcardlosses = ?, matchcardties = ?, omokwins = ?, omoklosses = ?, omokties = ?, dataString = ?, jailexpire = ?, partnerId = ?, marriageItemId = ?, lastExpGainTime = ?, ariantPoints = ?, partySearch = ? WHERE id = ?", Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, stats.level());
            ps.setInt(2, stats.fame());
            ps.setInt(3, stats.str());
            ps.setInt(4, stats.dex());
            ps.setInt(5, stats.luk());
            ps.setInt(6, stats.int_());
            ps.setInt(7, stats.exp());
            ps.setInt(8, stats.gachaExp());
            ps.setInt(9, stats.hp());
            ps.setInt(10, stats.mp());
            ps.setInt(11, stats.maxHp());
            ps.setInt(12, stats.maxMp());
            ps.setString(13, String.valueOf(stats.sp()));
            ps.setInt(14, stats.ap());
            ps.setInt(15, stats.gmLevel());
            ps.setInt(16, stats.skin());
            ps.setInt(17, stats.gender());
            ps.setInt(18, stats.job());
            ps.setInt(19, stats.hair());
            ps.setInt(20, stats.face());
            ps.setInt(21, stats.mapId());
            ps.setInt(22, stats.meso());
            ps.setInt(23, stats.hpMpApUsed());
            ps.setInt(24, stats.spawnPortal());
            ps.setInt(25, Objects.requireNonNullElse(stats.party(), -1));
            ps.setInt(26, stats.buddyCapacity());
            ps.setInt(27, Objects.requireNonNullElse(stats.messenger(), 0));
            ps.setInt(28, Objects.requireNonNullElse(stats.messengerPosition(), 4));
            ps.setInt(29, Objects.requireNonNullElse(stats.mountLevel(), 1));
            ps.setInt(30, Objects.requireNonNullElse(stats.mountExp(), 0));
            ps.setInt(31, Objects.requireNonNullElse(stats.mountTiredness(), 0));
            ps.setInt(32, stats.equipSlots());
            ps.setInt(33, stats.useSlots());
            ps.setInt(34, stats.setupSlots());
            ps.setInt(35, stats.etcSlots());
            ps.setInt(36, stats.monsterBookCover());
            ps.setInt(37, stats.dojoVanquisherStage());
            ps.setInt(38, stats.dojoPoints());
            ps.setInt(39, stats.dojoStage());
            ps.setInt(40, stats.dojoTutorialComplete() ? 1 : 0);
            ps.setInt(41, stats.dojoVanquisherKills());
            ps.setInt(42, stats.matchCardWins());
            ps.setInt(43, stats.matchCardLosses());
            ps.setInt(44, stats.matchCardTies());
            ps.setInt(45, stats.omokWins());
            ps.setInt(46, stats.omokLosses());
            ps.setInt(47, stats.omokTies());
            ps.setString(48, stats.dataString());
            ps.setLong(49, Objects.requireNonNullElse(stats.jailExpiration(), 0L));
            ps.setInt(50, Objects.requireNonNullElse(stats.partnerId(), -1));
            ps.setInt(51, Objects.requireNonNullElse(stats.marriageItemId(), -1));
            ps.setTimestamp(52, new Timestamp(stats.lastExpGainTime()));
            ps.setInt(53, stats.ariantPoints());
            ps.setBoolean(54, stats.canRecvPartySearchInvite());
            ps.setInt(55, stats.id());

            int updateRows = ps.executeUpdate();
            if (updateRows < 1) {
                throw new RuntimeException("Character not in database (" + chr.getId() + ")");
            }
        }
    }

    private void saveInventory(Connection con, Character chr) throws SQLException {
        List<Pair<Item, InventoryType>> itemsWithType = new ArrayList<>();
        for (InventoryType type : INVENTORIES_TO_SAVE) {
            Inventory inventory = chr.getInventory(type);
            for (Item item : inventory.list()) {
                itemsWithType.add(new Pair<>(item, type));
            }
        }
        ItemFactory.INVENTORY.saveItems(itemsWithType, chr.getId(), con);
    }

    private void saveCashShop(Connection con, Character chr) throws SQLException {
        CashShop cashShop = chr.getCashShop();
        if (cashShop == null) {
            return;
        }
        cashShop.save(con);
    }

    private void saveStorage(Connection con, Character chr) throws SQLException {
        Storage storage = chr.getStorage();
        if (storage == null || !chr.usedStorage()) {
            return;
        }
        storage.saveToDB(con);
        chr.resetUsedStorage();
    }

    private void savePets(Connection con, Character chr) throws SQLException {
        Pet[] pets = chr.getPets();
        for (Pet pet : pets) {
            if (pet == null) {
                continue;
            }
            savePet(con, pet);
        }
    }

    private void savePet(Connection con, Pet pet) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("UPDATE pets SET name = ?, level = ?, closeness = ?, fullness = ?, summoned = ?, flag = ? WHERE petid = ?")) {
            ps.setString(1, pet.getName());
            ps.setInt(2, pet.getLevel());
            ps.setInt(3, pet.getTameness());
            ps.setInt(4, pet.getFullness());
            ps.setInt(5, pet.isSummoned() ? 1 : 0);
            ps.setInt(6, pet.getPetAttribute());
            ps.setInt(7, pet.getUniqueId());
            ps.executeUpdate();
        }
    }

    private void saveBuddies(Connection con, Character chr) throws SQLException {
        deleteBuddies(con, chr);
        insertBuddies(con, chr);
    }

    private void deleteBuddies(Connection con, Character chr) throws SQLException {
        String sql = """
                DELETE FROM buddies
                WHERE characterid = ?
                AND pending = 0""";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, chr.getId());
            ps.executeUpdate();
        }
    }

    private void insertBuddies(Connection con, Character chr) throws SQLException {
        try (PreparedStatement psBuddy = con.prepareStatement("INSERT INTO buddies (characterid, `buddyid`, `pending`, `group`) VALUES (?, ?, 0, ?)")) {
            psBuddy.setInt(1, chr.getId());

            for (BuddylistEntry entry : chr.getBuddylist().getBuddies()) {
                if (entry.isVisible()) {
                    psBuddy.setInt(2, entry.getCharacterId());
                    psBuddy.setString(3, entry.getGroup());
                    psBuddy.addBatch();
                }
            }
            psBuddy.executeBatch();
        }
    }

    private void saveQuests(Connection con, Character chr) throws SQLException {
        deleteMedalMaps(con, chr);
        deleteQuestProgress(con, chr);
        deleteQuestStatus(con, chr);

        insertQuestStatusAndQuestProgressAndMedalMaps(con, chr);
    }

    private void deleteMedalMaps(Connection con, Character chr) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM medalmaps WHERE characterid = ?")) {
            ps.setInt(1, chr.getId());
            ps.executeUpdate();
        }
    }

    private void deleteQuestProgress(Connection con, Character chr) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM questprogress WHERE characterid = ?")) {
            ps.setInt(1, chr.getId());
            ps.executeUpdate();
        }
    }

    private void deleteQuestStatus(Connection con, Character chr) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM queststatus WHERE characterid = ?")) {
            ps.setInt(1, chr.getId());
            ps.executeUpdate();
        }
    }

    private void insertQuestStatusAndQuestProgressAndMedalMaps(Connection con, Character chr) throws SQLException {
        try (PreparedStatement psStatus = con.prepareStatement("INSERT INTO queststatus (`queststatusid`, `characterid`, `quest`, `status`, `time`, `expires`, `forfeited`, `completed`) VALUES (DEFAULT, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
             PreparedStatement psProgress = con.prepareStatement("INSERT INTO questprogress VALUES (DEFAULT, ?, ?, ?, ?)");
             PreparedStatement psMedal = con.prepareStatement("INSERT INTO medalmaps VALUES (DEFAULT, ?, ?, ?)")) {
            psStatus.setInt(1, chr.getId());

            for (QuestStatus qs : chr.getQuests()) {
                psStatus.setInt(2, qs.getQuest().getId());
                psStatus.setInt(3, qs.getStatus().getId());
                psStatus.setInt(4, (int) (qs.getCompletionTime() / 1000));
                psStatus.setLong(5, qs.getExpirationTime());
                psStatus.setInt(6, qs.getForfeited());
                psStatus.setInt(7, qs.getCompleted());
                psStatus.executeUpdate();

                try (ResultSet rs = psStatus.getGeneratedKeys()) {
                    rs.next();
                    for (int mob : qs.getProgress().keySet()) {
                        psProgress.setInt(1, chr.getId());
                        psProgress.setInt(2, rs.getInt(1));
                        psProgress.setInt(3, mob);
                        psProgress.setString(4, qs.getProgress(mob));
                        psProgress.addBatch();
                    }
                    psProgress.executeBatch();

                    for (int i = 0; i < qs.getMedalMaps().size(); i++) {
                        psMedal.setInt(1, chr.getId());
                        psMedal.setInt(2, rs.getInt(1));
                        psMedal.setInt(3, qs.getMedalMaps().get(i));
                        psMedal.addBatch();
                    }
                    psMedal.executeBatch();
                }
            }
        }
    }

    private void saveTeleportRockLocations(Connection con, Character chr) throws SQLException {
        deleteTeleportRockLocations(con, chr);
        insertRegularTeleportRockLocations(con, chr);
        insertVipTeleportRockLocations(con, chr);
    }

    private void deleteTeleportRockLocations(Connection con, Character chr) throws SQLException {
        String sql = """
                DELETE FROM trocklocations
                WHERE characterid = ?""";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, chr.getId());
            ps.executeUpdate();
        }
    }

    private void insertRegularTeleportRockLocations(Connection con, Character chr) throws SQLException {
        List<Integer> viptrockmaps = chr.getVipTrockMaps();
        try (PreparedStatement psReg = con.prepareStatement("INSERT INTO trocklocations(characterid, mapid, vip) VALUES (?, ?, 1)")) {
            for (int i = 0; i < chr.getVipTrockSize(); i++) {
                if (viptrockmaps.get(i) != MapId.NONE) {
                    psReg.setInt(1, chr.getId());
                    psReg.setInt(2, viptrockmaps.get(i));
                    psReg.addBatch();
                }
            }
            psReg.executeBatch();
        }
    }

    private void insertVipTeleportRockLocations(Connection con, Character chr) throws SQLException {
        List<Integer> trockmaps = chr.getTrockMaps();
        try (PreparedStatement psVip = con.prepareStatement("INSERT INTO trocklocations(characterid, mapid, vip) VALUES (?, ?, 0)")) {
            for (int i = 0; i < chr.getTrockSize(); i++) {
                if (trockmaps.get(i) != MapId.NONE) {
                    psVip.setInt(1, chr.getId());
                    psVip.setInt(2, trockmaps.get(i));
                    psVip.addBatch();
                }
            }
            psVip.executeBatch();
        }
    }

    private void saveMonsterBook(Connection con, Character chr) throws SQLException {
        chr.getMonsterBook().saveCards(con, chr.getId());
    }

    private void savePetIgnoredItems(Connection con, Character chr) throws SQLException {
        for (Map.Entry<Integer, Set<Integer>> es : chr.getExcluded().entrySet()) {
            try (PreparedStatement psIgnore = con.prepareStatement("DELETE FROM petignores WHERE petid=?")) {
                psIgnore.setInt(1, es.getKey());
                psIgnore.executeUpdate();
            }

            try (PreparedStatement psIgnore = con.prepareStatement("INSERT INTO petignores (petid, itemid) VALUES (?, ?)")) {
                psIgnore.setInt(1, es.getKey());
                for (Integer x : es.getValue()) {
                    psIgnore.setInt(2, x);
                    psIgnore.addBatch();
                }
                psIgnore.executeBatch();
            }
        }
    }

    private void saveKeymap(Connection con, Character chr) throws SQLException {
        deleteKeymap(con, chr.getId());
        insertKeymap(con, chr);
    }

    private void deleteKeymap(Connection con, int chrId) throws SQLException {
        String sql = """
                DELETE FROM keymap
                WHERE characterid = ?""";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, chrId);
            ps.executeUpdate();
        }
    }

    private void insertKeymap(Connection con, Character chr) throws SQLException {
        try (PreparedStatement psKey = con.prepareStatement("INSERT INTO keymap (characterid, `key`, `type`, `action`) VALUES (?, ?, ?, ?)")) {
            psKey.setInt(1, chr.getId());

            for (Map.Entry<Integer, KeyBinding> keybinding : chr.getKeymap().entrySet()) {
                psKey.setInt(2, keybinding.getKey());
                psKey.setInt(3, keybinding.getValue().getType());
                psKey.setInt(4, keybinding.getValue().getAction());
                psKey.addBatch();
            }
            psKey.executeBatch();
        }
    }

    private void saveQuickmap(Connection con, Character chr) throws SQLException {
        QuickslotBinding quickslotBinding = chr.getQuickslotBinding();
        if (quickslotBinding == null) {
            return;
        }
        long nQuickslotKeymapped = LongTool.BytesToLong(quickslotBinding.GetKeybindings());

        try (final PreparedStatement psQuick = con.prepareStatement("INSERT INTO quickslotkeymapped (accountid, keymap) VALUES (?, ?) ON DUPLICATE KEY UPDATE keymap = ?;")) {
            psQuick.setInt(1, chr.getAccountID());
            psQuick.setLong(2, nQuickslotKeymapped);
            psQuick.setLong(3, nQuickslotKeymapped);
            psQuick.executeUpdate();
        }

    }

    private void saveSkills(Connection con, Character chr) throws SQLException {
        try (PreparedStatement psSkill = con.prepareStatement("REPLACE INTO skills (characterid, skillid, skilllevel, masterlevel, expiration) VALUES (?, ?, ?, ?, ?)")) {
            psSkill.setInt(1, chr.getId());
            for (Map.Entry<Skill, Character.SkillEntry> skill : chr.getSkills().entrySet()) {
                psSkill.setInt(2, skill.getKey().getId());
                psSkill.setInt(3, skill.getValue().skillevel);
                psSkill.setInt(4, skill.getValue().masterlevel);
                psSkill.setLong(5, skill.getValue().expiration);
                psSkill.addBatch();
            }
            psSkill.executeBatch();
        }
    }

    private void saveCooldowns(Connection con, Character chr) throws SQLException {
        deleteCooldowns(con, chr);
        insertCooldowns(con, chr);
    }

    private void deleteCooldowns(Connection con, Character chr) throws SQLException {
        String sql = """
                DELETE FROM cooldowns
                WHERE charid = ?""";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, chr.getId());
            ps.executeUpdate();
        }
    }

    private void insertCooldowns(Connection con, Character chr) throws SQLException {
        List<PlayerCoolDownValueHolder> cooldowns = chr.getAllCooldowns();
        if (cooldowns.isEmpty()) {
            return;
        }
        try (PreparedStatement ps = con.prepareStatement("INSERT INTO cooldowns (charid, SkillID, StartTime, length) VALUES (?, ?, ?, ?)")) {
            ps.setInt(1, chr.getId());
            for (PlayerCoolDownValueHolder cooling : cooldowns) {
                ps.setInt(2, cooling.skillId);
                ps.setLong(3, cooling.startTime);
                ps.setLong(4, cooling.length);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void saveSkillMacros(Connection con, Character chr) throws SQLException {
        deleteSkillMacros(con, chr.getId());
        insertSkillMacros(con, chr);
    }

    private void deleteSkillMacros(Connection con, int chrId) throws SQLException {
        String sql = """
                DELETE FROM skillmacros
                WHERE characterid = ?""";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, chrId);
            ps.executeUpdate();
        }
    }

    private void insertSkillMacros(Connection con, Character chr) throws SQLException {
        SkillMacro[] skillMacros = chr.getMacros();
        try (PreparedStatement psMacro = con.prepareStatement("INSERT INTO skillmacros (characterid, skill1, skill2, skill3, name, shout, position) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            psMacro.setInt(1, chr.getId());
            for (int i = 0; i < 5; i++) {
                SkillMacro macro = skillMacros[i];
                if (macro != null) {
                    psMacro.setInt(2, macro.getSkill1());
                    psMacro.setInt(3, macro.getSkill2());
                    psMacro.setInt(4, macro.getSkill3());
                    psMacro.setString(5, macro.getName());
                    psMacro.setInt(6, macro.getShout());
                    psMacro.setInt(7, i);
                    psMacro.addBatch();
                }
            }
            psMacro.executeBatch();
        }
    }

    private void saveFamily(Connection con, Character chr) {
        FamilyEntry familyEntry = chr.getFamilyEntry();
        if (familyEntry == null) {
            return;
        }

        if (familyEntry.saveReputation(con)) {
            familyEntry.savedSuccessfully();
        }

        FamilyEntry senior = familyEntry.getSenior();
        if (senior == null || senior.getChr() != null) { // Only save for offline family members
            return;
        }
        if (senior.saveReputation(con)) {
            senior.savedSuccessfully();
        }

        FamilyEntry seniorsSenior = senior.getSenior();
        if (seniorsSenior == null || seniorsSenior.getChr() != null) {
            return;
        }
        if (senior.saveReputation(con)) {
            senior.savedSuccessfully();
        }
    }

    private void saveDiseases(Connection con, Character chr) throws SQLException {
        deleteDiseases(con, chr);
        insertDiseases(con, chr);
    }

    private void deleteDiseases(Connection con, Character chr) throws SQLException {
        String sql = """
                DELETE FROM playerdiseases
                WHERE charid = ?""";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, chr.getId());
            ps.executeUpdate();
        }
    }

    private void insertDiseases(Connection con, Character chr) throws SQLException {
        Map<Disease, Pair<Long, MobSkill>> diseases = chr.getAllDiseases();
        if (diseases.isEmpty()) {
            return;
        }
        try (PreparedStatement ps = con.prepareStatement("INSERT INTO playerdiseases (charid, disease, mobskillid, mobskilllv, length) VALUES (?, ?, ?, ?, ?)")) {
            ps.setInt(1, chr.getId());

            for (Map.Entry<Disease, Pair<Long, MobSkill>> e : diseases.entrySet()) {
                ps.setInt(2, e.getKey().ordinal());

                MobSkill ms = e.getValue().getRight();
                MobSkillId msId = ms.getId();
                ps.setInt(3, msId.type().getId());
                ps.setInt(4, msId.level());
                ps.setInt(5, e.getValue().getLeft().intValue());
                ps.addBatch();
            }

            ps.executeBatch();
        }
    }

    private void saveSavedLocations(Connection con, Character chr) throws SQLException {
        deleteSavedLocations(con, chr.getId());
        insertSavedLocations(con, chr);
    }

    private void deleteSavedLocations(Connection con, int chrId) throws SQLException {
        String sql = """
                DELETE FROM savedlocations
                WHERE characterid = ?""";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, chrId);
            ps.executeUpdate();
        }
    }

    private void insertSavedLocations(Connection con, Character chr) throws SQLException {
        SavedLocation[] savedLocations = chr.getSavedLocations();
        try (PreparedStatement psLoc = con.prepareStatement("INSERT INTO savedlocations (characterid, `locationtype`, `map`, `portal`) VALUES (?, ?, ?, ?)")) {
            psLoc.setInt(1, chr.getId());
            for (SavedLocationType savedLocationType : SavedLocationType.values()) {
                if (savedLocations[savedLocationType.ordinal()] != null) {
                    psLoc.setString(2, savedLocationType.name());
                    psLoc.setInt(3, savedLocations[savedLocationType.ordinal()].getMapId());
                    psLoc.setInt(4, savedLocations[savedLocationType.ordinal()].getPortal());
                    psLoc.addBatch();
                }
            }
            psLoc.executeBatch();
        }
    }

    private void saveAreaInfo(Connection con, Character chr) throws SQLException {
        deleteAreaInfo(con, chr);
        insertAreaInfo(con, chr);
    }

    private void deleteAreaInfo(Connection con, Character chr) throws SQLException {
        String sql = """
                DELETE FROM area_info
                WHERE charid = ?""";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, chr.getId());
            ps.executeUpdate();
        }
    }

    private void insertAreaInfo(Connection con, Character chr) throws SQLException {
        try (PreparedStatement psArea = con.prepareStatement("INSERT INTO area_info (id, charid, area, info) VALUES (DEFAULT, ?, ?, ?)")) {
            psArea.setInt(1, chr.getId());

            for (Map.Entry<Short, String> area : chr.getAreaInfos().entrySet()) {
                psArea.setInt(2, area.getKey());
                psArea.setString(3, area.getValue());
                psArea.addBatch();
            }
            psArea.executeBatch();
        }
    }

    private void saveEventStats(Connection con, Character chr) throws SQLException {
        deleteEventStats(con, chr);
        insertEventStats(con, chr);
    }

    private void deleteEventStats(Connection con, Character chr) throws SQLException {
        String sql = """
                DELETE FROM eventstats
                WHERE characterid = ?""";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, chr.getId());
            ps.executeUpdate();
        }
    }

    private void insertEventStats(Connection con, Character chr) throws SQLException {
        try (PreparedStatement psEvent = con.prepareStatement("INSERT INTO eventstats (characterid, name, info) VALUES (?, ?, ?)")) {
            psEvent.setInt(1, chr.getId());

            for (Map.Entry<String, Events> entry : chr.getEvents().entrySet()) {
                psEvent.setString(2, entry.getKey());
                psEvent.setInt(3, entry.getValue().getInfo());
                psEvent.addBatch();
            }

            psEvent.executeBatch();
        }
    }

    private void saveToPostgres(Character chr) {
        Instant before = Instant.now();

        try (Handle handle = pgConnection.getHandle()) {
            handle.useTransaction(h -> doPostgresSave(h, chr));
        } catch (Exception e) {
            System.err.println("Error saving chr to PG: " + e.getMessage());
            log.error("Error saving chr {} to PG", chr.getName(), e);
        }

        Duration saveDuration = Duration.between(before, Instant.now());
        log.debug("Saved {} to PostgreSQL in {} ms", chr.getName(), saveDuration.toMillis());
    }

    private void doPostgresSave(Handle handle, Character chr) {
        characterRepository.update(handle, chr.getCharacterStats());
        monsterCardRepository.save(handle, chr.getId(), chr.getMonsterBook().getCards());
    }

}
