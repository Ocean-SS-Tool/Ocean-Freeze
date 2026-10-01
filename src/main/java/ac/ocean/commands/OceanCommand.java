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
                if (!sender.hasPermission("ocean.staff")) {
                    sender.sendMessage(colorize(msg.getMessage("no-permission",
                            "&cYou don't have permission to use this command!")));
                    return true;
                }
                if (subCommand.equals("scan")) handleScan(sender, args);
                else if (subCommand.equals("lookup")) handleLookup(sender, args);
                else handleRiskScore(sender, args);
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
                completions.addAll(Arrays.asList("scan", "lookup", "riskscore"));
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
