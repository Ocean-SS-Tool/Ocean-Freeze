package ac.ocean.discord;

import ac.ocean.OceanPlugin;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Manager for Discord webhooks with elegant message formatting
 */
public class WebhookManager {

    private final OceanPlugin plugin;
    private static final SimpleDateFormat ISO_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    static {
        ISO_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public WebhookManager(OceanPlugin plugin) {
        this.plugin = plugin;
    }

    public void sendFreezeNotification(String playerName, String staffName, String pin) {
        String webhookUrl = plugin.getConfig().getString("discord.webhook-url");
        if (webhookUrl == null || webhookUrl.isEmpty() || webhookUrl.equals("none")) {
            plugin.getLogger().info("[Webhook] Freeze webhook skipped - webhook URL not configured");
            return;
        }

        if (!plugin.getConfig().getBoolean("discord.log-freeze", true)) {
            return;
        }

        String username = plugin.getConfig().getString("discord.bot-name", "Ocean Freeze");
        String avatarUrl = plugin.getConfig().getString("discord.bot-avatar", "");

        int color = plugin.getConfig().getInt("discord.freeze-embed-color", 3447003); // Blue

        String title = plugin.getConfig().getString("discord.freeze-embed-title", "🧊 Player Frozen")
                .replace("{player}", playerName)
                .replace("{staff}", staffName)
                .replace("{pin}", pin != null ? pin : "N/A");
        String description = plugin.getConfig().getString("discord.freeze-embed-description",
                        "**{player}** has been frozen by **{staff}**")
                .replace("{player}", playerName)
                .replace("{staff}", staffName)
                .replace("{pin}", pin != null ? pin : "N/A");

        DiscordWebhook webhook = new DiscordWebhook(webhookUrl)
                .setUsername(username);

        if (!avatarUrl.isEmpty()) {
            webhook.setAvatarUrl(avatarUrl);
        }

        DiscordWebhook.EmbedObject embed = new DiscordWebhook.EmbedObject()
                .setTitle(title)
                .setDescription(description)
                .setColor(color)
                .setTimestamp(ISO_FORMAT.format(new Date()));

        String playerFieldName = plugin.getConfig().getString("discord.freeze-field-player-name", "👤 Player");
        String playerFieldValue = plugin.getConfig().getString("discord.freeze-field-player-value", "{player}")
                .replace("{player}", playerName);
        embed.addField(playerFieldName, playerFieldValue, true);

        String staffFieldName = plugin.getConfig().getString("discord.freeze-field-staff-name", "👮 Staff");
        String staffFieldValue = plugin.getConfig().getString("discord.freeze-field-staff-value", "{staff}")
                .replace("{staff}", staffName);
        embed.addField(staffFieldName, staffFieldValue, true);

        if (pin != null && !pin.isEmpty()) {
            String pinFieldName = plugin.getConfig().getString("discord.freeze-field-pin-name", "🔑 Scan PIN");
            String pinFieldValue = plugin.getConfig().getString("discord.freeze-field-pin-value", "`{pin}`")
                    .replace("{pin}", pin);
            embed.addField(pinFieldName, pinFieldValue, true);
        }

        String footer = plugin.getConfig().getString("discord.freeze-footer", "Ocean Freeze Plugin");
        String footerIcon = plugin.getConfig().getString("discord.freeze-footer-icon", "");
        embed.setFooter(footer, footerIcon.isEmpty() ? null : footerIcon);

        plugin.getLogger().info("[Webhook] Sending freeze notification for " + playerName);
        webhook.addEmbed(embed).execute();
    }

    public void sendAdmitNotification(String playerName, String staffName) {
        String webhookUrl = plugin.getConfig().getString("discord.webhook-url");
        if (webhookUrl == null || webhookUrl.isEmpty() || webhookUrl.equals("none")) {
            return;
        }

        if (!plugin.getConfig().getBoolean("discord.log-freeze", true)) {
            return;
        }

        String username = plugin.getConfig().getString("discord.bot-name", "Ocean Freeze");
        String avatarUrl = plugin.getConfig().getString("discord.bot-avatar", "");

        int color = plugin.getConfig().getInt("discord.admit-embed-color", 15158332); // Red

        String title = plugin.getConfig().getString("discord.admit-embed-title", "\u26a0\ufe0f Player Admitted to Hacking")
                .replace("{player}", playerName)
                .replace("{staff}", staffName);
        String description = plugin.getConfig().getString("discord.admit-embed-description",
                        "**{player}** has admitted to hacking (frozen by **{staff}**)")
                .replace("{player}", playerName)
                .replace("{staff}", staffName);

        DiscordWebhook webhook = new DiscordWebhook(webhookUrl)
                .setUsername(username);

        if (!avatarUrl.isEmpty()) {
            webhook.setAvatarUrl(avatarUrl);
        }

        DiscordWebhook.EmbedObject embed = new DiscordWebhook.EmbedObject()
                .setTitle(title)
                .setDescription(description)
                .setColor(color)
                .setTimestamp(ISO_FORMAT.format(new Date()));

        embed.addField("\ud83d\udc64 Player", playerName, true);
        embed.addField("\ud83d\udc6e Staff", staffName, true);

        String footer = plugin.getConfig().getString("discord.freeze-footer", "Ocean Freeze Plugin");
        String footerIcon = plugin.getConfig().getString("discord.freeze-footer-icon", "");
        embed.setFooter(footer, footerIcon.isEmpty() ? null : footerIcon);

        plugin.getLogger().info("[Webhook] Sending admit notification for " + playerName);
        webhook.addEmbed(embed).execute();
    }

    public void sendScanCompletedNotification(String playerName, String staffName, String pin, String result,
                                              int detections, int warnings, int suspicious,
                                              String vpn, String country, String scanTime, String scanUrl,
                                              String detectionsDetail) {
        String webhookUrl = plugin.getConfig().getString("discord.webhook-url");
        if (webhookUrl == null || webhookUrl.isEmpty() || webhookUrl.equals("none")) {
            return;
        }

        if (!plugin.getConfig().getBoolean("discord.log-scan-results", true)) {
            return;
        }

        String username = plugin.getConfig().getString("discord.bot-name", "Ocean Freeze");
        String avatarUrl = plugin.getConfig().getString("discord.bot-avatar", "");

        // Determine color based on the result
        int color;
        String resultEmoji;
        if (result.equalsIgnoreCase("cheating")) {
            color = plugin.getConfig().getInt("discord.scan-embed-color-cheating", 15158332); // Red
            resultEmoji = "🚨";
        } else if (result.equalsIgnoreCase("suspicious")) {
            color = plugin.getConfig().getInt("discord.scan-embed-color-suspicious", 16776960); // Yellow
            resultEmoji = "⚠️";
        } else {
            color = plugin.getConfig().getInt("discord.scan-embed-color-legit", 3066993); // Green
            resultEmoji = "✅";
        }

        String title = replaceVariablesExtended(
                plugin.getConfig().getString("discord.scan-embed-title", "📊 Scan Completed"),
                playerName, staffName, pin, result, detections, warnings, suspicious, vpn, country, scanTime
        );
        String description = replaceVariablesExtended(
                plugin.getConfig().getString("discord.scan-embed-description", "Scan for **{player}** has been completed."),
                playerName, staffName, pin, result, detections, warnings, suspicious, vpn, country, scanTime
        );

        DiscordWebhook webhook = new DiscordWebhook(webhookUrl)
                .setUsername(username);

        if (!avatarUrl.isEmpty()) {
            webhook.setAvatarUrl(avatarUrl);
        }

        DiscordWebhook.EmbedObject embed = new DiscordWebhook.EmbedObject()
                .setTitle(title)
                .setDescription(description)
                .setColor(color)
                .setTimestamp(ISO_FORMAT.format(new Date()));

        addCustomField(embed, "discord.scan-field-player-name", "discord.scan-field-player-value",
                "👤 Player", "{player}", playerName, staffName, pin, result, detections, warnings, suspicious, vpn, country, scanTime);

        addCustomField(embed, "discord.scan-field-result-name", "discord.scan-field-result-value",
                "📋 The Result", resultEmoji + " {result}", playerName, staffName, pin, result, detections, warnings, suspicious, vpn, country, scanTime);

        addCustomField(embed, "discord.scan-field-detections-name", "discord.scan-field-detections-value",
                "🔴 Detections", "{detections}", playerName, staffName, pin, result, detections, warnings, suspicious, vpn, country, scanTime);

        addCustomField(embed, "discord.scan-field-warnings-name", "discord.scan-field-warnings-value",
                "🟡 Warnings", "{warnings}", playerName, staffName, pin, result, detections, warnings, suspicious, vpn, country, scanTime);

        addCustomField(embed, "discord.scan-field-suspicious-name", "discord.scan-field-suspicious-value",
                "🟠 Suspicious", "{suspicious}", playerName, staffName, pin, result, detections, warnings, suspicious, vpn, country, scanTime);

        addCustomField(embed, "discord.scan-field-vpn-name", "discord.scan-field-vpn-value",
                "🌐 VPN", "{vpn}", playerName, staffName, pin, result, detections, warnings, suspicious, vpn, country, scanTime);

        addCustomField(embed, "discord.scan-field-country-name", "discord.scan-field-country-value",
                "🌍 Country", "{country}", playerName, staffName, pin, result, detections, warnings, suspicious, vpn, country, scanTime);

        addCustomField(embed, "discord.scan-field-scan-time-name", "discord.scan-field-scan-time-value",
                "⏱️ Scan Time", "{scantime}", playerName, staffName, pin, result, detections, warnings, suspicious, vpn, country, scanTime);

        if (detectionsDetail != null && !detectionsDetail.isEmpty()) {
            embed.addField("🔍 Detection Details", detectionsDetail, false);
        }

        if (scanUrl != null && !scanUrl.isEmpty()) {
            String urlFieldName = plugin.getConfig().getString("discord.scan-field-url-name", "🔗 View Full Results");
            String urlFieldValue = plugin.getConfig().getString("discord.scan-field-url-value", "[Click here]({scanurl})");
            urlFieldValue = urlFieldValue
                    .replace("{scanurl}", scanUrl)
                    .replace("{scan_url}", scanUrl);
            embed.addField(urlFieldName, urlFieldValue, false);
        }

        String footer = plugin.getConfig().getString("discord.scan-footer", "Ocean Freeze Plugin");
        String footerIcon = plugin.getConfig().getString("discord.scan-footer-icon", "");
        embed.setFooter(footer, footerIcon.isEmpty() ? null : footerIcon);

        plugin.getLogger().info("[Webhook] Sending scan results for PIN " + pin);
        webhook.addEmbed(embed).execute();
    }

    private void addCustomField(DiscordWebhook.EmbedObject embed, String nameKey, String valueKey,
                                String defaultName, String defaultValue,
                                String player, String staff, String pin, String result,
                                int detections, int warnings, int suspicious,
                                String vpn, String country, String scanTime) {
        String name = plugin.getConfig().getString(nameKey, defaultName);
        String value = plugin.getConfig().getString(valueKey, defaultValue);
        value = replaceVariablesExtended(value, player, staff, pin, result, detections, warnings, suspicious, vpn, country, scanTime);
        boolean inline = plugin.getConfig().getBoolean(nameKey.replace("-name", "-inline"), true);
        embed.addField(name, value, inline);
    }

    private String replaceVariablesExtended(String text, String player, String staff, String pin,
                                            String result, int detections, int warnings, int suspicious,
                                            String vpn, String country, String scanTime) {
        return text
                .replace("{player}", player)
                .replace("{staff}", staff)
                .replace("{pin}", pin)
                .replace("{result}", result)
                .replace("{detections}", String.valueOf(detections))
                .replace("{warnings}", String.valueOf(warnings))
                .replace("{suspicious}", String.valueOf(suspicious))
                .replace("{vpn}", vpn)
                .replace("{country}", country)
                .replace("{scantime}", scanTime)
                .replace("{scan_time}", scanTime);
    }
}
