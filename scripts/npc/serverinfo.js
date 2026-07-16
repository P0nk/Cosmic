var status = -1;

function start() {
    cm.sendOk("Hello! Here are the available commands and their explanations:\r\n\r\n" +
        "#b@worldchat#k - Enables or disables world chat chatting.\r\n" +
        "#b@worldchat <toggle>#k - Toggles the visibility of the world chat.\r\n" +
        "#b@exchange#k - Allows you to exchange ores at a 2:1 ratio.\r\n" +
        "#b@check#k - Shows the current time, daily and weekly reset times, and server rates.\r\n" +
        "#b@qs#k - Lets you select items to sell in bulk.\r\n" +
        "#b@mule#k - Opens the Mule system.\r\n" +
        "#b@Muletoggle#k - Enables or disables direct looting into the Mule.\r\n" +
        "#b@shop#k - Opens the all-in-one shop for various items.\r\n" +
        "#b@elb#k - Displays the Expedition Leaderboard with daily and weekly ranks.\r\n" +
        "#b@bb#k - Allows you to buy back recently sold items.\r\n" +
        "#b@gfx <on/off>#k - Enables or disables skill effects.\r\n" +
        "#b@wd <item name>#k - Shows who drops the specified item.\r\n" +
        "#b@wdf <monster name>#k - Displays what items are dropped by the specified monster.\r\n\r\n" +
        "If you have any questions about a specific command, feel free to ask!");
    cm.dispose(); // Close the NPC conversation after displaying the information
}

function action(mode, type, selection) {
    cm.dispose(); // Ensures the NPC conversation is ended after showing the message
}
