/*
    Event Organizer NPC
    Triggered by: !event command
*/

var status = -1;
var manualEventPlayers = 50;

// List your automated Event Script names here (filenames without .js)
var autoEvents = [
    "OlaOla",
    "MapleFitness",
    "Snowball",
    "OxQuiz",
    "Coconut"
];

function start() {
    action(1, 0, 0);
}

function action(mode, type, selection) {
    if (mode == -1) {
        cm.dispose();
        return;
    }
    if (mode == 0 && status == 0) {
        cm.dispose();
        return;
    }
    if (mode == 1) status++;
    else status--;

    if (status == 0) {
        var msg = "#e<Event Management>#n\r\nWhat would you like to do?";
        msg += "\r\n#L0#Start an #bAutomated Event#k (Scripted)#l";
        msg += "\r\n#L1#Host a #rManual Event#k (On this map)#l";
        msg += "\r\n#L2#Close current Manual Event#l";
        cm.sendSimple(msg);
    }
    else if (status == 1) {
        if (selection == 0) {
            // --- Automated Event Selection ---
            var msg = "Select an event to start:\r\n";
            for (var i = 0; i < autoEvents.length; i++) {
                msg += "#L" + i + "#" + autoEvents[i] + "#l\r\n";
            }
            cm.sendSimple(msg);
        }
        else if (selection == 1) {
            // --- Manual Event Setup ---
            status = 9; // Jump to Manual Section
            cm.sendGetNumber("How many players should be allowed to join?", 50, 1, 200);
        }
        else if (selection == 2) {
            // --- Close Event ---
            var ch = cm.getClient().getChannelServer();
            ch.setEvent(null);
            cm.sendOk("The manual event entry has been closed.");
            cm.dispose();
        }
    }
    else if (status == 2) {
        // --- Execute Automated Event ---
        var eventName = autoEvents[selection];
        var em = cm.getEventManager(eventName);

        if (em == null) {
            cm.sendOk("Could not find the event script: " + eventName);
        } else {
            // Start the event
            em.startInstance(cm.getPlayer());
            cm.mapMessage(6, "[Event] " + eventName + " has been started by " + cm.getPlayer().getName());
        }
        cm.dispose();
    }
    else if (status == 10) {
        // --- Execute Manual Event ---
        manualEventPlayers = selection;

        // Java Class References
        // Note: Using Packages because NPCScriptManager usually doesn't have the new global injections yet
        var Event = Java.type("server.events.gm.Event");
        var Server = Java.type("net.server.Server");
        var PacketCreator = Java.type("tools.PacketCreator");

        // Set the event on the channel
        var ch = cm.getClient().getChannelServer();
        ch.setEvent(new Event(cm.getPlayer().getMapId(), manualEventPlayers));

        // Broadcast to World
        var mapName = cm.getPlayer().getMap().getMapName();
        var msg = "[Event] An event has started on " + mapName + " and will allow " + manualEventPlayers + " players to join. Type @joinevent to participate.";

        Server.getInstance().broadcastMessage(cm.getWorld(), PacketCreator.earnTitleMessage(msg));
        Server.getInstance().broadcastMessage(cm.getWorld(), PacketCreator.serverNotice(6, msg));

        cm.sendOk("Event opened on #b" + mapName + "#k for " + manualEventPlayers + " players.");
        cm.dispose();
    }
}