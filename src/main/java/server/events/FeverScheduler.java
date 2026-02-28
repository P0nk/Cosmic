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
    private FeverType lastFever = null;
    private long feverEndTime = 0;

    // Configuration
    private static final int MIN_INTERVAL_MIN = 0;
    private static final int MAX_INTERVAL_MIN = 10;
    private static final int MIN_DURATION_MIN = 5;
    private static final int MAX_DURATION_MIN = 10;

    // Original rates storage
    private final java.util.Map<Integer, Double> originalDropRates = new java.util.HashMap<>();
    private final java.util.Map<Integer, Double> originalMesoRates = new java.util.HashMap<>();
    private final java.util.Map<Integer, Double> originalExpRates = new java.util.HashMap<>();

    public enum FeverType {
        DROP("Drop Rate", 2), // 2x Drop
        MESO("Meso Rate", 2), // 2x Meso
        NX("NX Rate", 2), // 2x NX
        SPELL_TRACE("Spell Trace", 1), // Guaranteed Spell Trace
        EXP("Exp Rate", 2); // 2x Exp

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

    public long getFeverEndTime() {
        return feverEndTime;
    }

    public FeverType getCurrentFever() {
        return currentFever;
    }

    public boolean isFeverActive() {
        return isFeverActive;
    }

    public void scheduleNextFever() {
        if (feverTask != null && !feverTask.isDone()) {
            feverTask.cancel(false);
        }

        int interval = MIN_INTERVAL_MIN + rand.nextInt(MAX_INTERVAL_MIN - MIN_INTERVAL_MIN + 1);
        long delay = interval * 60 * 1000L;

        System.out.println("[FeverScheduler] Next fever scheduled in " + interval + " minutes.");

        feverTask = TimerManager.getInstance().schedule(new Runnable() {
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
            do {
                currentFever = types[rand.nextInt(types.length)];
            } while (currentFever == lastFever);
        }
        lastFever = currentFever;

        isFeverActive = true;

        // Clear old saved rates
        originalDropRates.clear();
        originalMesoRates.clear();
        originalExpRates.clear();

        int duration = MIN_DURATION_MIN + rand.nextInt(MAX_DURATION_MIN - MIN_DURATION_MIN + 1);
        long durationMs = duration * 60 * 1000L;
        feverEndTime = System.currentTimeMillis() + durationMs;

        // Apply Boost and save original rates
        for (World world : Server.getInstance().getWorlds()) {
            if (world == null)
                continue;

            int worldId = world.getWorldId();
            switch (currentFever) {
                case DROP:
                    originalDropRates.put(worldId, world.getDropRate());
                    world.setDropRate(world.getDropRate() * currentFever.getMultiplier());
                    break;
                case MESO:
                    originalMesoRates.put(worldId, world.getMesoRate());
                    world.setMesoRate(world.getMesoRate() * currentFever.getMultiplier());
                    break;
                case NX:
                    world.setNxFever(true);
                    break;
                case SPELL_TRACE:
                    world.setSpellTraceFever(true);
                    break;
                case EXP:
                    originalExpRates.put(worldId, world.getExpRate());
                    world.setExpRate(world.getExpRate() * currentFever.getMultiplier());
                    break;
            }

            // Broadcast Message
            java.text.DateFormat dateFormat = new java.text.SimpleDateFormat("HH:mm:ss");
            dateFormat.setTimeZone(java.util.TimeZone.getDefault());
            String startTimeStr = dateFormat.format(new java.util.Date());
            String endTimeStr = dateFormat.format(new java.util.Date(feverEndTime));

            String msg = "[Fever] " + currentFever.getName() + " Fever has started at " + startTimeStr
                    + "! Enjoy the boost for " + duration
                    + " more mins until " + endTimeStr + "!";
            world.broadcastPacket(PacketCreator.serverNotice(6, msg));
            // 5120009: Weather effect (Snow) - Example, adjust ID as needed
            world.broadcastPacket(PacketCreator.startMapEffect(msg, 5120009, true));
        }

        System.out.println(
                "[FeverScheduler] Fever started: " + currentFever.getName() + " for " + duration + " minutes.");

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

        for (World world : Server.getInstance().getWorlds()) {
            if (world != null) {
                int worldId = world.getWorldId();
                switch (currentFever) {
                    case DROP:
                        if (originalDropRates.containsKey(worldId)) {
                            world.setDropRate(originalDropRates.get(worldId));
                        }
                        break;
                    case MESO:
                        if (originalMesoRates.containsKey(worldId)) {
                            world.setMesoRate(originalMesoRates.get(worldId));
                        }
                        break;
                    case NX:
                        world.setNxFever(false);
                        break;
                    case SPELL_TRACE:
                        world.setSpellTraceFever(false);
                        break;
                    case EXP:
                        if (originalExpRates.containsKey(worldId)) {
                            world.setExpRate(originalExpRates.get(worldId));
                        }
                        break;
                }
                world.broadcastPacket(PacketCreator.serverNotice(6, "[Fever] The Fever event has ended."));
                world.broadcastPacket(PacketCreator.removeMapEffect());
            }
        }

        isFeverActive = false;
        currentFever = null;
        feverEndTime = 0;

        // Clean up memory
        originalDropRates.clear();
        originalMesoRates.clear();
        originalExpRates.clear();

        System.out.println("[FeverScheduler] Fever ended.");
    }
}
