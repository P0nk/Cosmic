package server.voting;

import tools.DatabaseConnection;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Properties;
import java.util.TimeZone;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class GTopVoteTask implements Runnable {
    private static final Logger log = Logger.getLogger(GTopVoteTask.class.getName());

    // --- CONSTANTS ---
    private static final String SITE_ID = "105154";
    private static final String BASE_URL = "https://gtop100.com/home/report2";

    private static String gtopPassword = null;

    // Global cooldown tracker
    private static long lastFetchTime = 0;
    private static final long FETCH_COOLDOWN = 35 * 1000; // 35 Seconds

    @Override
    public void run() {
        System.out.println("[VoteDebug] Task started.");

        if (gtopPassword == null) loadConfig();
        if (gtopPassword == null || gtopPassword.isEmpty()) {
            System.out.println("[VoteDebug] FATAL: GTOP_PASSWORD is missing in .env file.");
            return;
        }

        // --- 1. COOLDOWN CHECK ---
        long now = System.currentTimeMillis();
        if (now - lastFetchTime < FETCH_COOLDOWN) {
            // Uncomment to debug cooldown if needed, otherwise silent to avoid spam
            // long remaining = (FETCH_COOLDOWN - (now - lastFetchTime)) / 1000;
            // System.out.println("[VoteDebug] Cooldown active. Skipping pull. Wait " + remaining + "s.");
            return;
        }
        lastFetchTime = now;

        try {
            // --- 2. ASSEMBLE URL ---
            LocalDate gmtDate = LocalDate.now(ZoneId.of("GMT"));
            String dateStr = gmtDate.toString(); // YYYY-MM-DD

            StringBuilder urlBuilder = new StringBuilder(BASE_URL);
            urlBuilder.append("?siteid=").append(SITE_ID);
            urlBuilder.append("&pass=").append(URLEncoder.encode(gtopPassword, StandardCharsets.UTF_8.toString()));
            urlBuilder.append("&date=").append(dateStr);

            String requestUrl = urlBuilder.toString();
            // Log masked URL
            System.out.println("[VoteDebug] Requesting: " + requestUrl.replaceAll("pass=.*?&", "pass=*****&"));

            URL url = new URL(requestUrl);
            URLConnection conn = url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");

            // --- 3. READ RESPONSE ---
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuilder content = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();

            String rawXml = content.toString();
            // Optional: Print raw XML to verify date format if issues persist
            // System.out.println("[VoteDebug] Raw Response: " + rawXml);

            if (rawXml.isEmpty()) {
                System.out.println("[VoteDebug] Response was empty.");
                return;
            }

            // --- 4. PARSE XML ---
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(new InputSource(new StringReader(rawXml)));
            doc.getDocumentElement().normalize();

            // Check Error Code
            NodeList errNodes = doc.getElementsByTagName("errorcode");
            if (errNodes.getLength() > 0) {
                String err = errNodes.item(0).getTextContent();
                if (!"0".equals(err)) {
                    log.warning("[Vote] GTop returned error: " + err);
                    return;
                }
            }

            // --- 5. INGEST TO DB ---
            NodeList nList = doc.getElementsByTagName("entry");
            int count = nList.getLength();
            System.out.println("[VoteDebug] Found " + count + " entries for date " + dateStr);

            if (count == 0) return;

            // Parser for GTop Date Format: "2026-01-10 12:47:31"
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            // Assuming GTop XML uses GMT (Standard for APIs) or Server Time.
            // If the inserted time is consistently off by hours, adjust this TimeZone.
            sdf.setTimeZone(TimeZone.getTimeZone("GMT"));

            try (Connection con = DatabaseConnection.getConnection()) {
                PreparedStatement ps = con.prepareStatement(
                        "INSERT IGNORE INTO vote_records (vote_unique_id, username, ip_address, vote_time) VALUES (?, ?, ?, ?)"
                );

                for (int i = 0; i < count; i++) {
                    Node node = nList.item(i);
                    if (node.getNodeType() == Node.ELEMENT_NODE) {
                        Element el = (Element) node;
                        String uid = getVal("uniqueid", el);
                        String user = getVal("pingusername", el);
                        String ip = getVal("ip", el);
                        String timeStr = getVal("time", el); // Extract time string

                        long voteTime = System.currentTimeMillis(); // Fallback
                        if (timeStr != null) {
                            try {
                                Date d = sdf.parse(timeStr);
                                voteTime = d.getTime();
                            } catch (ParseException e) {
                                System.out.println("[VoteDebug] Failed to parse date: " + timeStr + ". Using current time.");
                            }
                        }

                        if (i == 0 || i % 10 == 0) {
                            System.out.println("[VoteDebug] Processing Vote #" + (i+1) + " | User: " + user + " | Time: " + timeStr);
                        }

                        ps.setString(1, uid);
                        ps.setString(2, (user != null && !user.isEmpty()) ? user : null);
                        ps.setString(3, ip);
                        // CHANGED: Use setTimestamp for DATETIME columns in MySQL
                        ps.setTimestamp(4, new java.sql.Timestamp(voteTime));
                        ps.addBatch();
                    }
                }

                int[] results = ps.executeBatch();
                System.out.println("[VoteDebug] Batch executed. " + results.length + " statements processed.");
            }
        } catch (Exception e) {
            System.out.println("[VoteDebug] EXCEPTION: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String getVal(String tag, Element el) {
        NodeList nl = el.getElementsByTagName(tag);
        if (nl != null && nl.getLength() > 0) {
            Node n = nl.item(0).getFirstChild();
            if (n != null) {
                return n.getNodeValue();
            }
        }
        return null;
    }

    private void loadConfig() {
        Properties p = new Properties();
        try (FileInputStream fis = new FileInputStream(".env")) {
            p.load(fis);
            gtopPassword = p.getProperty("GTOP_PASSWORD");
        } catch (Exception e) {
            log.severe("Missing .env file");
        }
    }
}