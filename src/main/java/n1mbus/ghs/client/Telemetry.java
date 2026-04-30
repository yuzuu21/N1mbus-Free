package n1mbus.ghs.client;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

public class Telemetry {
    private static final String API_URL = "https://nimbus-9ym.pages.dev/api/telemetry/log";
    private static final String VERSION = "1.0.0-Pre";

    public static void init() {
        new Thread(() -> {
            try {
                String hwid = generateHWID();
                sendHeartbeat(hwid);
            } catch (Exception e) {
                // Silent fail to avoid disrupting user experience
            }
        }, "Nimbus-Telemetry-Thread").start();
    }

    private static String generateHWID() throws Exception {
        String main = System.getProperty("user.name") + System.getenv("COMPUTERNAME") + System.getProperty("os.name");
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(main.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
    }

    private static void sendHeartbeat(String hwid) throws Exception {
        URL url = new URL(API_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        String json = String.format("{\"hwid\":\"%s\",\"version\":\"%s\"}", hwid, VERSION);
        
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = json.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int code = conn.getResponseCode();
        // We don't really care about the response code in the client
    }
}
