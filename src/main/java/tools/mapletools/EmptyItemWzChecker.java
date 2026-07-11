package tools.mapletools;

import provider.wz.WZFiles;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * @author RonanLana
 * <p>
 * This application has two objectives: it reports in a detailed file all itemids which is
 * currently missing either a name entry in the String.wz or an item entry in the Item.wz;
 * And it removes from the String.wz XMLs all entries which misses properties on Item.wz.
 */
public class EmptyItemWzChecker {
    private static final Path OUTPUT_FILE = ToolConstants.getOutputFile("empty_item_wz_report.txt");
    private static final String OUTPUT_PATH = ToolConstants.OUTPUT_DIRECTORY.toString();
    private static final int INITIAL_STRING_LENGTH = 50;
    private static final int ITEM_FILE_NAME_SIZE = 13;

    private static final Stack<String> currentPath = new Stack<>();
    private static final Map<Integer, String> stringWzItems = new HashMap<>();
    private static final Map<Integer, String> contentWzItems = new HashMap<>();
    private static final Set<Integer> handbookItems = new HashSet<>();

    private static PrintWriter printWriter = null;
    private static InputStreamReader fileReader = null;
    private static BufferedReader bufferedReader = null;
    private static byte status = 0;
    private static int currentItemid = 0;
    private static int currentDepth = 0;
    private static String currentFile;
    private static Set<Integer> nonPropItems;

    private static String getName(String token) {
        int i, j;
        char[] dest;
        String d;

        i = token.lastIndexOf("name");
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

    private static void listFiles(String directoryName, ArrayList<File> files) {
        File directory = new File(directoryName);

        // get all the files from a directory
        File[] fList = directory.listFiles();
        for (File file : fList) {
            if (file.isFile()) {
                files.add(file);
            } else if (file.isDirectory()) {
                listFiles(file.getAbsolutePath(), files);
            }
        }
    }

    private static int getItemIdFromFilename(String name) {
        try {
            return Integer.parseInt(name.substring(0, name.indexOf('.')));
        } catch (Exception e) {
            return -1;
        }
    }

    private static void inspectItemWzEntry() {
        String line = null;

        try {
            while ((line = bufferedReader.readLine()) != null) {
                translateItemToken(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String currentItemPath() {
        String s = currentFile + " -> ";

        for (String p : currentPath) {
            s += (p + "\\");
        }

        return s;
    }

    private static void translateItemToken(String token) {
        if (token.contains("/imgdir")) {
            status -= 1;

            currentPath.pop();
        } else if (token.contains("imgdir")) {
            status += 1;
            String d = getName(token);

            if (status == 2) {
                currentItemid = Integer.parseInt(d);
                contentWzItems.put(currentItemid, currentItemPath());

                forwardCursor(status);
            } else {
                currentPath.push(d);
            }
        }
    }

    private static void inspectStringWzEntry() {
        String line = null;

        try {
            while ((line = bufferedReader.readLine()) != null) {
                translateStringToken(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void translateStringToken(String token) {
        if (token.contains("/imgdir")) {
            status -= 1;
            currentPath.pop();
        } else if (token.contains("imgdir")) {
            status += 1;
            String d = getName(token);

            if (status == currentDepth) {
                currentItemid = Integer.parseInt(d);
                stringWzItems.put(currentItemid, currentItemPath());
                //if (currentItemid >= 4000000) System.out.println("  " + currentItemid);

                forwardCursor(status);
            } else {
                currentPath.push(d);
            }
        }
    }

    private static void loadStringWzFile(String filePath, int depth) throws IOException {
        fileReader = new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8);
        bufferedReader = new BufferedReader(fileReader);

        currentFile = filePath;
        currentDepth = 2 + depth;
        //System.out.println(filePath + " depth " + depth);
        inspectStringWzEntry();

        bufferedReader.close();
        fileReader.close();
    }

    private static void loadStringWz() throws IOException {
        System.out.println("Reading String.wz ...");
        String[][] stringWzFiles = {{"Cash", "Consume", "Ins", "Pet"}, {"Etc"}, {"Eqp"}};

        for (int i = 0; i < stringWzFiles.length; i++) {
            for (String dirFile : stringWzFiles[i]) {
                final String fileName = "/" + dirFile + ".img.xml";
                loadStringWzFile(WZFiles.STRING.getFilePath() + fileName, i);
            }
        }
    }

    private static void loadItemWz() throws IOException {
        System.out.println("Reading Item.wz ...");
        ArrayList<File> files = new ArrayList<>();
        listFiles(WZFiles.ITEM.getFilePath(), files);

        for (File f : files) {
            if (f.getParentFile().getName().contentEquals("Special")) {
                continue;
            }

            //System.out.println("Parsing " + f.getAbsolutePath());
            fileReader = new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8);
            bufferedReader = new BufferedReader(fileReader);

            currentFile = f.getCanonicalPath();

            if (f.getName().length() <= ITEM_FILE_NAME_SIZE) {
                inspectItemWzEntry();
            } else {    // pet file structure is similar to equips, maybe there are other item-types following this behaviour?
                int itemid = getItemIdFromFilename(f.getName());
                if (itemid < 0) {
                    continue;
                }

                currentItemid = itemid;
                contentWzItems.put(currentItemid, currentItemPath());
            }

            bufferedReader.close();
            fileReader.close();
        }
    }

    private static void loadCharacterWz() throws IOException {
        System.out.println("Reading Character.wz ...");
        ArrayList<File> files = new ArrayList<>();
        listFiles(WZFiles.CHARACTER.getFilePath(), files);

        for (File f : files) {
            if (f.getParentFile().getName().contentEquals("Character.wz")) {
                continue;
            }

            int itemid = getItemIdFromFilename(f.getName());
            if (itemid < 0) {
                continue;
            }

            currentFile = f.getCanonicalPath();
            currentItemid = itemid;
            contentWzItems.put(currentItemid, currentItemPath());
        }
    }

    private static void calculateItemNameDiff(Set<Integer> emptyItemNames, Set<Integer> emptyNameItems) {
        for (Integer i : contentWzItems.keySet()) {
            if (!stringWzItems.containsKey(i)) {
                emptyNameItems.add(i);
            }
        }

        for (Integer i : stringWzItems.keySet()) {
            if (!contentWzItems.containsKey(i)) {
                emptyItemNames.add(i);
            }
        }
    }

    private static void readHandbookItems() throws IOException {
        System.out.println("Reading handbook ...");
        String[] handbookPaths = {"Equip", "Cash.txt", "Etc.txt", "Pet.txt", "Setup.txt", "Use.txt"};

        for (String path : handbookPaths) {
            readHandbookPath(ToolConstants.HANDBOOK_PATH + "/" + path);
        }
    }

    private static void readHandbookPath(String filePath) throws IOException {
        ArrayList<File> files = new ArrayList<>();

        File testFile = new File(filePath);
        if (testFile.isDirectory()) {
            listFiles(filePath, files);
        } else {
            files.add(testFile);
        }

        for (File f : files) {
            fileReader = new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8);
            bufferedReader = new BufferedReader(fileReader);

            String line = null;

            try {
                while ((line = bufferedReader.readLine()) != null) {
                    String[] tokens = line.split(" - ");

                    if (tokens[0].length() > 0) {
                        int itemid = Integer.parseInt(tokens[0]);
                        handbookItems.add(itemid);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            bufferedReader.close();
            fileReader.close();
        }
    }

    private static void printReportFileHeader() {
        printWriter.println(" # Report File autogenerated from the MapleEmptyItemWzChecker feature by Ronan Lana.");
        printWriter.println(" # Generated data takes into account several data info from the server-side WZ.xmls.");
        printWriter.println();
    }

    private static List<Integer> getSortedItems(Set<Integer> items) {
        List<Integer> sortedItems = new ArrayList<>(items);
        Collections.sort(sortedItems);

        return sortedItems;
    }

    private static void printReportFileResults(Set<Integer> emptyItemNames, Set<Integer> emptyNameItems) {
        if (!emptyItemNames.isEmpty()) {
            printWriter.println("String.wz NAMES with no Item.wz node, " + emptyItemNames.size() + " entries:");

            for (Integer itemid : getSortedItems(emptyItemNames)) {
                printWriter.println("  " + itemid + " " + stringWzItems.get(itemid) + (handbookItems.contains(itemid) ? "" : " NOT FOUND"));
            }

            printWriter.println();
        }

        if (!emptyNameItems.isEmpty()) {
            printWriter.println("Item.wz ITEMS with no String.wz node, " + emptyNameItems.size() + " entries:");

            for (Integer itemid : getSortedItems(emptyNameItems)) {
                printWriter.println("  " + itemid + " " + contentWzItems.get(itemid) + (handbookItems.contains(itemid) ? "" : " NOT FOUND"));
            }

            printWriter.println();
        }
    }

    private static void reportItemNameDiff(Set<Integer> emptyItemNames, Set<Integer> emptyNameItems) throws IOException {
        System.out.println("Reporting results...");
        try(PrintWriter pw = new PrintWriter(Files.newOutputStream(OUTPUT_FILE))) {
        	printWriter = pw;
        	printReportFileHeader();
            printReportFileResults(emptyItemNames, emptyNameItems);
        }
    }

    private static void locateItemStringWzDiff() throws IOException {
        Set<Integer> emptyItemNames = new HashSet<>(), emptyNameItems = new HashSet<>();
        calculateItemNameDiff(emptyItemNames, emptyNameItems);

        reportItemNameDiff(emptyItemNames, emptyNameItems);
        nonPropItems = emptyItemNames;
    }

    private static void runEmptyItemWzChecker() throws IOException {
        readHandbookItems();

        loadCharacterWz();
        loadItemWz();
        loadStringWz();

        locateItemStringWzDiff();
    }

    private static void generateStringWzEntry() {
        String line = null;

        try {
            while ((line = bufferedReader.readLine()) != null) {
                updateStringToken(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void updateStringToken(String token) {
        if (token.contains("/imgdir")) {
            status -= 1;

        } else if (token.contains("imgdir")) {
            status += 1;

            if (status == currentDepth && nonPropItems.contains(Integer.valueOf(getName(token)))) {
                forwardCursor(status);
                return;
            }
        }

        printWriter.println(token);
    }

    private static void generateStringWzFile(String filePath, int depth) throws IOException {
        fileReader = new InputStreamReader(new FileInputStream(WZFiles.DIRECTORY + filePath), StandardCharsets.UTF_8);
        bufferedReader = new BufferedReader(fileReader);
        printWriter = new PrintWriter(OUTPUT_PATH + filePath, StandardCharsets.UTF_8);
        currentDepth = 2 + depth;

        //System.out.println(filePath + " depth " + depth);
        generateStringWzEntry();

        printWriter.close();
        bufferedReader.close();
        fileReader.close();
    }

    private static void generateStringWz() throws IOException {
        System.out.println("Generating clean String.wz ...");
        String[][] stringWzFiles = { { "Cash", "Consume", "Ins", "Pet" }, { "Etc" }, { "Eqp" } };
        String stringWzPath = "/String.wz/";

        File folder = new File(OUTPUT_PATH + "/String.wz/");
        if (!folder.exists()) {
            folder.mkdirs();
        }

        for (int i = 0; i < stringWzFiles.length; i++) {
            for (String dirFile : stringWzFiles[i]) {
                generateStringWzFile(stringWzPath + dirFile + ".img.xml", i);
            }
        }
    }

    public static void main(String[] args) {
        try {
            runEmptyItemWzChecker();
            generateStringWz();

            System.out.println("Done!");
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

}
