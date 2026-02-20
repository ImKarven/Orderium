package me.karven.orderium.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import me.karven.orderium.load.Orderium;
import org.bukkit.Bukkit;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

@Slf4j
public class UpdateUtils {
    private static final String itemsURL = "https://github.com/ImKarven/Orderium/raw/refs/heads/master/items.db";
    private static File itemsFile;
    private static Logger logger;

    private static final String API_URL = "https://api.modrinth.com/v2/project/";
    private static final String PROJECT_ID = "EH2l9h8i";
    private static final String mcVer = Bukkit.getMinecraftVersion();
    private static String plVer;

    public static void init(Orderium plugin) {
        itemsFile = new File(plugin.getDataFolder(), "items.db");
        logger = plugin.getLogger();
        plVer = plugin.getPluginMeta().getVersion();

        try {
            plugin.getDataFolder().mkdirs();
            itemsFile.createNewFile();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "An error occured while creating plugin's directory", e);
        }
    }

    public static boolean downloadItems() {
        logger.info("Downloading items list...");
        try (val in = new BufferedInputStream(URI.create(itemsURL).toURL().openStream());
             val out = new FileOutputStream(itemsFile)) {
            byte[] buffer = new byte[1024];
            int readBytes;
            while ((readBytes = in.read(buffer, 0, 1024)) != -1) {
                out.write(buffer, 0, readBytes);
            }
            logger.info("Successfully downloaded items list");
            return true;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to download items list", e);
            return false;
        }
    }
    /// Check for updates
    /// Returns the newer version if available, otherwise returns null
    public static String checkForUpdates() {
        final String latestVer = fetchLatestVer();
        if (latestVer == null || latestVer.compareTo(plVer) <= 0) return null;
        return latestVer;
    }

    private static String fetchLatestVer() {
        final String urlText =  API_URL + PROJECT_ID + "/version?game_versions=" + mcVer;
        try {
            HttpURLConnection connection = (HttpURLConnection) URI.create(urlText).toURL().openConnection();
            connection.setRequestProperty("User-Agent", "Orderium Update Checker");
            connection.setRequestMethod("GET");
            connection.connect();
            final int resCode = connection.getResponseCode();
            if (resCode != HttpURLConnection.HTTP_OK) {
                logger.severe("Failed to check for updates");
                return null;
            }

            final String sRes = getRes(connection);
            final JsonArray jRes = JsonParser.parseString(sRes).getAsJsonArray();
            final JsonObject latest = jRes.get(0).getAsJsonObject();
            return latest.get("version_number").getAsString();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to check for updates", e);
        }
        return null;
    }

    private static  String getRes(HttpURLConnection connection) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
    }
}
