package tools;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class FilePrinter {

    public static final String
            AUTOBAN_WARNING = "game/AutoBanWarning.txt",    // log naming version by Vcoc
            AUTOBAN_DC = "game/AutoBanDC.txt",
            ACCOUNT_STUCK = "players/AccountStuck.txt",
            COMMAND_GM = "reports/Gm.txt",
            COMMAND_BUG = "reports/Bug.txt",
            LOG_TRADE = "interactions/Trades.txt",
            LOG_EXPEDITION = "interactions/Expeditions.txt",
            LOG_LEAF = "interactions/MapleLeaves.txt",
            LOG_GACHAPON = "interactions/Gachapon.txt",
            LOG_CHAT = "interactions/ChatLog.txt",
            LOG_NPC_TRANSACTIONS = "interactions/NPC_Transactions.txt",
            LOG_RARE_ITEM_TRANSACTIONS = "interactions/Rare_Item_Transactions.txt",
            EVENTBANK_CLAIM = "game/eventbank/",
            QUEST_RESTORE_ITEM = "game/QuestItemRestore.txt",
            EXCEPTION_CAUGHT = "game/ExceptionCaught.txt",
            EXCEPTION_SPECIFIC = "game/Exceptions/",
            CLIENT_START = "game/ClientStartError.txt",
            MAPLE_MAP = "game/MapleMap.txt",
            ERROR38 = "game/Error38.txt",
            PACKET_LOG = "game/Log.txt",
            CASHITEM_BOUGHT = "interactions/CashLog.txt",
            EXCEPTION = "game/Exceptions.txt",
            LOGIN_EXCEPTION = "game/LoginExceptions.txt",
            TRADE_EXCEPTION = "game/TradeExceptions.txt",
            SQL_EXCEPTION = "game/SqlExceptions.txt",
            PACKET_HANDLER = "game/packethandler/",
            PORTAL = "game/portals/",
            PORTAL_STUCK = "game/portalblocks/",
            NPC = "game/npcs/",
            ITEM_SCRIPTS = "game/iScripts/",
            INVOCABLE = "game/invocable/",
            REACTOR = "game/reactors/",
            QUEST = "game/quests/",
            ITEM = "game/items/",
            MOB_MOVEMENT = "game/MobMovement.txt",
            MAP_SCRIPT = "game/mapscript/",
            DIRECTION = "game/directions/",
            GUILD_CHAR_ERROR = "guilds/GuildCharError.txt",
            SAVE_CHAR = "players/SaveToDB.txt",
            INSERT_CHAR = "players/InsertCharacter.txt",
            LOAD_CHAR = "players/LoadCharFromDB.txt",
            CREATED_CHAR = "players/createdchars/",
            DELETED_CHAR = "players/deletedchars/",
            UNHANDLED_EVENT = "game/DoesNotExist.txt",
            SESSION = "players/Sessions.txt",
            DCS = "game/disconnections/",
            EXPLOITS = "game/exploits/",
            CASHSHOP_EXPLOITS = "game/exploits/cashshop/",
            STORAGE = "game/storage/",
            PACKET_LOGS = "game/packetlogs/",
            PACKET_STREAM = "game/packetstream/",
            FREDRICK = "game/npcs/fredrick/",
            NPC_UNCODED = "game/npcs/UncodedNPCs.txt",
            QUEST_UNCODED = "game/quests/",
            AUTOSAVING_CHARACTER = "players/SaveCharAuto.txt",
            SAVING_CHARACTER = "players/SaveChar.txt",
            CHANGE_CHARACTER_NAME = "players/NameChange.txt",
            WORLD_TRANSFER = "players/WorldTransfer.txt",
            FAMILY_ERROR = "players/FamilyErrors.txt",
            USED_COMMANDS = "commands/UsedCommands.txt",
            DEADLOCK_ERROR = "deadlocks/Deadlocks.txt",
            DEADLOCK_STACK = "deadlocks/Path.txt",
            DEADLOCK_LOCKS = "deadlocks/Locks.txt",
            DEADLOCK_STATE = "deadlocks/State.txt",
            DISPOSED_LOCKS = "deadlocks/Disposed.txt",
            GML = "players/GML.txt",
            VP = "players/VP.txt",
            GM_COMMANDS = "commands/gms.txt",
            DAILY = "players/Daily.txt",
            SELL_ALL_ITEMS = "players/SellAllItems/",
            MAPLE_TREE_DONOR = "players/MapleTreeDonor/",
            ExceptionLogErrors = "ClientExceptionLog.txt",
            MissingHandlers = "MissingHandlers/",
            MissingUseCashItems = "cash/MissingUseCashItems.txt",
            MissingCashActions = "cash/MissingCashActions.txt";

    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd"); //for file system purposes, it's nice to use yyyy-MM-dd
    private static final String FILE_PATH = "logs/" + sdf.format(Calendar.getInstance().getTime()) + "/"; // + sdf.format(Calendar.getInstance().getTime()) + "/"
    private static final String ERROR = "error/";

    public static void printError(final String name, final Throwable t) {
        String stringT = getString(t);

        System.out.println("Error thrown: " + name);
        System.out.println(stringT);
        System.out.println();
        FileOutputStream out = null;
        final String file = FILE_PATH + ERROR + name;
        try {
            File outputFile = new File(file);
            if (outputFile.getParentFile() != null) {
                outputFile.getParentFile().mkdirs();
            }
            out = new FileOutputStream(file, true);
            out.write(stringT.getBytes());
            out.write("\r\n---------------------------------\r\n".getBytes());
            out.write("\r\n".getBytes()); // thanks Vcoc for suggesting review body log structure
        } catch (IOException ess) {
            ess.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException ignore) {
                ignore.printStackTrace();
            }
        }
    }

    public static void printError(final String name, final Throwable t, final String info) {
        String stringT = getString(t);

        System.out.println("Error thrown: " + name);
        System.out.println(stringT);
        System.out.println();
        FileOutputStream out = null;
        final String file = FILE_PATH + ERROR + name;
        try {
            File outputFile = new File(file);
            if (outputFile.getParentFile() != null) {
                outputFile.getParentFile().mkdirs();
            }
            out = new FileOutputStream(file, true);
            out.write((info + "\r\n").getBytes());
            out.write(stringT.getBytes());
            out.write("\r\n---------------------------------\r\n".getBytes());
            out.write("\r\n".getBytes());
        } catch (IOException ess) {
            ess.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException ignore) {
                ignore.printStackTrace();
            }
        }
    }

    public static void printError(final String name, final String s) {
        System.out.println("Error thrown: " + name);
        System.out.println(s);
        System.out.println();
        FileOutputStream out = null;
        final String file = FILE_PATH + ERROR + name;
        try {
            File outputFile = new File(file);
            if (outputFile.getParentFile() != null) {
                outputFile.getParentFile().mkdirs();
            }
            out = new FileOutputStream(file, true);
            out.write(s.getBytes());
            //out.write("\r\n---------------------------------\r\n".getBytes());
            out.write("\r\n".getBytes());
        } catch (IOException ess) {
            ess.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException ignore) {
                ignore.printStackTrace();
            }
        }
    }

    public static void print(final String name, final String s) {
        print(name, s, true);
    }

    public static void print(final String name, final String s, boolean line) {
        if (name != FilePrinter.USED_COMMANDS && name != FilePrinter.LOG_CHAT && name != FilePrinter.LOG_GACHAPON && name != FilePrinter.SAVE_CHAR && name != FilePrinter.AUTOSAVING_CHARACTER && name != FilePrinter.SAVING_CHARACTER) {
            System.out.println("Log: " + name);
            System.out.println(s);

            System.out.println();
        }
        FileOutputStream out = null;
        String file = FILE_PATH + name;
        try {
            File outputFile = new File(file);
            if (outputFile.getParentFile() != null) {
                outputFile.getParentFile().mkdirs();
            }
            out = new FileOutputStream(file, true);
            out.write(s.getBytes());
            if (line) {
                out.write("\r\n---------------------------------\r\n".getBytes());
            }
            out.write("\r\n".getBytes());
        } catch (IOException ess) {
            ess.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException ignore) {
                ignore.printStackTrace();
            }
        }
    }

    private static String getString(final Throwable e) {
        String retValue = null;
        StringWriter sw = null;
        PrintWriter pw = null;
        try {
            sw = new StringWriter();
            pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            retValue = sw.toString();
        } finally {
            try {
                if (pw != null) {
                    pw.close();
                }
                if (sw != null) {
                    sw.close();
                }
            } catch (IOException ignore) {
                ignore.printStackTrace();
            }
        }
        return retValue;
    }
}