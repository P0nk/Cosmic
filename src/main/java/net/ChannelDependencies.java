package net;

import client.command.CommandsExecutor;
import client.creator.CharacterCreator;
import client.processor.action.MakerProcessor;
import client.processor.npc.FredrickProcessor;
import database.character.CharacterLoader;
import database.character.CharacterSaver;
import database.drop.DropProvider;
import lombok.Builder;
import server.ban.HwidBanManager;
import server.ban.IpBanManager;
import server.ban.MacBanManager;
import server.shop.ShopFactory;
import service.AccountService;
import service.BanService;
import service.NoteService;
import service.TransitionService;

import java.util.Objects;

/**
 * @author Ponk
 */
@Builder
public record ChannelDependencies(
        AccountService accountService,
        CharacterCreator characterCreator, CharacterLoader characterLoader, CharacterSaver characterSaver,
        NoteService noteService, FredrickProcessor fredrickProcessor, MakerProcessor makerProcessor,
        DropProvider dropProvider, CommandsExecutor commandsExecutor, ShopFactory shopFactory,
        TransitionService transitionService, IpBanManager ipBanManager, MacBanManager macBanManager,
        HwidBanManager hwidBanManager,BanService banService
) {

    public ChannelDependencies {
        Objects.requireNonNull(accountService);
        Objects.requireNonNull(characterCreator);
        Objects.requireNonNull(characterLoader);
        Objects.requireNonNull(characterSaver);
        Objects.requireNonNull(noteService);
        Objects.requireNonNull(fredrickProcessor);
        Objects.requireNonNull(makerProcessor);
        Objects.requireNonNull(dropProvider);
        Objects.requireNonNull(commandsExecutor);
        Objects.requireNonNull(shopFactory);
        Objects.requireNonNull(transitionService);
        Objects.requireNonNull(ipBanManager);
        Objects.requireNonNull(macBanManager);
        Objects.requireNonNull(hwidBanManager);
        Objects.requireNonNull(banService);
    }
}
