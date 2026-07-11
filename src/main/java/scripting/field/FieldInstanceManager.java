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
package scripting.field;

import client.Character;
import client.Skill;
import client.SkillFactory;
import config.YamlConfig;
import constants.inventory.ItemConstants;
import net.server.coordinator.world.EventRecallCoordinator;
import net.server.world.Party;
import net.server.world.PartyCharacter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scripting.AbstractPlayerInteraction;
import scripting.event.EventManager;
import scripting.event.scheduler.EventScriptScheduler;
import scripting.field.scheduler.FieldScriptScheduler;
import server.ItemInformationProvider;
import server.StatEffect;
import server.ThreadManager;
import server.TimerManager;
import server.expeditions.Expedition;
import server.life.*;
import server.maps.MapManager;
import server.maps.MapleMap;
import server.maps.Portal;
import server.maps.Reactor;
import tools.PacketCreator;
import tools.Pair;

import javax.script.ScriptException;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static java.util.concurrent.TimeUnit.MINUTES;

/**
 * @author Matze
 * @author Ronan
 */
public class FieldInstanceManager {
    private static final Logger log = LoggerFactory.getLogger(FieldInstanceManager.class);
    private final Map<Integer, Character> chars = new HashMap<>();
    private final List<Monster> mobs = new LinkedList<>();
    private final Map<Character, Integer> killCount = new HashMap<>();
    private FieldScriptManager fieldScriptManager;
    private FieldScriptScheduler fss;
    private MapleMap mapInstance;
    private final Properties props = new Properties();
    private final Map<String, Object> objectProps = new HashMap<>();
    private long timeStarted = 0;
    private long eventTime = 0;
    private final List<Integer> mapIds = new LinkedList<>();

    private final Lock readLock;
    private final Lock writeLock;

    private final Lock propertyLock = new ReentrantLock(true);
    private final Lock scriptLock = new ReentrantLock(true);

    private ScheduledFuture<?> field_schedule = null;
    private boolean disposed = false;


    // registers player status on an event (null on this Map structure equals to 0)
    private final Map<Integer, Integer> playerGrid = new HashMap<>();

    // registers all opened gates on the event. Will help late characters to encounter next stages gates already opened
    private final Map<Integer, Pair<String, Integer>> openedGates = new HashMap<>();

    public FieldInstanceManager(FieldScriptManager fieldScriptManager, MapleMap mapInstance) {
        this.fieldScriptManager = fieldScriptManager;
        this.mapInstance = mapInstance;
        this.fss = new FieldScriptScheduler();

        ReadWriteLock readWriteLock = new ReentrantReadWriteLock(true);
        this.readLock = readWriteLock.readLock();
        this.writeLock = readWriteLock.writeLock();
    }

    public FieldScriptManager getFieldScriptManager() {
        scriptLock.lock();
        try {
            return fieldScriptManager;
        } finally {
            scriptLock.unlock();
        }
    }
}
