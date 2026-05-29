package ac.ocean.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.*;
import ac.ocean.OceanPlugin;
import ac.ocean.gui.FreezeGUI;
import ac.ocean.utils.ClickableMessage;

import java.util.HashSet;
import java.util.Set;

public class FreezeListener implements Listener {

    private final OceanPlugin plugin;
    private Set<String> allowedCommands;

    public FreezeListener(OceanPlugin plugin) {
        this.plugin = plugin;
        reloadAllowedCommands();
    }

    public void reloadAllowedCommands() {
        Set<String> cmds = new HashSet<>();
        for (String cmd : plugin.getConfig().getStringList("settings.allowed-commands")) {
            cmds.add("/" + cmd.toLowerCase());
        }
        this.allowedCommands = cmds;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
                event.getFrom().getBlockY() == event.getTo().getBlockY() &&
                event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        if (plugin.getFreezeManager().isFrozen(event.getPlayer())) {
            event.setTo(event.getFrom());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (plugin.getFreezeManager().isFrozen(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (plugin.getFreezeManager().isFrozen(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        if (plugin.getFreezeManager().isFrozen(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockDamage(BlockDamageEvent event) {
        if (plugin.getFreezeManager().isFrozen(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (plugin.getFreezeManager().isFrozen(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (plugin.getFreezeManager().isFrozen(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        if (plugin.getFreezeManager().isFrozen(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event) {
        if (plugin.getFreezeManager().isFrozen(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerBucketFill(PlayerBucketFillEvent event) {
        if (plugin.getFreezeManager().isFrozen(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player) {
            if (plugin.getFreezeManager().isFrozen((Player) event.getEntity())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
        if (plugin.getFreezeManager().isFrozen(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        if (event.getView().getTitle().equals(FreezeGUI.GUI_TITLE)) {
            return;
        }

        if (plugin.getFreezeManager().isFrozen((Player) event.getWhoClicked())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();

        if (!plugin.getFreezeManager().isFrozen(player)) {
            return;
        }

        String command = event.getMessage().split(" ")[0].toLowerCase();

        if (!allowedCommands.contains(command)) {
            event.setCancelled(true);
            player.sendMessage(colorize(plugin.getMessageManager().getMessage("command-blocked",
                    "&cYou cannot use commands while frozen!")));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        if (plugin.getFreezeManager().isFrozen((Player) event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            if (plugin.getFreezeManager().isFrozen((Player) event.getDamager())) {
                event.setCancelled(true);
            }
        }

        if (event.getEntity() instanceof Player && event.getDamager() instanceof Player) {
            Player victim = (Player) event.getEntity();
            Player attacker = (Player) event.getDamager();
            if (plugin.getFreezeManager().isFrozen(victim)) {
                event.setCancelled(true);
                String msg = plugin.getMessageManager().getMessage("hit-frozen-player",
                        "&c%player% is currently frozen and cannot be attacked.");
                attacker.sendMessage(colorize(msg.replace("%player%", victim.getName())));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (plugin.getFreezeManager().isFrozen(event.getPlayer())) {
            if (event.getCause() != PlayerTeleportEvent.TeleportCause.PLUGIN &&
                    event.getCause() != PlayerTeleportEvent.TeleportCause.COMMAND) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerSneak(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) {
            return;
        }

        if (plugin.getFreezeManager().isFrozen(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        if (!plugin.getFreezeManager().isFrozen(player)) {
            return;
        }

        plugin.getLogger().warning(player.getName() + " disconnected while frozen!");

        final String playerName = player.getName();

        plugin.getFreezeManager().markAsDisconnected(player.getUniqueId());
        plugin.getFreezeGUI().cleanup(player.getUniqueId());

        String messageText = plugin.getMessageManager().getMessage("quit-while-frozen-clickable",
                "&c%player% &edisconnected while frozen! ").replace("%player%", playerName);
        String clickText = plugin.getMessageManager().getMessage("quit-while-frozen-click-text",
                "&c&l[CLICK TO BAN]");
        String hoverText = plugin.getMessageManager().getMessage("quit-while-frozen-hover",
                "&eClick to ban %player%").replace("%player%", playerName);

        String banCommandTemplate = plugin.getConfig().getString("settings.ban-command",
                "ban %player% Disconnected during screenshare");
        final String banCommand = "/" + banCommandTemplate.replace("%player%", playerName);

        for (Player staff : plugin.getServer().getOnlinePlayers()) {
            if (staff.hasPermission("ocean.staff")) {
                ClickableMessage.send(staff, messageText, clickText, hoverText, banCommand);
            }
        }

        if (plugin.getConfig().getBoolean("settings.auto-ban-on-quit", false)) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                String cmd = banCommandTemplate.replace("%player%", playerName);
                plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), cmd);
            }, 20L);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (!plugin.getFreezeManager().wasFrozen(player.getUniqueId())) {
            return;
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            plugin.getFreezeManager().reapplyFreeze(player);

            String reconnectMsg = colorize(plugin.getMessageManager().getMessage("reconnected-while-frozen",
                    "&e%player% &areconnected while frozen!").replace("%player%", player.getName()));

            for (Player staff : Bukkit.getOnlinePlayers()) {
                if (staff.hasPermission("ocean.staff")) {
                    staff.sendMessage(reconnectMsg);
                }
            }

            plugin.getLogger().info(player.getName() + " reconnected while frozen - freeze reapplied");
        }, 10L);
    }

    private String colorize(String message) {
        return message.replace("&", "\u00a7");
    }
}
