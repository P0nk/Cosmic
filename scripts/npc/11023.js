var status;

function start() {
    cm.dispose();  // Close any previous interactions before opening the new NPC
    cm.openNpc(1101000, "karmareward");  // Redirect to SubordinateNPC.js
}

function action(mode, type, selection) {
    // This section is kept for any future expansions or debugging
    if (mode == -1) {
        cm.dispose();  // Close interaction if needed
    } else {
        cm.dispose();  // Close interaction for the time being
    }
}
