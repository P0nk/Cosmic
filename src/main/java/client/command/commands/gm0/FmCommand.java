package client.command.commands.gm0;
import client.command.commands.CommandMapConstants;
import client.Character;
import client.Client;
import client.command.Command;
import server.maps.MapleMap;

public class FmCommand extends Command {

    public FmCommand() {
        setDescription("Warp to the Free Market.");
    }

    @Override
    public void execute(Client c, String[] params) {

        Character player = c.getPlayer();

        if (player.getTrade() != null) {
            player.dropMessage(5, "You cannot use @fm while trading.");
            return;
        }

        if (player.getJailExpirationTimeLeft() > 0) {
            player.dropMessage(5, "You cannot use @fm while jailed.");
            return;
        }

        if (player.isChangingMaps()) {
            return;
        }

        MapleMap fm = c.getChannelServer()
                .getMapFactory()
                .getMap(CommandMapConstants.FREE_MARKET);

        player.changeMap(fm);

    }
}
