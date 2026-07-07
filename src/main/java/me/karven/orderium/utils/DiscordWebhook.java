package me.karven.orderium.utils;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import me.karven.orderium.config.util.component.discord.WebhookOptionConfig;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static me.karven.orderium.Orderium.plugin;

public class DiscordWebhook {

    private static final HttpClient HTTP = HttpClient.newHttpClient();

    public static void send(final WebhookOptionConfig webhookConfig, final String... replacements) {
        final JsonArray embedsArray = new JsonArray(1);
        embedsArray.add(webhookConfig.embedConfig.jsonObject(replacements));

        final JsonObject payload = new JsonObject();
        payload.add("embeds", embedsArray);

        final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(webhookConfig.webhook))
                .header("Content-Type", "application/json")
                .header("User-Agent", plugin.getPluginMeta().getName() + " " + plugin.getPluginMeta().getVersion())
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        HTTP.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                .exceptionally(e -> {
                    Log.error("Failed to send webhook message", e);
                    return null;
                });
    }
}
