package net;

import client.command.CommandsExecutor;
import client.processor.action.MakerProcessor;
import client.processor.npc.FredrickProcessor;
import database.character.CharacterLoader;
import database.character.CharacterSaver;
import database.drop.DropProvider;
import server.shop.ShopFactory;
import service.BanService;
import service.NoteService;
import service.TransitionService;

import java.util.Objects;

/**
 * @author Ponk
 */
public record ChannelDependencies(
        CharacterLoader characterLoader, CharacterSaver characterSaver, NoteService noteService,
        FredrickProcessor fredrickProcessor, MakerProcessor makerProcessor, DropProvider dropProvider,
        CommandsExecutor commandsExecutor, ShopFactory shopFactory, TransitionService transitionService,
        BanService banService
) {

    public ChannelDependencies {
        Objects.requireNonNull(characterLoader);
        Objects.requireNonNull(characterSaver);
        Objects.requireNonNull(noteService);
        Objects.requireNonNull(fredrickProcessor);
        Objects.requireNonNull(makerProcessor);
        Objects.requireNonNull(dropProvider);
        Objects.requireNonNull(commandsExecutor);
        Objects.requireNonNull(shopFactory);
        Objects.requireNonNull(transitionService);
        Objects.requireNonNull(banService);
    }
}
