package ac.ocean.manager;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import ac.ocean.OceanPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

public class MessageManager {

    private final OceanPlugin plugin;
    private FileConfiguration messagesConfig;
    private File messagesFile;

    public MessageManager(OceanPlugin plugin) {
        this.plugin = plugin;
        loadMessages();
    }

    public void loadMessages() {
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");

        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }

        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);

        InputStream defaultStream = plugin.getResource("messages.yml");
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
            messagesConfig.setDefaults(defaultConfig);
        }
    }

    public void reload() {
        loadMessages();
    }

    public String getMessage(String path) {
        return messagesConfig.getString(path, "");
    }

    public String getMessage(String path, String defaultValue) {
        return messagesConfig.getString(path, defaultValue);
    }

    public List<String> getMessageList(String path) {
        List<String> list = messagesConfig.getStringList(path);
        return list != null ? list : Collections.emptyList();
    }

    public FileConfiguration getConfig() {
        return messagesConfig;
    }
}
