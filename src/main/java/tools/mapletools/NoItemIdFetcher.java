package tools.mapletools;

import provider.wz.WZFiles;

import java.io.*;
import java.nio.charset.StandardCharsets;
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
 * This application finds inexistent itemids within the drop data from
 * the Maplestory database specified in the URL below. This program
 * assumes all itemids uses 7 digits.
 * <p>
 * A file is generated listing all the inexistent ids.
 */
public class NoItemIdFetcher {
    private static final Path OUTPUT_FILE = ToolConstants.getOutputFile("no_item_id_report.txt");
    private static final Connection con = SimpleDatabaseConnection.getConnection();

    private static final Set<Integer> existingIds = new HashSet<>();
    private static final Set<Integer> nonExistingIds = new HashSet<>();

    private static PrintWriter printWriter = null;
    private static BufferedReader bufferedReader = null;
    private static byte status = 0;
    private static int itemId = -1;

    private static String getName(String token) {
        int i, j;
        char[] dest;
        String d;

        i = token.lastIndexOf("name");
        i = token.indexOf("\"", i) + 1; //lower bound of the string
        j = token.indexOf("\"", i);     //upper bound

        dest = new char[100];
        token.getChars(i, j, dest, 0);

        d = new String(dest);
        return (d);
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

    private static void translateToken(String token) {
        String d;

        if (token.contains("/imgdir")) {
            status -= 1;
        } else if (token.contains("imgdir")) {
            if (status == 1) {           //getting ItemId
                d = getName(token);
                itemId = Integer.parseInt(d.substring(1, 8));

                existingIds.add(itemId);
                forwardCursor(status);
            }

            status += 1;
        }
    }

    private static void readItemDataFile(File file) {
        // This will reference one line at a time
        String line = null;

        try {
            InputStreamReader fileReader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8);
            bufferedReader = new BufferedReader(fileReader);

            status = 0;
            try {
                while ((line = bufferedReader.readLine()) != null) {
                    translateToken(line);
                }
            } catch (NumberFormatException npe) {
                // second criteria, itemid is on the name of the file

                try {
                    itemId = Integer.parseInt(file.getName().substring(0, 7));
                    existingIds.add(itemId);
                } catch (NumberFormatException npe2) {
                }
            }

            bufferedReader.close();
            fileReader.close();
        } catch (FileNotFoundException ex) {
            System.out.println("Unable to open file '" + file.getName() + "'");
        } catch (IOException ex) {
            System.out.println("Error reading file '" + file.getName() + "'");
        }
    }

    private static void readEquipDataDirectory(String dirPath) {
        File[] folders = new File(dirPath).listFiles();
        //If this pathname does not denote a directory, then listFiles() returns null.

        for (File folder : folders) {   // enter all subfolders
            if (folder.isDirectory()) {
                System.out.println("Reading '" + dirPath + "/" + folder.getName() + "'...");

                try {
                    File[] files = folder.listFiles();

                    for (File file : files) {   // enter all XML files under subfolders
                        if (file.isFile()) {
                            itemId = Integer.parseInt(file.getName().substring(0, 8));
                            existingIds.add(itemId);
                        }
                    }
                } catch (NumberFormatException nfe) {
                }
            }
        }
    }

    private static void readItemDataDirectory(String dirPath) {
        File[] folders = new File(dirPath).listFiles();
        //If this pathname does not denote a directory, then listFiles() returns null.

        for (File folder : folders) {   // enter all subfolders
            if (folder.isDirectory()) {
                System.out.println("Reading '" + dirPath + "/" + folder.getName() + "'...");

                File[] files = folder.listFiles();

                for (File file : files) {   // enter all XML files under subfolders
                    if (file.isFile()) {
                        readItemDataFile(file);
                    }
                }
            }
        }
    }

    private static void evaluateDropsFromTable(String table) throws SQLException {
        PreparedStatement ps = con.prepareStatement("SELECT DISTINCT itemid FROM " + table + ";");
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            if (!existingIds.contains(rs.getInt(1))) {
                nonExistingIds.add(rs.getInt(1));
            }
        }

        rs.close();
        ps.close();
    }

    private static void evaluateDropsFromDb() {
        try (con) {
            System.out.println("Evaluating item data on DB...");

            evaluateDropsFromTable("drop_data");
            evaluateDropsFromTable("reactordrops");

            if (!nonExistingIds.isEmpty()) {
                List<Integer> list = new ArrayList<>(nonExistingIds);
                Collections.sort(list);

                for (Integer i : list) {
                    printWriter.println(i);
                }
            }

            System.out.println("Inexistent itemid count: " + nonExistingIds.size());
            System.out.println("Total itemid count: " + existingIds.size());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try (PrintWriter pw = new PrintWriter(Files.newOutputStream(OUTPUT_FILE))) {
            printWriter = pw;
            existingIds.add(0); // meso itemid
            readEquipDataDirectory(WZFiles.CHARACTER.getFilePath());
            readItemDataDirectory(WZFiles.ITEM.getFilePath());

            evaluateDropsFromDb();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}