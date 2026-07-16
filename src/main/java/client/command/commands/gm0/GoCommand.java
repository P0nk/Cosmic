package client.command.commands.gm0;
import client.command.commands.CommandMapConstants;
import client.Client;
import client.Character;
import client.command.Command;
import server.maps.MapleMap;

public class GoCommand extends Command {

    {
        setDescription("Warp to common towns.");
    }

    @Override
    public void execute(Client c, String[] params) {

        Character player = c.getPlayer();

        if (params.length < 1) {
            player.dropMessage(5,
                    "Usage: @go <destination>\n" +
                            "Available: hene, kern, peri, elia, harbor, sleepy, fm, orbis, ludi, aqua");
            return;
        }
        if (player.getTrade() != null) {
            player.dropMessage(5, "You cannot use @go while trading.");
            return;
        }

        if (player.getJailExpirationTimeLeft() > 0) {
            player.dropMessage(5, "You cannot use @go while jailed.");
            return;

        }

        if (player.isChangingMaps()) {
            return;
        }

        String map = params[0].toLowerCase();

        int mapid = -1;

        switch (map) {

            case "hene":
            case "henesys":
                mapid = 100000000;
                break;

            case "kern":
            case "kerning":
                mapid = 103000000;
                break;

            case "peri":
            case "perion":
                mapid = 102000000;
                break;

            case "elia":
            case "ellinia":
                mapid = 101000000;
                break;

            case "harbor":
            case "lith":
            case "lithharbor":
                mapid = 104000000;
                break;

            case "sleepy":
            case "sleepywood":
                mapid = 105040300;
                break;

            case "fm":
            case "freemarket":
                mapid = CommandMapConstants.FREE_MARKET;
                break;

            case "orbis":
                mapid = 200000000;
                break;

            case "ludi":
            case "ludibrium":
                mapid = 220000000;
                break;

            case "aqua":
            case "aquarium":
                mapid = 230000000;
                break;
        }

        if (mapid == -1) {
            player.dropMessage(5,
                    "Unknown destination. Try: hene, kern, peri, elia, harbor, sleepy, fm, orbis, ludi, aqua");
            return;
        }

        MapleMap target = player.getClient()
                .getChannelServer()
                .getMapFactory()
                .getMap(mapid);

        player.changeMap(target, target.getPortal(0));
    }
}