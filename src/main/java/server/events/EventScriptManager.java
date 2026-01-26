package server.events;

import net.server.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scripting.AbstractScriptManager;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Handles the loading and execution of Event Scripts.
 * Updated to correctly inject Static Types using Java.type().
 */
public class EventScriptManager extends AbstractScriptManager {
    private static final Logger log = LoggerFactory.getLogger(EventScriptManager.class);

    private final Channel channelServer;
    private final String[] scripts;
    private final Map<String, EventEntry> events = new LinkedHashMap<>();

    public EventScriptManager(Channel channelServer, String[] scripts) {
        this.channelServer = channelServer;
        this.scripts = scripts;
    }

    public void init() {
        log.info("[EventScriptManager] Starting initialization of {} scripts...", scripts.length);

        for (String script : scripts) {
            if (!events.containsKey(script)) {
                initializeEventEntry(script);
            }
        }

        log.info("[EventScriptManager] Initialization complete. Active events: {}", events.size());
    }

    private void initializeEventEntry(String script) {
        try {
            ScriptEngine engine = getInvocableScriptEngine("event/" + script + ".js");

            if (engine == null) {
                log.error("[EventScriptManager] CRITICAL: Engine is NULL for script '{}'. Check file existence or syntax.", script);
                return;
            }

            Invocable iv = (Invocable) engine;

            // Create the EventManager Java Object
            EventManager em = new EventManager(channelServer, iv, script);

            // --- [FIX] GLOBAL INJECTION BLOCK ---

            // 1. Inject INSTANCES (Objects) using .put()
            engine.put("em", em);

            // 2. Inject STATIC CLASSES (Types) using .eval()
            // This allows the script to call static methods like LifeFactory.getMonster()
            String imports = "var LifeFactory = Java.type('server.life.LifeFactory');" +
                    "var PacketCreator = Java.type('tools.PacketCreator');" +
                    "var ExpeditionType = Java.type('server.expeditions.ExpeditionType');" +
                    "var MapleMap = Java.type('server.maps.MapleMap');";

            engine.eval(imports);
            // ------------------------------------

            // Invoke the 'init' function in the JS file
            iv.invokeFunction("init", (Object) null);

            // Save to map
            events.put(script, new EventEntry(script, iv, em));
//            log.info("[EventScriptManager] Successfully started '{}'.", script);

        } catch (ScriptException | NoSuchMethodException e) {
            log.error("[EventScriptManager] Error executing init() in script '{}': {}", script, e.getMessage());
        } catch (Exception e) {
            log.error("[EventScriptManager] Unexpected error loading '{}': ", script, e);
        }
    }

    public void cancel() {
        for (EventEntry entry : events.values()) {
            if (entry.em != null) {
                entry.em.cancel();
            }
        }
    }

    public void dispose() {
        for (EventEntry entry : events.values()) {
            if (entry.em != null) {
                entry.em.dispose();
            }
        }
        events.clear();
    }

    public EventManager getEventManager(String event) {
        EventEntry entry = events.get(event);
        if (entry == null) {
            return null;
        }
        return entry.em;
    }

    public boolean isActive() {
        return !events.isEmpty();
    }

    // Inner class to hold event data
    public static class EventEntry {
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