package ac.ocean.api;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import ac.ocean.OceanPlugin;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Elegant API client for Ocean User Lookup and Risk Score APIs
 */
public class UserLookupAPI {

    private static final String BASE_URL = "https://api.anticheat.ac/v1";
    private final OceanPlugin plugin;
    private final Gson gson;

    public UserLookupAPI(OceanPlugin plugin) {
        this.plugin = plugin;
        this.gson = new Gson();
    }

    public void lookupUser(CommandSender sender, String discordId) {
        String apiKey = plugin.getConfig().getString("anticheat.api-key");

        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("YOUR_API_KEY_HERE")) {
            sender.sendMessage(colorize(plugin.getMessageManager().getMessage("api-key-not-configured",
                    "&c✗ AntiCheat.ac API key is not configured!")));
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String fullUrl = BASE_URL + "/scanned-users/lookup/" + discordId;
            try {
                plugin.getLogger().info("[API] GET " + fullUrl);
                URL url = new URL(fullUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("x-api-key", apiKey);
                conn.setRequestProperty("Accept", "application/json");

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

                    Bukkit.getScheduler().runTask(plugin, () -> {
                        displayUserLookup(sender, data);
                    });

                } else if (responseCode == 404) {
                    plugin.getLogger().info("[API] User not found: " + discordId);
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        String msg = plugin.getMessageManager().getMessage("lookup-not-found",
                                "&c✗ User not found in scan database.");
                        sender.sendMessage(colorize(msg.replace("%discordid%", discordId)));
                    });
                } else {
                    String errorBody = readErrorStream(conn);
                    logApiError("GET", fullUrl, responseCode, errorBody);

                    Bukkit.getScheduler().runTask(plugin, () -> {
                        sender.sendMessage(colorize(plugin.getMessageManager().getMessage("api-error",
                                "&cAPI Error: %error%").replace("%error%", String.valueOf(responseCode))));
                    });
                }

                conn.disconnect();

            } catch (Exception e) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    sender.sendMessage(colorize(plugin.getMessageManager().getMessage("api-error",
                            "&cAPI Error: %error%").replace("%error%", e.getMessage())));
                });
                logApiException("lookupUser", fullUrl, e);
            }
        });
    }

    public void getRiskScore(CommandSender sender, String discordId) {
        String apiKey = plugin.getConfig().getString("anticheat.api-key");

        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("YOUR_API_KEY_HERE")) {
            sender.sendMessage(colorize(plugin.getMessageManager().getMessage("api-key-not-configured",
                    "&c✗ AntiCheat.ac API key is not configured!")));
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String fullUrl = BASE_URL + "/users/" + discordId + "/risk-score";
            try {
                plugin.getLogger().info("[API] GET " + fullUrl);
                URL url = new URL(fullUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("x-api-key", apiKey);
                conn.setRequestProperty("Accept", "application/json");

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

                    Bukkit.getScheduler().runTask(plugin, () -> {
                        displayRiskScore(sender, data);
                    });

                } else if (responseCode == 404) {
                    plugin.getLogger().info("[API] No risk data found for: " + discordId);
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        String msg = plugin.getMessageManager().getMessage("riskscore-not-found",
                                "&c✗ No risk data found for this user.");
                        sender.sendMessage(colorize(msg.replace("%discordid%", discordId)));
                    });
                } else {
                    String errorBody = readErrorStream(conn);
                    logApiError("GET", fullUrl, responseCode, errorBody);

                    Bukkit.getScheduler().runTask(plugin, () -> {
                        sender.sendMessage(colorize(plugin.getMessageManager().getMessage("api-error",
                                "&cAPI Error: %error%").replace("%error%", String.valueOf(responseCode))));
                    });
                }

                conn.disconnect();

            } catch (Exception e) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    sender.sendMessage(colorize(plugin.getMessageManager().getMessage("api-error",
                            "&cAPI Error: %error%").replace("%error%", e.getMessage())));
                });
                logApiException("getRiskScore", fullUrl, e);
            }
        });
    }

    private void displayUserLookup(CommandSender sender, JsonObject data) {
        boolean found = data.has("found") && data.get("found").getAsBoolean();

        if (!found) {
            String msg = plugin.getMessageManager().getMessage("lookup-not-found",
                    "&c✗ User not found in scan database.");
            sender.sendMessage(colorize(msg));
            return;
        }

        String discordId = data.has("discordId") ? data.get("discordId").getAsString() : "Unknown";
        String username = data.has("username") ? data.get("username").getAsString() : "Unknown";
        int totalScans = data.has("totalScans") ? data.get("totalScans").getAsInt() : 0;
        int totalDetections = data.has("totalDetections") ? data.get("totalDetections").getAsInt() : 0;
        String overallStatus = data.has("overallStatus") ? data.get("overallStatus").getAsString() : "unknown";
        boolean isBanned = data.has("isBanned") && data.get("isBanned").getAsBoolean();
        boolean isWarned = data.has("isWarned") && data.get("isWarned").getAsBoolean();

        sender.sendMessage(colorize(plugin.getMessageManager().getMessage("lookup-header", "&7&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")));
        sender.sendMessage(colorize(plugin.getMessageManager().getMessage("lookup-title", "&e&lUser Lookup Results")));

        sender.sendMessage(colorize(plugin.getMessageManager().getMessage("lookup-discord", "&eDiscord ID: &f%discordid%")
                .replace("%discordid%", discordId)));
        sender.sendMessage(colorize(plugin.getMessageManager().getMessage("lookup-username", "&eUsername: &f%username%")
                .replace("%username%", username)));
        sender.sendMessage(colorize(plugin.getMessageManager().getMessage("lookup-status", "&eStatus: &f%status%")
                .replace("%status%", overallStatus.toUpperCase())));
        sender.sendMessage(colorize(plugin.getMessageManager().getMessage("lookup-scans", "&eTotal Scans: &f%scans%")
                .replace("%scans%", String.valueOf(totalScans))));
        sender.sendMessage(colorize(plugin.getMessageManager().getMessage("lookup-detections", "&eTotal Detections: &f%detections%")
                .replace("%detections%", String.valueOf(totalDetections))));
        sender.sendMessage(colorize(plugin.getMessageManager().getMessage("lookup-banned", "&eBanned: &f%banned%")
                .replace("%banned%", isBanned ? "YES" : "NO")));
        sender.sendMessage(colorize(plugin.getMessageManager().getMessage("lookup-warned", "&eWarned: &f%warned%")
                .replace("%warned%", isWarned ? "YES" : "NO")));

        if (data.has("detections") && data.get("detections").isJsonArray()) {
            JsonArray detectionsArray = data.getAsJsonArray("detections");
            if (detectionsArray.size() > 0) {
                sender.sendMessage(colorize(plugin.getMessageManager().getMessage("lookup-detections-title", "&c&lDetections:")));
                for (int i = 0; i < Math.min(detectionsArray.size(), 10); i++) {
                    String detection = detectionsArray.get(i).getAsString();
                    sender.sendMessage(colorize(plugin.getMessageManager().getMessage("lookup-detection-item", "&7- &f%detection%")
                            .replace("%detection%", detection)));
                }
                if (detectionsArray.size() > 10) {
                    sender.sendMessage(colorize("&7... and " + (detectionsArray.size() - 10) + " more"));
                }
            }
        }

        if (data.has("hasRelatedAccounts") && data.get("hasRelatedAccounts").getAsBoolean()) {
            int relatedCount = data.has("totalRelatedAccounts") ? data.get("totalRelatedAccounts").getAsInt() : 0;
            sender.sendMessage(colorize(plugin.getMessageManager().getMessage("lookup-related", "&6⚠ Related Accounts: &f%count%")
                    .replace("%count%", String.valueOf(relatedCount))));

            if (data.has("relatedAccounts") && data.get("relatedAccounts").isJsonArray()) {
                JsonArray relatedArray = data.getAsJsonArray("relatedAccounts");
                for (int i = 0; i < Math.min(relatedArray.size(), 5); i++) {
                    JsonObject related = relatedArray.get(i).getAsJsonObject();
                    String relatedUsername = related.has("username") ? related.get("username").getAsString() : "Unknown";
                    String relatedStatus = related.has("overallStatus") ? related.get("overallStatus").getAsString() : "unknown";
                    sender.sendMessage(colorize("  &7- &f" + relatedUsername + " &7(" + relatedStatus + ")"));
                }
            }
        }

        sender.sendMessage(colorize(plugin.getMessageManager().getMessage("lookup-footer", "&7&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")));
    }

    private void displayRiskScore(CommandSender sender, JsonObject data) {
        boolean found = data.has("found") && data.get("found").getAsBoolean();

        if (!found) {
            String msg = plugin.getMessageManager().getMessage("riskscore-not-found",
                    "&c✗ No risk data found for this user.");
            sender.sendMessage(colorize(msg));
            return;
        }

        String discordId = data.has("discordId") ? data.get("discordId").getAsString() : "Unknown";
        int riskScore = data.has("riskScore") ? data.get("riskScore").getAsInt() : 0;
        String riskLevel = data.has("riskLevel") ? data.get("riskLevel").getAsString() : "unknown";
        int linkedProfiles = data.has("linkedProfiles") ? data.get("linkedProfiles").getAsInt() : 0;

        String scoreColor = "&a";
        if (riskLevel.equals("medium")) scoreColor = "&e";
        else if (riskLevel.equals("high")) scoreColor = "&6";
        else if (riskLevel.equals("critical")) scoreColor = "&c";

        sender.sendMessage(colorize(plugin.getMessageManager().getMessage("riskscore-header", "&7&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")));
        sender.sendMessage(colorize(plugin.getMessageManager().getMessage("riskscore-title", "&e&lRisk Score Analysis")));

        sender.sendMessage(colorize(plugin.getMessageManager().getMessage("riskscore-discord", "&eDiscord ID: &f%discordid%")
                .replace("%discordid%", discordId)));
        sender.sendMessage(colorize(plugin.getMessageManager().getMessage("riskscore-score", "&eRisk Score: %color%%score%/100")
                .replace("%color%", scoreColor)
                .replace("%score%", String.valueOf(riskScore))));
        sender.sendMessage(colorize(plugin.getMessageManager().getMessage("riskscore-level", "&eRisk Level: &f%level%")
                .replace("%level%", riskLevel.toUpperCase())));
        sender.sendMessage(colorize(plugin.getMessageManager().getMessage("riskscore-profiles", "&eLinked Profiles: &f%profiles%")
                .replace("%profiles%", String.valueOf(linkedProfiles))));

        if (data.has("stats")) {
            JsonObject stats = data.getAsJsonObject("stats");
            sender.sendMessage(colorize(plugin.getMessageManager().getMessage("riskscore-stats-title", "&b&lScan Statistics:")));
            sender.sendMessage(colorize(plugin.getMessageManager().getMessage("riskscore-total-scans", "&7Total Scans: &f%scans%")
                    .replace("%scans%", String.valueOf(stats.has("totalScans") ? stats.get("totalScans").getAsInt() : 0))));
            sender.sendMessage(colorize(plugin.getMessageManager().getMessage("riskscore-cheating-scans", "&cCheating Scans: &f%scans%")
                    .replace("%scans%", String.valueOf(stats.has("cheatingScans") ? stats.get("cheatingScans").getAsInt() : 0))));
            sender.sendMessage(colorize(plugin.getMessageManager().getMessage("riskscore-suspicious-scans", "&6Suspicious Scans: &f%scans%")
                    .replace("%scans%", String.valueOf(stats.has("suspiciousScans") ? stats.get("suspiciousScans").getAsInt() : 0))));
            sender.sendMessage(colorize(plugin.getMessageManager().getMessage("riskscore-clean-scans", "&aClean Scans: &f%scans%")
                    .replace("%scans%", String.valueOf(stats.has("cleanScans") ? stats.get("cleanScans").getAsInt() : 0))));
        }

        if (data.has("uniqueCheatsDetected") && data.get("uniqueCheatsDetected").isJsonArray()) {
            JsonArray cheatsArray = data.getAsJsonArray("uniqueCheatsDetected");
            if (cheatsArray.size() > 0) {
                sender.sendMessage(colorize(plugin.getMessageManager().getMessage("riskscore-cheats-title", "&c&lCheats Detected:")));
                for (int i = 0; i < Math.min(cheatsArray.size(), 10); i++) {
                    String cheat = cheatsArray.get(i).getAsString();
                    sender.sendMessage(colorize("&7- &f" + cheat));
                }
            }
        }

        sender.sendMessage(colorize(plugin.getMessageManager().getMessage("riskscore-footer", "&7&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")));
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
