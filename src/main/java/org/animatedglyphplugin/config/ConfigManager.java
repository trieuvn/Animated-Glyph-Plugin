package org.animatedglyphplugin.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class ConfigManager {
    private final JavaPlugin plugin;
    private FileConfiguration config;
    private final File configFile;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "animatedGlyph/config.yml");
        createDefaultConfig();
        reload();
    }

    private void createDefaultConfig() {
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            try {
                YamlConfiguration defaultConfig = new YamlConfiguration();
                defaultConfig.set("debug-level", 1);
                defaultConfig.set("default-duration", 2.0);
                defaultConfig.set("max-texture-size", 4096);
                defaultConfig.save(configFile);
                plugin.getLogger().info("Đã tạo file config mặc định: " + configFile.getPath());
            } catch (Exception e) {
                plugin.getLogger().severe("Không thể tạo config mặc định: " + e.getMessage());
            }
        }
    }

    public void reload() {
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    public int getDebugLevel() {
        return config.getInt("debug-level", 1);
    }

    public double getDefaultDuration() {
        return config.getDouble("default-duration", 2.0);
    }

    public int getMaxTextureSize() {
        return config.getInt("max-texture-size", 4096);
    }
}
