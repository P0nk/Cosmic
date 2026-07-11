/*
 This file is part of the HeavenMS MapleStory Server
 Copyleft (L) 2016 - 2019 RonanLana

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as
 published by the Free Software Foundation version 3 as published by
 the Free Software Foundation. You may not use, modify or distribute
 this program under any other version of the GNU Affero General Public
 License.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package scripting.field.scheduler;

import config.YamlConfig;
import net.server.Server;
import server.ThreadManager;
import server.TimerManager;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Ronan
 */
public class FieldScriptScheduler {
    private boolean disposed = false;
    private int idleProcs = 0;
    private final Map<String, FieldScriptScheduleEntry> registeredEntries = new HashMap<>();

    private ScheduledFuture<?> schedulerTask = null;
    private final Lock schedulerLock = new ReentrantLock(true);
    private final Runnable monitorTask = () -> runBaseSchedule();


    private void runBaseSchedule() {
        List<Runnable> toRemove;
        Map<String, FieldScriptScheduleEntry> registeredEntriesCopy;

        schedulerLock.lock();
        try {
            if (registeredEntries.isEmpty()) {
                idleProcs++;

                if (idleProcs >= YamlConfig.config.server.MOB_STATUS_MONITOR_LIFE) {
                    if (schedulerTask != null) {
                        schedulerTask.cancel(false);
                        schedulerTask = null;
                    }
                }

                return;
            }

            idleProcs = 0;
            registeredEntriesCopy = new HashMap<>(registeredEntries);
        } finally {
            schedulerLock.unlock();
        }

        long timeNow = Server.getInstance().getCurrentTime();
        toRemove = new LinkedList<>();
        for (Entry<String, FieldScriptScheduleEntry> rmd : registeredEntriesCopy.entrySet()) {
            FieldScriptScheduleEntry entry = rmd.getValue();

            if (entry.getDuration() < timeNow) {
                Runnable r = entry.getRunnable();

                r.run();  // runs the scheduled action
                toRemove.add(r);
            }
        }

        if (!toRemove.isEmpty()) {
            schedulerLock.lock();
            try {
                for (Runnable r : toRemove) {
                    registeredEntries.remove(r);
                }
            } finally {
                schedulerLock.unlock();
            }
        }
    }

    public void registerEntry(final String name, final Runnable scheduledAction, final long duration) {

        ThreadManager.getInstance().newTask(() -> {
            schedulerLock.lock();
            try {
                idleProcs = 0;
                if (schedulerTask == null) {
                    if (disposed) {
                        return;
                    }

                    schedulerTask = TimerManager.getInstance().register(monitorTask, YamlConfig.config.server.MOB_STATUS_MONITOR_PROC, YamlConfig.config.server.MOB_STATUS_MONITOR_PROC);
                }

                registeredEntries.put(name, new FieldScriptScheduleEntry());
            } finally {
                schedulerLock.unlock();
            }
        });
    }

    public void cancelEntry(final Runnable scheduledAction) {
        ThreadManager.getInstance().newTask(() -> {
            schedulerLock.lock();
            try {
                registeredEntries.entrySet().removeIf(entry -> entry.getValue().getRunnable() == scheduledAction);
            } finally {
                schedulerLock.unlock();
            }
        });
    }

    public void cancelEntry(final String entryName) {
        ThreadManager.getInstance().newTask(() -> {
            schedulerLock.lock();
            try {
                registeredEntries.remove(entryName);
            } finally {
                schedulerLock.unlock();
            }
        });
    }

    public void dispose() {

        ThreadManager.getInstance().newTask(() -> {
            schedulerLock.lock();
            try {
                if (schedulerTask != null) {
                    schedulerTask.cancel(false);
                    schedulerTask = null;
                }

                registeredEntries.clear();
                disposed = true;
            } finally {
                schedulerLock.unlock();
            }
        });
    }
}
