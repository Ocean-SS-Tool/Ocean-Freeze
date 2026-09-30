package ac.ocean.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import ac.ocean.OceanPlugin;
import ac.ocean.manager.MessageManager;
import ac.ocean.model.FrozenPlayer;
import ac.ocean.utils.ClickableMessage;
import ac.ocean.utils.VersionSupport;

import java.util.*;

public class FreezeGUI implements Listener {

    private final OceanPlugin plugin;
    public static final String GUI_TITLE = "\u00a7c\u00a7lFreeze Response";

    private final Set<UUID> suppressReopen = new HashSet<>();
    private final Set<UUID> processing = new HashSet<>();

    public FreezeGUI(OceanPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        MessageManager msg = plugin.getMessageManager();
        Inventory gui = Bukkit.createInventory(null, 27, GUI_TITLE);

        ItemStack filler = createFiller();
        for (int i = 0; i < 27; i++) {
            gui.setItem(i, filler);
        }

        String admitTitle = colorize(msg.getMessage("freeze-gui-admit-title",
                "&c&lAdmit to Hacking"));
        List<String> admitLore = colorizeList(msg.getMessageList("freeze-gui-admit-lore"));
        if (admitLore.isEmpty()) {
            admitLore = colorizeList(Arrays.asList(
                    "",
                    "&7Click to admit to using hacks.",
                    "&7This will result in a &cban&7.",
                    "",
                    "&c&lClick to confirm"
            ));
        }
        gui.setItem(11, createColoredWoolItem((short) 14, "RED_WOOL", admitTitle, admitLore));

        String proceedTitle = colorize(msg.getMessage("freeze-gui-proceed-title",
                "&a&lI'm Legit - Start Screenshare"));
        List<String> proceedLore = colorizeList(msg.getMessageList("freeze-gui-proceed-lore"));
        if (proceedLore.isEmpty()) {
            proceedLore = colorizeList(Arrays.asList(
                    "",
                    "&7You believe you are not cheating.",
                    "&7Clicking this will close the GUI",
                    "&7and begin the screenshare process.",
                    "",
                    "&7Follow the instructions in &fchat",
                    "&7to complete the screenshare.",
                    "",
                    "&a&lClick to start screenshare"
            ));
        }
        gui.setItem(15, createColoredWoolItem((short) 5, "LIME_WOOL", proceedTitle, proceedLore));

        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        if (!event.getView().getTitle().equals(GUI_TITLE)) {
            return;
        }

        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        UUID uuid = player.getUniqueId();

        if (!plugin.getFreezeManager().isFrozen(player)) {
            suppressReopen.add(uuid);
            player.closeInventory();
            return;
        }

        if (processing.contains(uuid)) {
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) {
            return;
        }

        switch (event.getSlot()) {
            case 11:
                processing.add(uuid);
                suppressReopen.add(uuid);
                player.closeInventory();
                handleAdmit(player);
                break;

            case 15:
                processing.add(uuid);
                suppressReopen.add(uuid);
                player.closeInventory();
                handleProceed(player);
                break;
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }

        if (!event.getView().getTitle().equals(GUI_TITLE)) {
            return;
        }

        Player player = (Player) event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (suppressReopen.contains(uuid)) {
            return;
        }

        if (!plugin.getFreezeManager().isFrozen(player)) {
            return;
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && plugin.getFreezeManager().isFrozen(player)
                    && !suppressReopen.contains(uuid)) {
                open(player);
            }
        }, 1L);
    }

    private void handleAdmit(Player player) {
        UUID uuid = player.getUniqueId();
        String playerName = player.getName();
        MessageManager msg = plugin.getMessageManager();

        FrozenPlayer frozenPlayer = plugin.getFreezeManager().getFrozenPlayer(uuid);
        String staffName = "Console";
        if (frozenPlayer != null) {
            Player staff = Bukkit.getPlayer(frozenPlayer.getFreezerUuid());
            if (staff != null) {
                staffName = staff.getName();
            }
        }

        String banCommandTemplate = plugin.getConfig().getString("settings.freeze-gui-admit-command",
                "ban %player% Admitted to using hacks");
        String banCommand = "/" + banCommandTemplate.replace("%player%", playerName);

        String staffMsg = colorize(msg.getMessage("freeze-gui-admit-staff",
                "&c&l%player% &ehas admitted to hacking! ")
                .replace("%player%", playerName));
        String clickText = msg.getMessage("freeze-gui-admit-click-text",
                "&c&l[CLICK TO BAN]");
        String hoverText = msg.getMessage("freeze-gui-admit-hover",
                "&eClick to ban %player%").replace("%player%", playerName);

        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (staff.hasPermission("ocean.staff")) {
                ClickableMessage.send(staff, staffMsg, colorize(clickText), colorize(hoverText), banCommand);
            }
        }

        plugin.getWebhookManager().sendAdmitNotification(playerName, staffName);
    }

    private void handleProceed(Player player) {
        MessageManager msg = plugin.getMessageManager();
        String proceedMsg = colorize(msg.getMessage("freeze-gui-proceed-player",
                "&aYou have chosen to proceed with the screenshare. Follow the instructions in chat."));
        player.sendMessage(proceedMsg);

        String freezeMode = plugin.getConfig().getString("settings.freeze-mode", "AUTO");

        if (freezeMode.equalsIgnoreCase("AUTO")) {
            String staffMsg = colorize(msg.getMessage("freeze-gui-proceed-staff-auto",
                    "&a%player% &ehas accepted the screenshare. Generating scan PIN automatically...")
                    .replace("%player%", player.getName()));
            for (Player staff : Bukkit.getOnlinePlayers()) {
                if (staff.hasPermission("ocean.staff")) {
                    staff.sendMessage(staffMsg);
                }
            }

            FrozenPlayer frozenPlayer = plugin.getFreezeManager().getFrozenPlayer(player.getUniqueId());
            if (frozenPlayer != null) {
                Player freezer = Bukkit.getPlayer(frozenPlayer.getFreezerUuid());
                if (freezer != null && freezer.isOnline()) {
                    plugin.getAntiCheatAPI().createPin(player, freezer);
                }
            }
        } else {
            String staffMsg = colorize(msg.getMessage("freeze-gui-proceed-staff-manual",
                    "&a%player% &ehas accepted the screenshare. Use &a/ocean scan %player% &eto start.")
                    .replace("%player%", player.getName()));
            for (Player staff : Bukkit.getOnlinePlayers()) {
                if (staff.hasPermission("ocean.staff")) {
                    staff.sendMessage(staffMsg);
                }
            }
        }
    }

    public void cleanup(UUID uuid) {
        suppressReopen.remove(uuid);
        processing.remove(uuid);
    }

    public boolean isSuppressed(UUID uuid) {
        return suppressReopen.contains(uuid);
    }

    private ItemStack createFiller() {
        ItemStack item = VersionSupport.createGrayGlassPane();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createColoredWoolItem(short legacyData, String modernName, String name, List<String> lore) {
        ItemStack item = VersionSupport.createColoredWool(legacyData, modernName);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null) {
                meta.setLore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private String colorize(String message) {
        return message.replace("&", "\u00a7");
    }

    private List<String> colorizeList(List<String> list) {
        List<String> result = new ArrayList<>();
        for (String s : list) {
            result.add(colorize(s));
        }
        return result;
    }
}
