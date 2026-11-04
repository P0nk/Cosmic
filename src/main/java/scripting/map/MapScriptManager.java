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
package scripting.map;

import client.Character;
import client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scripting.AbstractScriptManager;
import scripting.SynchronizedInvocable;

import javax.script.Invocable;
import javax.script.ScriptException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MapScriptManager extends AbstractScriptManager {
    private static final Logger log = LoggerFactory.getLogger(MapScriptManager.class);
    private static final MapScriptManager instance = new MapScriptManager();

    /**
     * One engine per script, shared by every player entering any map that uses it - so it is invoked
     * from whichever channel event loop that player's connection lives on. A GraalJS context refuses
     * to be entered by a second thread while another is inside it ({@code IllegalStateException:
     * Multi threaded access requested ... not allowed for language(s) js}), and that exception escaped
     * {@code MapleMap#addPlayer} halfway through {@code Character#changeMapInternal}, leaving the
     * character half-transferred and unable to change maps or log back in cleanly. Found when two
     * party members walked into Kerning City ({@code onUserEnter=explorationPoint}) in the same
     * instant. Same cure as {@code EventScriptManager}: serialize each engine with
     * {@link SynchronizedInvocable}; sequential use from different threads is allowed.
     */
    private final Map<String, Invocable> scripts = new ConcurrentHashMap<>();

    public static MapScriptManager getInstance() {
        return instance;
    }

    public void reloadScripts() {
        scripts.clear();
    }

    public boolean runMapScript(Client c, String mapScriptPath, boolean firstUser) {
        if (firstUser) {
            Character chr = c.getPlayer();
            int mapid = chr.getMapId();
            if (chr.hasEntered(mapScriptPath, mapid)) {
                return false;
            } else {
                chr.enteredScript(mapScriptPath, mapid);
            }
        }

        Invocable iv = scripts.get(mapScriptPath);
        if (iv != null) {
            try {
                iv.invokeFunction("start", new MapScriptMethods(c));
                return true;
            } catch (final ScriptException | NoSuchMethodException e) {
                log.error("Error running map script {}", mapScriptPath, e);
                return false;
            }
        }

        try {
            Invocable created = (Invocable) getInvocableScriptEngine("map/" + mapScriptPath + ".js");
            if (created == null) {
                return false;
            }

            // Two players can load the same script at once; both must end up using one engine.
            scripts.putIfAbsent(mapScriptPath, SynchronizedInvocable.of(created));
            scripts.get(mapScriptPath).invokeFunction("start", new MapScriptMethods(c));
            return true;
        } catch (final Exception e) {
            log.error("Error running map script {}", mapScriptPath, e);
        }

        return false;
    }
}