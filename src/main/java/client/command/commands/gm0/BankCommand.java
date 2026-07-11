package client.command.commands.gm0;

import client.Character;
import client.Client;
import client.command.Command;
import java.text.NumberFormat;

public class BankCommand extends Command {
    {
        setDescription("Retrieve or store Mesos");
    }

    @Override
    public void execute(Client c, String[] params) {
        Character player = c.getPlayer();
        long bank = player.getBankMesos();
        NumberFormat numberFormat = NumberFormat.getInstance();

        if (params.length == 0) {
            player.showHint("Usage: @bank <take/store/balance> <amount>");
            return;
        }

        String action = params[0].toLowerCase();

        // ✅ Handle balance WITHOUT needing amount
        if (action.equals("balance")) {
            String formattedBank = numberFormat.format(bank);
            player.showHint("You currently have " + formattedBank + " mesos stored.");
            return;
        }

        // Everything below requires an amount
        if (params.length < 2) {
            player.showHint("Please specify an amount.");
            return;
        }

        try {
            int amount = Integer.parseInt(params[1]);

            if (amount <= 0) {
                player.showHint("Please specify a valid amount of mesos.");
                return;
            }

            switch (action) {
                case "store":
                    handleStore(player, amount);
                    break;
                case "take":
                    handleTake(player, amount);
                    break;
                default:
                    player.showHint("Invalid command. Use '@bank take <amount>' or '@bank store <amount>'");
                    break;
            }

        } catch (NumberFormatException e) {
            player.showHint("Please specify a valid numeric amount.");
        }
    }

    private void handleStore(Character player, int amount) {
        if (amount > player.getMeso()) {
            player.showHint("You don't have enough mesos to deposit.");
        } else {
            player.showHint("Deposited #r" + NumberFormat.getInstance().format(amount) + "#k mesos to your bank.");
            player.gainMeso(-amount);
            player.gainBankMeso(amount);
        }
    }

    private void handleTake(Character player, int amount) {
        long bank = player.getBankMesos();
        int meso = player.getMeso();

        if (bank < amount) {
            player.showHint("You don't have enough mesos to withdraw.");
            return;
        }

        // Prevent int overflow
        if ((long) meso + amount > Integer.MAX_VALUE) {
            String formattedMax = NumberFormat.getInstance().format(Integer.MAX_VALUE - meso);
            player.showHint("You can only withdraw up to #r" + formattedMax + "#k mesos.");
            return;
        }

        player.showHint("Withdrew #r" + NumberFormat.getInstance().format(amount) + "#k mesos from your bank.");
        player.gainBankMeso(-amount);
        player.gainMeso(amount);
    }
}