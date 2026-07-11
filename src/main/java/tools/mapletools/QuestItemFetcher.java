package tools.mapletools;

import provider.wz.WZFiles;
import server.ItemInformationProvider;
import tools.DatabaseConnection;
import tools.Pair;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * @author RonanLana
 * <p>
 * This application has 2 objectives: fetch missing drop data relevant to quests,
 * and update the questid from items that are labeled as "Quest Item" on the DB.
 * <p>
 * Running it should generate a report file under "output" folder with the search results.
 * <p>
 * Estimated parse time: 1 minute
 */
public class QuestItemFetcher {
    private static final Path OUTPUT_FILE = ToolConstants.getOutputFile("quest_report.txt");
    private static final Collection<String> RELEVANT_FILE_EXTENSIONS = Set.of(".sql", ".js", ".txt", ".java");
    private static final int INITIAL_STRING_LENGTH = 50;
    private static final int INITIAL_LENGTH = 200;
    private static final boolean DISPLAY_EXTRA_INFO = true;     // display items with zero quantity over the quest act WZ

    private static final Connection con = SimpleDatabaseConnection.getConnection();
    private static final Map<Integer, Set<Integer>> startQuestItems = new HashMap<>(INITIAL_LENGTH);
    private static final Map<Integer, Set<Integer>> completeQuestItems = new HashMap<>(INITIAL_LENGTH);
    private static final Map<Integer, Set<Integer>> zeroedStartQuestItems = new HashMap<>();
    private static final Map<Integer, Set<Integer>> zeroedCompleteQuestItems = new HashMap<>();
    private static final Map<Integer, int[]> mixedQuestidItems = new HashMap<>();
    private static final Set<Integer> limitedQuestids = new HashSet<>();

    private static ItemInformationProvider ii;
    private static PrintWriter printWriter = null;
    private static BufferedReader bufferedReader = null;
    private static byte status = 0;
    private static int questId = -1;
    private static int isCompleteState = 0;
    private static int currentItemid = 0;
    private static int currentCount = 0;

    private static String getName(String token) {
        int i, j;
        char[] dest;
        String d;

        i = token.lastIndexOf("name");
        i = token.indexOf("\"", i) + 1; //lower bound of the string
        j = token.indexOf("\"", i);     //upper bound

        if (j < i) {
            return "0";           //node value containing 'name' in it's scope, cheap fix since we don't deal with strings anyway
        }

        dest = new char[INITIAL_STRING_LENGTH];
        token.getChars(i, j, dest, 0);

        d = new String(dest);
        return (d.trim());
    }

    private static String getValue(String token) {
        int i, j;
        char[] dest;
        String d;

        i = token.lastIndexOf("value");
        i = token.indexOf("\"", i) + 1; //lower bound of the string
        j = token.indexOf("\"", i);     //upper bound

        dest = new char[INITIAL_STRING_LENGTH];
        token.getChars(i, j, dest, 0);

        d = new String(dest);
        return (d.trim());
    }

    private static void forwardCursor(int st) {
        String line = null;

        try {
            while (status >= st && (line = bufferedReader.readLine()) != null) {
                simpleToken(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void simpleToken(String token) {
        if (token.contains("/imgdir")) {
            status -= 1;
        } else if (token.contains("imgdir")) {
            status += 1;
        }
    }

    private static void inspectQuestItemList(int st) {
        String line = null;

        try {
            while (status >= st && (line = bufferedReader.readLine()) != null) {
                readItemToken(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void processCurrentItem() {
        try {
            if (ii.isQuestItem(currentItemid)) {
                if (currentCount != 0) {
                    if (isCompleteState == 1) {
                        if (currentCount < 0) {
                            Set<Integer> qi = completeQuestItems.get(questId);
                            if (qi == null) {
                                Set<Integer> newSet = new HashSet<>();
                                newSet.add(currentItemid);

                                completeQuestItems.put(questId, newSet);
                            } else {
                                qi.add(currentItemid);
                            }
                        }
                    } else {
                        if (currentCount > 0) {
                            Set<Integer> qi = startQuestItems.get(questId);
                            if (qi == null) {
                                Set<Integer> newSet = new HashSet<>();
                                newSet.add(currentItemid);

                                startQuestItems.put(questId, newSet);
                            } else {
                                qi.add(currentItemid);
                            }
                        }
                    }
                } else {
                    if (isCompleteState == 1) {
                        Set<Integer> qi = zeroedCompleteQuestItems.get(questId);
                        if (qi == null) {
                            Set<Integer> newSet = new HashSet<>();
                            newSet.add(currentItemid);

                            zeroedCompleteQuestItems.put(questId, newSet);
                        } else {
                            qi.add(currentItemid);
                        }
                    } else {
                        Set<Integer> qi = zeroedStartQuestItems.get(questId);
                        if (qi == null) {
                            Set<Integer> newSet = new HashSet<>();
                            newSet.add(currentItemid);

                            zeroedStartQuestItems.put(questId, newSet);
                        } else {
                            qi.add(currentItemid);
                        }
                    }
                }
            }
        } catch (Exception e) {
        }
    }

    private static void readItemToken(String token) {
        if (token.contains("/imgdir")) {
            status -= 1;

            processCurrentItem();

            currentItemid = 0;
            currentCount = 0;
        } else if (token.contains("imgdir")) {
            status += 1;
        } else {
            String d = getName(token);

            if (d.equals("id")) {
                currentItemid = Integer.parseInt(getValue(token));
            } else if (d.equals("count")) {
                currentCount = Integer.parseInt(getValue(token));
            }
        }
    }

    private static void translateActToken(String token) {
        String d;
        int temp;

        if (token.contains("/imgdir")) {
            status -= 1;
        } else if (token.contains("imgdir")) {
            if (status == 1) {           //getting QuestId
                d = getName(token);
                questId = Integer.parseInt(d);
            } else if (status == 2) {      //start/complete
                d = getName(token);
                isCompleteState = Integer.parseInt(d);
            } else if (status == 3) {
                d = getName(token);

                if (d.contains("item")) {
                    temp = status;
                    inspectQuestItemList(temp);
                } else {
                    forwardCursor(status);
                }
            }

            status += 1;
        } else {
            if (status == 3) {
                d = getName(token);

                if (d.equals("end")) {
                    limitedQuestids.add(questId);
                }
            }
        }
    }

    private static void translateCheckToken(String token) {
        String d;

        if (token.contains("/imgdir")) {
            status -= 1;
        } else if (token.contains("imgdir")) {
            if (status == 1) {           //getting QuestId
                d = getName(token);
                questId = Integer.parseInt(d);
            } else if (status == 2) {      //start/complete
                d = getName(token);
                isCompleteState = Integer.parseInt(d);
            } else if (status == 3) {
                forwardCursor(status);
            }

            status += 1;
        } else {
            if (status == 3) {
                d = getName(token);

                if (d.equals("end")) {
                    limitedQuestids.add(questId);
                }
            }
        }
    }

    private static void calculateQuestItemDiff() {
        // This will remove started quest items from the "to complete" item set.

        for (Map.Entry<Integer, Set<Integer>> qd : startQuestItems.entrySet()) {
            for (Integer qi : qd.getValue()) {
                Set<Integer> questSet = completeQuestItems.get(qd.getKey());

                if (questSet != null) {
                    if (questSet.remove(qi)) {
                        if (completeQuestItems.isEmpty()) {
                            completeQuestItems.remove(qd.getKey());
                        }
                    }
                }
            }
        }
    }

    private static List<Pair<Integer, Integer>> getPairsQuestItem() {   // quest items not gained at WZ's quest start
        List<Pair<Integer, Integer>> list = new ArrayList<>(INITIAL_LENGTH);

        for (Map.Entry<Integer, Set<Integer>> qd : completeQuestItems.entrySet()) {
            for (Integer qi : qd.getValue()) {
                list.add(new Pair<>(qi, qd.getKey()));
            }
        }

        return list;
    }

    private static String getTableName(boolean dropdata) {
        return dropdata ? "drop_data" : "reactordrops";
    }

    private static void filterQuestDropsOnTable(Pair<Integer, Integer> iq, List<Pair<Integer, Integer>> itemsWithQuest, boolean dropdata) throws SQLException {
        PreparedStatement ps = con.prepareStatement("SELECT questid FROM " + getTableName(dropdata) + " WHERE itemid = ?;");
        ps.setInt(1, iq.getLeft());
        ResultSet rs = ps.executeQuery();

        if (rs.isBeforeFirst()) {
            while (rs.next()) {
                int curQuest = rs.getInt(1);
                if (curQuest != iq.getRight()) {
                    Set<Integer> sqSet = startQuestItems.get(curQuest);
                    if (sqSet != null && sqSet.contains(iq.getLeft())) {
                        continue;
                    }

                    int[] mixed = new int[3];
                    mixed[0] = iq.getLeft();
                    mixed[1] = curQuest;
                    mixed[2] = iq.getRight();

                    mixedQuestidItems.put(iq.getLeft(), mixed);
                }
            }

            itemsWithQuest.remove(iq);
        }

        rs.close();
        ps.close();
    }

    private static void filterQuestDropsOnDB(List<Pair<Integer, Integer>> itemsWithQuest) throws SQLException {
        List<Pair<Integer, Integer>> copyItemsWithQuest = new ArrayList<>(itemsWithQuest);
        try {
            for (Pair<Integer, Integer> iq : copyItemsWithQuest) {
                filterQuestDropsOnTable(iq, itemsWithQuest, true);
                filterQuestDropsOnTable(iq, itemsWithQuest, false);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void filterDirectorySearchMatchingData(String filePath, List<Pair<Integer, Integer>> itemsWithQuest) {
        try {
            Files.walk(Path.of(filePath))
                    .filter(QuestItemFetcher::isRelevantFile)
                    .forEach(path -> fileSearchMatchingData(path, itemsWithQuest));
        } catch (IOException e) {
            throw new RuntimeException("Error during recursive file walk", e);
        }
    }

    private static boolean isRelevantFile(Path file) {
        String fileName = file.getFileName().toString();
        return RELEVANT_FILE_EXTENSIONS.stream().anyMatch(fileName::endsWith);
    }

    private static boolean foundMatchingDataOnFile(String fileContent, String searchStr) {
        return fileContent.contains(searchStr);
    }

    private static void fileSearchMatchingData(Path file, List<Pair<Integer, Integer>> itemsWithQuest) {
        try {
            String fileContent = Files.readString(file);

            List<Pair<Integer, Integer>> copyItemsWithQuest = new ArrayList<>(itemsWithQuest);
            for (Pair<Integer, Integer> iq : copyItemsWithQuest) {
                if (foundMatchingDataOnFile(fileContent, String.valueOf(iq.getLeft()))) {
                    itemsWithQuest.remove(iq);
                }
            }
        } catch (IOException ioe) {
            System.out.println("Failed to read file: " + file.getFileName().toAbsolutePath().toString());
            ioe.printStackTrace();
        }
    }

    private static void printReportFileHeader() {
        printWriter.println(" # Report File autogenerated from the MapleQuestItemFetcher feature by Ronan Lana.");
        printWriter.println(" # Generated data takes into account several data info from the underlying DB, server source files and the server-side WZ.xmls.");
        printWriter.println();
    }

    private static List<Map.Entry<Integer, Integer>> getSortedMapEntries0(Map<Integer, Integer> map) {
        List<Map.Entry<Integer, Integer>> list = new ArrayList<>(map.size());
        list.addAll(map.entrySet());

        list.sort(Comparator.comparingInt(Map.Entry::getKey));

        return list;
    }

    private static List<Map.Entry<Integer, int[]>> getSortedMapEntries1(Map<Integer, int[]> map) {
        List<Map.Entry<Integer, int[]>> list = new ArrayList<>(map.size());
        list.addAll(map.entrySet());

        list.sort(Comparator.comparingInt(Map.Entry::getKey));

        return list;
    }

    private static List<Pair<Integer, List<Integer>>> getSortedMapEntries2(Map<Integer, Set<Integer>> map) {
        List<Pair<Integer, List<Integer>>> list = new ArrayList<>(map.size());
        for (Map.Entry<Integer, Set<Integer>> e : map.entrySet()) {
            List<Integer> il = new ArrayList<>(2);
            il.addAll(e.getValue());

            il.sort(Comparator.comparingInt(o -> o));

            list.add(new Pair<>(e.getKey(), il));
        }

        list.sort(Comparator.comparingInt(Pair::getLeft));

        return list;
    }

    private static String getExpiredStringLabel(int questid) {
        return (!limitedQuestids.contains(questid) ? "" : " EXPIRED");
    }

    private static void reportQuestItemData() {
        // This will reference one line at a time
        String line = null;
        Path file = null;

        try (PrintWriter pw = new PrintWriter(Files.newOutputStream(OUTPUT_FILE))) {
            System.out.println("Reading WZs...");

            file = WZFiles.QUEST.getFile().resolve("Check.img.xml");
            bufferedReader = Files.newBufferedReader(file);

            while ((line = bufferedReader.readLine()) != null) {
                translateCheckToken(line); // fetch expired quests through here as well
            }

            bufferedReader.close();

            file = WZFiles.QUEST.getFile().resolve("Act.img.xml");
            bufferedReader = Files.newBufferedReader(file);

            while ((line = bufferedReader.readLine()) != null) {
                translateActToken(line);
            }

            bufferedReader.close();

            System.out.println("Calculating table diffs...");
            calculateQuestItemDiff();

            System.out.println("Filtering drops on DB...");
            List<Pair<Integer, Integer>> itemsWithQuest = getPairsQuestItem();

            filterQuestDropsOnDB(itemsWithQuest);
            con.close();

            System.out.println("Filtering drops on project files...");
            // finally, filter whether this item is mentioned on the source code or not.
            filterDirectorySearchMatchingData("scripts", itemsWithQuest);
            filterDirectorySearchMatchingData("database/sql", itemsWithQuest);
            filterDirectorySearchMatchingData("src", itemsWithQuest);

            System.out.println("Reporting results...");
            // report suspects of missing quest drop data, as well as those drop data that
            // may have incorrect questids.
            printWriter = pw;

            printReportFileHeader();

            if (!mixedQuestidItems.isEmpty()) {
                printWriter.println("INCORRECT QUESTIDS ON DB");
                for (Map.Entry<Integer, int[]> emqi : getSortedMapEntries1(mixedQuestidItems)) {
                    int[] mqi = emqi.getValue();
                    printWriter.println(mqi[0] + " : " + mqi[1] + " -> " + mqi[2] + getExpiredStringLabel(mqi[2]));
                }
                printWriter.println("\n\n\n\n\n");
            }

            if (!itemsWithQuest.isEmpty()) {
                Map<Integer, Integer> mapIwq = new HashMap<>(itemsWithQuest.size());
                for (Pair<Integer, Integer> iwq : itemsWithQuest) {
                    mapIwq.put(iwq.getLeft(), iwq.getRight());
                }

                printWriter.println("ITEMS WITH NO QUEST DROP DATA ON DB");
                for (Map.Entry<Integer, Integer> iwq : getSortedMapEntries0(mapIwq)) {
                    printWriter.println(iwq.getKey() + " - " + iwq.getValue() + getExpiredStringLabel(iwq.getValue()));
                }
                printWriter.println("\n\n\n\n\n");
            }

            if (DISPLAY_EXTRA_INFO) {
                if (!zeroedStartQuestItems.isEmpty()) {
                    printWriter.println("START QUEST ITEMS WITH ZERO QUANTITY");
                    for (Pair<Integer, List<Integer>> iwq : getSortedMapEntries2(zeroedStartQuestItems)) {
                        printWriter.println(iwq.getLeft() + getExpiredStringLabel(iwq.getLeft()) + ":");
                        for (Integer i : iwq.getRight()) {
                            printWriter.println("  " + i);
                        }
                        printWriter.println();
                    }
                    printWriter.println("\n\n\n\n\n");
                }

                if (!zeroedCompleteQuestItems.isEmpty()) {
                    printWriter.println("COMPLETE QUEST ITEMS WITH ZERO QUANTITY");
                    for (Pair<Integer, List<Integer>> iwq : getSortedMapEntries2(zeroedCompleteQuestItems)) {
                        printWriter.println(iwq.getLeft() + getExpiredStringLabel(iwq.getLeft()) + ":");
                        for (Integer i : iwq.getRight()) {
                            printWriter.println("  " + i);
                        }
                        printWriter.println();
                    }
                    printWriter.println("\n\n\n\n\n");
                }
            }

            System.out.println("Done!");
        } catch (FileNotFoundException ex) {
            System.out.println("Unable to open file '" + file + "'");
        } catch (IOException ex) {
            System.out.println("Error reading file '" + file + "'");
        } catch (SQLException e) {
            System.out.println("Warning: Could not establish connection to database to report quest data.");
            System.out.println(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        DatabaseConnection.initializeConnectionPool(); // ItemInformationProvider loads some unrelated db data
        ii = ItemInformationProvider.getInstance();

        reportQuestItemData();
    }
}

