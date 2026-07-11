package client.command.commands.gm0;

import client.Character;
import client.Client;
import client.command.Command;
import config.YamlConfig;
import constants.game.GameConstants;
import server.transactions.Transaction;
import server.transactions.TransactionService;

import java.util.List;

public class BuyBackCommand extends Command {
    {
        setDescription("Buys back the last sold items or lists recent transactions for buyback.");
    }

    @Override
    public void execute(Client c, String[] params) {
        Character player = c.getPlayer();

        if (!player.isAlive()) {
            player.dropMessage(6, "This command cannot be used when you're dead.");
            return;
        }

        if (player.getEventInstance() != null) {
            player.dropMessage(6, "This command cannot be used in expeditions or special instances.");
            return;
        }

        try {
            if (params.length == 1 && params[0].equalsIgnoreCase("list")) {
                List<Transaction> transactions = TransactionService.getLastTransactions(player.getId(), YamlConfig.config.server.MAXIMUM_TRANSACTIONS_FOR_BUYBACK);

                if (transactions.isEmpty()) {
                    player.dropMessage(6, "There are no previous transactions at this time.");
                    return;
                }

                StringBuilder stringBuilder = new StringBuilder("Last transactions:\r\n\r\n");
                for (int i = 0; i < transactions.size(); i++) {
                    Transaction transaction = transactions.get(i);
                    int totalItems = transaction.getItems().size();
                    int totalMesos = transaction.getTotalPurchasePrice();

                    String suffix = transaction.isBuybackUsed() ? " [Used]" : "";

                    stringBuilder.append(i + 1).append(". ").append(totalItems).append(" items (").append(GameConstants.numberWithCommas(totalMesos)).append(" mesos)").append(suffix).append("\r\n");
                }

                player.dropMessage(6, stringBuilder.toString());
                return;
            }

            switch (params.length) {
                case 0:
                    c.getAbstractPlayerInteraction().openNpc(9000041, "buyback"); //This uses Donation Box NPC
                    break;
                case 1:
                {
                    String resultMessage = tryProcessTransaction(params, player);

                    player.dropMessage(6, resultMessage);
                    break;
                }
                default:
                    player.dropMessage(6, "Usage: list or <transactionNumber>");
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            player.dropMessage(6, "An error occurred while processing the buyback. Please try again later.");
        }
    }

    private static String tryProcessTransaction(String[] params, Character player) {
        int transactionIndex;

        try {
            transactionIndex = Integer.parseInt(params[0]) - 1;
        } catch (NumberFormatException e) {
            return "Invalid transaction number.";
        }

        if (transactionIndex < 0) {
            return "Invalid transaction number.";
        }

        return TransactionService.processTransaction(player, transactionIndex).getMessage();
    }
}
