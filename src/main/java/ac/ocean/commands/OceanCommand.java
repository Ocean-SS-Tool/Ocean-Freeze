package ac.ocean.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import ac.ocean.OceanPlugin;
import ac.ocean.gui.ConfigGUI;
import ac.ocean.manager.MessageManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OceanCommand implements CommandExecutor, TabCompleter {

    private final OceanPlugin plugin;

    public OceanCommand(OceanPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        MessageManager msg = plugin.getMessageManager();

        if (!sender.hasPermission("ocean.staff") && !sender.hasPermission("ocean.admin")) {
            sender.sendMessage(colorize(msg.getMessage("no-permission",
                    "&cYou don't have permission to use this command!")));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "scan":
            case "lookup":
            case "riskscore":
            case "db":
            case "database":
            case "ai":
            case "ask":
                if (!sender.hasPermission("ocean.staff")) {
                    sender.sendMessage(colorize(msg.getMessage("no-permission",
                            "&cYou don't have permission to use this command!")));
                    return true;
                }
                if (subCommand.equals("scan")) handleScan(sender, args);
                else if (subCommand.equals("lookup")) handleLookup(sender, args);
                else if (subCommand.equals("riskscore")) handleRiskScore(sender, args);
                else if (subCommand.equals("db") || subCommand.equals("database")) handleDatabase(sender, args);
                else if (subCommand.equals("ai")) handleAI(sender, args);
                else handleAsk(sender, args);
                break;

            case "config":
            case "reload":
            case "mode":
                if (!sender.hasPermission("ocean.admin")) {
                    sender.sendMessage(colorize(msg.getMessage("no-permission",
                            "&cYou don't have permission to use this command!")));
                    return true;
                }
                if (subCommand.equals("config")) handleConfig(sender);
                else if (subCommand.equals("reload")) handleReload(sender);
                else handleMode(sender, args);
                break;

            case "help":
                sendHelp(sender);
                break;

            default:
                sender.sendMessage(colorize(msg.getMessage("unknown-subcommand",
                        "&cUnknown subcommand. Use &e/ocean help")));
                break;
        }

        return true;
    }

    private void handleScan(CommandSender sender, String[] args) {
        MessageManager msg = plugin.getMessageManager();

        if (args.length < 2) {
            sender.sendMessage(colorize(msg.getMessage("usage-scan",
                    "&cUsage: /ocean scan <player>")));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);

        if (target == null || !target.isOnline()) {
            sender.sendMessage(colorize(msg.getMessage("player-not-found",
                    "&cPlayer not found or not online!")));
            return;
        }

        if (!plugin.getFreezeManager().isFrozen(target)) {
            sender.sendMessage(colorize(msg.getMessage("player-not-frozen",
                    "&c%player% &eis not frozen!")
                    .replace("%player%", target.getName())));
            return;
        }

        List<String> instructions = msg.getMessageList("scan-instructions");

        plugin.getAntiCheatAPI().createPinForScan(target, (Player) sender, (pin) -> {
            for (String line : instructions) {
                target.sendMessage(colorize(line.replace("%pin%", pin)));
            }

            String staffMsg = msg.getMessage("scan-started-staff",
                    "&aScreenshare scan initiated for &e%player%&a! PIN: &e%pin%");
            sender.sendMessage(colorize(staffMsg
                    .replace("%player%", target.getName())
                    .replace("%pin%", pin)));

            if (plugin.getConfig().getBoolean("settings.broadcast-to-staff", true)) {
                for (Player staff : Bukkit.getOnlinePlayers()) {
                    if (staff.hasPermission("ocean.staff") && !staff.equals(sender)) {
                        staff.sendMessage(colorize(staffMsg
                                .replace("%player%", target.getName())
                                .replace("%pin%", pin)));
                    }
                }
            }
        });
    }

    private void handleLookup(CommandSender sender, String[] args) {
        MessageManager msg = plugin.getMessageManager();

        if (args.length < 2) {
            sender.sendMessage(colorize(msg.getMessage("usage-lookup",
                    "&cUsage: /ocean lookup <discordId>")));
            return;
        }

        String discordId = args[1];

        sender.sendMessage(colorize(msg.getMessage("retrieving-lookup",
                "&eRetrieving scan history for Discord ID: &f%discordid%")
                .replace("%discordid%", discordId)));
        plugin.getUserLookupAPI().lookupUser(sender, discordId);
    }

    private void handleRiskScore(CommandSender sender, String[] args) {
        MessageManager msg = plugin.getMessageManager();

        if (args.length < 2) {
            sender.sendMessage(colorize(msg.getMessage("usage-riskscore",
                    "&cUsage: /ocean riskscore <discordId>")));
            return;
        }

        String discordId = args[1];

        sender.sendMessage(colorize(msg.getMessage("retrieving-riskscore",
                "&eAnalyzing risk score for Discord ID: &f%discordid%")
                .replace("%discordid%", discordId)));
        plugin.getUserLookupAPI().getRiskScore(sender, discordId);
    }

    private void handleDatabase(CommandSender sender, String[] args) {
        MessageManager msg = plugin.getMessageManager();

        if (args.length < 2) {
            sender.sendMessage(colorize(msg.getMessage("usage-database",
                    "&cUsage: /ocean db <discordId>")));
            return;
        }

        String discordId = args[1];

        sender.sendMessage(colorize(msg.getMessage("retrieving-database",
                "&eSearching Cheater Database for Discord ID: &f%discordid%")
                .replace("%discordid%", discordId)));
        plugin.getDatabaseAPI().queryUser(sender, discordId);
    }

    private void handleAI(CommandSender sender, String[] args) {
        MessageManager msg = plugin.getMessageManager();

        if (args.length < 2) {
            sender.sendMessage(colorize(msg.getMessage("usage-ai",
                    "&cUsage: /ocean ai <player | pin | discordId>")));
            return;
        }

        String target = args[1];

        Player onlinePlayer = Bukkit.getPlayer(target);
        if (onlinePlayer != null && onlinePlayer.isOnline()) {
            boolean frozen = plugin.getFreezeManager().isFrozen(onlinePlayer);
            String pin = null;
            if (frozen && plugin.getFreezeManager().getFrozenPlayer(onlinePlayer.getUniqueId()) != null) {
                pin = plugin.getFreezeManager().getFrozenPlayer(onlinePlayer.getUniqueId()).getScanPin();
            }

            if (pin != null && !pin.isEmpty()) {
                fetchAndAnalyzePin(sender, pin, onlinePlayer.getName());
                return;
            } else {
                sender.sendMessage(colorize("&7[AI] &ePlayer &f" + onlinePlayer.getName() + " &ehas no active scan PIN. Consulting AI on player..."));
                plugin.getAIManager().ask(sender, "Provide an anti-cheat review and recommendation for player " + onlinePlayer.getName() + " (currently frozen: " + frozen + ")");
                return;
            }
        }

        if (target.matches("^\\d{5,25}$")) {
            plugin.getAIManager().analyzeDiscordId(sender, target);
            return;
        }

        String cleanPin = target.replaceAll("[^a-zA-Z0-9]", "");
        fetchAndAnalyzePin(sender, cleanPin, cleanPin);
    }

    private void fetchAndAnalyzePin(CommandSender sender, String pin, String playerName) {
        String apiKey = plugin.getConfig().getString("anticheat.api-key");
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("YOUR_API_KEY_HERE")) {
            sender.sendMessage(colorize(plugin.getMessageManager().getMessage("api-key-not-configured",
                    "&c✗ AntiCheat.ac API key is not configured!")));
            return;
        }

        sender.sendMessage(colorize("&7[AI] &eRetrieving scan results for PIN &f" + pin + "&e..."));

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                java.net.URL url = new java.net.URL("https://api.anticheat.ac/v1/pins/" + pin + "/results");
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("x-api-key", apiKey);
                conn.setRequestProperty("Accept", "application/json");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                int respCode = conn.getResponseCode();
                if (respCode == 200) {
                    java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(conn.getInputStream(), java.nio.charset.StandardCharsets.UTF_8));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                    br.close();

                    com.google.gson.JsonObject results = new com.google.gson.Gson().fromJson(sb.toString(), com.google.gson.JsonObject.class);
                    plugin.getAIManager().analyzeScan(sender, pin, playerName, results);
                } else {
                    final int code = respCode;
                    Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(colorize("&cFailed to retrieve scan results for PIN " + pin + " (HTTP " + code + ")")));
                }
                conn.disconnect();
            } catch (Exception e) {
                Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(colorize("&cError fetching PIN results: " + e.getMessage())));
            }
        });
    }

    private void handleAsk(CommandSender sender, String[] args) {
        MessageManager msg = plugin.getMessageManager();

        if (args.length < 2) {
            sender.sendMessage(colorize(msg.getMessage("usage-ask",
                    "&cUsage: /ocean ask <your question or investigation prompt>")));
            return;
        }

        StringBuilder question = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            question.append(args[i]).append(" ");
        }

        plugin.getAIManager().ask(sender, question.toString().trim());
    }

    private void handleConfig(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(colorize(plugin.getMessageManager().getMessage("only-players",
                    "&cThis command can only be executed by players!")));
            return;
        }

        Player player = (Player) sender;
        ConfigGUI gui = new ConfigGUI(plugin);
        gui.open(player);
    }

    private void handleReload(CommandSender sender) {
        plugin.reloadConfig();
        plugin.getMessageManager().reload();
        plugin.getFreezeListener().reloadAllowedCommands();
        sender.sendMessage(colorize(plugin.getMessageManager().getMessage("config-reloaded",
                "&aConfiguration reloaded from disk!")));
    }

    private void handleMode(CommandSender sender, String[] args) {
        MessageManager msg = plugin.getMessageManager();

        if (args.length < 2) {
            String currentMode = plugin.getConfig().getString("settings.freeze-mode", "AUTO");
            sender.sendMessage(colorize(msg.getMessage("mode-current",
                    "&eCurrent freeze mode: &f%mode%").replace("%mode%", currentMode)));
            sender.sendMessage(colorize(msg.getMessage("mode-current-hint",
                    "&7Use &e/ocean mode <AUTO|MANUAL> &7to change")));
            return;
        }

        String mode = args[1].toUpperCase();

        if (!mode.equals("AUTO") && !mode.equals("MANUAL")) {
            sender.sendMessage(colorize(msg.getMessage("invalid-mode",
                    "&cInvalid freeze mode! Use AUTO or MANUAL")));
            return;
        }

        plugin.getConfig().set("settings.freeze-mode", mode);
        plugin.saveConfig();

        sender.sendMessage(colorize(msg.getMessage("mode-set",
                "&aFreeze mode set to: &e%mode%").replace("%mode%", mode)));

        if (mode.equals("AUTO")) {
            sender.sendMessage(colorize(msg.getMessage("mode-auto-hint",
                    "&7Scan PIN will be created automatically when freezing players")));
        } else {
            sender.sendMessage(colorize(msg.getMessage("mode-manual-hint",
                    "&7Use &e/ocean scan <player> &7to initiate scans manually")));
        }
    }

    private void sendHelp(CommandSender sender) {
        MessageManager msg = plugin.getMessageManager();
        sender.sendMessage(colorize(msg.getMessage("help-header", "&7&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")));
        sender.sendMessage(colorize(msg.getMessage("help-title", "&e&lOcean Plugin")));
        sender.sendMessage(colorize(msg.getMessage("help-scan", "&e/ocean scan <player> &7- Start screenshare scan")));
        sender.sendMessage(colorize(msg.getMessage("help-lookup", "&e/ocean lookup <discordId> &7- View user scan history")));
        sender.sendMessage(colorize(msg.getMessage("help-riskscore", "&e/ocean riskscore <discordId> &7- Check user risk analysis")));
        sender.sendMessage(colorize(msg.getMessage("help-database", "&e/ocean db <discordId> &7- Query Cheater Database")));
        sender.sendMessage(colorize(msg.getMessage("help-ai", "&e/ocean ai <player|pin|discordId> &7- AI screenshare verdict & analysis")));
        sender.sendMessage(colorize(msg.getMessage("help-ask", "&e/ocean ask <question> &7- Ask Ocean AI assistant")));
        sender.sendMessage(colorize(msg.getMessage("help-config", "&e/ocean config &7- Open configuration GUI")));
        sender.sendMessage(colorize(msg.getMessage("help-mode", "&e/ocean mode [AUTO|MANUAL] &7- Change freeze mode")));
        sender.sendMessage(colorize(msg.getMessage("help-reload", "&e/ocean reload &7- Reload configuration")));
        sender.sendMessage(colorize(msg.getMessage("help-help", "&e/ocean help &7- Show this help")));
        sender.sendMessage(colorize(msg.getMessage("help-footer", "&7&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (!sender.hasPermission("ocean.staff") && !sender.hasPermission("ocean.admin")) {
            return completions;
        }

        if (args.length == 1) {
            if (sender.hasPermission("ocean.staff")) {
                completions.addAll(Arrays.asList("scan", "lookup", "riskscore", "db", "database", "ai", "ask"));
            }
            if (sender.hasPermission("ocean.admin")) {
                completions.addAll(Arrays.asList("config", "mode", "reload"));
            }
            completions.add("help");
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("scan") && sender.hasPermission("ocean.staff")) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (plugin.getFreezeManager().isFrozen(player)) {
                        completions.add(player.getName());
                    }
                }
            } else if (args[0].equalsIgnoreCase("ai") && sender.hasPermission("ocean.staff")) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    completions.add(player.getName());
                }
            } else if (args[0].equalsIgnoreCase("mode") && sender.hasPermission("ocean.admin")) {
                completions.addAll(Arrays.asList("AUTO", "MANUAL"));
            }
        }

        return completions;
    }

    private String colorize(String message) {
        return message.replace("&", "\u00a7");
    }
}
