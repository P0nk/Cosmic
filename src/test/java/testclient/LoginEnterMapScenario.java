package testclient;

import net.opcodes.RecvOpcode;
import net.opcodes.SendOpcode;

import java.net.InetAddress;

import static testclient.ScenarioContext.check;

/**
 * Login server: password login, world list, character list (creates "QaBot" if the account has none), character
 * select. Channel server: PLAYER_LOGGEDIN, wait for SET_FIELD (the map-entry packet), then close the socket.
 */
public final class LoginEnterMapScenario implements Scenario {
    static final String BOT_NAME = "QaBot";
    // The real client sends these; the server only validates their format (12 hex mac + '_' + 8 hex hwid).
    private static final String MACS = "00-11-22-33-44-55";
    private static final String HOST_STRING = "001122334455_AABBCCDD";

    @Override
    public String run(ScenarioContext ctx) throws Exception {
        int charId;
        String charName;
        int mapId;
        int enteredMapId;
        String channelHost;
        int channelPort;

        // ---- login server ----
        try (HeadlessClient login = new HeadlessClient(ctx.host, ctx.loginPort, ctx.logger())) {
            login.send(RecvOpcode.LOGIN_PASSWORD, w -> w.writeString(ctx.account).writeString(ctx.password)
                    .writeBytes(new byte[6])                              // masked part of the hwid
                    .writeBytes(new byte[]{(byte) 0xAA, (byte) 0xBB, (byte) 0xCC, (byte) 0xDD})); // hwid nibbles
            PacketReader status = login.expect(SendOpcode.LOGIN_STATUS);
            int code = status.readByte();
            if (code == 23) { // terms of service not yet accepted for this account
                ctx.log("accepting terms of service");
                login.send(RecvOpcode.ACCEPT_TOS, w -> w.writeByte(1));
                status = login.expect(SendOpcode.LOGIN_STATUS);
                code = status.readByte();
            }
            check(code == 0, "login rejected, LOGIN_STATUS code " + code + " (4=bad password, 5=no such account, 7=already logged in)");
            ctx.log("logged in as " + ctx.account);

            login.send(RecvOpcode.SERVERLIST_REQUEST, w -> { });
            int worlds = 0;
            while (true) {
                PacketReader sl = login.expect(SendOpcode.SERVERLIST);
                int id = sl.readByte();
                if (id == 0xFF) {
                    break; // end-of-list marker
                }
                ctx.log("world " + id + ": " + sl.readString());
                worlds++;
            }
            check(worlds > 0, "server list was empty");

            login.send(RecvOpcode.CHARLIST_REQUEST, w -> w.writeByte(2).writeByte(ctx.world).writeByte(ctx.channel));
            PacketReader list = login.expect(SendOpcode.CHARLIST);
            check(list.readByte() == 0, "CHARLIST status not 0");
            int count = list.readByte();
            ctx.log("account has " + count + " character(s)");

            PacketReader entry;
            if (count == 0) {
                entry = createCharacter(login, ctx);
            } else {
                entry = list; // cursor is at the first entry; later entries are ignored on purpose
            }
            charId = entry.readInt();
            charName = entry.readFixedString(13);
            mapId = readMapId(entry);
            ctx.log("selecting character " + charName + " (id " + charId + ", map " + mapId + ")");

            final int selectedId = charId;
            login.send(RecvOpcode.CHAR_SELECT, w -> w.writeInt(selectedId).writeString(MACS).writeString(HOST_STRING));
            PacketReader ip = login.expect(SendOpcode.SERVER_IP);
            check(ip.readShort() == 0, "SERVER_IP error code (account/IP/session coordinator refused the select)");
            byte[] addr = ip.readBytes(4);
            channelPort = ip.readShort();
            check(ip.readInt() == charId, "SERVER_IP returned a different character id");
            channelHost = InetAddress.getByAddress(addr).getHostAddress();
            ctx.log("channel is " + channelHost + ":" + channelPort);
        }

        // ---- channel server ----
        try (HeadlessClient game = new HeadlessClient(channelHost, channelPort, ctx.logger())) {
            game.send(RecvOpcode.PLAYER_LOGGEDIN, w -> w.writeInt(charId));
            PacketReader setField = game.expect(SendOpcode.SET_FIELD);
            ctx.log("SET_FIELD received (" + setField.available() + " bytes)");
            // Login-time SET_FIELD (PacketCreator.getCharInfo): channel, 1, 1, short 0, 3 random ints, then the full
            // character data, whose stats block carries the map the server put the character in.
            setField.skip(4 + 1 + 1 + 2 + 12);
            setField.skip(8 + 1);   // inventory-flags long, 0 byte
            int enteredId = setField.readInt();
            String enteredName = setField.readFixedString(13);
            int enteredMap = readMapIdAfterName(setField);
            check(enteredId == charId, "SET_FIELD is for character id " + enteredId + ", expected " + charId);
            check(enteredName.equals(charName), "SET_FIELD is for character '" + enteredName + "', expected '" + charName + "'");
            check(mapId > 0, "could not read the saved map id from the character list");
            check(enteredMap == mapId, "SET_FIELD map " + enteredMap + " != saved map " + mapId);
            ctx.log("ENTERED MAP " + enteredMap + " (" + mapName(ctx.wzPath, enteredMap) + ")");

            // Prove the server really placed us in a map: say something and expect the map broadcast back to us.
            String token = "qa-" + Long.toHexString(System.nanoTime());
            game.send(RecvOpcode.GENERAL_CHAT, w -> w.writeString(token).writeByte(0));
            while (true) {
                PacketReader chat = game.expect(SendOpcode.CHATTEXT);
                int from = chat.readInt();
                chat.readByte();
                String text = chat.readString();
                if (from == charId && text.equals(token)) {
                    break;
                }
            }
            ctx.log("map chat echo OK: server delivered our chat to everyone in map " + enteredMap);
            enteredMapId = enteredMap;
        } // close == clean logout

        return "character=" + charName + " id=" + charId + " entered-map=" + enteredMapId + " channel=" + (ctx.channel + 1);
    }

    /** CHECK_CHAR_NAME then CREATE_CHAR a Beginner; returns the ADD_NEW_CHAR_ENTRY cursor at the entry start. */
    private static PacketReader createCharacter(HeadlessClient login, ScenarioContext ctx) throws Exception {
        login.send(RecvOpcode.CHECK_CHAR_NAME, w -> w.writeString(BOT_NAME));
        PacketReader name = login.expect(SendOpcode.CHAR_NAME_RESPONSE);
        name.readString();
        check(name.readByte() == 0, "character name " + BOT_NAME + " is taken by another account");
        login.send(RecvOpcode.CREATE_CHAR, w -> w.writeString(BOT_NAME)
                .writeInt(1)        // 0 Cygnus, 1 Adventurer, 2 Aran
                .writeInt(20000)    // face
                .writeInt(30000)    // hair
                .writeInt(0)        // hair color
                .writeInt(0)        // skin
                .writeInt(1040002)  // top
                .writeInt(1060002)  // bottom
                .writeInt(1072001)  // shoes
                .writeInt(1302000)  // weapon
                .writeByte(0));     // gender
        PacketReader added = login.expect(SendOpcode.ADD_NEW_CHAR_ENTRY);
        check(added.readByte() == 0, "CREATE_CHAR failed");
        ctx.log("created character " + BOT_NAME);
        return added;
    }

    /** Cursor is just after the 13-byte name; returns the saved map id from CharacterStats, or -1 if unreadable. */
    private static int readMapId(PacketReader e) {
        try {
            return readMapIdAfterName(e);
        } catch (RuntimeException ex) {
            return -1;
        }
    }

    /** Cursor just after the 13-byte name: skip through CharacterStats and read its map id. */
    static int readMapIdAfterName(PacketReader e) {
        e.skip(1 + 1 + 4 + 4 + 24); // gender, skin, face, hair, 3 pet ids
        e.skip(1);                  // level
        int job = e.readShort();
        e.skip(4 * 2 + 4 * 2);      // str dex int luk, hp maxhp mp maxmp
        e.skip(2);                  // ap
        if (job == 2001 || (job >= 2200 && job <= 2218)) {
            e.skip(e.readByte() * 2); // Evan: SP table (count, then id+amount pairs)
        } else {
            e.skip(2);              // sp
        }
        e.skip(4 + 2 + 4);          // exp, fame, gacha exp
        return e.readInt();
    }

    /** Map display name from the server's String.wz export ("Street - Map"), or "unknown" if not found. */
    static String mapName(String wzPath, int mapId) {
        try {
            java.nio.file.Path f = java.nio.file.Path.of(wzPath, "String.wz", "Map.img.xml");
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("<imgdir name=\"" + mapId + "\">\\s*<string name=\"streetName\" value=\"([^\"]*)\"/>\\s*<string name=\"mapName\" value=\"([^\"]*)\"")
                    .matcher(java.nio.file.Files.readString(f));
            return m.find() ? m.group(1) + " - " + m.group(2) : "unknown";
        } catch (Exception ex) {
            return "unknown";
        }
    }
}
