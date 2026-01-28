package client.command.commands.gm0;

import client.Client;
import client.command.Command;

public class WithdrawMesoCommand extends Command {

    {
        setDescription("Withdraw all accumulated merchant earnings (Mesos and B-Coins) from the database.");
    }

    @Override
    public void execute(Client c, String[] params) {
        c.getPlayer().dropMessage(5, "Checking for available merchant earnings...");
        c.getPlayer().withdrawMerchantMesos();
    }
}
