package client.command;

import database.character.CharacterSaver;
import database.drop.DropProvider;
import server.shop.ShopFactory;
import service.BanService;
import service.TransitionService;

/**
 * @author Ponk
 */
public record CommandContext(CommandsExecutor commandsExecutor, DropProvider dropProvider, ShopFactory shopFactory,
                             CharacterSaver characterSaver, TransitionService transitionService, BanService banService
) {

    public CommandContext with(CommandsExecutor ce) {
        return new CommandContext(ce, dropProvider, shopFactory, characterSaver, transitionService, banService);
    }
}
