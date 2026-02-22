package client.command.commands.gm0;

import client.Client;
import client.command.Command;
import server.events.FeverScheduler;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class FeverCommand extends Command {
    {
        setDescription("Checks the status of the current Fever event.");
    }

    @Override
    public void execute(Client client, String[] params) {
        FeverScheduler scheduler = FeverScheduler.getInstance();

        if (scheduler.isFeverActive()) {
            DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
            dateFormat.setTimeZone(TimeZone.getDefault());
            String currentTimeStr = dateFormat.format(new Date());
            String endTimeStr = dateFormat.format(new Date(scheduler.getFeverEndTime()));

            client.getPlayer().yellowMessage("Currently Active: " + scheduler.getCurrentFever().getName()
                    + " Fever | Ends at: " + endTimeStr + " (Current Server Time: " + currentTimeStr + ")");
        } else {
            client.getPlayer().yellowMessage("There is no active Fever event at the moment.");
        }
    }
}
