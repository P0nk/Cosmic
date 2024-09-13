package client.command;

import client.Character;
import client.Client;
import config.YamlConfig;

public abstract class StatCommand extends Command {

    protected enum Stat {DEX, INT, LUK, STR}
    protected Stat stat;

    public void execute(Client c, String[] params) {
        AssignStat(c, params, stat);
    }

    private void AssignStat(Client client, String[] params, Stat stat)
    {
        Character character = client.getPlayer();
        if (params.length < 1) {
            character.dropMessage("Enter an amount of AP you want to assign.");
        }
        try {
            int amount = Integer.parseInt(params[0]);
            int remainingAp = character.getRemainingAp();
            if (isValidAmount(amount, remainingAp)) {
                switch (stat) {
                    case DEX -> character.assignDex(amount);
                    case INT -> character.assignInt(amount);
                    case LUK -> character.assignLuk(amount);
                    case STR -> character.assignStr(amount);
                    default -> throw new IllegalStateException("Unexpected value: " + stat);
                }
                character.dropMessage("Assigned " + amount + " into " + stat);
            }
            else {
                character.dropMessage("You have entered an invalid amount of AP.");
            }
        }
        catch (NumberFormatException ex) {
            character.dropMessage("Enter a valid number!");
        }
    }
    private boolean isValidAmount(int amount, int remainingAp) {
        return amount > 0
                && amount <= remainingAp
                && amount <= YamlConfig.config.server.MAX_AP;
    }

    protected void setStat(Stat stat) {
        this.stat = stat;
    }
}
