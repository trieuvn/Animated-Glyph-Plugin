package org.animatedglyphplugin.glyph;

import org.animatedglyphplugin.config.ConfigManager;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;

public class GlyphManager {
    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final List<GlyphDefinition> glyphs = new ArrayList<>();
    private final Set<String> usedChars = new HashSet<>();

    public GlyphManager(JavaPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    public void reload() {
        glyphs.clear();
        usedChars.clear();
        loadGlyphs();
    }

    private void loadGlyphs() {
        File glyphDir = new File(plugin.getDataFolder(), "animatedGlyph/glyph");
        if (!glyphDir.exists()) {
            glyphDir.mkdirs();
            createExampleGlyph();
            return;
        }

        File[] glyphFiles = glyphDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (glyphFiles == null || glyphFiles.length == 0) {
            createExampleGlyph();
            return;
        }

        for (File glyphFile : glyphFiles) {
            try {
                YamlConfiguration glyphConfig = YamlConfiguration.loadConfiguration(glyphFile);
                String name = glyphConfig.getString("name");
                String file = glyphConfig.getString("file");
                int ascent = glyphConfig.getInt("ascent", 8);
                int height = glyphConfig.getInt("height", 16);
                double duration = glyphConfig.getDouble("duration", configManager.getDefaultDuration());
                List<String> chars = glyphConfig.getStringList("chars");

                // Nếu chars rỗng, tự sinh ký tự Unicode không trùng
                if (chars == null || chars.isEmpty()) {
                    chars = generateUniqueChars(1);
                }

                GlyphDefinition glyph = new GlyphDefinition(name, file, ascent, height, chars, duration);
                glyphs.add(glyph);
                usedChars.addAll(chars);

                if (configManager.getDebugLevel() > 0) {
                    plugin.getLogger().info("Đã tải glyph: " + name + " với ký tự: " + chars);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Không thể tải file glyph: " + glyphFile.getName() + " - " + e.getMessage());
            }
        }
    }

    private void createExampleGlyph() {
        File exampleFile = new File(plugin.getDataFolder(), "animatedGlyph/glyph/example_fire.yml");
        try {
            exampleFile.getParentFile().mkdirs();
            YamlConfiguration example = new YamlConfiguration();
            example.set("name", "fire");
            example.set("file", "fire.gif");
            example.set("ascent", 11);
            example.set("height", 15);
            example.set("duration", 2.0);
            example.set("chars", Arrays.asList("ல")); // Ký tự Tamil để demo
            example.save(exampleFile);
            plugin.getLogger().info("Đã tạo file glyph mẫu: " + exampleFile.getPath());
        } catch (Exception e) {
            plugin.getLogger().severe("Không thể tạo glyph mẫu: " + e.getMessage());
        }
    }

    private List<String> generateUniqueChars(int count) {
        List<String> chars = new ArrayList<>();
        // Sử dụng Private Use Area Unicode (U+E000-U+F8FF)
        int start = 0xE000;

        for (int i = 0; i < count; i++) {
            String unicode;
            do {
                unicode = new String(Character.toChars(start++));
            } while (usedChars.contains(unicode) && start < 0xF8FF);

            chars.add(unicode);
            usedChars.add(unicode);
        }
        return chars;
    }

    public List<GlyphDefinition> getGlyphs() {
        return new ArrayList<>(glyphs);
    }
}
