package ac.ocean.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import ac.ocean.OceanPlugin;

import java.util.ArrayList;
import java.util.List;

public class ConfigGUI implements Listener {

    private final OceanPlugin plugin;
    private static final String GUI_TITLE = "§e§lOcean Configuration";

    public ConfigGUI(OceanPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory gui = Bukkit.createInventory(null, 45, GUI_TITLE);

        ItemStack filler = createFiller();
        for (int i = 0; i < 45; i++) {
            gui.setItem(i, filler);
        }

        gui.setItem(10, createItem(
                Material.EMERALD_BLOCK,
                "§e§lFreeze Mode",
                "§7Current: §f" + plugin.getConfig().getString("settings.freeze-mode", "AUTO"),
                "",
                "§aAUTO §7- Creates scan PIN automatically",
                "§eMANUAL §7- Use /ocean scan to start",
                "",
                "§eClick to toggle!"
        ));

        boolean broadcast = plugin.getConfig().getBoolean("settings.broadcast-to-staff", true);
        gui.setItem(12, createItem(
                broadcast ? Material.EMERALD : Material.REDSTONE,
                "§e§lBroadcast to Staff",
                "§7Status: " + (broadcast ? "§aEnabled" : "§cDisabled"),
                "",
                "§7Broadcasts freeze actions to all",
                "§7staff members",
                "",
                "§eClick to toggle!"
        ));

        boolean autoBan = plugin.getConfig().getBoolean("settings.auto-ban-on-quit", true);
        gui.setItem(14, createItem(
                autoBan ? Material.DIAMOND_SWORD : Material.WOOD_SWORD,
                "§e§lAuto-ban on Quit",
                "§7Status: " + (autoBan ? "§aEnabled" : "§cDisabled"),
                "",
                "§7Automatically bans players who",
                "§7disconnect while frozen",
                "",
                "§eClick to toggle!"
        ));

        boolean ramDump = plugin.getConfig().getBoolean("anticheat.ram-dump", false);
        gui.setItem(16, createItem(
                ramDump ? Material.GOLD_BLOCK : Material.IRON_BLOCK,
                "§e§lRAM Dump Analysis",
                "§7Status: " + (ramDump ? "§aEnabled" : "§cDisabled"),
                "",
                "§7Enables RAM dump analysis",
                "§7in Ocean scans",
                "",
                "§eClick to toggle!"
        ));

        boolean privatePins = plugin.getConfig().getBoolean("anticheat.private-pins", false);
        gui.setItem(19, createItem(
                privatePins ? Material.CHEST : Material.ENDER_CHEST,
                "§e§lPrivate Pins",
                "§7Status: " + (privatePins ? "§aEnabled" : "§cDisabled"),
                "",
                "§7Makes pins visible only to you",
                "§7in your Enterprise dashboard",
                "",
                "§eClick to toggle!"
        ));

        gui.setItem(21, createItem(
                Material.BOOK,
                "§e§lGame Type",
                "§7Current: §f" + plugin.getConfig().getString("anticheat.game-type", "Java"),
                "",
                "§7Click to cycle through:",
                "§fJava, Bedrock, FiveM, RedM,",
                "§fRageMP, AltV, Roblox, etc.",
                "",
                "§eLeft-click: Next | Right-click: Previous"
        ));

        gui.setItem(23, createItem(
                Material.PAPER,
                "§e§lBan Reason",
                "§7Current: §f" + plugin.getConfig().getString("settings.ban-reason", "Disconnected during screenshare"),
                "",
                "§cCannot be edited in-game",
                "§7Edit in §econfig.yml"
        ));

        boolean freezeGui = plugin.getConfig().getBoolean("settings.freeze-gui-enabled", true);
        gui.setItem(25, createItem(
                freezeGui ? Material.EMERALD : Material.REDSTONE,
                "§e§lFreeze Response GUI",
                "§7Status: " + (freezeGui ? "§aEnabled" : "§cDisabled"),
                "",
                "§7Shows a GUI to frozen players",
                "§7with Admit or Proceed options",
                "",
                "§eClick to toggle!"
        ));

        gui.setItem(31, createItem(
                Material.WOOL,
                "§a§lSave & Close",
                "§7Saves configuration and closes",
                "",
                "§eClick to save!"
        ));

        player.openInventory(gui);
    }

    private ItemStack createFiller() {
        ItemStack item = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 7);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            item.setItemMeta(meta);
        }
        return item;
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
        ItemStack clicked = event.getCurrentItem();

        if (clicked == null || !clicked.hasItemMeta()) {
            return;
        }

        String displayName = clicked.getItemMeta().getDisplayName();

        switch (event.getSlot()) {
            case 10:
                toggleFreezeMode(player);
                open(player);
                break;

            case 12:
                toggleBoolean(player, "settings.broadcast-to-staff");
                open(player);
                break;

            case 14:
                toggleBoolean(player, "settings.auto-ban-on-quit");
                open(player);
                break;

            case 16:
                toggleBoolean(player, "anticheat.ram-dump");
                open(player);
                break;

            case 19:
                toggleBoolean(player, "anticheat.private-pins");
                open(player);
                break;

            case 21:
                cycleGameType(player, event.getClick() == ClickType.RIGHT);
                open(player);
                break;

            case 25:
                toggleBoolean(player, "settings.freeze-gui-enabled");
                open(player);
                break;

            case 31:
                plugin.saveConfig();
                player.closeInventory();
                player.sendMessage(colorize(plugin.getMessageManager().getMessage("config-updated",
                        "&aConfiguration updated successfully!")));
                break;
        }
    }

    private void toggleFreezeMode(Player player) {
        String current = plugin.getConfig().getString("settings.freeze-mode", "AUTO");
        String newMode = current.equals("AUTO") ? "MANUAL" : "AUTO";
        plugin.getConfig().set("settings.freeze-mode", newMode);

        player.sendMessage(colorize(plugin.getMessageManager().getMessage("config-mode-changed",
                "&7Freeze mode changed to: &e%mode%").replace("%mode%", newMode)));
    }

    private void toggleBoolean(Player player, String path) {
        boolean current = plugin.getConfig().getBoolean(path, false);
        plugin.getConfig().set(path, !current);

        if (!current) {
            player.sendMessage(colorize(plugin.getMessageManager().getMessage("config-toggled-enabled",
                    "&7Setting toggled: &aEnabled")));
        } else {
            player.sendMessage(colorize(plugin.getMessageManager().getMessage("config-toggled-disabled",
                    "&7Setting toggled: &cDisabled")));
        }
    }

    private void cycleGameType(Player player, boolean reverse) {
        String[] types = {"Java", "Bedrock", "FiveM", "RedM", "RageMP", "AltV", "Roblox", "SanAndreas", "Cod", "Rust"};
        String current = plugin.getConfig().getString("anticheat.game-type", "Java");

        int index = 0;
        for (int i = 0; i < types.length; i++) {
            if (types[i].equals(current)) {
                index = i;
                break;
            }
        }

        if (reverse) {
            index = (index - 1 + types.length) % types.length;
        } else {
            index = (index + 1) % types.length;
        }

        String newType = types[index];
        plugin.getConfig().set("anticheat.game-type", newType);

        player.sendMessage(colorize(plugin.getMessageManager().getMessage("config-gametype-changed",
                "&7Game type changed to: &e%type%").replace("%type%", newType)));
    }

    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(name);

            List<String> loreList = new ArrayList<>();
            for (String line : lore) {
                loreList.add(line);
            }
            meta.setLore(loreList);

            item.setItemMeta(meta);
        }

        return item;
    }

    private String colorize(String message) {
        return message.replace("&", "§");
    }
}
