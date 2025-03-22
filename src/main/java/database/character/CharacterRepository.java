package database.character;

import client.CharacterStats;
import org.jdbi.v3.core.Handle;

import java.sql.Timestamp;

public class CharacterRepository {

    public int insert(Handle handle, CharacterStats stats) {
        String sql = """
                INSERT INTO chr (account, world, name, level, exp, str, dex, "int", luk, hp, mp, max_hp, max_mp, ap, sp,
                    job, fame, gender, skin, hair, face, meso, map_id, spawn_portal, gacha_exp, used_hp_mp_ap, gm_level,
                    party_id, buddy_capacity, equip_slots, use_slots, setup_slots, etc_slots)
                VALUES (:account, :world, :name, :level, :exp, :str, :dex, :int, :luk, :hp, :mp, :max_hp, :max_mp, :ap,
                    :sp, :job, :fame, :gender, :skin, :hair, :face, :meso, :map_id, :spawn_portal, :gacha_exp,
                    :used_hp_mp_ap, :gm_level, :party_id, :buddy_capacity, :equip_slots, :use_slots, :setup_slots,
                    :etc_slots)""";
        return handle.createUpdate(sql)
                .bind("account", stats.account())
                .bind("world", stats.world())
                .bind("name", stats.name())
                .bind("level", stats.level())
                .bind("exp", stats.exp())
                .bind("str", stats.str())
                .bind("dex", stats.dex())
                .bind("int", stats.int_())
                .bind("luk", stats.luk())
                .bind("hp", stats.hp())
                .bind("mp", stats.mp())
                .bind("max_hp", stats.maxHp())
                .bind("max_mp", stats.maxMp())
                .bind("ap", stats.ap())
                .bind("sp", stats.sp())
                .bind("job", stats.job())
                .bind("fame", stats.fame())
                .bind("gender", stats.gender())
                .bind("skin", stats.skin())
                .bind("hair", stats.hair())
                .bind("face", stats.face())
                .bind("meso", stats.meso())
                .bind("map_id", stats.mapId())
                .bind("spawn_portal", stats.spawnPortal())
                .bind("gacha_exp", stats.gachaExp())
                .bind("used_hp_mp_ap", stats.hpMpApUsed())
                .bind("gm_level", stats.gmLevel())
                .bind("party_id", stats.party())
                .bind("buddy_capacity", stats.buddyCapacity())
                .bind("equip_slots", stats.equipSlots())
                .bind("use_slots", stats.useSlots())
                .bind("setup_slots", stats.setupSlots())
                .bind("etc_slots", stats.etcSlots())
                .executeAndReturnGeneratedKeys("id")
                .mapTo(Integer.class)
                .one();
    }

    public boolean update(Handle handle, CharacterStats stats) {
        String sql = """
                UPDATE chr
                SET level = :level, exp = :exp, str = :str, dex = :dex, "int" = :int, luk = :luk, hp = :hp, mp = :mp,
                    max_hp = :max_hp, max_mp = :max_mp, ap = :ap, sp = :sp, job = :job, fame = :fame, gender = :gender,
                    skin = :skin, hair = :hair, face = :face, meso = :meso, map_id = :map_id, spawn_portal = :spawn_portal,
                    gacha_exp = :gacha_exp, used_hp_mp_ap = :used_hp_mp_ap, gm_level = :gm_level, party_id = :party_id,
                    buddy_capacity = :buddy_capacity, messenger_id = :messenger_id,
                    messenger_position = :messenger_position, mount_level = :mount_level, mount_exp = :mount_exp,
                    mount_tiredness = :mount_tiredness, omok_wins = :omok_wins, omok_losses = :omok_losses,
                    omok_ties = :omok_ties, matchcard_wins = :matchcard_wins, matchcard_losses = :matchcard_losses,
                    matchcard_ties = :matchcard_ties, equip_slots = :equip_slots, use_slots = :use_slots,
                    setup_slots = :setup_slots, etc_slots = :etc_slots, monster_book_cover = :monster_book_cover,
                    dojo_tutorial_complete = :dojo_tutorial_complete, dojo_points = :dojo_points,
                    dojo_last_stage = :dojo_last_stage, dojo_vanquisher_stage = :dojo_vanquisher_stage,
                    dojo_vanquisher_kills = :dojo_vanquisher_kills, ariant_points = :ariant_points,
                    data_string = :data_string, party_search = :party_search, jail_expire = :jail_expire,
                    last_exp_gain = :last_exp_gain, partner_id = :partner_id, marriage_item_id = :marriage_item_id,
                    updated_at = now()
                WHERE id = :id""";

        int updatedRows = handle.createUpdate(sql)
                .bind("level", stats.level())
                .bind("exp", stats.exp())
                .bind("str", stats.str())
                .bind("dex", stats.dex())
                .bind("int", stats.int_())
                .bind("luk", stats.luk())
                .bind("hp", stats.hp())
                .bind("mp", stats.mp())
                .bind("max_hp", stats.maxHp())
                .bind("max_mp", stats.maxMp())
                .bind("ap", stats.ap())
                .bind("sp", stats.sp())
                .bind("job", stats.job())
                .bind("fame", stats.fame())
                .bind("gender", stats.gender())
                .bind("skin", stats.skin())
                .bind("hair", stats.hair())
                .bind("face", stats.face())
                .bind("meso", stats.meso())
                .bind("map_id", stats.mapId())
                .bind("spawn_portal", stats.spawnPortal())
                .bind("gacha_exp", stats.gachaExp())
                .bind("used_hp_mp_ap", stats.hpMpApUsed())
                .bind("gm_level", stats.gmLevel())
                .bind("party_id", stats.party())
                .bind("buddy_capacity", stats.buddyCapacity())
                .bind("messenger_id", stats.messenger())
                .bind("messenger_position", stats.messengerPosition())
                .bind("mount_level", stats.mountLevel())
                .bind("mount_exp", stats.mountExp())
                .bind("mount_tiredness", stats.mountTiredness())
                .bind("omok_wins", stats.omokWins())
                .bind("omok_losses", stats.omokLosses())
                .bind("omok_ties", stats.omokTies())
                .bind("matchcard_wins", stats.matchCardWins())
                .bind("matchcard_losses", stats.matchCardLosses())
                .bind("matchcard_ties", stats.matchCardTies())
                .bind("equip_slots", stats.equipSlots())
                .bind("use_slots", stats.useSlots())
                .bind("setup_slots", stats.setupSlots())
                .bind("etc_slots", stats.etcSlots())
                .bind("monster_book_cover", stats.monsterBookCover())
                .bind("dojo_tutorial_complete", stats.dojoTutorialComplete())
                .bind("dojo_points", stats.dojoPoints())
                .bind("dojo_last_stage", stats.dojoStage())
                .bind("dojo_vanquisher_stage", stats.dojoVanquisherStage())
                .bind("dojo_vanquisher_kills", stats.dojoVanquisherKills())
                .bind("ariant_points", stats.ariantPoints())
                .bind("data_string", stats.dataString())
                .bind("party_search", stats.canRecvPartySearchInvite())
                .bind("jail_expire", stats.jailExpiration() != null ? new Timestamp(stats.jailExpiration()) : null)
                .bind("last_exp_gain", stats.lastExpGainTime() != null ? new Timestamp(stats.lastExpGainTime()) : null)
                .bind("partner_id", stats.partnerId())
                .bind("marriage_item_id", stats.marriageItemId())
                .bind("id", stats.id())
                .execute();

        return updatedRows > 0;
    }
}
