load("scripts/lib.js");
load("scripts/npc/interactions/common/base.js");
load('scripts/npc/interactions/ExpeditionLeaderboardInteraction.js');

let status;
let currentInteraction;

function start() {
    status = 0;
    action(1, 0, 0);
}

function action(mode, type, selection) {
    if (cm.isEndChat(mode, type)) {
        cm.dispose();
        return;
    }

    if (mode === 1) {
        status++;
    } else {
        status--;
    }

    currentInteraction = currentInteraction || new ExpeditionLeaderboardInteraction(cm);

    interactOrDispose(mode, type, selection);
}

function interactOrDispose(mode, type, selection) {
    if (currentInteraction && status > -1) {
        let result = currentInteraction.action(mode, type, selection, status);

        switch (result) {
            case ScriptInteractionResult.NoHandler:
                cm.logToConsole("Interaction Failed: No handler found for status.")
                cm.dispose();
                return;
            case ScriptInteractionResult.HandlerError:
                cm.logToConsole(`Interaction Failed: An error occurred while executing a handler for status ${status}`)
                cm.dispose();
                return;
            case ScriptInteractionResult.Dispose:
                cm.dispose();
                return;
        }

        status = result;
    } else {
        cm.dispose();
    }
}