package tools.mapletools;

import provider.wz.WZFiles;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * @author RonanLana
 * <p>
 * This application gathers information about the Cash Shop's EXP & DROP coupons,
 * such as applied rates, active times of day and days of week and dumps them in
 * a SQL table that the server will make use.
 */
public class CouponInstaller {
    private static final Path COUPON_INPUT_FILE_1 = WZFiles.ITEM.getFile().resolve("Cash/0521.img.xml");
    private static final Path COUPON_INPUT_FILE_2 = WZFiles.ITEM.getFile().resolve("Cash/0536.img.xml");
    private static final Connection con = SimpleDatabaseConnection.getConnection();
    private static BufferedReader bufferedReader = null;
    private static byte status = 0;
    private static int itemId = -1;
    private static int itemMultiplier = 1;
    private static int startHour = -1;
    private static int endHour = -1;
    private static int activeDay = 0;

    private static String getName(String token) {
        int i, j;
        char[] dest;
        String d;

        i = token.lastIndexOf("name");
        if (i < 0) {
            return "";
        }

        i = token.indexOf("\"", i) + 1; //lower bound of the string
        j = token.indexOf("\"", i);     //upper bound

        dest = new char[8];
        token.getChars(i, j, dest, 0);

        d = new String(dest);
        return (d);
    }

    private static String getNodeValue(String token) {
        int i, j;
        char[] dest;
        String d;

        i = token.lastIndexOf("value=");
        i = token.indexOf("\"", i) + 1; //lower bound of the string
        j = token.indexOf("\"", i);     //upper bound

        if (j - i < 1) {
            return "";
        }

        dest = new char[j - i];
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

    private static int getDayOfWeek(String day) {
        return switch (day) {
            case "SUN" -> 1;
            case "MON" -> 2;
            case "TUE" -> 3;
            case "WED" -> 4;
            case "THU" -> 5;
            case "FRI" -> 6;
            case "SAT" -> 7;
            default -> 0;
        };
    }

    private static void processHourTimeString(String time) {
        startHour = Integer.parseInt(time.substring(4, 6));
        endHour = Integer.parseInt(time.substring(7, 9));
    }

    private static void processDayTimeString(String time) {
        String day = time.substring(0, 3);
        int d = getDayOfWeek(day);

        activeDay |= (1 << d);
    }

    private static void loadTimeFromCoupon(int st) {
        System.out.println("Loading coupon id " + itemId + ". Rate: " + itemMultiplier + "x.");

        String line = null;
        try {
            startHour = -1;
            endHour = -1;
            activeDay = 0;

            String time = null;
            while ((line = bufferedReader.readLine()) != null) {
                simpleToken(line);
                if (status < st) {
                    break;
                }

                time = getNodeValue(line);
                processDayTimeString(time);

                simpleToken(line);
            }

            if (time != null) {
                processHourTimeString(time);

                PreparedStatement ps = con.prepareStatement("INSERT INTO nxcoupons (couponid, rate, activeday, starthour, endhour) VALUES (?, ?, ?, ?, ?)");
                ps.setInt(1, itemId);
                ps.setInt(2, itemMultiplier);
                ps.setInt(3, activeDay);
                ps.setInt(4, startHour);
                ps.setInt(5, endHour);
                ps.execute();

                ps.close();
            }
        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }
    }

    private static void translateToken(String token) {
        String d;

        if (token.contains("/imgdir")) {
            status -= 1;
        } else if (token.contains("imgdir")) {
            if (status == 1) {           //getting ItemId
                d = getName(token);
                itemId = Integer.parseInt(d);
            } else if (status == 2) {
                d = getName(token);

                if (!d.contains("info")) {
                    forwardCursor(status);
                }
            } else if (status == 3) {
                d = getName(token);

                if (!d.contains("time")) {
                    forwardCursor(status);
                } else {
                    loadTimeFromCoupon(status);
                }
            }

            status += 1;
        } else {
            if (status == 3) {
                d = getName(token);

                if (d.contains("rate")) {
                    String r = getNodeValue(token);

                    double db = Double.parseDouble(r);
                    itemMultiplier = (int) db;
                }
            }
        }
    }

    private static void installRateCoupons(Path file) {
        // This will reference one line at a time
        String line = null;

        try (BufferedReader br = Files.newBufferedReader(file)) {
            bufferedReader = br;

            while ((line = bufferedReader.readLine()) != null) {
                translateToken(line);
            }

        } catch (FileNotFoundException ex) {
            System.out.println("Unable to open file '" + file + "'");
        } catch (IOException ex) {
            System.out.println("Error reading file '" + file + "'");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void installCouponsTable() {
        try {
            PreparedStatement ps = con.prepareStatement("DROP TABLE IF EXISTS `nxcoupons`;");
            ps.execute();
            ps.close();

            ps = con.prepareStatement(
                    """
                            CREATE TABLE IF NOT EXISTS `nxcoupons` (
                              `id` int(11) NOT NULL AUTO_INCREMENT,
                              `couponid` int(11) NOT NULL DEFAULT '0',
                              `rate` int(11) NOT NULL DEFAULT '0',
                              `activeday` int(11) NOT NULL DEFAULT '0',
                              `starthour` int(11) NOT NULL DEFAULT '0',
                              `endhour` int(11) NOT NULL DEFAULT '0',
                              PRIMARY KEY (`id`)
                            ) ENGINE=InnoDB DEFAULT CHARSET=latin1 AUTO_INCREMENT=1;"""
            );

            ps.execute();
            ps.close();

            installRateCoupons(COUPON_INPUT_FILE_1);
            installRateCoupons(COUPON_INPUT_FILE_2);

            con.close();
        } catch (SQLException e) {
            System.out.println("Warning: Could not establish connection to database to change card chance rate.");
            System.out.println(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        installCouponsTable();
    }
}
