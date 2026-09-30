package ac.ocean.utils;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;

public class VersionSupport {

    private static final int MAJOR_VERSION;

    static {
        String version = Bukkit.getBukkitVersion();
        String[] parts = version.split("-")[0].split("\\.");
        MAJOR_VERSION = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
    }

    public static boolean isLegacy() {
        return MAJOR_VERSION < 13;
    }

    public static int getMajorVersion() {
        return MAJOR_VERSION;
    }

    public static Material getMaterial(String... names) {
        for (String name : names) {
            try {
                return Material.valueOf(name);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return Material.STONE;
    }

    public static ItemStack createGrayGlassPane() {
        if (isLegacy()) {
            return new ItemStack(Material.valueOf("STAINED_GLASS_PANE"), 1, (short) 7);
        }
        return new ItemStack(getMaterial("GRAY_STAINED_GLASS_PANE"));
    }

    public static ItemStack createColoredWool(short legacyData, String modernName) {
        if (isLegacy()) {
            return new ItemStack(Material.valueOf("WOOL"), 1, legacyData);
        }
        return new ItemStack(getMaterial(modernName, "WHITE_WOOL"));
    }

    public static Material getWoodenSword() {
        return getMaterial("WOODEN_SWORD", "WOOD_SWORD");
    }

    public static Material getWhiteWool() {
        return getMaterial("WHITE_WOOL", "WOOL");
    }

    public static ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) {
                meta.setLore(Arrays.asList(lore));
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
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
}
