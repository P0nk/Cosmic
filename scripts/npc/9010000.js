/* Filename: scripts/npc/9901000.js
   Function: Bot Check (Sticky Mode)
   - Wrong Answer: Asks again.
   - ESC / Close: Re-opens the window (Infinite Loop) until answered.
*/

var status = -1;
var firstNum = 0;
var secondNum = 0;
var answer = 0;

function start() {
    generateMath();
    action(1, 0, 0);
}

function action(mode, type, selection) {
    // [STICKY LOGIC]
    // If mode is 0 (ESC) or -1, they tried to close the window.
    // Instead of Jailing, we catch it and force the loop to restart.
    if (mode != 1) {
        // Reset status so the next 'Next' click brings them back to the start
        status = -1;
        cm.sendNext("Security Verification Required. \r\nYou cannot close this window until you answer the question.");
        return;
    }

    status++;

    if (status == 0) {
        // Ask the Math Question
        // "selection" is irrelevant here, we just show the prompt.
        cm.sendGetNumber("Security Verification:\r\nWhat is #b" + firstNum + " + " + secondNum + "#k?", 0, 0, 200);

    } else if (status == 1) {
        // selection contains the number they typed
        if (selection == answer) {
            // Correct Answer
            cm.sendOk("Verification passed. Happy Mapling!");
            cm.dispose(); // Releases the player
        } else {
            // Wrong Answer - Loop back
            status = -1; // Reset to -1 so next click goes to 0 (Math Question)
            generateMath(); // Generate new numbers to prevent macroing one key
            cm.sendNext("That answer was incorrect. Please try again.");
        }
    }
}

function generateMath() {
    firstNum = Math.floor(Math.random() * 50); // 0-49
    secondNum = Math.floor(Math.random() * 50); // 0-49
    answer = firstNum + secondNum;
}