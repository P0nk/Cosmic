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
package scripting.event;

import database.drop.DropProvider;
import net.server.channel.Channel;
import org.slf4j.LoggerFactory;
import scripting.AbstractScriptManager;
import scripting.SynchronizedInvocable;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Matze
 */
public class EventScriptManager extends AbstractScriptManager {
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(EventScriptManager.class);
    private static final String INJECTED_VARIABLE_NAME = "em";
    private static EventEntry fallback;
    private final Map<String, EventEntry> events = new ConcurrentHashMap<>();
    private final DropProvider dropProvider;
    private boolean active = false;

    private static class EventEntry {

        public EventEntry(Invocable iv, EventManager em) {
            this.iv = iv;
            this.em = em;
        }

        public Invocable iv;
        public EventManager em;
    }

    public EventScriptManager(final Channel channel, String[] scripts, DropProvider dropProvider) {
        this.dropProvider = dropProvider;
        for (String script : scripts) {
            if (!script.isEmpty()) {
                events.put(script, initializeEventEntry(script, channel));
            }
        }

        init();
        fallback = events.remove("0_EXAMPLE");
    }

    public EventManager getEventManager(String event) {
        EventEntry entry = events.get(event);
        if (entry == null) {
            return fallback.em;
        }
        return entry.em;
    }

    public boolean isActive() {
        return active;
    }

    public final void init() {
        for (EventEntry entry : events.values()) {
            try {
                entry.iv.invokeFunction("init", (Object) null);
            } catch (Exception ex) {
                log.error("Error on script: {}", entry.em.getName(), ex);
            }
        }

        active = events.size() > 1; // bootup loads only 1 script
    }

    private void reloadScripts() {
        Set<Entry<String, EventEntry>> eventEntries = new HashSet<>(events.entrySet());
        if (eventEntries.isEmpty()) {
            return;
        }

        Channel channel = eventEntries.iterator().next().getValue().em.getChannelServer();
        for (Entry<String, EventEntry> entry : eventEntries) {
            String script = entry.getKey();
            events.put(script, initializeEventEntry(script, channel));
        }
    }

    private EventEntry initializeEventEntry(String script, Channel channel) {
        ScriptEngine engine = getInvocableScriptEngine("event/" + script + ".js");
        Invocable iv = SynchronizedInvocable.of((Invocable) engine);
        EventManager eventManager = new EventManager(channel, iv, script, dropProvider);
        engine.put(INJECTED_VARIABLE_NAME, eventManager);
        return new EventEntry(iv, eventManager);
    }

    // Is never being called
    public void reload() {
        cancel();
        reloadScripts();
        init();
    }

    public void cancel() {
        active = false;
        for (EventEntry entry : events.values()) {
            entry.em.cancel();
        }
    }

    public void dispose() {
        if (events.isEmpty()) {
            return;
        }

        Set<EventEntry> eventEntries = new HashSet<>(events.values());
        events.clear();

        active = false;
        for (EventEntry entry : eventEntries) {
            entry.em.cancel();
        }
    }
}
