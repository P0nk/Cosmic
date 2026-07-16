package client.command.commands.gm0;

import client.Character;
import client.Client;
import client.command.Command;

public class SaveCommand extends Command {

    public SaveCommand() {
        setDescription("Save your ForgeMS character.");
    }

    @Override
    public void execute(Client c, String[] params) {
        Character player = c.getPlayer();

        try {
            player.saveCharToDB();

            player.dropMessage(
                    5,
                    "Your character has been saved successfully."
            );
        } catch (Exception e) {
            player.dropMessage(
                    5,
                    "Your character could not be saved. Please notify a GM."
            );

            e.printStackTrace();
        }
    }
}