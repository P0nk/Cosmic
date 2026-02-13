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
package net;

import net.netty.LoginServer;
import net.opcodes.RecvOpcode;
import net.server.channel.handlers.AcceptFamilyHandler;
import net.server.channel.handlers.AdminChatHandler;
import net.server.channel.handlers.AdminCommandHandler;
import net.server.channel.handlers.AdminLogHandler;
import net.server.channel.handlers.AllianceOperationHandler;
import net.server.channel.handlers.AranComboHandler;
import net.server.channel.handlers.AutoAggroHandler;
import net.server.channel.handlers.AutoAssignHandler;
import net.server.channel.handlers.BBSOperationHandler;
import net.server.channel.handlers.BeholderHandler;
import net.server.channel.handlers.BuddylistModifyHandler;
import net.server.channel.handlers.CancelBuffHandler;
import net.server.channel.handlers.CancelChairHandler;
import net.server.channel.handlers.CancelDebuffHandler;
import net.server.channel.handlers.CancelItemEffectHandler;
import net.server.channel.handlers.CashOperationHandler;
import net.server.channel.handlers.CashShopSurpriseHandler;
import net.server.channel.handlers.ChangeChannelHandler;
import net.server.channel.handlers.ChangeMapHandler;
import net.server.channel.handlers.ChangeMapSpecialHandler;
import net.server.channel.handlers.CharInfoRequestHandler;
import net.server.channel.handlers.ClickGuideHandler;
import net.server.channel.handlers.CloseChalkboardHandler;
import net.server.channel.handlers.CloseRangeDamageHandler;
import net.server.channel.handlers.CoconutHandler;
import net.server.channel.handlers.CouponCodeHandler;
import net.server.channel.handlers.DamageSummonHandler;
import net.server.channel.handlers.DenyAllianceRequestHandler;
import net.server.channel.handlers.DenyGuildRequestHandler;
import net.server.channel.handlers.DenyPartyRequestHandler;
import net.server.channel.handlers.DistributeAPHandler;
import net.server.channel.handlers.DistributeSPHandler;
import net.server.channel.handlers.DoorHandler;
import net.server.channel.handlers.DueyHandler;
import net.server.channel.handlers.EnterCashShopHandler;
import net.server.channel.handlers.EnterMTSHandler;
import net.server.channel.handlers.FaceExpressionHandler;
import net.server.channel.handlers.FamilyAddHandler;
import net.server.channel.handlers.FamilyPreceptsHandler;
import net.server.channel.handlers.FamilySeparateHandler;
import net.server.channel.handlers.FamilySummonResponseHandler;
import net.server.channel.handlers.FamilyUseHandler;
import net.server.channel.handlers.FieldDamageMobHandler;
import net.server.channel.handlers.FredrickHandler;
import net.server.channel.handlers.GeneralChatHandler;
import net.server.channel.handlers.GiveFameHandler;
import net.server.channel.handlers.GrenadeEffectHandler;
import net.server.channel.handlers.GuildOperationHandler;
import net.server.channel.handlers.HealOvertimeHandler;
import net.server.channel.handlers.HiredMerchantRequest;
import net.server.channel.handlers.InnerPortalHandler;
import net.server.channel.handlers.InventoryMergeHandler;
import net.server.channel.handlers.InventorySortHandler;
import net.server.channel.handlers.ItemMoveHandler;
import net.server.channel.handlers.ItemPickupHandler;
import net.server.channel.handlers.ItemRewardHandler;
import net.server.channel.handlers.KeymapChangeHandler;
import net.server.channel.handlers.LeftKnockbackHandler;
import net.server.channel.handlers.MTSHandler;
import net.server.channel.handlers.MagicDamageHandler;
import net.server.channel.handlers.MakerSkillHandler;
import net.server.channel.handlers.MesoDropHandler;
import net.server.channel.handlers.MessengerHandler;
import net.server.channel.handlers.MobBanishPlayerHandler;
import net.server.channel.handlers.MobDamageMobFriendlyHandler;
import net.server.channel.handlers.MobDamageMobHandler;
import net.server.channel.handlers.MonsterBombHandler;
import net.server.channel.handlers.MonsterBookCoverHandler;
import net.server.channel.handlers.MonsterCarnivalHandler;
import net.server.channel.handlers.MoveDragonHandler;
import net.server.channel.handlers.MoveLifeHandler;
import net.server.channel.handlers.MovePetHandler;
import net.server.channel.handlers.MovePlayerHandler;
import net.server.channel.handlers.MoveSummonHandler;
import net.server.channel.handlers.MultiChatHandler;
import net.server.channel.handlers.NPCAnimationHandler;
import net.server.channel.handlers.NPCMoreTalkHandler;
import net.server.channel.handlers.NPCShopHandler;
import net.server.channel.handlers.NPCTalkHandler;
import net.server.channel.handlers.NewYearCardHandler;
import net.server.channel.handlers.NoteActionHandler;
import net.server.channel.handlers.OpenFamilyHandler;
import net.server.channel.handlers.OpenFamilyPedigreeHandler;
import net.server.channel.handlers.OwlWarpHandler;
import net.server.channel.handlers.PartyOperationHandler;
import net.server.channel.handlers.PartySearchRegisterHandler;
import net.server.channel.handlers.PartySearchStartHandler;
import net.server.channel.handlers.PartySearchUpdateHandler;
import net.server.channel.handlers.PetAutoPotHandler;
import net.server.channel.handlers.PetChatHandler;
import net.server.channel.handlers.PetCommandHandler;
import net.server.channel.handlers.PetExcludeItemsHandler;
import net.server.channel.handlers.PetFoodHandler;
import net.server.channel.handlers.PetLootHandler;
import net.server.channel.handlers.PlayerInteractionHandler;
import net.server.channel.handlers.PlayerLoggedinHandler;
import net.server.channel.handlers.PlayerMapTransitionHandler;
import net.server.channel.handlers.QuestActionHandler;
import net.server.channel.handlers.QuickslotKeyMappedModifiedHandler;
import net.server.channel.handlers.RPSActionHandler;
import net.server.channel.handlers.RaiseIncExpHandler;
import net.server.channel.handlers.RaiseUIStateHandler;
import net.server.channel.handlers.RangedAttackHandler;
import net.server.channel.handlers.ReactorHitHandler;
import net.server.channel.handlers.RemoteGachaponHandler;
import net.server.channel.handlers.RemoteStoreHandler;
import net.server.channel.handlers.ReportHandler;
import net.server.channel.handlers.RingActionHandler;
import net.server.channel.handlers.ScriptedItemHandler;
import net.server.channel.handlers.ScrollHandler;
import net.server.channel.handlers.SkillBookHandler;
import net.server.channel.handlers.SkillEffectHandler;
import net.server.channel.handlers.SkillMacroHandler;
import net.server.channel.handlers.SnowballHandler;
import net.server.channel.handlers.SpawnPetHandler;
import net.server.channel.handlers.SpecialMoveHandler;
import net.server.channel.handlers.SpouseChatHandler;
import net.server.channel.handlers.StorageHandler;
import net.server.channel.handlers.SummonDamageHandler;
import net.server.channel.handlers.TakeDamageHandler;
import net.server.channel.handlers.TouchMonsterDamageHandler;
import net.server.channel.handlers.TouchReactorHandler;
import net.server.channel.handlers.TouchingCashShopHandler;
import net.server.channel.handlers.TransferNameHandler;
import net.server.channel.handlers.TransferNameResultHandler;
import net.server.channel.handlers.TransferWorldHandler;
import net.server.channel.handlers.TrockAddMapHandler;
import net.server.channel.handlers.UseCashItemHandler;
import net.server.channel.handlers.UseCatchItemHandler;
import net.server.channel.handlers.UseChairHandler;
import net.server.channel.handlers.UseDeathItemHandler;
import net.server.channel.handlers.UseGachaExpHandler;
import net.server.channel.handlers.UseHammerHandler;
import net.server.channel.handlers.UseItemEffectHandler;
import net.server.channel.handlers.UseItemHandler;
import net.server.channel.handlers.UseMapleLifeHandler;
import net.server.channel.handlers.UseMountFoodHandler;
import net.server.channel.handlers.UseOwlOfMinervaHandler;
import net.server.channel.handlers.UseSolomonHandler;
import net.server.channel.handlers.UseSummonBagHandler;
import net.server.channel.handlers.UseWaterOfLifeHandler;
import net.server.channel.handlers.WeddingHandler;
import net.server.channel.handlers.WeddingTalkHandler;
import net.server.channel.handlers.WeddingTalkMoreHandler;
import net.server.channel.handlers.WhisperHandler;
import net.server.handlers.CustomPacketHandler;
import net.server.handlers.KeepAliveHandler;
import net.server.handlers.LoginRequiringNoOpHandler;
import net.server.handlers.login.AcceptToSHandler;
import net.server.handlers.login.AfterLoginHandler;
import net.server.handlers.login.CharSelectedHandler;
import net.server.handlers.login.CharSelectedWithPicHandler;
import net.server.handlers.login.CharlistRequestHandler;
import net.server.handlers.login.CheckCharNameHandler;
import net.server.handlers.login.CreateCharHandler;
import net.server.handlers.login.DeleteCharHandler;
import net.server.handlers.login.GuestLoginHandler;
import net.server.handlers.login.LoginPasswordHandler;
import net.server.handlers.login.RegisterPicHandler;
import net.server.handlers.login.RegisterPinHandler;
import net.server.handlers.login.RelogRequestHandler;
import net.server.handlers.login.ServerStatusRequestHandler;
import net.server.handlers.login.ServerlistRequestHandler;
import net.server.handlers.login.SetGenderHandler;
import net.server.handlers.login.ViewAllCharHandler;
import net.server.handlers.login.ViewAllCharRegisterPicHandler;
import net.server.handlers.login.ViewAllCharSelectedHandler;
import net.server.handlers.login.ViewAllCharSelectedWithPicHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

public final class PacketProcessor {
    private static final Logger log = LoggerFactory.getLogger(PacketProcessor.class);
    private static final Map<String, PacketProcessor> instances = new LinkedHashMap<>();

    private static ChannelDependencies channelDeps;

    private PacketHandler[] handlers;

    private PacketProcessor() {
        int maxRecvOp = 0;
        for (RecvOpcode op : RecvOpcode.values()) {
            if (op.getValue() > maxRecvOp) {
                maxRecvOp = op.getValue();
            }
        }
        handlers = new PacketHandler[maxRecvOp + 1];
    }

    public static void registerGameHandlerDependencies(ChannelDependencies channelDependencies) {
        PacketProcessor.channelDeps = channelDependencies;
    }

    public static PacketProcessor getLoginServerProcessor() {
        return getProcessor(LoginServer.WORLD_ID, LoginServer.CHANNEL_ID);
    }

    public static PacketProcessor getChannelServerProcessor(int world, int channel) {
        if (channelDeps == null) {
            throw new IllegalStateException("Unable to get channel server processor - dependencies are not registered");
        }

        return getProcessor(world, channel);
    }

    public PacketHandler getHandler(short packetId) {
        if (packetId > handlers.length) {
            return null;
        }
        PacketHandler handler = handlers[packetId];
        return handler;
    }

    public void registerHandler(RecvOpcode code, PacketHandler handler) {
        try {
            handlers[code.getValue()] = handler;
        } catch (ArrayIndexOutOfBoundsException e) {
            log.error("Error registering handler {}", code.name(), e);
        }
    }

    public synchronized static PacketProcessor getProcessor(int world, int channel) {
        final String processorId = world + " " + channel;
        PacketProcessor processor = instances.get(processorId);
        if (processor == null) {
            processor = new PacketProcessor();
            processor.reset(channel);
            instances.put(processorId, processor);
        }
        return processor;
    }

    public void reset(int channel) {
        handlers = new PacketHandler[handlers.length];

        registerCommonHandlers();

        if (channel < 0) {
            registerLoginHandlers();
        } else {
            registerChannelHandlers();
        }
    }

    private void registerCommonHandlers() {
        registerHandler(RecvOpcode.PONG, new KeepAliveHandler());
        registerHandler(RecvOpcode.CUSTOM_PACKET, new CustomPacketHandler());
    }

    private void registerLoginHandlers() {
        registerHandler(RecvOpcode.ACCEPT_TOS, new AcceptToSHandler(channelDeps.accountService()));
        registerHandler(RecvOpcode.AFTER_LOGIN, new AfterLoginHandler(channelDeps.accountService()));
        registerHandler(RecvOpcode.SERVERLIST_REREQUEST, new ServerlistRequestHandler());
        registerHandler(RecvOpcode.CHARLIST_REQUEST, new CharlistRequestHandler());
        registerHandler(RecvOpcode.CHAR_SELECT, new CharSelectedHandler(channelDeps.accountService(),
                channelDeps.banService(), channelDeps.transitionService()));
        registerHandler(RecvOpcode.LOGIN_PASSWORD, new LoginPasswordHandler(channelDeps.accountService(),
                channelDeps.transitionService(), channelDeps.banService()));
        registerHandler(RecvOpcode.RELOG, new RelogRequestHandler());
        registerHandler(RecvOpcode.SERVERLIST_REQUEST, new ServerlistRequestHandler());
        registerHandler(RecvOpcode.SERVERSTATUS_REQUEST, new ServerStatusRequestHandler());
        registerHandler(RecvOpcode.CHECK_CHAR_NAME, new CheckCharNameHandler());
        registerHandler(RecvOpcode.CREATE_CHAR, new CreateCharHandler(channelDeps.characterCreator()));
        registerHandler(RecvOpcode.DELETE_CHAR, new DeleteCharHandler());
        registerHandler(RecvOpcode.VIEW_ALL_CHAR, new ViewAllCharHandler());
        registerHandler(RecvOpcode.PICK_ALL_CHAR, new ViewAllCharSelectedHandler(channelDeps.accountService(),
                channelDeps.banService(), channelDeps.transitionService()));
        registerHandler(RecvOpcode.REGISTER_PIN, new RegisterPinHandler(channelDeps.accountService()));
        registerHandler(RecvOpcode.GUEST_LOGIN, new GuestLoginHandler());
        registerHandler(RecvOpcode.REGISTER_PIC, new RegisterPicHandler(channelDeps.banService(),
                channelDeps.accountService(), channelDeps.transitionService()));
        registerHandler(RecvOpcode.CHAR_SELECT_WITH_PIC, new CharSelectedWithPicHandler(channelDeps.accountService(),
                channelDeps.banService(), channelDeps.transitionService()));
        registerHandler(RecvOpcode.SET_GENDER, new SetGenderHandler(channelDeps.accountService()));
        registerHandler(RecvOpcode.VIEW_ALL_WITH_PIC, new ViewAllCharSelectedWithPicHandler(channelDeps.accountService(),
                channelDeps.banService(), channelDeps.transitionService()));
        registerHandler(RecvOpcode.VIEW_ALL_PIC_REGISTER, new ViewAllCharRegisterPicHandler(channelDeps.banService(),
                channelDeps.accountService(), channelDeps.transitionService()));
    }

    private void registerChannelHandlers() {
        registerHandler(RecvOpcode.NAME_TRANSFER, new TransferNameHandler());
        registerHandler(RecvOpcode.CHECK_CHAR_NAME, new TransferNameResultHandler());
        registerHandler(RecvOpcode.WORLD_TRANSFER, new TransferWorldHandler());
        registerHandler(RecvOpcode.CHANGE_CHANNEL, new ChangeChannelHandler(channelDeps.transitionService()));
        registerHandler(RecvOpcode.STRANGE_DATA, LoginRequiringNoOpHandler.getInstance());
        registerHandler(RecvOpcode.GENERAL_CHAT, new GeneralChatHandler(channelDeps.commandsExecutor()));
        registerHandler(RecvOpcode.WHISPER, new WhisperHandler());
        registerHandler(RecvOpcode.NPC_TALK, new NPCTalkHandler(channelDeps.shopFactory()));
        registerHandler(RecvOpcode.NPC_TALK_MORE, new NPCMoreTalkHandler());
        registerHandler(RecvOpcode.QUEST_ACTION, new QuestActionHandler());
        registerHandler(RecvOpcode.GRENADE_EFFECT, new GrenadeEffectHandler());
        registerHandler(RecvOpcode.NPC_SHOP, new NPCShopHandler());
        registerHandler(RecvOpcode.ITEM_SORT, new InventoryMergeHandler());
        registerHandler(RecvOpcode.ITEM_MOVE, new ItemMoveHandler());
        registerHandler(RecvOpcode.MESO_DROP, new MesoDropHandler());
        registerHandler(RecvOpcode.PLAYER_LOGGEDIN, new PlayerLoggedinHandler(channelDeps.characterLoader(),
                channelDeps.accountService(), channelDeps.noteService()));
        registerHandler(RecvOpcode.CHANGE_MAP, new ChangeMapHandler(channelDeps.transitionService()));
        registerHandler(RecvOpcode.MOVE_LIFE, new MoveLifeHandler());
        registerHandler(RecvOpcode.CLOSE_RANGE_ATTACK, new CloseRangeDamageHandler(channelDeps.dropProvider(), channelDeps.banService()));
        registerHandler(RecvOpcode.RANGED_ATTACK, new RangedAttackHandler(channelDeps.dropProvider(), channelDeps.banService()));
        registerHandler(RecvOpcode.MAGIC_ATTACK, new MagicDamageHandler(channelDeps.dropProvider(), channelDeps.banService()));
        registerHandler(RecvOpcode.TAKE_DAMAGE, new TakeDamageHandler());
        registerHandler(RecvOpcode.MOVE_PLAYER, new MovePlayerHandler());
        registerHandler(RecvOpcode.USE_CASH_ITEM, new UseCashItemHandler(channelDeps.noteService(),
                channelDeps.shopFactory(), channelDeps.accountService()));
        registerHandler(RecvOpcode.USE_ITEM, new UseItemHandler());
        registerHandler(RecvOpcode.USE_RETURN_SCROLL, new UseItemHandler());
        registerHandler(RecvOpcode.USE_UPGRADE_SCROLL, new ScrollHandler());
        registerHandler(RecvOpcode.USE_SUMMON_BAG, new UseSummonBagHandler());
        registerHandler(RecvOpcode.FACE_EXPRESSION, new FaceExpressionHandler());
        registerHandler(RecvOpcode.HEAL_OVER_TIME, new HealOvertimeHandler(channelDeps.banService()));
        registerHandler(RecvOpcode.ITEM_PICKUP, new ItemPickupHandler());
        registerHandler(RecvOpcode.CHAR_INFO_REQUEST, new CharInfoRequestHandler());
        registerHandler(RecvOpcode.SPECIAL_MOVE, new SpecialMoveHandler());
        registerHandler(RecvOpcode.USE_INNER_PORTAL, new InnerPortalHandler());
        registerHandler(RecvOpcode.CANCEL_BUFF, new CancelBuffHandler());
        registerHandler(RecvOpcode.CANCEL_ITEM_EFFECT, new CancelItemEffectHandler());
        registerHandler(RecvOpcode.PLAYER_INTERACTION, new PlayerInteractionHandler(channelDeps.characterSaver()));
        registerHandler(RecvOpcode.RPS_ACTION, new RPSActionHandler());
        registerHandler(RecvOpcode.DISTRIBUTE_AP, new DistributeAPHandler());
        registerHandler(RecvOpcode.DISTRIBUTE_SP, new DistributeSPHandler());
        registerHandler(RecvOpcode.CHANGE_KEYMAP, new KeymapChangeHandler());
        registerHandler(RecvOpcode.CHANGE_MAP_SPECIAL, new ChangeMapSpecialHandler());
        registerHandler(RecvOpcode.STORAGE, new StorageHandler());
        registerHandler(RecvOpcode.GIVE_FAME, new GiveFameHandler());
        registerHandler(RecvOpcode.PARTY_OPERATION, new PartyOperationHandler());
        registerHandler(RecvOpcode.DENY_PARTY_REQUEST, new DenyPartyRequestHandler());
        registerHandler(RecvOpcode.MULTI_CHAT, new MultiChatHandler());
        registerHandler(RecvOpcode.USE_DOOR, new DoorHandler());
        registerHandler(RecvOpcode.ENTER_MTS, new EnterMTSHandler(channelDeps.characterSaver()));
        registerHandler(RecvOpcode.ENTER_CASHSHOP, new EnterCashShopHandler(channelDeps.characterSaver()));
        registerHandler(RecvOpcode.DAMAGE_SUMMON, new DamageSummonHandler());
        registerHandler(RecvOpcode.MOVE_SUMMON, new MoveSummonHandler());
        registerHandler(RecvOpcode.SUMMON_ATTACK, new SummonDamageHandler(channelDeps.dropProvider(), channelDeps.banService()));
        registerHandler(RecvOpcode.BUDDYLIST_MODIFY, new BuddylistModifyHandler());
        registerHandler(RecvOpcode.USE_ITEMEFFECT, new UseItemEffectHandler());
        registerHandler(RecvOpcode.USE_CHAIR, new UseChairHandler());
        registerHandler(RecvOpcode.CANCEL_CHAIR, new CancelChairHandler());
        registerHandler(RecvOpcode.DAMAGE_REACTOR, new ReactorHitHandler());
        registerHandler(RecvOpcode.GUILD_OPERATION, new GuildOperationHandler());
        registerHandler(RecvOpcode.DENY_GUILD_REQUEST, new DenyGuildRequestHandler());
        registerHandler(RecvOpcode.BBS_OPERATION, new BBSOperationHandler());
        registerHandler(RecvOpcode.SKILL_EFFECT, new SkillEffectHandler());
        registerHandler(RecvOpcode.MESSENGER, new MessengerHandler());
        registerHandler(RecvOpcode.NPC_ACTION, new NPCAnimationHandler());
        registerHandler(RecvOpcode.CHECK_CASH, new TouchingCashShopHandler());
        registerHandler(RecvOpcode.CASHSHOP_OPERATION, new CashOperationHandler(channelDeps.noteService(),
                channelDeps.accountService()));
        registerHandler(RecvOpcode.COUPON_CODE, new CouponCodeHandler());
        registerHandler(RecvOpcode.SPAWN_PET, new SpawnPetHandler());
        registerHandler(RecvOpcode.MOVE_PET, new MovePetHandler());
        registerHandler(RecvOpcode.PET_CHAT, new PetChatHandler());
        registerHandler(RecvOpcode.PET_COMMAND, new PetCommandHandler());
        registerHandler(RecvOpcode.PET_FOOD, new PetFoodHandler());
        registerHandler(RecvOpcode.PET_LOOT, new PetLootHandler());
        registerHandler(RecvOpcode.AUTO_AGGRO, new AutoAggroHandler());
        registerHandler(RecvOpcode.MONSTER_BOMB, new MonsterBombHandler());
        registerHandler(RecvOpcode.CANCEL_DEBUFF, new CancelDebuffHandler());
        registerHandler(RecvOpcode.USE_SKILL_BOOK, new SkillBookHandler());
        registerHandler(RecvOpcode.SKILL_MACRO, new SkillMacroHandler());
        registerHandler(RecvOpcode.NOTE_ACTION, new NoteActionHandler(channelDeps.noteService()));
        registerHandler(RecvOpcode.CLOSE_CHALKBOARD, new CloseChalkboardHandler());
        registerHandler(RecvOpcode.USE_MOUNT_FOOD, new UseMountFoodHandler());
        registerHandler(RecvOpcode.MTS_OPERATION, new MTSHandler());
        registerHandler(RecvOpcode.RING_ACTION, new RingActionHandler(channelDeps.noteService()));
        registerHandler(RecvOpcode.SPOUSE_CHAT, new SpouseChatHandler());
        registerHandler(RecvOpcode.PET_AUTO_POT, new PetAutoPotHandler());
        registerHandler(RecvOpcode.PET_EXCLUDE_ITEMS, new PetExcludeItemsHandler());
        registerHandler(RecvOpcode.OWL_ACTION, new UseOwlOfMinervaHandler());
        registerHandler(RecvOpcode.OWL_WARP, new OwlWarpHandler());
        registerHandler(RecvOpcode.TOUCH_MONSTER_ATTACK, new TouchMonsterDamageHandler(channelDeps.dropProvider(), channelDeps.banService()));
        registerHandler(RecvOpcode.TROCK_ADD_MAP, new TrockAddMapHandler());
        registerHandler(RecvOpcode.HIRED_MERCHANT_REQUEST, new HiredMerchantRequest());
        registerHandler(RecvOpcode.MOB_BANISH_PLAYER, new MobBanishPlayerHandler());
        registerHandler(RecvOpcode.MOB_DAMAGE_MOB, new MobDamageMobHandler());
        registerHandler(RecvOpcode.REPORT, new ReportHandler());
        registerHandler(RecvOpcode.MONSTER_BOOK_COVER, new MonsterBookCoverHandler());
        registerHandler(RecvOpcode.AUTO_DISTRIBUTE_AP, new AutoAssignHandler());
        registerHandler(RecvOpcode.MAKER_SKILL, new MakerSkillHandler(channelDeps.makerProcessor()));
        registerHandler(RecvOpcode.OPEN_FAMILY_PEDIGREE, new OpenFamilyPedigreeHandler());
        registerHandler(RecvOpcode.OPEN_FAMILY, new OpenFamilyHandler());
        registerHandler(RecvOpcode.ADD_FAMILY, new FamilyAddHandler());
        registerHandler(RecvOpcode.SEPARATE_FAMILY_BY_SENIOR, new FamilySeparateHandler());
        registerHandler(RecvOpcode.SEPARATE_FAMILY_BY_JUNIOR, new FamilySeparateHandler());
        registerHandler(RecvOpcode.USE_FAMILY, new FamilyUseHandler());
        registerHandler(RecvOpcode.CHANGE_FAMILY_MESSAGE, new FamilyPreceptsHandler());
        registerHandler(RecvOpcode.FAMILY_SUMMON_RESPONSE, new FamilySummonResponseHandler());
        registerHandler(RecvOpcode.USE_HAMMER, new UseHammerHandler());
        registerHandler(RecvOpcode.SCRIPTED_ITEM, new ScriptedItemHandler());
        registerHandler(RecvOpcode.TOUCHING_REACTOR, new TouchReactorHandler());
        registerHandler(RecvOpcode.BEHOLDER, new BeholderHandler());
        registerHandler(RecvOpcode.ADMIN_COMMAND, new AdminCommandHandler(channelDeps.banService()));
        registerHandler(RecvOpcode.ADMIN_LOG, new AdminLogHandler());
        registerHandler(RecvOpcode.ALLIANCE_OPERATION, new AllianceOperationHandler());
        registerHandler(RecvOpcode.DENY_ALLIANCE_REQUEST, new DenyAllianceRequestHandler());
        registerHandler(RecvOpcode.USE_SOLOMON_ITEM, new UseSolomonHandler());
        registerHandler(RecvOpcode.USE_GACHA_EXP, new UseGachaExpHandler(channelDeps.banService()));
        registerHandler(RecvOpcode.NEW_YEAR_CARD_REQUEST, new NewYearCardHandler());
        registerHandler(RecvOpcode.CASHSHOP_SURPRISE, new CashShopSurpriseHandler());
        registerHandler(RecvOpcode.USE_ITEM_REWARD, new ItemRewardHandler());
        registerHandler(RecvOpcode.USE_REMOTE, new RemoteGachaponHandler());
        registerHandler(RecvOpcode.ACCEPT_FAMILY, new AcceptFamilyHandler());
        registerHandler(RecvOpcode.DUEY_ACTION, new DueyHandler());
        registerHandler(RecvOpcode.USE_DEATHITEM, new UseDeathItemHandler());
        registerHandler(RecvOpcode.PLAYER_MAP_TRANSFER, new PlayerMapTransitionHandler());
        registerHandler(RecvOpcode.USE_MAPLELIFE, new UseMapleLifeHandler());
        registerHandler(RecvOpcode.USE_CATCH_ITEM, new UseCatchItemHandler());
        registerHandler(RecvOpcode.FIELD_DAMAGE_MOB, new FieldDamageMobHandler());
        registerHandler(RecvOpcode.MOB_DAMAGE_MOB_FRIENDLY, new MobDamageMobFriendlyHandler());
        registerHandler(RecvOpcode.PARTY_SEARCH_REGISTER, new PartySearchRegisterHandler());
        registerHandler(RecvOpcode.PARTY_SEARCH_START, new PartySearchStartHandler());
        registerHandler(RecvOpcode.PARTY_SEARCH_UPDATE, new PartySearchUpdateHandler());
        registerHandler(RecvOpcode.ITEM_SORT2, new InventorySortHandler());
        registerHandler(RecvOpcode.LEFT_KNOCKBACK, new LeftKnockbackHandler());
        registerHandler(RecvOpcode.SNOWBALL, new SnowballHandler());
        registerHandler(RecvOpcode.COCONUT, new CoconutHandler());
        registerHandler(RecvOpcode.ARAN_COMBO_COUNTER, new AranComboHandler());
        registerHandler(RecvOpcode.CLICK_GUIDE, new ClickGuideHandler());
        registerHandler(RecvOpcode.FREDRICK_ACTION, new FredrickHandler(channelDeps.fredrickProcessor()));
        registerHandler(RecvOpcode.MONSTER_CARNIVAL, new MonsterCarnivalHandler());
        registerHandler(RecvOpcode.REMOTE_STORE, new RemoteStoreHandler());
        registerHandler(RecvOpcode.WEDDING_ACTION, new WeddingHandler(channelDeps.characterSaver()));
        registerHandler(RecvOpcode.WEDDING_TALK, new WeddingTalkHandler());
        registerHandler(RecvOpcode.WEDDING_TALK_MORE, new WeddingTalkMoreHandler());
        registerHandler(RecvOpcode.WATER_OF_LIFE, new UseWaterOfLifeHandler());
        registerHandler(RecvOpcode.ADMIN_CHAT, new AdminChatHandler());
        registerHandler(RecvOpcode.MOVE_DRAGON, new MoveDragonHandler());
        registerHandler(RecvOpcode.OPEN_ITEMUI, new RaiseUIStateHandler());
        registerHandler(RecvOpcode.USE_ITEMUI, new RaiseIncExpHandler());
        registerHandler(RecvOpcode.CHANGE_QUICKSLOT, new QuickslotKeyMappedModifiedHandler());
    }
}
