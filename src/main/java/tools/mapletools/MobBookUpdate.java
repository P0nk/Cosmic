package tools.mapletools;

import provider.wz.WZFiles;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author RonanLana
 * <p>
 * This application updates the Monster Book drop data with the actual underlying drop data from
 * the Maplestory database specified in the URL below.
 * <p>
 * In other words all items drops from monsters listed inside the Mob Book feature will be patched to match exactly like the item
 * drop list specified in the URL's Maplestory database.
 * <p>
 * The original file "MonsterBook.img.xml" from String.wz must be copied to the directory of this application and only then
 * executed. This program will generate another file that must replace the original server file to make the effects take place
 * to on your server.
 * <p>
 * After replacing on server, this XML must be updated on the client via WZ Editor (HaRepack for instance). Once inside the repack,
 * remove the property 'MonsterBook.img' inside 'string.wz' and choose to import the xml generated with this software.
 */
public class MobBookUpdate {
    private static final Path INPUT_FILE = WZFiles.STRING.getFile().resolve("MonsterBook.img.xml");
    private static final Path OUTPUT_FILE = ToolConstants.getOutputFile("MonsterBook_updated.img.xml");
    private static final Connection con = SimpleDatabaseConnection.getConnection();

    private static PrintWriter printWriter = null;
    private static BufferedReader bufferedReader = null;
    private static byte status = 0;
    private static int mobId = -1;

    private static String getName(String token) {
        int i, j;
        char[] dest;
        String d;

        i = token.lastIndexOf("name");
        i = token.indexOf("\"", i) + 1; //lower bound of the string
        j = token.indexOf("\"", i);     //upper bound

        if (j - i < 7) {
            dest = new char[6];
        } else {
            dest = new char[7];
        }
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
            if (line != null) {
                printWriter.println(line);
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

    private static void loadDropsFromMob() {
        System.out.println("Loading mob id " + mobId);

        try {
            String toPrint;
            int itemId, cont = 0;

            PreparedStatement ps = con.prepareStatement("SELECT itemid FROM drop_data WHERE (dropperid = ? AND itemid > 0) GROUP BY itemid;");
            ps.setInt(1, mobId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                toPrint = "";
                for (int k = 0; k <= status; k++) {
                    toPrint += "  ";
                }

                toPrint += "<int name=\"";
                toPrint += cont;
                toPrint += "\" value=\"";

                itemId = rs.getInt("itemid");
                toPrint += itemId;
                toPrint += "\" />";

                printWriter.println(toPrint);
                cont++;
            }

            rs.close();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void translateToken(String token) {
        String d;
        int temp;

        printWriter.println(token);

        if (token.contains("/imgdir")) {
            status -= 1;
        } else if (token.contains("imgdir")) {
            if (status == 1) {           //getting MobId
                d = getName(token);
                mobId = Integer.parseInt(d);
            } else if (status == 2) {
                d = getName(token);

                if (d.contains("reward")) {
                    temp = status;

                    loadDropsFromMob();
                    forwardCursor(temp);
                }
            }

            status += 1;
        }

    }

    private static void updateFromDropData() {
        try (con;
                PrintWriter pw = new PrintWriter(Files.newOutputStream(OUTPUT_FILE));
                BufferedReader br = Files.newBufferedReader(INPUT_FILE);) {
            printWriter = pw;
            bufferedReader = br;

            String line = null;

            while ((line = bufferedReader.readLine()) != null) {
                translateToken(line);
            }
        } catch (FileNotFoundException ex) {
            System.out.println("Unable to open file '" + INPUT_FILE + "'");
        } catch (IOException ex) {
            System.out.println("Error reading file '" + INPUT_FILE + "'");
        } catch (SQLException e) {
            System.out.println("Warning: Could not establish connection to database to change card chance rate.");
            System.out.println(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        updateFromDropData();
    }
}

