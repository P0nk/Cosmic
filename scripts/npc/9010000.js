/*
    Forgiving Bot Check NPC
    - Wrong Answer: Asks again.
    - ESC / Cancel: JAIL (The Trap).
*/

var status = -1;
var firstNum = 0;
var secondNum = 0;
var answer = 0;
var attempts = 0;

function start() {
    generateMath();
    action(1, 0, 0);
}

function action(mode, type, selection) {
    // [THE TRAP]
    // If they press ESC (mode 0) or close the window, we jail them.
    // Legitimate players MUST answer the question to leave.
    if (mode == 0) {
        cm.sendOk("Security Verification Failed. \r\nYou attempted to bypass the check.");
        cm.getChar().getAutobanManager().jailPlayer("Bot Check Bypass (ESC/Cancel)", 60);
        cm.dispose();
        return;
    }

    status++;

    if (status == 0) {
        // Ask the Math Question
        cm.sendGetNumber("Security Verification:\r\nWhat is #b" + firstNum + " + " + secondNum + "#k?\r\n(Please answer to continue playing)", 0, 0, 200);

    } else if (status == 1) {
        if (selection == answer) {
            // Correct Answer
            cm.sendOk("Verification passed. Happy Mapling!");
            cm.dispose();
        } else {
            // Wrong Answer - Just loop back
            attempts++;
            status = -1; // Reset to start
            generateMath(); // Optional: Generate new numbers so they can't spam one key
            cm.sendNext("That answer was incorrect. Please try again.");
        }
    }
}

function generateMath() {
    firstNum = Math.floor(Math.random() * 50);
    secondNum = Math.floor(Math.random() * 50);
    answer = firstNum + secondNum;
}