package tools.mapletools;

import provider.wz.WZFiles;

import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

/**
 * @author RonanLana
 * <p>
 * This application seeks from the XMLs all mapid entries that holds the specified
 * fieldLimit.
 */
public class MapFieldLimitChecker {
    private static final int INITIAL_STRING_LENGTH = 50;
    private static final int FIELD_LIMIT = 0x400000;

    private static BufferedReader bufferedReader = null;
    private static byte status = 0;
    private static int mapid = 0;

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

    private static void listFiles(Path directory, ArrayList<Path> files) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
            for (Path path : stream) {
                if (Files.isRegularFile(path)) {
                    files.add(path);
                } else if (Files.isDirectory(path)) {
                    listFiles(path, files);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static int getMapIdFromFilename(String name) {
        try {
            return Integer.parseInt(name.substring(0, name.indexOf('.')));
        } catch (Exception e) {
            return -1;
        }
    }

    private static void translateToken(String token) {
        if (token.contains("/imgdir")) {
            status -= 1;
        } else if (token.contains("imgdir")) {
            status += 1;

            if (status == 2) {
                String d = getName(token);
                if (!d.contentEquals("info")) {
                    forwardCursor(status);
                }
            }
        } else {
            if (status == 2) {
                String d = getName(token);

                if (d.contentEquals("fieldLimit")) {
                    int value = Integer.parseInt(getValue(token));
                    if ((value & FIELD_LIMIT) == FIELD_LIMIT) {
                        System.out.println(mapid + " " + value);
                    }
                }
            }
        }
    }

    private static void inspectMapEntry() {
        String line = null;

        try {
            while ((line = bufferedReader.readLine()) != null) {
                translateToken(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void loadMapWz() throws IOException {
        System.out.println("Reading Map.wz ...");
        ArrayList<Path> files = new ArrayList<>();
        listFiles(WZFiles.MAP.getFile().resolve("Map"), files);

        for (Path f : files) {
            try (BufferedReader br = Files.newBufferedReader(f)) {
                bufferedReader = br;

                mapid = getMapIdFromFilename(f.getFileName().toString());
                inspectMapEntry();

            }
        }
    }

    public static void main(String[] args) {
        try {
            loadMapWz();
            System.out.println("Done!");
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
}
