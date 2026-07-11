package client.command.commands.gm0;

import client.Character;
import client.Client;
import client.command.Command;
import java.text.NumberFormat;

public class WorldChatCommand extends Command {
    {
        setDescription("Toggles World Chat on or off.");
    }

    @Override
    public void execute(Client c, String[] params) {
        Character player = c.getPlayer();

        if(params.length == 0) {
            if (player.getWorldChatOnOff()) { //World Chat enabled
                player.toggleWorldChat();
                player.showHint("#rYou have stopped talking globally!#k");
            } else { //Local chat
                if(!player.getGlobalMessageOnOff()) {
                    player.showHint("Do @worldchat toggle to enable worldchat view again.");
                } else {
                    player.toggleWorldChat();
                    player.showHint("#bYou are now talking globally!#k");
                }
            }
        } else if(params.length == 1) {
            if(params[0].equalsIgnoreCase("toggle")) {
                if(player.getGlobalMessageOnOff()) {
                    player.toggleGlobalMessage();
                    player.showHint("#rWorld chat messages are now hidden! and you have stopped talking globally!#k");
                    if(player.getWorldChatOnOff()) {
                        player.toggleWorldChat();
                        player.showHint("#rWorld chat messages are now hidden! and you have stopped talking globally!#k");
                    }
                } else {
                    player.toggleGlobalMessage();
                    player.showHint("#bYou can now see World chat and send messages there again!#k");
                }
            }
        }


    }
}