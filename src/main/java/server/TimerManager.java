/*
	This file is part of the OdinMS Maple Story Server
    Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
		       Matthias Butz <matze@odinms.de>
		       Jan Christian Meyer <vimes@odinms.de>

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
package server;

import net.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;

public class TimerManager implements TimerManagerMBean {
    private static final Logger log = LoggerFactory.getLogger(TimerManager.class);
    private static final TimerManager instance = new TimerManager();

    public static TimerManager getInstance() {
        return instance;
    }

    private ScheduledThreadPoolExecutor ses;

    private TimerManager() {
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        try {
            mBeanServer.registerMBean(this, new ObjectName("server:type=TimerManger"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void start() {
        if (ses != null && !ses.isShutdown() && !ses.isTerminated()) {
            return;
        }
        ScheduledThreadPoolExecutor stpe = new ScheduledThreadPoolExecutor(4, new ThreadFactory() {
            private final AtomicInteger threadNumber = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setName("TimerManager-Worker-" + threadNumber.getAndIncrement());
                return t;
            }
        });
        //this is a no-no, it actually does nothing..then why the fuck are you doing it?
        stpe.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
        stpe.setRemoveOnCancelPolicy(true);

        stpe.setKeepAliveTime(5, MINUTES);
        stpe.allowCoreThreadTimeOut(true);

        ses = stpe;
    }

    public void stop() {
        ses.shutdownNow();
    }

    public Runnable purge() {//Yay?
        return () -> {
            Server.getInstance().forceUpdateCurrentTime();
            ses.purge();
        };
    }

    public ScheduledFuture<?> register(Runnable r, long repeatTime, long delay) {
        return ses.scheduleAtFixedRate(new LoggingSaveRunnable(r), delay, repeatTime, MILLISECONDS);
    }

    public ScheduledFuture<?> register(Runnable r, long repeatTime) {
        return ses.scheduleAtFixedRate(new LoggingSaveRunnable(r), 0, repeatTime, MILLISECONDS);
    }

    public ScheduledFuture<?> schedule(Runnable r, long delay) {
        return ses.schedule(new LoggingSaveRunnable(r), delay, MILLISECONDS);
    }

    public ScheduledFuture<?> scheduleAtTimestamp(Runnable r, long timestamp) {
        return schedule(r, timestamp - System.currentTimeMillis());
    }

    @Override
    public long getActiveCount() {
        return ses.getActiveCount();
    }

    @Override
    public long getCompletedTaskCount() {
        return ses.getCompletedTaskCount();
    }

    @Override
    public int getQueuedTasks() {
        return ses.getQueue().toArray().length;
    }

    @Override
    public long getTaskCount() {
        return ses.getTaskCount();
    }

    @Override
    public boolean isShutdown() {
        return ses.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return ses.isTerminated();
    }

    public ScheduledFuture<?> scheduleAtFixedRate(Runnable r, int initialDelay, int period) {
        return ses.scheduleAtFixedRate(r, initialDelay, period, MILLISECONDS);
    }


    private static class LoggingSaveRunnable implements Runnable {
        Runnable r;

        public LoggingSaveRunnable(Runnable r) {
            this.r = r;
        }

        @Override
        public void run() {
            try {
                r.run();
            } catch (Throwable t) {
                log.error("Error in scheduled task", t);
            }
        }
    }


}
