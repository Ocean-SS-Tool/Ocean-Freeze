package ac.ocean.api;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ac.ocean.OceanPlugin;
import ac.ocean.utils.ClickableMessage;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class DatabaseAPI {

    private static final String BASE_URL = "https://api.anticheat.ac/v1";
    private final OceanPlugin plugin;
    private final Gson gson;

    public DatabaseAPI(OceanPlugin plugin) {
        this.plugin = plugin;
        this.gson = new Gson();
    }

    public void queryUser(CommandSender sender, String discordId) {
        String apiKey = plugin.getConfig().getString("anticheat.api-key");

        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("YOUR_API_KEY_HERE")) {
            sender.sendMessage(colorize(plugin.getMessageManager().getMessage("api-key-not-configured",
                    "&c✗ AntiCheat.ac API key is not configured!")));
            return;
        }

        if (!discordId.matches("^\\d{5,25}$")) {
            sender.sendMessage(colorize(plugin.getMessageManager().getMessage("db-invalid-discord-id",
                    "&c✗ Invalid Discord ID! It must be between 5 and 25 digits.")));
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String fullUrl = BASE_URL + "/db/query/" + discordId;
            try {
                plugin.getLogger().info("[API] GET " + fullUrl);
                URL url = new URL(fullUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("x-api-key", apiKey);
                conn.setRequestProperty("Accept", "application/json");

                String serverName = plugin.getConfig().getString("anticheat.server-name", "");
                if (serverName != null && !serverName.trim().isEmpty()) {
                    conn.setRequestProperty("x-server-name", serverName.trim());
                }

                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);

                int responseCode = conn.getResponseCode();
                plugin.getLogger().info("[API] Response: " + responseCode + " from GET " + fullUrl);

                if (responseCode == 200) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }
                    br.close();

                    JsonObject data = gson.fromJson(response.toString(), JsonObject.class);

                    Bukkit.getScheduler().runTask(plugin, () -> displayDatabaseQuery(sender, data, discordId));

                } else if (responseCode == 400) {
                    Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(colorize(plugin.getMessageManager().getMessage("db-invalid-discord-id",
                            "&c✗ Invalid Discord ID! It must be between 5 and 25 digits."))));
                } else if (responseCode == 401) {
                    Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(colorize(plugin.getMessageManager().getMessage("db-unauthorized",
                            "&c✗ API key is missing, invalid, or expired!"))));
                } else if (responseCode == 403) {
                    Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(colorize(plugin.getMessageManager().getMessage("db-forbidden",
                            "&c✗ Access denied: API key lacks db:query permission or account does not have DB Access."))));
                } else if (responseCode == 429) {
                    Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(colorize(plugin.getMessageManager().getMessage("db-rate-limited",
                            "&c✗ Rate limit exceeded! Please slow down."))));
                } else {
                    String errorBody = readErrorStream(conn);
                    logApiError("GET", fullUrl, responseCode, errorBody);

                    Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(colorize(plugin.getMessageManager().getMessage("api-error",
                            "&cAPI Error: %error%").replace("%error%", String.valueOf(responseCode)))));
                }

                conn.disconnect();

            } catch (Exception e) {
                Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(colorize(plugin.getMessageManager().getMessage("api-error",
                        "&cAPI Error: %error%").replace("%error%", e.getMessage()))));
                logApiException("queryUser", fullUrl, e);
            }
        });
    }

    private void displayDatabaseQuery(CommandSender sender, JsonObject data, String requestedDiscordId) {
        String discordId = data.has("discordId") ? data.get("discordId").getAsString() : requestedDiscordId;

        JsonObject botProfile = (data.has("botProfile") && !data.get("botProfile").isJsonNull()) ? data.getAsJsonObject("botProfile") : null;
        JsonObject networkLookup = (data.has("networkLookup") && !data.get("networkLookup").isJsonNull()) ? data.getAsJsonObject("networkLookup") : null;
        JsonObject oceanObj = (data.has("ocean") && !data.get("ocean").isJsonNull()) ? data.getAsJsonObject("ocean") : null;
        JsonObject scannedUser = (oceanObj != null && oceanObj.has("scannedUser") && !oceanObj.get("scannedUser").isJsonNull()) ? oceanObj.getAsJsonObject("scannedUser") : null;
        JsonArray scanMatches = (oceanObj != null && oceanObj.has("scanMatches") && !oceanObj.get("scanMatches").isJsonNull()) ? oceanObj.getAsJsonArray("scanMatches") : new JsonArray();

        boolean hasAnyRecord = botProfile != null || networkLookup != null || scannedUser != null || (scanMatches != null && scanMatches.size() > 0);

        sender.sendMessage(colorize(plugin.getMessageManager().getMessage("db-header", "&7&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")));
        sender.sendMessage(colorize(plugin.getMessageManager().getMessage("db-title", "&e&lCheater Database Results &7- &f%discordid%")
                .replace("%discordid%", discordId)));

        if (!hasAnyRecord) {
            sender.sendMessage(colorize(plugin.getMessageManager().getMessage("db-no-records",
                    "&a✔ No database records or detections found for this user (Clean).")));
            sender.sendMessage(colorize(plugin.getMessageManager().getMessage("db-footer", "&7&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")));
            return;
        }

        if (botProfile != null) {
            String username = botProfile.has("username") && !botProfile.get("username").isJsonNull() ? botProfile.get("username").getAsString() : "Unknown";
            String displayName = botProfile.has("displayName") && !botProfile.get("displayName").isJsonNull() ? botProfile.get("displayName").getAsString() : username;
            boolean isConfirmed = botProfile.has("isConfirmedCheater") && botProfile.get("isConfirmedCheater").getAsBoolean();

            sender.sendMessage(colorize(plugin.getMessageManager().getMessage("db-profile-header", "&6&l[Community Profile] &f%username% &7(%displayname%)")
                    .replace("%username%", username)
                    .replace("%displayname%", displayName)));

            if (isConfirmed) {
                String reason = "Unspecified";
                String caughtBy = "Staff";
                if (botProfile.has("confirmedCheater") && !botProfile.get("confirmedCheater").isJsonNull()) {
                    JsonObject conf = botProfile.getAsJsonObject("confirmedCheater");
                    if (conf.has("reason") && !conf.get("reason").isJsonNull()) reason = conf.get("reason").getAsString();
                    if (conf.has("caughtby") && !conf.get("caughtby").isJsonNull()) caughtBy = conf.get("caughtby").getAsString();
                }
                sender.sendMessage(colorize(plugin.getMessageManager().getMessage("db-confirmed-cheater",
                        "  &c&l⚠ CONFIRMED CHEATER: &f%reason% &7(Caught by: %caughtby%)")
                        .replace("%reason%", reason)
                        .replace("%caughtby%", caughtBy)));
            } else {
                sender.sendMessage(colorize(plugin.getMessageManager().getMessage("db-not-confirmed-cheater",
                        "  &aConfirmed Cheater: &fNo")));
            }

            if (botProfile.has("totals") && !botProfile.get("totals").isJsonNull()) {
                JsonObject totals = botProfile.getAsJsonObject("totals");
                int servers = totals.has("servers") ? totals.get("servers").getAsInt() : 0;
                int messages = totals.has("messages") ? totals.get("messages").getAsInt() : 0;
                sender.sendMessage(colorize(plugin.getMessageManager().getMessage("db-profile-totals",
                        "  &7Activity: &e%servers% &7servers | &e%messages% &7messages")
                        .replace("%servers%", String.valueOf(servers))
                        .replace("%messages%", String.valueOf(messages))));
            }
        }

        if (networkLookup != null) {
            sender.sendMessage(colorize(plugin.getMessageManager().getMessage("db-network-header", "&9&l[External Network]")));

            String pName = networkLookup.has("playerName") && !networkLookup.get("playerName").isJsonNull() ? networkLookup.get("playerName").getAsString() : null;
            if (pName != null) {
                sender.sendMessage(colorize("  &7Player Name: &f" + pName));
            }

            if (networkLookup.has("bans") && !networkLookup.get("bans").isJsonNull()) {
                JsonObject bans = networkLookup.getAsJsonObject("bans");
                int activeBans = bans.has("activeCount") ? bans.get("activeCount").getAsInt() : 0;
                int totalBans = bans.has("total") ? bans.get("total").getAsInt() : 0;
                String banColor = activeBans > 0 ? "&c" : "&a";
                sender.sendMessage(colorize(plugin.getMessageManager().getMessage("db-network-bans",
                        "  &7Bans: %color%%active% Active &7/ &e%total% Total")
                        .replace("%color%", banColor)
                        .replace("%active%", String.valueOf(activeBans))
                        .replace("%total%", String.valueOf(totalBans))));
            }

            if (networkLookup.has("blacklist") && !networkLookup.get("blacklist").isJsonNull()) {
                JsonObject bl = networkLookup.getAsJsonObject("blacklist");
                boolean listed = bl.has("listed") && bl.get("listed").getAsBoolean();
                if (listed) {
                    sender.sendMessage(colorize(plugin.getMessageManager().getMessage("db-network-blacklisted",
                            "  &c&l⚠ BLACKLISTED ON NETWORK!")));
                }
            }

            if (networkLookup.has("dangerousDiscords") && !networkLookup.get("dangerousDiscords").isJsonNull()) {
                JsonObject dang = networkLookup.getAsJsonObject("dangerousDiscords");
                int count = dang.has("count") ? dang.get("count").getAsInt() : 0;
                if (count > 0) {
                    sender.sendMessage(colorize(plugin.getMessageManager().getMessage("db-network-dangerous",
                            "  &6⚠ Dangerous Discord Guilds: &e%count%")
                            .replace("%count%", String.valueOf(count))));
                }
            }
        }

        if (scannedUser != null) {
            sender.sendMessage(colorize(plugin.getMessageManager().getMessage("db-ocean-header", "&b&l[Ocean Scans]")));

            String status = scannedUser.has("overallStatus") && !scannedUser.get("overallStatus").isJsonNull() ? scannedUser.get("overallStatus").getAsString() : "unknown";
            int totalScans = scannedUser.has("totalScans") ? scannedUser.get("totalScans").getAsInt() : 0;
            int totalDetections = scannedUser.has("totalDetections") ? scannedUser.get("totalDetections").getAsInt() : 0;

            String statusColor = "&a";
            if (status.equalsIgnoreCase("cheating")) statusColor = "&c&l";
            else if (status.equalsIgnoreCase("suspicious")) statusColor = "&6";

            sender.sendMessage(colorize(plugin.getMessageManager().getMessage("db-ocean-status",
                    "  &7Overall Status: %color%%status% &7| Scans: &e%scans% &7| Detections: &c%detections%")
                    .replace("%color%", statusColor)
                    .replace("%status%", status.toUpperCase())
                    .replace("%scans%", String.valueOf(totalScans))
                    .replace("%detections%", String.valueOf(totalDetections))));

            if (scannedUser.has("allDetections") && scannedUser.get("allDetections").isJsonArray()) {
                JsonArray detects = scannedUser.getAsJsonArray("allDetections");
                if (detects.size() > 0) {
                    sender.sendMessage(colorize(plugin.getMessageManager().getMessage("db-ocean-detections-title", "  &c&lDetections:")));
                    for (int i = 0; i < Math.min(detects.size(), 5); i++) {
                        sender.sendMessage(colorize("    &c» &f" + detects.get(i).getAsString()));
                    }
                    if (detects.size() > 5) {
                        sender.sendMessage(colorize("    &7... and " + (detects.size() - 5) + " more"));
                    }
                }
            }
        }

        if (scanMatches != null && scanMatches.size() > 0 && scannedUser == null) {
            sender.sendMessage(colorize(plugin.getMessageManager().getMessage("db-scan-matches",
                    "&b&l[Scan Matches]: &e%count% match(es)")
                    .replace("%count%", String.valueOf(scanMatches.size()))));
        }

        if (sender instanceof Player) {
            Player staff = (Player) sender;
            String lookupBtn = plugin.getMessageManager().getMessage("scan-results-related-lookup-btn", "&e[Lookup]");
            String lookupCmd = "/ocean lookup " + discordId;
            String lookupHover = plugin.getMessageManager().getMessage("scan-results-related-lookup-hover", "&eClick to view full scan history");

            String riskBtn = plugin.getMessageManager().getMessage("scan-results-related-risk-btn", "&6[Risk Score]");
            String riskCmd = "/ocean riskscore " + discordId;
            String riskHover = plugin.getMessageManager().getMessage("scan-results-related-risk-hover", "&6Click to check risk analysis");

            ClickableMessage.sendDouble(staff, "  ", lookupBtn, lookupCmd, lookupHover, riskBtn, riskCmd, riskHover);
        }

        sender.sendMessage(colorize(plugin.getMessageManager().getMessage("db-footer", "&7&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")));
    }

    private String readErrorStream(HttpURLConnection conn) {
        try {
            if (conn.getErrorStream() != null) {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
                br.close();
                return sb.toString();
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    private void logApiError(String method, String url, int responseCode, String errorResponse) {
        plugin.getLogger().warning("╔══════════ API ERROR ══════════");
        plugin.getLogger().warning("║ Method:        " + method);
        plugin.getLogger().warning("║ URL:           " + url);
        plugin.getLogger().warning("║ Response Code: " + responseCode);
        plugin.getLogger().warning("║ Response Body: " + (errorResponse.isEmpty() ? "(empty)" : errorResponse));
        plugin.getLogger().warning("╚══════════════════════════════");
    }

    private void logApiException(String methodName, String url, Exception e) {
        plugin.getLogger().severe("╔══════════ API EXCEPTION ══════════");
        plugin.getLogger().severe("║ Method:    " + methodName);
        plugin.getLogger().severe("║ URL:       " + url);
        plugin.getLogger().severe("║ Exception: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        plugin.getLogger().severe("╚══════════════════════════════════");
    }

    private String colorize(String message) {
        return message.replace("&", "§");
    }
}
