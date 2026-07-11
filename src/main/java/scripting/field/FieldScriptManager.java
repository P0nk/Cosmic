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

import client.Client;
import net.server.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scripting.AbstractScriptManager;
import scripting.event.EventManager;
import scripting.npc.NPCConversationManager;
import scripting.npc.NPCScriptManager;
import server.ItemInformationProvider.ScriptedItem;

import javax.script.Invocable;
import javax.script.ScriptEngine;

public class FieldScriptManager extends AbstractScriptManager {
    private static final Logger log = LoggerFactory.getLogger(FieldScriptManager.class);

    private static class FieldScriptEntry {

        public FieldScriptEntry(Invocable iv, EventManager em) {
            this.iv = iv;
            this.em = em;
        }

        public Invocable iv;
        public EventManager em;
    }

    public FieldScriptManager(Channel channel, String[] scripts) {

    }

    public void runFieldScript(Client c, ScriptedItem scriptItem) {
        NPCScriptManager.getInstance().start(c, scriptItem, null);
    }

    public boolean isFieldScriptAvailable(Client c, String fileName) {
        ScriptEngine engine = null;
        if (fileName != null) {
            engine = getInvocableScriptEngine("map/field/" + fileName + ".js", c);
        }

        return engine != null;
    }


}