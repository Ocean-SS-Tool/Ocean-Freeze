package ac.ocean.discord;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * Elegant Discord Webhook implementation with full UTF-8 support
 */
public class DiscordWebhook {

    private static final Logger LOGGER = Logger.getLogger("Ocean");
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Ocean-Webhook");
        t.setDaemon(true);
        return t;
    });
    private final String webhookUrl;
    private String content;
    private String username;
    private String avatarUrl;
    private final List<EmbedObject> embeds;

    public DiscordWebhook(String webhookUrl) {
        this.webhookUrl = webhookUrl;
        this.embeds = new ArrayList<>();
    }

    public DiscordWebhook setContent(String content) {
        this.content = content;
        return this;
    }

    public DiscordWebhook setUsername(String username) {
        this.username = username;
        return this;
    }

    public DiscordWebhook setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
        return this;
    }

    public DiscordWebhook addEmbed(EmbedObject embed) {
        this.embeds.add(embed);
        return this;
    }

    public void execute() {
        if (webhookUrl == null || webhookUrl.isEmpty() || webhookUrl.equals("none")) {
            return;
        }

        EXECUTOR.submit(() -> {
            try {
                JsonObject json = new JsonObject();

                if (content != null) {
                    json.addProperty("content", content);
                }
                if (username != null) {
                    json.addProperty("username", username);
                }
                if (avatarUrl != null) {
                    json.addProperty("avatar_url", avatarUrl);
                }

                if (!embeds.isEmpty()) {
                    JsonArray embedArray = new JsonArray();
                    for (EmbedObject embed : embeds) {
                        embedArray.add(embed.toJson());
                    }
                    json.add("embeds", embedArray);
                }

                URL url = new URL(webhookUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                connection.setRequestProperty("User-Agent", "Ocean-Freeze-Plugin");
                connection.setDoOutput(true);

                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = json.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int responseCode = connection.getResponseCode();
                if (responseCode != 204 && responseCode != 200) {
                    String errorBody = "";
                    try {
                        if (connection.getErrorStream() != null) {
                            BufferedReader br = new BufferedReader(new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8));
                            StringBuilder sb = new StringBuilder();
                            String errorLine;
                            while ((errorLine = br.readLine()) != null) {
                                sb.append(errorLine);
                            }
                            br.close();
                            errorBody = sb.toString();
                        }
                    } catch (Exception ignored) {
                    }
                    LOGGER.warning("╔══════════ WEBHOOK ERROR ══════════");
                    LOGGER.warning("║ URL:           " + webhookUrl);
                    LOGGER.warning("║ Response Code: " + responseCode);
                    LOGGER.warning("║ Response Body: " + (errorBody.isEmpty() ? "(empty)" : errorBody));
                    LOGGER.warning("╚══════════════════════════════════");
                } else {
                    LOGGER.info("[Webhook] Successfully sent to Discord (code: " + responseCode + ")");
                }

                connection.disconnect();

            } catch (Exception e) {
                LOGGER.severe("╔══════════ WEBHOOK EXCEPTION ══════════");
                LOGGER.severe("║ URL:       " + webhookUrl);
                LOGGER.severe("║ Exception: " + e.getClass().getSimpleName() + ": " + e.getMessage());
                LOGGER.severe("╚══════════════════════════════════════");
            }
        });
    }

    public static class EmbedObject {
        private String title;
        private String description;
        private String url;
        private Integer color;
        private final List<Field> fields;
        private Footer footer;
        private Thumbnail thumbnail;
        private String timestamp;

        public EmbedObject() {
            this.fields = new ArrayList<>();
        }

        public EmbedObject setTitle(String title) {
            this.title = title;
            return this;
        }

        public EmbedObject setDescription(String description) {
            this.description = description;
            return this;
        }

        public EmbedObject setUrl(String url) {
            this.url = url;
            return this;
        }

        public EmbedObject setColor(int color) {
            this.color = color;
            return this;
        }

        public EmbedObject addField(String name, String value, boolean inline) {
            this.fields.add(new Field(name, value, inline));
            return this;
        }

        public EmbedObject setFooter(String text, String iconUrl) {
            this.footer = new Footer(text, iconUrl);
            return this;
        }

        public EmbedObject setThumbnail(String url) {
            this.thumbnail = new Thumbnail(url);
            return this;
        }

        public EmbedObject setTimestamp(String timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        private JsonObject toJson() {
            JsonObject json = new JsonObject();

            if (title != null) json.addProperty("title", title);
            if (description != null) json.addProperty("description", description);
            if (url != null) json.addProperty("url", url);
            if (color != null) json.addProperty("color", color);
            if (timestamp != null) json.addProperty("timestamp", timestamp);

            if (!fields.isEmpty()) {
                JsonArray fieldsArray = new JsonArray();
                for (Field field : fields) {
                    JsonObject fieldJson = new JsonObject();
                    fieldJson.addProperty("name", field.name);
                    fieldJson.addProperty("value", field.value);
                    fieldJson.addProperty("inline", field.inline);
                    fieldsArray.add(fieldJson);
                }
                json.add("fields", fieldsArray);
            }

            if (footer != null) {
                JsonObject footerJson = new JsonObject();
                footerJson.addProperty("text", footer.text);
                if (footer.iconUrl != null) {
                    footerJson.addProperty("icon_url", footer.iconUrl);
                }
                json.add("footer", footerJson);
            }

            if (thumbnail != null) {
                JsonObject thumbnailJson = new JsonObject();
                thumbnailJson.addProperty("url", thumbnail.url);
                json.add("thumbnail", thumbnailJson);
            }

            return json;
        }

        private static class Field {
            private final String name;
            private final String value;
            private final boolean inline;

            Field(String name, String value, boolean inline) {
                this.name = name;
                this.value = value;
                this.inline = inline;
            }
        }

        private static class Footer {
            private final String text;
            private final String iconUrl;

            Footer(String text, String iconUrl) {
                this.text = text;
                this.iconUrl = iconUrl;
            }
        }

        private static class Thumbnail {
            private final String url;

            Thumbnail(String url) {
                this.url = url;
            }
        }
    }
}
