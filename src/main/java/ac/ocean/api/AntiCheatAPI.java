package ac.ocean.api;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import ac.ocean.OceanPlugin;
import ac.ocean.manager.MessageManager;
import ac.ocean.model.FrozenPlayer;
import ac.ocean.utils.ClickableMessage;

import org.bukkit.scheduler.BukkitRunnable;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AntiCheatAPI {

    private static final String BASE_URL = "https://api.anticheat.ac/v1";
    private final OceanPlugin plugin;
    private final Gson gson;

    public AntiCheatAPI(OceanPlugin plugin) {
        this.plugin = plugin;
        this.gson = new Gson();
    }

    public interface PinCallback {
        void onPinCreated(String pin);
    }

    public void createPinForScan(Player target, Player staff, PinCallback callback) {
        String apiKey = plugin.getConfig().getString("anticheat.api-key");

        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("YOUR_API_KEY_HERE")) {
            staff.sendMessage(colorize(plugin.getMessageManager().getMessage("api-key-not-configured",
                    "&cOcean API key is not configured!")));
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String fullUrl = BASE_URL + "/pins/create";
            try {
                plugin.getLogger().info("[API] POST " + fullUrl + " (target: " + target.getName() + ")");
                URL url = new URL(fullUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("x-api-key", apiKey);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Accept", "application/json");
                conn.setDoOutput(true);

                JsonObject requestBody = new JsonObject();
                requestBody.addProperty("type", plugin.getConfig().getString("anticheat.game-type", "Java"));
                requestBody.addProperty("pinName", "Freeze: " + target.getName() + " by " + staff.getName());
                //requestBody.addProperty("ramDump", plugin.getConfig().getBoolean("anticheat.ram-dump", false));
                requestBody.addProperty("private", plugin.getConfig().getBoolean("anticheat.private-pins", false));

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = gson.toJson(requestBody).getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int responseCode = conn.getResponseCode();
                plugin.getLogger().info("[API] Response: " + responseCode + " from POST " + fullUrl);

                if (responseCode == 200 || responseCode == 201) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }
                    br.close();

                    JsonObject responseJson = gson.fromJson(response.toString(), JsonObject.class);
                    String pin = responseJson.get("pin").getAsString();

                    FrozenPlayer frozenPlayer = plugin.getFreezeManager().getFrozenPlayer(target.getUniqueId());
                    if (frozenPlayer != null) {
                        frozenPlayer.setScanPin(pin);
                    }

                    Bukkit.getScheduler().runTask(plugin, () -> callback.onPinCreated(pin));

                    startScanMonitoring(pin, target.getUniqueId(), staff.getUniqueId(), target.getName());

                } else {
                    String errorResponse = readErrorStream(conn);
                    String errorMessage = parseErrorMessage(errorResponse);

                    String finalMessage = errorMessage;
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        MessageManager msg = plugin.getMessageManager();
                        staff.sendMessage(colorize(msg.getMessage("scan-pin-failed",
                                "&cFailed to create scan PIN!")));
                        staff.sendMessage(colorize(msg.getMessage("scan-pin-error",
                                "&cError %code%: &f%error%")
                                .replace("%code%", String.valueOf(responseCode))
                                .replace("%error%", finalMessage)));
                    });
                    logApiError("POST", fullUrl, responseCode, errorResponse);
                }

                conn.disconnect();

            } catch (Exception e) {
                Bukkit.getScheduler().runTask(plugin, () ->
                        staff.sendMessage(colorize(plugin.getMessageManager().getMessage("api-error",
                                "&cAPI Error: %error%").replace("%error%", e.getMessage()))));
                logApiException("createPinForScan", fullUrl, e);
            }
        });
    }

    public void createPin(Player target, Player staff) {
        String apiKey = plugin.getConfig().getString("anticheat.api-key");

        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("YOUR_API_KEY_HERE")) {
            staff.sendMessage(colorize(plugin.getMessageManager().getMessage("api-key-not-configured",
                    "&cOcean API key is not configured!")));
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String fullUrl = BASE_URL + "/pins/create";
            try {
                plugin.getLogger().info("[API] POST " + fullUrl + " (target: " + target.getName() + ")");
                URL url = new URL(fullUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("x-api-key", apiKey);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Accept", "application/json");
                conn.setDoOutput(true);

                JsonObject requestBody = new JsonObject();
                requestBody.addProperty("type", plugin.getConfig().getString("anticheat.game-type", "Java"));
                requestBody.addProperty("pinName", "Freeze: " + target.getName() + " by " + staff.getName());
                //requestBody.addProperty("ramDump", plugin.getConfig().getBoolean("anticheat.ram-dump", false));
                requestBody.addProperty("private", plugin.getConfig().getBoolean("anticheat.private-pins", false));

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = gson.toJson(requestBody).getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int responseCode = conn.getResponseCode();
                plugin.getLogger().info("[API] Response: " + responseCode + " from POST " + fullUrl);

                if (responseCode == 200 || responseCode == 201) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }
                    br.close();

                    JsonObject responseJson = gson.fromJson(response.toString(), JsonObject.class);
                    String pin = responseJson.get("pin").getAsString();

                    FrozenPlayer frozenPlayer = plugin.getFreezeManager().getFrozenPlayer(target.getUniqueId());
                    if (frozenPlayer != null) {
                        frozenPlayer.setScanPin(pin);
                    }

                    Bukkit.getScheduler().runTask(plugin, () -> {
                        MessageManager msg = plugin.getMessageManager();
                        String pinMessage = msg.getMessage("scan-pin",
                                "&e&lSCREENSHARE PIN: &a&l%pin%");
                        target.sendMessage(colorize(pinMessage.replace("%pin%", pin)));
                        staff.sendMessage(colorize(msg.getMessage("scan-pin-created",
                                "&aScan PIN created: &e%pin%").replace("%pin%", pin)));

                        plugin.getFreezeManager().startMessageRepetition(target, pin);
                    });

                    startScanMonitoring(pin, target.getUniqueId(), staff.getUniqueId(), target.getName());

                } else {
                    String errorResponse = readErrorStream(conn);
                    String errorMessage = parseErrorMessage(errorResponse);

                    String finalMessage = errorMessage;
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        MessageManager msg = plugin.getMessageManager();
                        staff.sendMessage(colorize(msg.getMessage("scan-pin-failed",
                                "&cFailed to create scan PIN!")));
                        staff.sendMessage(colorize(msg.getMessage("scan-pin-error",
                                "&cError %code%: &f%error%")
                                .replace("%code%", String.valueOf(responseCode))
                                .replace("%error%", finalMessage)));
                    });
                    logApiError("POST", fullUrl, responseCode, errorResponse);
                }

                conn.disconnect();

            } catch (Exception e) {
                Bukkit.getScheduler().runTask(plugin, () ->
                        staff.sendMessage(colorize(plugin.getMessageManager().getMessage("api-error",
                                "&cAPI Error: %error%").replace("%error%", e.getMessage()))));
                logApiException("createPin", fullUrl, e);
            }
        });
    }

    public void startScanMonitoring(String pin, UUID targetUuid, UUID staffUuid, String targetName) {
        FrozenPlayer fpCheck = plugin.getFreezeManager().getFrozenPlayer(targetUuid);
        if (fpCheck != null && fpCheck.isScanFinished()) {
            return;
        }

        int checkIntervalSeconds = plugin.getConfig().getInt("settings.scan-check-interval", 5);
        int timeoutMinutes = plugin.getConfig().getInt("settings.scan-timeout", 30);
        long checkIntervalTicks = checkIntervalSeconds * 20L;
        final int maxAttempts = (timeoutMinutes * 60) / checkIntervalSeconds;

        plugin.getLogger().info("[Scan] Starting monitoring for PIN " + pin + " (" + targetName + ") - interval: " + checkIntervalSeconds + "s, timeout: " + timeoutMinutes + "min");

        final long scanStartTime = System.currentTimeMillis();
        int taskId = new BukkitRunnable() {
            private int attempts = 0;

            @Override
            public void run() {
                attempts++;

                if (attempts > maxAttempts) {
                    plugin.getLogger().info("[Scan] Monitoring stopped for PIN " + pin + " (" + targetName + ") - timeout reached (" + timeoutMinutes + "min)");
                    cancel();
                    return;
                }

                FrozenPlayer fp = plugin.getFreezeManager().getFrozenPlayer(targetUuid);
                if (fp == null) {
                    plugin.getLogger().info("[Scan] Monitoring stopped for PIN " + pin + " (" + targetName + ") - player no longer frozen");
                    cancel();
                    return;
                }

                Player targetCheck = Bukkit.getPlayer(targetUuid);
                if (targetCheck == null || !targetCheck.isOnline()) {
                    plugin.getLogger().info("[Scan] Monitoring paused for PIN " + pin + " (" + targetName + ") - player disconnected");
                    fp.setScanMonitoringTaskId(-1);
                    cancel();
                    return;
                }

                try {
                    String apiKey = plugin.getConfig().getString("anticheat.api-key");
                    URL url = new URL(BASE_URL + "/pins/" + pin + "/status");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("x-api-key", apiKey);
                    conn.setRequestProperty("Accept", "application/json");
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(10000);

                    int responseCode = conn.getResponseCode();

                    if (responseCode == 200) {
                        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = br.readLine()) != null) {
                            response.append(line);
                        }
                        br.close();

                        String rawResponse = response.toString();
                        JsonObject statusJson = gson.fromJson(rawResponse, JsonObject.class);
                        int progress = statusJson.has("progress") ? statusJson.get("progress").getAsInt() : 0;
                        boolean isFinished = statusJson.has("isFinished") && statusJson.get("isFinished").getAsBoolean();
                        boolean isScanning = statusJson.has("isScanning") && statusJson.get("isScanning").getAsBoolean();

                        if (isScanning) {
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                FrozenPlayer frozenPlayer = plugin.getFreezeManager().getFrozenPlayer(targetUuid);
                                if (frozenPlayer != null && !frozenPlayer.isScanStarted()) {
                                    frozenPlayer.setScanStarted(true);
                                }
                            });
                        }

                        Bukkit.getScheduler().runTask(plugin, () -> {
                            Player staff = Bukkit.getPlayer(staffUuid);
                            if (staff != null && staff.isOnline()) {
                                MessageManager msg = plugin.getMessageManager();
                                String progressMsg = msg.getMessage("scan-progress",
                                        "&7[Scan] &e%player% &7(PIN: &e%pin%&7) &e%progress% &7- %status%");
                                String statusText = "";
                                if (statusJson.has("statusMessage") && !statusJson.get("statusMessage").isJsonNull()) {
                                    statusText = statusJson.get("statusMessage").getAsString();
                                }
                                staff.sendMessage(colorize(progressMsg
                                        .replace("%player%", targetName)
                                        .replace("%pin%", pin)
                                        .replace("%progress%", progress + "%")
                                        .replace("%status%", statusText)));
                            }
                        });

                        if (isFinished) {
                            plugin.getLogger().info("[Scan] PIN " + pin + " (" + targetName + ") finished! Fetching results...");
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                FrozenPlayer frozenPlayer = plugin.getFreezeManager().getFrozenPlayer(targetUuid);
                                if (frozenPlayer != null) {
                                    frozenPlayer.setScanFinished(true);
                                    frozenPlayer.setScanMonitoringTaskId(-1);
                                }
                            });
                            cancel();
                            long scanDuration = (System.currentTimeMillis() - scanStartTime) / 1000;
                            getResults(pin, targetUuid, staffUuid, scanDuration, targetName);
                        }
                    } else {
                        plugin.getLogger().warning("[Scan] Status check returned " + responseCode + " for PIN " + pin);
                    }

                    conn.disconnect();

                } catch (Exception e) {
                    plugin.getLogger().warning("[Scan] Error monitoring PIN " + pin + " (" + targetName + "): " + e.getMessage());
                }
            }
        }.runTaskTimerAsynchronously(plugin, checkIntervalTicks, checkIntervalTicks).getTaskId();

        if (fpCheck != null) {
            fpCheck.setScanMonitoringTaskId(taskId);
        }
    }

    private List<String> jsonArrayToList(JsonArray array) {
        List<String> list = new ArrayList<>();
        if (array != null) {
            for (int i = 0; i < array.size(); i++) {
                list.add(array.get(i).getAsString());
            }
        }
        return list;
    }

    private void getResults(String pin, UUID targetUuid, UUID staffUuid, long scanTime, String knownTargetName) {
        plugin.getLogger().info("[Scan] Fetching results for PIN " + pin + " (" + knownTargetName + ")...");

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String fullUrl = BASE_URL + "/pins/" + pin + "/results";
            try {
                plugin.getLogger().info("[API] GET " + fullUrl);
                String apiKey = plugin.getConfig().getString("anticheat.api-key");
                URL url = new URL(fullUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("x-api-key", apiKey);
                conn.setRequestProperty("Accept", "application/json");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);

                int responseCode = conn.getResponseCode();
                plugin.getLogger().info("[API] Response: " + responseCode + " from GET " + fullUrl);

                if (responseCode == 200 || responseCode == 201) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }
                    br.close();

                    JsonObject resultsJson = gson.fromJson(response.toString(), JsonObject.class);
                    String result = resultsJson.has("result") ? resultsJson.get("result").getAsString() : "none";
                    String scanUrl = plugin.getConfig().getString("anticheat.results-base-url",
                            "https://anticheat.ac/results/pin/") + pin;

                    plugin.getLogger().info("[Scan] Results parsed for PIN " + pin + " - Result: " + result);

                    List<String> detectionsList = new ArrayList<>();
                    List<String> suspiciousList = new ArrayList<>();
                    List<String> warningsList = new ArrayList<>();
                    List<String> integrityList = new ArrayList<>();
                    String vpn = "Unknown";
                    String country = "Unknown";
                    String scantime = "";
                    String windows = "";

                    if (resultsJson.has("data") && !resultsJson.get("data").isJsonNull()) {
                        JsonObject data = resultsJson.getAsJsonObject("data");

                        if (data.has("detections") && data.get("detections").isJsonArray()) {
                            detectionsList = jsonArrayToList(data.getAsJsonArray("detections"));
                        }
                        if (data.has("suspicious") && data.get("suspicious").isJsonArray()) {
                            suspiciousList = jsonArrayToList(data.getAsJsonArray("suspicious"));
                        }
                        if (data.has("warnings") && data.get("warnings").isJsonArray()) {
                            warningsList = jsonArrayToList(data.getAsJsonArray("warnings"));
                        }
                        if (data.has("integrity") && data.get("integrity").isJsonArray()) {
                            integrityList = jsonArrayToList(data.getAsJsonArray("integrity"));
                        }
                        if (data.has("vpn")) vpn = data.get("vpn").getAsString();
                        if (data.has("country")) country = data.get("country").getAsString();
                        if (data.has("scantime")) scantime = data.get("scantime").getAsString();
                        if (data.has("windows")) windows = data.get("windows").getAsString();
                    }

                    final List<String> fDetections = detectionsList;
                    final List<String> fSuspicious = suspiciousList;
                    final List<String> fWarnings = warningsList;
                    final List<String> fIntegrity = integrityList;
                    final String fVpn = vpn;
                    final String fCountry = country;
                    final String fScantime = scantime;
                    final String fWindows = windows;
                    final String fScanTimeDisplay = scantime.isEmpty() ? scanTime + "s" : scantime;

                    try {
                        StringBuilder detectionsText = new StringBuilder();
                        for (String d : fDetections) {
                            detectionsText.append("\u2022 ").append(d).append("\n");
                        }
                        for (String s : fSuspicious) {
                            detectionsText.append("\u2022 ").append(s).append(" (suspicious)\n");
                        }

                        plugin.getLogger().info("[Webhook] Preparing scan results webhook for PIN " + pin + " (" + knownTargetName + ")");
                        Player staffPlayer = Bukkit.getPlayer(staffUuid);
                        String staffName = staffPlayer != null ? staffPlayer.getName() : "Unknown";

                        plugin.getWebhookManager().sendScanCompletedNotification(
                            knownTargetName,
                            staffName,
                            pin,
                            result,
                            fDetections.size(),
                            fWarnings.size(),
                            fSuspicious.size(),
                            fVpn,
                            fCountry,
                            fScanTimeDisplay,
                            scanUrl,
                            detectionsText.toString().trim()
                        );
                    } catch (Exception e) {
                        plugin.getLogger().severe("[Webhook] Failed to send scan results webhook for PIN " + pin + ": " + e.getMessage());
                    }

                    Bukkit.getScheduler().runTask(plugin, () -> {
                        Player staff = Bukkit.getPlayer(staffUuid);
                        Player target = Bukkit.getPlayer(targetUuid);
                        String targetName = target != null ? target.getName() : knownTargetName;
                        MessageManager msg = plugin.getMessageManager();

                        try {
                            if (staff != null && staff.isOnline()) {
                                staff.sendMessage(colorize(msg.getMessage("scan-results-header", "&7&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")));
                                staff.sendMessage(colorize(msg.getMessage("scan-results-title", "&e&lScan Results - PIN: &a%pin%").replace("%pin%", pin)));
                                staff.sendMessage(colorize(msg.getMessage("scan-results-result", "&eResult: &f%result%").replace("%result%", result)));
                                staff.sendMessage("");

                                if (!fDetections.isEmpty()) {
                                    staff.sendMessage(colorize(msg.getMessage("scan-results-detections-title",
                                            "&c&lDetections (%count%):").replace("%count%", String.valueOf(fDetections.size()))));
                                    String detItem = msg.getMessage("scan-results-detections-item", "  &c\u00bb &f%detection%");
                                    for (String d : fDetections) {
                                        staff.sendMessage(colorize(detItem.replace("%detection%", d)));
                                    }
                                }

                                if (!fSuspicious.isEmpty()) {
                                    staff.sendMessage(colorize(msg.getMessage("scan-results-suspicious-title",
                                            "&6&lSuspicious (%count%):").replace("%count%", String.valueOf(fSuspicious.size()))));
                                    String susItem = msg.getMessage("scan-results-suspicious-item", "  &6\u00bb &f%item%");
                                    for (String s : fSuspicious) {
                                        staff.sendMessage(colorize(susItem.replace("%item%", s)));
                                    }
                                }

                                if (!fWarnings.isEmpty()) {
                                    staff.sendMessage(colorize(msg.getMessage("scan-results-warnings-title",
                                            "&e&lWarnings (%count%):").replace("%count%", String.valueOf(fWarnings.size()))));
                                    String warnItem = msg.getMessage("scan-results-warnings-item", "  &e\u00bb &f%item%");
                                    for (String w : fWarnings) {
                                        staff.sendMessage(colorize(warnItem.replace("%item%", w)));
                                    }
                                }

                                if (!fIntegrity.isEmpty()) {
                                    staff.sendMessage(colorize(msg.getMessage("scan-results-integrity-title",
                                            "&d&lIntegrity (%count%):").replace("%count%", String.valueOf(fIntegrity.size()))));
                                    String intItem = msg.getMessage("scan-results-integrity-item", "  &d\u00bb &f%item%");
                                    for (String ig : fIntegrity) {
                                        staff.sendMessage(colorize(intItem.replace("%item%", ig)));
                                    }
                                }

                                staff.sendMessage("");
                                staff.sendMessage(colorize(msg.getMessage("scan-results-vpn", "&bVPN: &f%vpn%").replace("%vpn%", fVpn)));
                                staff.sendMessage(colorize(msg.getMessage("scan-results-country", "&bCountry: &f%country%").replace("%country%", fCountry)));
                                if (!fScantime.isEmpty()) {
                                    staff.sendMessage(colorize(msg.getMessage("scan-results-scantime", "&7Scan Time: &f%scantime%").replace("%scantime%", fScantime)));
                                }
                                if (!fWindows.isEmpty()) {
                                    staff.sendMessage(colorize(msg.getMessage("scan-results-windows", "&7Windows: &f%windows%").replace("%windows%", fWindows)));
                                }

                                try {
                                    if (resultsJson.has("data") && !resultsJson.get("data").isJsonNull()) {
                                        JsonObject data = resultsJson.getAsJsonObject("data");
                                        if (data.has("discord") && !data.get("discord").isJsonNull() && data.get("discord").isJsonArray()) {
                                            JsonArray discordAccounts = data.getAsJsonArray("discord");
                                            if (!discordAccounts.isEmpty()) {
                                                staff.sendMessage("");
                                                String relatedTitle = msg.getMessage("scan-results-related-title",
                                                        "&6&lDiscord Accounts Detected: %count%");
                                                staff.sendMessage(colorize(relatedTitle.replace("%count%", String.valueOf(discordAccounts.size()))));

                                                for (int i = 0; i < discordAccounts.size(); i++) {
                                                    JsonObject account = discordAccounts.get(i).getAsJsonObject();
                                                    String discordId = account.has("id") ? account.get("id").getAsString() : "";
                                                    String username = account.has("username") ? account.get("username").getAsString() : "Unknown";

                                                    if (!discordId.isEmpty()) {
                                                        String accountMsg = msg.getMessage("scan-results-related-account",
                                                                "&7\u00bb &fDiscord: &e%username% &7(ID: %id%)");
                                                        staff.sendMessage(colorize(accountMsg
                                                                .replace("%username%", username)
                                                                .replace("%id%", discordId)));
                                                        sendClickableButtons(staff, discordId);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    plugin.getLogger().warning("[Scan] Error parsing Discord accounts: " + e.getMessage());
                                }

                                staff.sendMessage(colorize(msg.getMessage("scan-results-url",
                                        "&b&lView Full Results: &f%url%").replace("%url%", scanUrl)));
                                staff.sendMessage(colorize(msg.getMessage("scan-results-footer", "&7&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")));
                            }

                            String broadcastMsg = msg.getMessage("scan-results-broadcast",
                                    "&e[Ocean] &aScan completed for &e%player% &a- Result: &f%result%");
                            for (Player onlineStaff : Bukkit.getOnlinePlayers()) {
                                if (onlineStaff.hasPermission("ocean.staff") && !onlineStaff.equals(staff)) {
                                    onlineStaff.sendMessage(colorize(broadcastMsg
                                            .replace("%player%", targetName)
                                            .replace("%result%", result)));
                                }
                            }
                        } catch (Exception e) {
                            plugin.getLogger().warning("[Scan] Error sending scan results to staff: " + e.getMessage());
                        }
                    });
                } else {
                    String errorResponse = readErrorStream(conn);
                    logApiError("GET", fullUrl, responseCode, errorResponse);
                }

                conn.disconnect();

            } catch (Exception e) {
                logApiException("getResults", fullUrl, e);
            }
        });
    }

    private void sendClickableButtons(Player player, String discordId) {
        MessageManager msg = plugin.getMessageManager();
        String lookupBtn = msg.getMessage("scan-results-related-lookup-btn", "&e[Lookup]");
        String lookupCmd = "/ocean lookup " + discordId;
        String lookupHover = msg.getMessage("scan-results-related-lookup-hover",
                "&eClick to view the full scan history");

        String riskBtn = msg.getMessage("scan-results-related-risk-btn", "&6[Risk Score]");
        String riskCmd = "/ocean riskscore " + discordId;
        String riskHover = msg.getMessage("scan-results-related-risk-hover",
                "&6Click to check the risk analysis");

        ClickableMessage.sendDouble(player, "  ", lookupBtn, lookupCmd, lookupHover, riskBtn, riskCmd, riskHover);
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

    private String parseErrorMessage(String errorResponse) {
        if (errorResponse == null || errorResponse.isEmpty()) {
            return "Unknown error";
        }
        try {
            JsonObject errorJson = gson.fromJson(errorResponse, JsonObject.class);
            if (errorJson.has("message")) {
                return errorJson.get("message").getAsString();
            }
            if (errorJson.has("error")) {
                return errorJson.get("error").getAsString();
            }
        } catch (Exception ignored) {
        }
        return errorResponse;
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
        plugin.getLogger().severe("║ Stack:     " + java.util.Arrays.toString(e.getStackTrace()));
        plugin.getLogger().severe("╚══════════════════════════════════");
    }

    private String colorize(String message) {
        return message.replace("&", "\u00a7");
    }
}
