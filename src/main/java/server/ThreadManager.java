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
package server;

import java.util.concurrent.*;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * @author Ronan
 */
public class ThreadManager {
    private static final ThreadManager instance = new ThreadManager();

    public static ThreadManager getInstance() {
        return instance;
    }

    private ThreadPoolExecutor tpe;

    private ThreadManager() {}

    private class RejectedExecutionHandlerImpl implements RejectedExecutionHandler {

        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            Thread t = new Thread(r);
            t.start();
        }

    }

    public void newTask(Runnable r) {
        tpe.execute(r);
    }

    public void start() {
        RejectedExecutionHandler reh = new RejectedExecutionHandlerImpl();
        ThreadFactory tf = Executors.defaultThreadFactory();

        tpe = new ThreadPoolExecutor(20, 1000, 77, SECONDS, new ArrayBlockingQueue<>(50), tf, reh);
    }

    public void stop() {
        if (tpe == null)
        {
            return;
        }

        tpe.shutdown();
        try {
            tpe.awaitTermination(5, MINUTES);
        } catch (InterruptedException ie) {
        }
    }

}
