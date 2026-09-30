package ac.ocean.manager;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import ac.ocean.OceanPlugin;
import ac.ocean.model.FrozenPlayer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FreezeManager {

    private final OceanPlugin plugin;
    private final Map<UUID, FrozenPlayer> frozenPlayers;

    public FreezeManager(OceanPlugin plugin) {
        this.plugin = plugin;
        this.frozenPlayers = new ConcurrentHashMap<>();
    }

    public void freezePlayer(Player player, Player freezer) {
        if (isFrozen(player)) {
            return;
        }

        Location freezeLocation = player.getLocation();
        GameMode originalGameMode = player.getGameMode();

        FrozenPlayer frozenPlayer = new FrozenPlayer(
                player.getUniqueId(),
                freezer.getUniqueId(),
                freezeLocation,
                originalGameMode,
                System.currentTimeMillis()
        );

        frozenPlayers.put(player.getUniqueId(), frozenPlayer);

        player.setWalkSpeed(0f);
        player.setFlySpeed(0f);
        player.setAllowFlight(false);

        applyFreezeEffects(player);

        sendFreezeMessages(player, freezer);

        plugin.getWebhookManager().sendFreezeNotification(player.getName(), freezer.getName(), null);

        int freezeInterval = plugin.getConfig().getInt("settings.freeze-message-interval", 30);
        if (freezeInterval > 0) {
            startFreezeMessageTask(player, freezeInterval);
        }

        String freezeMode = plugin.getConfig().getString("settings.freeze-mode", "AUTO");
        boolean guiEnabled = plugin.getConfig().getBoolean("settings.freeze-gui-enabled", true);

        if (freezeMode.equalsIgnoreCase("AUTO") && !guiEnabled) {
            plugin.getAntiCheatAPI().createPin(player, freezer);
        } else if (freezeMode.equalsIgnoreCase("AUTO") && guiEnabled) {
            String staffMsg = plugin.getMessageManager().getMessage("freeze-staff",
                    "&a%staff% &ehas frozen &a%player%&e. Scan PIN will be created when the player accepts the screenshare.");
            freezer.sendMessage(colorize(staffMsg
                    .replace("%player%", player.getName())
                    .replace("%staff%", freezer.getName())));
        } else {
            String manualMsg = plugin.getMessageManager().getMessage("freeze-staff-manual",
                    "&aYou have frozen &e%player%&a! Use &e/ocean scan %player% &ato start scan.");
            freezer.sendMessage(colorize(manualMsg.replace("%player%", player.getName())));
        }

        if (plugin.getConfig().getBoolean("settings.freeze-gui-enabled", true)) {
            plugin.getFreezeGUI().open(player);
        }
    }

    public void unfreezePlayer(Player player) {
        FrozenPlayer frozenPlayer = frozenPlayers.remove(player.getUniqueId());
        if (frozenPlayer == null) {
            return;
        }

        cancelTasks(frozenPlayer);
        plugin.getFreezeGUI().cleanup(player.getUniqueId());

        removeFreezeEffects(player);

        player.setWalkSpeed(0.2f);
        player.setFlySpeed(0.1f);

        if (frozenPlayer.getOriginalGameMode() != null) {
            player.setGameMode(frozenPlayer.getOriginalGameMode());
        }

        String unfreezeMsg = plugin.getMessageManager().getMessage("unfrozen", "&aYou have been unfrozen!");
        player.sendMessage(colorize(unfreezeMsg));
    }

    public void unfreezeAll() {
        for (UUID uuid : new HashSet<>(frozenPlayers.keySet())) {
            plugin.getFreezeGUI().cleanup(uuid);
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                unfreezePlayer(player);
            }
        }
        frozenPlayers.clear();
    }

    public boolean isFrozen(Player player) {
        return player != null && frozenPlayers.containsKey(player.getUniqueId());
    }

    public FrozenPlayer getFrozenPlayer(UUID uuid) {
        return frozenPlayers.get(uuid);
    }

    public Map<UUID, FrozenPlayer> getFrozenPlayers() {
        return Collections.unmodifiableMap(frozenPlayers);
    }

    public void markAsDisconnected(UUID uuid) {
        FrozenPlayer frozenPlayer = frozenPlayers.get(uuid);
        if (frozenPlayer != null) {
            cancelTasks(frozenPlayer);
        }
    }

    public boolean wasFrozen(UUID uuid) {
        return frozenPlayers.containsKey(uuid);
    }

    public void reapplyFreeze(Player player) {
        FrozenPlayer frozenPlayer = frozenPlayers.get(player.getUniqueId());
        if (frozenPlayer == null) {
            return;
        }

        player.setWalkSpeed(0f);
        player.setFlySpeed(0f);
        player.setAllowFlight(false);

        applyFreezeEffects(player);

        player.teleport(frozenPlayer.getFreezeLocation());

        List<String> frozenMessages = plugin.getMessageManager().getMessageList("frozen");
        for (String msg : frozenMessages) {
            player.sendMessage(colorize(msg));
        }

        int freezeInterval = plugin.getConfig().getInt("settings.freeze-message-interval", 30);
        if (freezeInterval > 0) {
            startFreezeMessageTask(player, freezeInterval);
        }

        if (frozenPlayer.getScanPin() != null && !frozenPlayer.getScanPin().isEmpty()) {
            sendScanInstructions(player, frozenPlayer.getScanPin());
            int scanInterval = plugin.getConfig().getInt("settings.scan-message-interval", 30);
            if (scanInterval > 0) {
                startScanInstructionsTask(player, frozenPlayer.getScanPin(), scanInterval);
            }

            if (!frozenPlayer.isScanFinished()) {
                plugin.getAntiCheatAPI().startScanMonitoring(
                        frozenPlayer.getScanPin(),
                        player.getUniqueId(),
                        frozenPlayer.getFreezerUuid(),
                        player.getName()
                );
            }
        }

        if (plugin.getConfig().getBoolean("settings.freeze-gui-enabled", true)
                && !plugin.getFreezeGUI().isSuppressed(player.getUniqueId())) {
            plugin.getFreezeGUI().open(player);
        }
    }

    public void startMessageRepetition(Player player, String pin) {
        FrozenPlayer frozenPlayer = getFrozenPlayer(player.getUniqueId());
        if (frozenPlayer == null) {
            return;
        }

        int scanInterval = plugin.getConfig().getInt("settings.scan-message-interval", 30);

        sendScanInstructions(player, pin);

        if (scanInterval > 0) {
            startScanInstructionsTask(player, pin, scanInterval);
        }
    }

    private void startFreezeMessageTask(Player player, int interval) {
        final UUID uuid = player.getUniqueId();

        FrozenPlayer existing = getFrozenPlayer(uuid);
        if (existing != null && existing.getMessageTaskId() != -1) {
            Bukkit.getScheduler().cancelTask(existing.getMessageTaskId());
            existing.setMessageTaskId(-1);
        }

        int taskId = new BukkitRunnable() {
            @Override
            public void run() {
                FrozenPlayer fp = getFrozenPlayer(uuid);
                if (fp == null || !player.isOnline()) {
                    cancel();
                    return;
                }
                sendFreezeMessage(player);
            }
        }.runTaskTimer(plugin, interval * 20L, interval * 20L).getTaskId();

        FrozenPlayer fp = getFrozenPlayer(uuid);
        if (fp != null) {
            fp.setMessageTaskId(taskId);
        }
    }

    private void startScanInstructionsTask(Player player, String pin, int interval) {
        final UUID uuid = player.getUniqueId();
        int taskId = new BukkitRunnable() {
            @Override
            public void run() {
                FrozenPlayer fp = getFrozenPlayer(uuid);
                if (fp == null || !player.isOnline() || fp.isScanStarted()) {
                    cancel();
                    return;
                }
                sendScanInstructions(player, pin);
            }
        }.runTaskTimer(plugin, interval * 20L, interval * 20L).getTaskId();

        FrozenPlayer fp = getFrozenPlayer(uuid);
        if (fp != null) {
            fp.setScanInstructionsTaskId(taskId);
        }
    }

    private void cancelTasks(FrozenPlayer frozenPlayer) {
        if (frozenPlayer.getMessageTaskId() != -1) {
            Bukkit.getScheduler().cancelTask(frozenPlayer.getMessageTaskId());
            frozenPlayer.setMessageTaskId(-1);
        }
        if (frozenPlayer.getScanInstructionsTaskId() != -1) {
            Bukkit.getScheduler().cancelTask(frozenPlayer.getScanInstructionsTaskId());
            frozenPlayer.setScanInstructionsTaskId(-1);
        }
        if (frozenPlayer.getScanMonitoringTaskId() != -1) {
            Bukkit.getScheduler().cancelTask(frozenPlayer.getScanMonitoringTaskId());
            frozenPlayer.setScanMonitoringTaskId(-1);
        }
    }

    private void sendFreezeMessage(Player player) {
        MessageManager msg = plugin.getMessageManager();
        List<String> frozenMessages = msg.getMessageList("frozen");
        for (String m : frozenMessages) {
            player.sendMessage(colorize(m));
        }
    }

    private void sendScanInstructions(Player player, String pin) {
        MessageManager msg = plugin.getMessageManager();
        List<String> scanInstructions = msg.getMessageList("scan-instructions");
        for (String line : scanInstructions) {
            player.sendMessage(colorize(line.replace("%pin%", pin)));
        }
    }

    private void sendFreezeMessages(Player player, Player freezer) {
        MessageManager msg = plugin.getMessageManager();
        List<String> playerMessages = msg.getMessageList("frozen");
        for (String m : playerMessages) {
            player.sendMessage(colorize(m));
        }

        if (plugin.getConfig().getBoolean("settings.broadcast-to-staff", true)) {
            String broadcastMsg = colorize(msg.getMessage("freeze-broadcast",
                    "&e%staff% &ahas frozen &e%player%")
                    .replace("%staff%", freezer.getName())
                    .replace("%player%", player.getName()));

            for (Player staff : Bukkit.getOnlinePlayers()) {
                if (staff.hasPermission("ocean.staff") && !staff.equals(player)) {
                    staff.sendMessage(broadcastMsg);
                }
            }
        }
    }

    private void applyFreezeEffects(Player player) {
        if (!plugin.getConfig().getBoolean("settings.freeze-effects.enabled", false)) {
            return;
        }

        List<Map<?, ?>> effects = plugin.getConfig().getMapList("settings.freeze-effects.effects");
        for (Map<?, ?> effectMap : effects) {
            String typeName = String.valueOf(effectMap.get("type")).toUpperCase();
            int amplifier = effectMap.containsKey("amplifier") ? Integer.parseInt(String.valueOf(effectMap.get("amplifier"))) : 0;

            PotionEffectType type = PotionEffectType.getByName(typeName);
            if (type == null) {
                plugin.getLogger().warning("Unknown potion effect type: " + typeName);
                continue;
            }

            player.addPotionEffect(new PotionEffect(type, Integer.MAX_VALUE, amplifier, false, false), true);
        }
    }

    private void removeFreezeEffects(Player player) {
        if (!plugin.getConfig().getBoolean("settings.freeze-effects.enabled", false)) {
            return;
        }

        List<Map<?, ?>> effects = plugin.getConfig().getMapList("settings.freeze-effects.effects");
        for (Map<?, ?> effectMap : effects) {
            String typeName = String.valueOf(effectMap.get("type")).toUpperCase();
            PotionEffectType type = PotionEffectType.getByName(typeName);
            if (type != null) {
                player.removePotionEffect(type);
            }
        }
    }

    private String colorize(String message) {
        return message.replace("&", "\u00a7");
    }
}
