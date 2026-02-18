package me.karven.orderium.utils;

import lombok.val;
import me.karven.orderium.load.Orderium;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.logging.Logger;

public class UpdateUtils {
    private static final String itemsURL = "https://github.com/ImKarven/Orderium/raw/refs/heads/master/items.db";
    private static File itemsFile;
    private static Logger logger;

    public static void init(Orderium plugin) {
        itemsFile = new File(plugin.getDataFolder(), "items.db");
        logger = plugin.getLogger();

        try {
            plugin.getDataFolder().mkdirs();
            itemsFile.createNewFile();
        } catch (IOException e) {
            logger.severe(e.toString());
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
            logger.severe("Failed to download items list");
            logger.severe(e.toString());
            return false;
        }
    }
}
