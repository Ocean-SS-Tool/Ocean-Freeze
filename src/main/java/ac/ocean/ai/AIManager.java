package ac.ocean.ai;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ac.ocean.OceanPlugin;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class AIManager {

    private final OceanPlugin plugin;
    private final Gson gson;

    public AIManager(OceanPlugin plugin) {
        this.plugin = plugin;
        this.gson = new Gson();
    }

    public boolean isConfigured() {
        if (!plugin.getConfig().getBoolean("ai.enabled", true)) {
            return false;
        }
        String apiKey = plugin.getConfig().getString("ai.api-key", "");
        return apiKey != null && !apiKey.trim().isEmpty() && !apiKey.equalsIgnoreCase("YOUR_AI_API_KEY_HERE");
    }

    public interface AICallback {
        void onSuccess(String response);
        void onError(String errorMessage);
    }

    public void ask(CommandSender sender, String query) {
        if (!isConfigured()) {
            sender.sendMessage(colorize(plugin.getMessageManager().getMessage("ai-not-configured",
                    "&c✗ AI integration is not configured! Please set 'ai.api-key' in config.yml.")));
            return;
        }

        sender.sendMessage(colorize(plugin.getMessageManager().getMessage("ai-thinking",
                "&7[AI] &eConsulting AI assistant, please wait...")));

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String contextData = extractAndFetchContext(query);

                String liveDocs = fetchDocumentationQuietly();

                String basePrompt = plugin.getConfig().getString("ai.system-prompt",
                        "You are Ocean AI, an expert screenshare forensic security assistant for Minecraft server staff. " +
                        "Provide concise, authoritative advice formatted cleanly for Minecraft chat using bullet points. " +
                        "When evaluating a PIN, scan report, or detections, always provide a clear Verdict [CLEAN / SUSPICIOUS / HIGH RISK / CONFIRMED CHEATER], " +
                        "a Decision [Ban / Tempban / Watch / Release], and specific technical reasons from Ocean anti-cheat.");

                String systemPrompt = basePrompt +
                        (liveDocs.isEmpty() ? "" : "\n\n=== Live Documentation Read from https://anticheat.ac/docs ===\n" + liveDocs) +
                        (contextData.isEmpty() ? "" : "\n\n=== Live Scan / Player Intelligence ===\n" + contextData);

                JsonArray messages = new JsonArray();
                JsonObject sysMsg = new JsonObject();
                sysMsg.addProperty("role", "system");
                sysMsg.addProperty("content", systemPrompt);
                messages.add(sysMsg);

                JsonObject userMsg = new JsonObject();
                userMsg.addProperty("role", "user");
                userMsg.addProperty("content", query);
                messages.add(userMsg);

                sendChatCompletion(messages, new AICallback() {
                    @Override
                    public void onSuccess(String response) {
                        Bukkit.getScheduler().runTask(plugin, () -> displayAIResponse(sender, "&6&l[Ocean AI Answer]", response));
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(colorize(plugin.getMessageManager().getMessage("ai-error",
                                "&c[AI Error]: &f%error%").replace("%error%", errorMessage))));
                    }
                });

            } catch (Exception e) {
                Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(colorize(plugin.getMessageManager().getMessage("ai-error",
                        "&c[AI Error]: &f%error%").replace("%error%", e.getMessage()))));
            }
        });
    }

    public void analyzeScan(CommandSender sender, String pin, String targetPlayerName, JsonObject scanResults) {
        if (!isConfigured()) {
            if (sender != null) {
                sender.sendMessage(colorize(plugin.getMessageManager().getMessage("ai-not-configured",
                        "&c✗ AI integration is not configured! Please set 'ai.api-key' in config.yml.")));
            }
            return;
        }

        if (sender != null) {
            sender.sendMessage(colorize(plugin.getMessageManager().getMessage("ai-analyzing-scan",
                    "&7[AI] &eGenerating forensic AI analysis for &f%player% &7(PIN: &e%pin%&7)...")
                    .replace("%player%", targetPlayerName)
                    .replace("%pin%", pin)));
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                JsonObject finalScanResults = scanResults;
                if (finalScanResults == null || !finalScanResults.has("data") || finalScanResults.get("data").isJsonNull()) {
                    String fetchedJson = fetchPinResultsQuietly(pin);
                    if (!fetchedJson.isEmpty()) {
                        try {
                            finalScanResults = gson.fromJson(fetchedJson, JsonObject.class);
                        } catch (Exception ignored) {}
                    }
                }

                String pinStatus = fetchPinStatusQuietly(pin);

                StringBuilder fullContext = new StringBuilder();
                fullContext.append("=== COMPREHENSIVE SCAN REPORT ===\n");
                fullContext.append("PIN: ").append(pin).append("\n");
                fullContext.append("Target Player: ").append(targetPlayerName).append("\n");
                if (!pinStatus.isEmpty()) {
                    fullContext.append("Live Scan Progress & Status: ").append(pinStatus).append("\n");
                }

                if (finalScanResults != null) {
                    if (finalScanResults.has("result")) {
                        fullContext.append("Overall Result: ").append(finalScanResults.get("result").getAsString()).append("\n");
                    }

                    if (finalScanResults.has("data") && !finalScanResults.get("data").isJsonNull()) {
                        JsonObject data = finalScanResults.getAsJsonObject("data");

                        if (data.has("detections") && data.get("detections").isJsonArray()) {
                            JsonArray arr = data.getAsJsonArray("detections");
                            fullContext.append("Direct/Confirmed Detections (").append(arr.size()).append("): ");
                            for (int i = 0; i < arr.size(); i++) {
                                JsonElement el = arr.get(i);
                                fullContext.append(el.isJsonPrimitive() ? el.getAsString() : el.toString()).append("; ");
                            }
                            fullContext.append("\n");
                        }

                        if (data.has("suspicious") && data.get("suspicious").isJsonArray()) {
                            JsonArray arr = data.getAsJsonArray("suspicious");
                            fullContext.append("Suspicious Logs/Processes (").append(arr.size()).append("): ");
                            for (int i = 0; i < arr.size(); i++) {
                                JsonElement el = arr.get(i);
                                fullContext.append(el.isJsonPrimitive() ? el.getAsString() : el.toString()).append("; ");
                            }
                            fullContext.append("\n");
                        }

                        if (data.has("warnings") && data.get("warnings").isJsonArray()) {
                            JsonArray arr = data.getAsJsonArray("warnings");
                            fullContext.append("Warning Logs & Evasion Indicators (").append(arr.size()).append("): ");
                            for (int i = 0; i < arr.size(); i++) {
                                JsonElement el = arr.get(i);
                                fullContext.append(el.isJsonPrimitive() ? el.getAsString() : el.toString()).append("; ");
                            }
                            fullContext.append("\n");
                        }
                        if (data.has("integrity") && data.get("integrity").isJsonArray()) {
                            JsonArray arr = data.getAsJsonArray("integrity");
                            fullContext.append("Integrity Checks / RUIN Violations (").append(arr.size()).append("): ");
                            for (int i = 0; i < arr.size(); i++) {
                                JsonElement el = arr.get(i);
                                fullContext.append(el.isJsonPrimitive() ? el.getAsString() : el.toString()).append("; ");
                            }
                            fullContext.append("\n");
                        }

                        if (data.has("vpn") && !data.get("vpn").isJsonNull()) fullContext.append("VPN Detected: ").append(data.get("vpn").getAsString()).append("\n");
                        if (data.has("country") && !data.get("country").isJsonNull()) fullContext.append("Country: ").append(data.get("country").getAsString()).append("\n");
                        if (data.has("windows") && !data.get("windows").isJsonNull()) fullContext.append("Windows Build: ").append(data.get("windows").getAsString()).append("\n");
                        if (data.has("scantime") && !data.get("scantime").isJsonNull()) fullContext.append("Scan Duration: ").append(data.get("scantime").getAsString()).append("\n");

                        if (data.has("discord") && data.get("discord").isJsonArray()) {
                            JsonArray discordArr = data.getAsJsonArray("discord");
                            for (int i = 0; i < Math.min(discordArr.size(), 2); i++) {
                                JsonObject acc = discordArr.get(i).getAsJsonObject();
                                if (acc.has("id")) {
                                    String dId = acc.get("id").getAsString();
                                    String user = acc.has("username") ? acc.get("username").getAsString() : dId;
                                    fullContext.append("\nLinked Discord Account: ").append(user).append(" (ID: ").append(dId).append(")");
                                    String dbInfo = fetchDbQuietly(dId);
                                    if (!dbInfo.isEmpty()) {
                                        fullContext.append("\n- Cheater Database Profile: ").append(dbInfo);
                                    }
                                    String riskInfo = fetchRiskQuietly(dId);
                                    if (!riskInfo.isEmpty()) {
                                        fullContext.append("\n- Calculated Risk Score: ").append(riskInfo);
                                    }
                                }
                            }
                        }
                    }
                }

                String liveDocs = fetchDocumentationQuietly();
                String systemPrompt = "You are Ocean AI, an expert Minecraft AntiCheat and screenshare forensics investigator. " +
                        (liveDocs.isEmpty() ? "" : "\nUse the following official Ocean documentation read from https://anticheat.ac/docs to interpret all detection types, warning logs, suspicious residues, and integrity results:\n" + liveDocs + "\n") +
                        "Evaluate ALL possible results in this scan: Detections, Suspicious tools, Warnings (Prefetch/ActivitiesCache), Integrity (RUIN, hooks, memory injection), Network/VPN, and Linked Discord history. " +
                        "Make a definitive decision following this structure for Minecraft chat: " +
                        "\n1. Verdict: [CLEAN / SUSPICIOUS / HIGH RISK / CONFIRMED CHEATER]" +
                        "\n2. Decision: [Permanent Ban / 7-Day Tempban / Watch / Release]" +
                        "\n3. Decisive Reasons: (2-3 concise bullet points detailing why this decision was reached based on the scan findings)" +
                        "\n4. Action for Staff: (1 clear operational next step, e.g. 'Execute /ban <player> Cheating [PIN]', or 'Request modlist check')" +
                        "\nKeep it concise and readable in Minecraft chat. Avoid markdown headers (#) and code blocks.";

                JsonArray messages = new JsonArray();
                JsonObject sysMsg = new JsonObject();
                sysMsg.addProperty("role", "system");
                sysMsg.addProperty("content", systemPrompt);
                messages.add(sysMsg);

                JsonObject userMsg = new JsonObject();
                userMsg.addProperty("role", "user");
                userMsg.addProperty("content", "Please analyze this scan:\n" + fullContext.toString());
                messages.add(userMsg);

                sendChatCompletion(messages, new AICallback() {
                    @Override
                    public void onSuccess(String response) {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            String header = "&6&l[Ocean AI Analysis - PIN: " + pin + "]";
                            if (sender != null) {
                                displayAIResponse(sender, header, response);
                            } else {
                                for (Player p : Bukkit.getOnlinePlayers()) {
                                    if (p.hasPermission("ocean.staff")) {
                                        displayAIResponse(p, header, response);
                                    }
                                }
                            }
                        });
                    }

                    @Override
                    public void onError(String errorMessage) {
                        if (sender != null) {
                            Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(colorize(plugin.getMessageManager().getMessage("ai-error",
                                    "&c[AI Error]: &f%error%").replace("%error%", errorMessage))));
                        }
                    }
                });

            } catch (Exception e) {
                if (sender != null) {
                    Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(colorize(plugin.getMessageManager().getMessage("ai-error",
                            "&c[AI Error]: &f%error%").replace("%error%", e.getMessage()))));
                }
            }
        });
    }

    public void analyzeDiscordId(CommandSender sender, String discordId) {
        if (!isConfigured()) {
            sender.sendMessage(colorize(plugin.getMessageManager().getMessage("ai-not-configured",
                    "&c✗ AI integration is not configured! Please set 'ai.api-key' in config.yml.")));
            return;
        }

        sender.sendMessage(colorize(plugin.getMessageManager().getMessage("ai-analyzing-user",
                "&7[AI] &eInvestigating Discord ID &f%discordid% &ewith AI intelligence...")
                .replace("%discordid%", discordId)));

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String dbData = fetchDbQuietly(discordId);
                String riskData = fetchRiskQuietly(discordId);
                String lookupData = fetchLookupQuietly(discordId);

                String fullContext = "Discord ID: " + discordId +
                        "\nCheater Database Query:\n" + dbData +
                        "\nRisk Score Analysis:\n" + riskData +
                        "\nScanned User History:\n" + lookupData;

                String liveDocs = fetchDocumentationQuietly();
                String systemPrompt = "You are Ocean AI Security Auditor. Analyze all intelligence for this Discord user. " +
                        (liveDocs.isEmpty() ? "" : "\nUse the official Ocean documentation read from https://anticheat.ac/docs to interpret detections:\n" + liveDocs + "\n") +
                        "Summarize their threat level, ban history, confirmed cheater status, and previous detections in 3-5 concise bullet points for Minecraft staff.";

                JsonArray messages = new JsonArray();
                JsonObject sysMsg = new JsonObject();
                sysMsg.addProperty("role", "system");
                sysMsg.addProperty("content", systemPrompt);
                messages.add(sysMsg);

                JsonObject userMsg = new JsonObject();
                userMsg.addProperty("role", "user");
                userMsg.addProperty("content", "Analyze this player profile:\n" + fullContext);
                messages.add(userMsg);

                sendChatCompletion(messages, new AICallback() {
                    @Override
                    public void onSuccess(String response) {
                        Bukkit.getScheduler().runTask(plugin, () -> displayAIResponse(sender, "&6&l[Ocean AI Profile Check: " + discordId + "]", response));
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(colorize(plugin.getMessageManager().getMessage("ai-error",
                                "&c[AI Error]: &f%error%").replace("%error%", errorMessage))));
                    }
                });

            } catch (Exception e) {
                Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(colorize(plugin.getMessageManager().getMessage("ai-error",
                        "&c[AI Error]: &f%error%").replace("%error%", e.getMessage()))));
            }
        });
    }

    private void sendChatCompletion(JsonArray messages, AICallback callback) {
        String endpoint = plugin.getConfig().getString("ai.endpoint", "https://openrouter.ai/api/v1/chat/completions");
        String apiKey = plugin.getConfig().getString("ai.api-key", "");
        String model = plugin.getConfig().getString("ai.model", "openai/gpt-4o-mini");
        double temperature = plugin.getConfig().getDouble("ai.temperature", 0.3);
        int maxTokens = plugin.getConfig().getInt("ai.max-tokens", 600);

        try {
            URL url = new URL(endpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);

            boolean isAgentRouter = endpoint.contains("agentrouter") || plugin.getConfig().getBoolean("ai.agentrouter-mode", false);

            if (isAgentRouter) {
                conn.setRequestProperty("User-Agent", plugin.getConfig().getString("ai.custom-user-agent", "claude-cli/0.2.29 (external, cli)"));
                conn.setRequestProperty("x-app", "cli");
                conn.setRequestProperty("anthropic-version", "2023-06-01");
                conn.setRequestProperty("anthropic-beta", "claude-code-20250219,interleaved-thinking-2025-05-14");
                conn.setRequestProperty("anthropic-dangerous-direct-browser-access", "true");
                conn.setRequestProperty("X-Stainless-Lang", "js");
                conn.setRequestProperty("X-Stainless-Package-Version", "0.2.29");
                conn.setRequestProperty("X-Stainless-OS", "Windows");
                conn.setRequestProperty("X-Stainless-Arch", "x64");
                conn.setRequestProperty("X-Stainless-Runtime", "node");
                conn.setRequestProperty("X-Stainless-Runtime-Version", "v20.18.0");
            } else {
                String customUserAgent = plugin.getConfig().getString("ai.custom-user-agent", "");
                if (customUserAgent != null && !customUserAgent.trim().isEmpty()) {
                    conn.setRequestProperty("User-Agent", customUserAgent.trim());
                } else {
                    conn.setRequestProperty("User-Agent", "Ocean-Minecraft-Plugin/1.0");
                }

                String siteUrl = plugin.getConfig().getString("ai.site-url", "https://anticheat.ac");
                String siteName = plugin.getConfig().getString("ai.site-name", "Ocean Freeze AntiCheat");
                conn.setRequestProperty("HTTP-Referer", siteUrl);
                conn.setRequestProperty("X-Title", siteName);
            }

            if (plugin.getConfig().isConfigurationSection("ai.custom-headers")) {
                org.bukkit.configuration.ConfigurationSection sec = plugin.getConfig().getConfigurationSection("ai.custom-headers");
                for (String key : sec.getKeys(false)) {
                    conn.setRequestProperty(key, sec.getString(key));
                }
            }

            conn.setConnectTimeout(45000);
            conn.setReadTimeout(45000);
            conn.setDoOutput(true);

            JsonObject body = new JsonObject();
            body.addProperty("model", model);
            body.add("messages", messages);
            body.addProperty("temperature", temperature);
            body.addProperty("max_tokens", maxTokens);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = gson.toJson(body).getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();

            if (responseCode >= 200 && responseCode < 300) {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder resp = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    resp.append(line);
                }
                br.close();

                JsonObject json = gson.fromJson(resp.toString(), JsonObject.class);
                if (json.has("choices") && json.getAsJsonArray("choices").size() > 0) {
                    JsonObject choice = json.getAsJsonArray("choices").get(0).getAsJsonObject();
                    if (choice.has("message") && choice.get("message").isJsonObject()) {
                        JsonObject msg = choice.getAsJsonObject("message");
                        if (msg.has("content") && !msg.get("content").isJsonNull()) {
                            String text = msg.get("content").getAsString();
                            if (!text.trim().isEmpty()) {
                                callback.onSuccess(text);
                                return;
                            }
                        }
                        if (msg.has("reasoning_content") && !msg.get("reasoning_content").isJsonNull()) {
                            String text = msg.get("reasoning_content").getAsString();
                            if (!text.trim().isEmpty()) {
                                callback.onSuccess(text);
                                return;
                            }
                        }
                    }
                }
                callback.onError("AI provider returned an empty completion.");
            } else {
                StringBuilder err = new StringBuilder();
                if (conn.getErrorStream() != null) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
                    String line;
                    while ((line = br.readLine()) != null) {
                        err.append(line);
                    }
                    br.close();
                }
                callback.onError("HTTP " + responseCode + " - " + parseAiError(err.toString()));
            }

            conn.disconnect();

        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    private String parseAiError(String raw) {
        try {
            JsonObject json = gson.fromJson(raw, JsonObject.class);
            if (json.has("error")) {
                JsonElement el = json.get("error");
                if (el.isJsonObject() && el.getAsJsonObject().has("message")) {
                    return el.getAsJsonObject().get("message").getAsString();
                } else {
                    return el.getAsString();
                }
            }
        } catch (Exception ignored) {
        }
        return raw.isEmpty() ? "Unknown API error" : raw;
    }

    private void displayAIResponse(CommandSender sender, String title, String content) {
        if (content == null || content.trim().isEmpty()) {
            sender.sendMessage(colorize("&c[AI]: No response was generated."));
            return;
        }

        sender.sendMessage(colorize(plugin.getMessageManager().getMessage("ai-header", "&7&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")));
        sender.sendMessage(colorize(title));
        sender.sendMessage("");

        String[] lines = content.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue; 

            String formatted = trimmed.replaceAll("^#+\\s*", "")
                                      .replaceAll("\\*\\*(.*?)\\*\\*", "§e$1§f")
                                      .replaceAll("\\*(.*?)\\*", "§7$1§f")
                                      .replaceAll("`([^`]+)`", "§b$1§f");
            sender.sendMessage(colorize("&f" + formatted));
        }

        sender.sendMessage("");
        sender.sendMessage(colorize(plugin.getMessageManager().getMessage("ai-footer", "&7&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")));
    }

    private String extractAndFetchContext(String query) {
        StringBuilder ctx = new StringBuilder();

        String[] words = query.split("\\s+");
        for (String w : words) {
            String clean = w.replaceAll("[^a-zA-Z0-9]", "");
            if ((clean.length() >= 6 && clean.length() <= 8) && !clean.equalsIgnoreCase("frozen") && !clean.equalsIgnoreCase("freeze") && !clean.equalsIgnoreCase("analyze")) {
                String pinResults = fetchPinResultsQuietly(clean);
                if (!pinResults.isEmpty()) {
                    ctx.append("\nScan Results for PIN '").append(clean).append("':\n").append(pinResults);
                } else {
                    String pinStatus = fetchPinStatusQuietly(clean);
                    if (!pinStatus.isEmpty()) {
                        ctx.append("\nScan Status for PIN '").append(clean).append("':\n").append(pinStatus);
                    }
                }
            }
        }

        for (String w : words) {
            String clean = w.replaceAll("[^0-9]", "");
            if (clean.length() >= 5 && clean.length() <= 25) {
                String db = fetchDbQuietly(clean);
                String risk = fetchRiskQuietly(clean);
                String lookup = fetchLookupQuietly(clean);
                ctx.append("\nDiscord ID Mentioned (").append(clean).append("):");
                if (!db.isEmpty()) ctx.append("\n- Cheater DB: ").append(db);
                if (!risk.isEmpty()) ctx.append("\n- Risk Score: ").append(risk);
                if (!lookup.isEmpty()) ctx.append("\n- Scanned History: ").append(lookup);
            }
        }

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (query.toLowerCase().contains(p.getName().toLowerCase())) {
                boolean frozen = plugin.getFreezeManager().isFrozen(p);
                String pin = "None";
                if (frozen && plugin.getFreezeManager().getFrozenPlayer(p.getUniqueId()) != null) {
                    pin = plugin.getFreezeManager().getFrozenPlayer(p.getUniqueId()).getScanPin();
                    if (pin != null && !pin.isEmpty() && !pin.equalsIgnoreCase("None")) {
                        String scanRes = fetchPinResultsQuietly(pin);
                        if (!scanRes.isEmpty()) {
                            ctx.append("\nActive Scan Data for ").append(p.getName()).append(" (PIN: ").append(pin).append("):\n").append(scanRes);
                        }
                    }
                }
                ctx.append("\nPlayer Mentioned: ").append(p.getName()).append(" (Frozen: ").append(frozen).append(", Active PIN: ").append(pin).append(")");
            }
        }

        return ctx.toString().trim();
    }

    private String fetchPinResultsQuietly(String pin) {
        String apiKey = plugin.getConfig().getString("anticheat.api-key");
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("YOUR_API_KEY_HERE")) return "";
        try {
            URL u = new URL("https://api.anticheat.ac/v1/pins/" + pin + "/results");
            HttpURLConnection c = (HttpURLConnection) u.openConnection();
            c.setRequestMethod("GET");
            c.setRequestProperty("x-api-key", apiKey);
            c.setRequestProperty("Accept", "application/json");
            c.setConnectTimeout(6000);
            c.setReadTimeout(6000);
            if (c.getResponseCode() == 200) {
                BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String l;
                while ((l = br.readLine()) != null) sb.append(l);
                br.close();
                return sb.toString();
            }
        } catch (Exception ignored) {}
        return "";
    }

    private String fetchPinStatusQuietly(String pin) {
        String apiKey = plugin.getConfig().getString("anticheat.api-key");
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("YOUR_API_KEY_HERE")) return "";
        try {
            URL u = new URL("https://api.anticheat.ac/v1/pins/" + pin + "/status");
            HttpURLConnection c = (HttpURLConnection) u.openConnection();
            c.setRequestMethod("GET");
            c.setRequestProperty("x-api-key", apiKey);
            c.setRequestProperty("Accept", "application/json");
            c.setConnectTimeout(6000);
            c.setReadTimeout(6000);
            if (c.getResponseCode() == 200) {
                BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String l;
                while ((l = br.readLine()) != null) sb.append(l);
                br.close();
                return sb.toString();
            }
        } catch (Exception ignored) {}
        return "";
    }

    private String fetchDbQuietly(String discordId) {
        String apiKey = plugin.getConfig().getString("anticheat.api-key");
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("YOUR_API_KEY_HERE")) return "";
        try {
            URL u = new URL("https://api.anticheat.ac/v1/db/query/" + discordId);
            HttpURLConnection c = (HttpURLConnection) u.openConnection();
            c.setRequestMethod("GET");
            c.setRequestProperty("x-api-key", apiKey);
            c.setRequestProperty("Accept", "application/json");
            c.setConnectTimeout(6000);
            c.setReadTimeout(6000);
            if (c.getResponseCode() == 200) {
                BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String l;
                while ((l = br.readLine()) != null) sb.append(l);
                br.close();
                return sb.toString();
            }
        } catch (Exception ignored) {}
        return "";
    }

    private String fetchRiskQuietly(String discordId) {
        String apiKey = plugin.getConfig().getString("anticheat.api-key");
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("YOUR_API_KEY_HERE")) return "";
        try {
            URL u = new URL("https://api.anticheat.ac/v1/users/" + discordId + "/risk-score");
            HttpURLConnection c = (HttpURLConnection) u.openConnection();
            c.setRequestMethod("GET");
            c.setRequestProperty("x-api-key", apiKey);
            c.setRequestProperty("Accept", "application/json");
            c.setConnectTimeout(6000);
            c.setReadTimeout(6000);
            if (c.getResponseCode() == 200) {
                BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String l;
                while ((l = br.readLine()) != null) sb.append(l);
                br.close();
                return sb.toString();
            }
        } catch (Exception ignored) {}
        return "";
    }

    private String fetchLookupQuietly(String discordId) {
        String apiKey = plugin.getConfig().getString("anticheat.api-key");
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("YOUR_API_KEY_HERE")) return "";
        try {
            URL u = new URL("https://api.anticheat.ac/v1/scanned-users/lookup/" + discordId);
            HttpURLConnection c = (HttpURLConnection) u.openConnection();
            c.setRequestMethod("GET");
            c.setRequestProperty("x-api-key", apiKey);
            c.setRequestProperty("Accept", "application/json");
            c.setConnectTimeout(6000);
            c.setReadTimeout(6000);
            if (c.getResponseCode() == 200) {
                BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String l;
                while ((l = br.readLine()) != null) sb.append(l);
                br.close();
                return sb.toString();
            }
        } catch (Exception ignored) {}
        return "";
    }

    private volatile String cachedDocs = "";
    private volatile long lastDocsFetchTime = 0L;
    private static final long DOCS_CACHE_TTL_MS = 15 * 60 * 1000L;

    private String fetchDocumentationQuietly() {
        long now = System.currentTimeMillis();
        if (!cachedDocs.isEmpty() && (now - lastDocsFetchTime < DOCS_CACHE_TTL_MS)) {
            return cachedDocs;
        }

        try {
            URL u = new URL("https://anticheat.ac/docs");
            HttpURLConnection conn = (HttpURLConnection) u.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(7000);

            if (conn.getResponseCode() == 200) {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder rawHtml = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    rawHtml.append(line).append("\n");
                }
                br.close();

                String cleaned = cleanHtmlToText(rawHtml.toString());
                if (!cleaned.isEmpty()) {
                    cachedDocs = cleaned;
                    lastDocsFetchTime = now;
                    return cleaned;
                }
            }
        } catch (Exception ignored) {}

        return cachedDocs;
    }
    private String cleanHtmlToText(String html) {
        if (html == null || html.isEmpty()) return "";
        String stripped = html.replaceAll("(?is)<script.*?</script>", " ")
                              .replaceAll("(?is)<style.*?</style>", " ")
                              .replaceAll("(?is)<svg.*?</svg>", " ")
                              .replaceAll("(?is)<noscript.*?</noscript>", " ")
                              .replaceAll("<[^>]+>", " ")
                              .replaceAll("&nbsp;", " ")
                              .replaceAll("&amp;", "&")
                              .replaceAll("&quot;", "\"")
                              .replaceAll("&apos;", "'")
                              .replaceAll("&lt;", "<")
                              .replaceAll("&gt;", ">");

        stripped = stripped.replaceAll("\\s+", " ").trim();

        if (stripped.length() > 6000) {
            stripped = stripped.substring(0, 6000);
        }
        return stripped;
    }

    private String colorize(String msg) {
        return msg.replace("&", "\u00a7");
    }
}
