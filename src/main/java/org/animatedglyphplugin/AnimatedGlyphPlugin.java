package org.animatedglyphplugin;

import org.animatedglyphplugin.config.ConfigManager;
import org.animatedglyphplugin.gif.GifToPngConverter;
import org.animatedglyphplugin.glyph.GlyphManager;
import org.animatedglyphplugin.resourcepack.ResourcePackBuilder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.awt.image.BufferedImage;
import java.io.File;

public final class AnimatedGlyphPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private GlyphManager glyphManager;
    private ResourcePackBuilder resourcePackBuilder;

    @Override
    public void onEnable() {
        getLogger().info("Khởi động AnimatedGlyph Plugin...");

        try {
            // Tạo thư mục plugin nếu chưa tồn tại
            if (!getDataFolder().exists()) {
                getDataFolder().mkdirs();
            }

            // Khởi tạo các manager
            configManager = new ConfigManager(this);
            glyphManager = new GlyphManager(this, configManager);
            resourcePackBuilder = new ResourcePackBuilder(this, glyphManager, configManager);

            // Tạo resourcepack
            buildResourcePack();

            getLogger().info("AnimatedGlyph Plugin đã khởi động thành công!");
        } catch (Exception e) {
            getLogger().severe("Lỗi khi khởi động plugin: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("AnimatedGlyph Plugin đã tắt.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("animatedglyph")) {
            if (args.length > 0) {
                switch (args[0].toLowerCase()) {
                    case "reload":
                        if (sender.hasPermission("animatedglyph.reload")) {
                            try {
                                buildResourcePack();
                                sender.sendMessage("§a[AnimatedGlyph] ✅ Resourcepack đã được tạo lại thành công!");
                            } catch (Exception e) {
                                sender.sendMessage("§c[AnimatedGlyph] ❌ Lỗi: " + e.getMessage());
                                getLogger().severe("Lỗi reload: " + e.getMessage());
                                e.printStackTrace();
                            }
                        } else {
                            sender.sendMessage("§c[AnimatedGlyph] Bạn không có quyền!");
                        }
                        return true;

                    case "debug":
                        if (sender.hasPermission("animatedglyph.reload")) {
                            File buildDir = new File(getDataFolder(), "animatedGlyph/build");
                            sender.sendMessage("§e[AnimatedGlyph] 🔍 Debug Info:");
                            sender.sendMessage("§7Build dir exists: " + buildDir.exists());
                            if (buildDir.exists()) {
                                File texturesDir = new File(buildDir, "assets/minecraft/textures/gif");
                                sender.sendMessage("§7Textures dir: " + texturesDir.exists());
                                if (texturesDir.exists()) {
                                    String[] files = texturesDir.list();
                                    sender.sendMessage("§7PNG files: " + (files != null ? files.length : 0));
                                    if (files != null) {
                                        for (String file : files) {
                                            sender.sendMessage("§7  - " + file);
                                        }
                                    }
                                }
                            }
                        }
                        return true;

                    case "test":
                        if (sender.hasPermission("animatedglyph.reload")) {
                            try {
                                File testGif = new File(getDataFolder(), "animatedGlyph/gif/fire.gif");
                                if (testGif.exists()) {
                                    BufferedImage result = GifToPngConverter.convertGifToPngSheet(testGif, 2.0);
                                    sender.sendMessage("§a✅ Test conversion thành công!");
                                    sender.sendMessage("§7Kích thước result: " + result.getWidth() + "x" + result.getHeight());
                                } else {
                                    sender.sendMessage("§c❌ Không tìm thấy fire.gif để test");
                                }
                            } catch (Exception e) {
                                sender.sendMessage("§c❌ Test thất bại: " + e.getMessage());
                            }
                        }
                        return true;
                }
            }
            sender.sendMessage("§e[AnimatedGlyph] Lệnh: /animatedglyph reload | debug");
            return true;
        }
        return false;
    }

    private void buildResourcePack() throws Exception {
        configManager.reload();
        glyphManager.reload();
        resourcePackBuilder.build();
    }
}
