/*
    Refactored EventScriptManager.java
*/
package server.events;

import net.server.channel.Channel;
import scripting.AbstractScriptManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EventScriptManager extends AbstractScriptManager {
    private static final Logger log = LoggerFactory.getLogger(EventScriptManager.class);
    private final Channel channelServer;
    private final Map<String, EventEntry> events = new ConcurrentHashMap<>();

    public EventScriptManager(Channel channelServer, String[] scripts) {
        this.channelServer = channelServer;
        for (String script : scripts) {
            if (!script.equals("")) {
                initializeEventEntry(script);
            }
        }
    }

    private void initializeEventEntry(String script) {
        ScriptEngine engine = getInvocableScriptEngine("event/" + script + ".js");

        if (engine == null) {
            log.error("CRITICAL: Failed to load event script '{}'. Engine is null (Check syntax).", script);
            return;
        }

        // [FIX 1] Cast engine to Invocable
        EventManager em = new EventManager(channelServer, (Invocable) engine, script);

        engine.put("em", em);

        String globalInjections =
                "var LifeFactory = Java.type('server.life.LifeFactory');" +
                        "var PacketCreator = Java.type('tools.PacketCreator');" +
                        "var InventoryManipulator = Java.type('client.inventory.manipulator.InventoryManipulator');" +
                        "var Item = Java.type('client.inventory.Item');" +
                        "var ExpeditionType = Java.type('server.expeditions.ExpeditionType');";

        try {
            engine.eval(globalInjections);
        } catch (ScriptException e) {
            log.error("Failed to inject global variables into script: {}", script, e);
        }

        events.put(script, new EventEntry(script, (Invocable) engine, em));
    }

    public EventManager getEventManager(String event) {
        EventEntry entry = events.get(event);
        return (entry != null) ? entry.em : null;
    }

    public void init() {
        for (EventEntry entry : events.values()) {
            try {
                ((Invocable) entry.iv).invokeFunction("init", (Object) null);
            } catch (NoSuchMethodException | ScriptException ex) {
                log.warn("Error initializing event: {}", entry.script, ex);
            }
        }
    }

    public void dispose() {
        for (EventEntry entry : events.values()) {
            entry.em.cancel();
            entry.em.dispose(); // [FIX 2] Requires Method in EventManager
        }
        events.clear();
    }

    public void cancel() {
        for (EventEntry entry : events.values()) {
            entry.em.cancel();
        }
    }

    public boolean isActive() {
        return !events.isEmpty();
    }

    private static class EventEntry {
        public String script;
        public Invocable iv;
        public EventManager em;

        public EventEntry(String script, Invocable iv, EventManager em) {
            this.script = script;
            this.iv = iv;
            this.em = em;
        }
    }
}