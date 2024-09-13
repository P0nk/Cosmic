package client.command.commands.gm0;

import client.Client;
import client.command.StatCommand;

public class AddStrCommand extends StatCommand {

    {
        setStat(Stat.STR);
        setDescription("Assigns AP into " + stat);
    }

    @Override
    public void execute(Client client, String[] params) {
        super.execute(client, params);
    }
}
