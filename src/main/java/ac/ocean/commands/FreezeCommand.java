package ac.ocean.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ac.ocean.OceanPlugin;
import ac.ocean.manager.MessageManager;

public class FreezeCommand implements CommandExecutor {

    private final OceanPlugin plugin;

    public FreezeCommand(OceanPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        MessageManager msg = plugin.getMessageManager();

        if (!(sender instanceof Player)) {
            sender.sendMessage(colorize(msg.getMessage("only-players",
                    "&cThis command can only be executed by players!")));
            return true;
        }

        Player staff = (Player) sender;

        if (!staff.hasPermission("ocean.freeze")) {
            staff.sendMessage(colorize(msg.getMessage("no-permission",
                    "&cYou don't have permission to use this command!")));
            return true;
        }

        if (args.length != 1) {
            staff.sendMessage(colorize(msg.getMessage("usage-ss",
                    "&cUsage: /%label% <player>").replace("%label%", label)));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);

        if (target == null || !target.isOnline()) {
            staff.sendMessage(colorize(msg.getMessage("player-not-found",
                    "&cPlayer not found or not online!")));
            return true;
        }

        if (target.equals(staff)) {
            staff.sendMessage(colorize(msg.getMessage("cannot-freeze-self",
                    "&cYou cannot freeze yourself!")));
            return true;
        }

        if (target.hasPermission("ocean.freeze.bypass")) {
            staff.sendMessage(colorize(msg.getMessage("cannot-freeze-player",
                    "&cYou cannot freeze this player!")));
            return true;
        }

        if (plugin.getFreezeManager().isFrozen(target)) {
            plugin.getFreezeManager().unfreezePlayer(target);
            staff.sendMessage(colorize(msg.getMessage("unfreeze-staff",
                    "&aYou have unfrozen &e%player%&a!")
                    .replace("%player%", target.getName())));
        } else {
            plugin.getFreezeManager().freezePlayer(target, staff);
        }

        return true;
    }

    private String colorize(String message) {
        return message.replace("&", "\u00a7");
    }
}
