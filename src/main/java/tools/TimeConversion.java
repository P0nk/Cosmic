package tools;

import java.util.concurrent.TimeUnit;

public class TimeConversion {

    public static String millisecondsToTimeString(long duration) {
        long hours = TimeUnit.MILLISECONDS.toHours(duration);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(duration) - TimeUnit.HOURS.toMinutes(hours);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(duration) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration));

        String formattedHours = String.format("%02d", hours);
        String formattedMinutes = String.format("%02d", minutes);
        String formattedSeconds = String.format("%02d", seconds);

        return formattedHours + ":" + formattedMinutes + ":" + formattedSeconds;
    }
}
