package server.events;

import java.util.Random;
import java.util.concurrent.ScheduledFuture;
import net.server.Server;
import net.server.world.World;
import server.TimerManager;
import tools.PacketCreator;

public class FeverScheduler {

    private static final FeverScheduler instance = new FeverScheduler();
    private ScheduledFuture<?> feverTask;
    private ScheduledFuture<?> stopTask;
    private final Random rand = new Random();
    private boolean isFeverActive = false;
    private FeverType currentFever = null;

    // Configuration
    private static final int MIN_INTERVAL_MIN = 20;
    private static final int MAX_INTERVAL_MIN = 45;
    private static final int MIN_DURATION_MIN = 5;
    private static final int MAX_DURATION_MIN = 10;

    public enum FeverType {
        DROP("Drop Rate", 2), // 2x Drop
        MESO("Meso Rate", 2), // 2x Meso
        NX("NX Rate", 2), // 2x NX
        SPELL_TRACE("Spell Trace", 1); // Guaranteed Spell Trace

        private final String name;
        private final int multiplier;

        FeverType(String name, int multiplier) {
            this.name = name;
            this.multiplier = multiplier;
        }

        public String getName() {
            return name;
        }

        public int getMultiplier() {
            return multiplier;
        }
    }

    public static FeverScheduler getInstance() {
        return instance;
    }

    public void start() {
        scheduleNextFever();
        System.out.println("[FeverScheduler] Started.");
    }

    public void scheduleNextFever() {
        if (feverTask != null && !feverTask.isDone()) {
            feverTask.cancel(false);
        }

        int interval = MIN_INTERVAL_MIN + rand.nextInt(MAX_INTERVAL_MIN - MIN_INTERVAL_MIN + 1);
        long delay = interval * 60 * 1000L;

        System.out.println("[FeverScheduler] Next fever scheduled in " + interval + " minutes.");

        feverTask = TimerManager.getInstance().register(new Runnable() {
            @Override
            public void run() {
                startFever(null); // Random fever
            }
        }, delay);
    }

    public void startFever(FeverType forcedType) {
        if (isFeverActive) {
            stopFever();
        }

        // Select fever type
        if (forcedType != null) {
            currentFever = forcedType;
        } else {
            FeverType[] types = FeverType.values();
            currentFever = types[rand.nextInt(types.length)];
        }

        isFeverActive = true;

        // Apply Boost
        World world = Server.getInstance().getWorld(0); // Assuming World 0
        if (world == null)
            return;

        switch (currentFever) {
            case DROP:
                world.setDropRate(world.getDropRate() * currentFever.getMultiplier());
                break;
            case MESO:
                world.setMesoRate(world.getMesoRate() * currentFever.getMultiplier());
                break;
            case NX:
                world.setNxFever(true);
                break;
            case SPELL_TRACE:
                world.setSpellTraceFever(true);
                break;
        }

        // Broadcast Message
        int duration = MIN_DURATION_MIN + rand.nextInt(MAX_DURATION_MIN - MIN_DURATION_MIN + 1);
        String msg = "[Fever] " + currentFever.getName() + " Fever has started! Enjoy the boost for " + duration
                + " minutes!";
        world.broadcastPacket(PacketCreator.serverNotice(6, msg));
        // 5120009: Weather effect (Snow) - Example, adjust ID as needed
        world.broadcastPacket(PacketCreator.startMapEffect(msg, 5120009, true));

        System.out.println(
                "[FeverScheduler] Fever started: " + currentFever.getName() + " for " + duration + " minutes.");

        long durationMs = duration * 60 * 1000L;

        if (stopTask != null && !stopTask.isDone()) {
            stopTask.cancel(false);
        }

        stopTask = TimerManager.getInstance().schedule(new Runnable() {
            @Override
            public void run() {
                stopFever();
                scheduleNextFever(); // Schedule next cycle after this one ends
            }
        }, durationMs);
    }

    public void stopFever() {
        if (!isFeverActive)
            return;

        World world = Server.getInstance().getWorld(0);
        if (world != null) {
            switch (currentFever) {
                case DROP:
                    world.setDropRate(world.getDropRate() / currentFever.getMultiplier());
                    break;
                case MESO:
                    world.setMesoRate(world.getMesoRate() / currentFever.getMultiplier());
                    break;
                case NX:
                    world.setNxFever(false);
                    break;
                case SPELL_TRACE:
                    world.setSpellTraceFever(false);
                    break;
            }
            world.broadcastPacket(PacketCreator.serverNotice(6, "[Fever] The Fever event has ended."));
            world.broadcastPacket(PacketCreator.removeMapEffect());
        }

        isFeverActive = false;
        currentFever = null;
        System.out.println("[FeverScheduler] Fever ended.");
    }
}
