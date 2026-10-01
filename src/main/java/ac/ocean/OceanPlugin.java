package ac.ocean;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import ac.ocean.commands.FreezeCommand;
import ac.ocean.commands.OceanCommand;
import ac.ocean.discord.WebhookManager;
import ac.ocean.listeners.FreezeListener;
import ac.ocean.manager.FreezeManager;
import ac.ocean.api.AntiCheatAPI;
import ac.ocean.api.UserLookupAPI;
import ac.ocean.gui.ConfigGUI;
import ac.ocean.gui.FreezeGUI;
import ac.ocean.manager.MessageManager;

public class OceanPlugin extends JavaPlugin {

    private static OceanPlugin instance;
    private FreezeManager freezeManager;
    private MessageManager messageManager;
    private AntiCheatAPI antiCheatAPI;
    private UserLookupAPI userLookupAPI;
    private WebhookManager webhookManager;
    private FreezeListener freezeListener;
    private FreezeGUI freezeGUI;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        getConfig().options().copyDefaults(true);
        saveConfig();

        messageManager = new MessageManager(this);
        freezeManager = new FreezeManager(this);
        antiCheatAPI = new AntiCheatAPI(this);
        userLookupAPI = new UserLookupAPI(this);
        webhookManager = new WebhookManager(this);

        FreezeCommand freezeCommand = new FreezeCommand(this);
        getCommand("ss").setExecutor(freezeCommand);
        getCommand("freeze").setExecutor(freezeCommand);
        getCommand("froze").setExecutor(freezeCommand);

        OceanCommand oceanCommand = new OceanCommand(this);
        getCommand("ocean").setExecutor(oceanCommand);
        getCommand("ocean").setTabCompleter(oceanCommand);

        freezeListener = new FreezeListener(this);
        freezeGUI = new FreezeGUI(this);
        getServer().getPluginManager().registerEvents(freezeListener, this);
        getServer().getPluginManager().registerEvents(freezeGUI, this);
        getServer().getPluginManager().registerEvents(new ConfigGUI(this), this);

        printStartupBanner();
    }

    @Override
    public void onDisable() {
        if (freezeManager != null) {
            freezeManager.unfreezeAll();
        }

        getLogger().info("╔════════════════════════════════════════╗");
        getLogger().info("║     Ocean - Disabled            ║");
        getLogger().info("║     Thanks for using Ocean!            ║");
        getLogger().info("╚════════════════════════════════════════╝");
    }

    private void printStartupBanner() {
        String version = getDescription().getVersion();
        String author = getDescription().getAuthors().isEmpty() ? "Ocean Development" : getDescription().getAuthors().get(0);
        String freezeMode = getConfig().getString("settings.freeze-mode", "AUTO");
        String apiKey = getConfig().getString("anticheat.api-key", "YOUR_API_KEY_HERE");
        boolean apiConfigured = !apiKey.equals("YOUR_API_KEY_HERE") && !apiKey.isEmpty();
        String webhookUrl = getConfig().getString("discord.webhook-url", "none");
        boolean webhookConfigured = !webhookUrl.equals("none") && !webhookUrl.isEmpty();

        getLogger().info("╔════════════════════════════════════════╗");
        getLogger().info("║                                        ║");
        getLogger().info("║        🌊 OCEAN PLUGIN 🌊       ║");
        getLogger().info("║                                        ║");
        getLogger().info("╠════════════════════════════════════════╣");
        getLogger().info("║  Version: " + String.format("%-28s", version) + " ║");
        getLogger().info("║  Author:  " + String.format("%-28s", author) + " ║");
        getLogger().info("╠════════════════════════════════════════╣");
        getLogger().info("║  📋 CONFIGURATION STATUS               ║");
        getLogger().info("║  ├─ Freeze Mode: " + String.format("%-20s", freezeMode) + " ║");
        getLogger().info("║  ├─ Ocean API: " + String.format("%-15s", apiConfigured ? "✓ Configured" : "✗ Not Set") + " ║");
        getLogger().info("║  └─ Discord Webhook:  " + String.format("%-15s", webhookConfigured ? "✓ Configured" : "✗ Not Set") + " ║");
        getLogger().info("╠════════════════════════════════════════╣");
        getLogger().info("║  🚀 Plugin successfully initialized!   ║");
        getLogger().info("╚════════════════════════════════════════╝");

        if (!apiConfigured) {
            getLogger().warning("⚠ Ocean API key not configured!");
            getLogger().warning("⚠ Set 'anticheat.api-key' in config.yml");
        }
    }

    public static OceanPlugin getInstance() {
        return instance;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public FreezeManager getFreezeManager() {
        return freezeManager;
    }

    public AntiCheatAPI getAntiCheatAPI() {
        return antiCheatAPI;
    }

    public UserLookupAPI getUserLookupAPI() {
        return userLookupAPI;
    }

    public WebhookManager getWebhookManager() {
        return webhookManager;
    }

    public FreezeListener getFreezeListener() {
        return freezeListener;
    }

    public FreezeGUI getFreezeGUI() {
        return freezeGUI;
    }
}
