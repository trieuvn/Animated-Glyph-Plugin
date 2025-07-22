package org.animatedglyphplugin.resourcepack;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.animatedglyphplugin.config.ConfigManager;
import org.animatedglyphplugin.gif.GifToPngConverter;
import org.animatedglyphplugin.glyph.GlyphDefinition;
import org.animatedglyphplugin.glyph.GlyphManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResourcePackBuilder {
    private final JavaPlugin plugin;
    private final GlyphManager glyphManager;
    private final ConfigManager configManager;
    private final Gson gson;

    public ResourcePackBuilder(JavaPlugin plugin, GlyphManager glyphManager, ConfigManager configManager) {
        this.plugin = plugin;
        this.glyphManager = glyphManager;
        this.configManager = configManager;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    public void build() throws Exception {
        File buildDir = new File(plugin.getDataFolder(), "animatedGlyph/build");
        if (buildDir.exists()) {
            deleteDirectory(buildDir);
        }
        buildDir.mkdirs();

        // Tạo pack.mcmeta
        createPackMcmeta(buildDir);

        // Tạo cấu trúc thư mục đúng chuẩn
        File assetsDir = new File(buildDir, "assets/minecraft");
        new File(assetsDir, "textures/gif").mkdirs();  // Thay đổi: gif thay vì animatedGif
        new File(assetsDir, "font").mkdirs();
        new File(assetsDir, "shaders/core").mkdirs();

        // Xử lý từng glyph và tập hợp vào default.json
        List<GlyphDefinition> glyphs = glyphManager.getGlyphs();
        List<Map<String, Object>> allProviders = new ArrayList<>();

        for (GlyphDefinition glyph : glyphs) {
            String pngFileName = processGlyph(glyph, assetsDir);
            if (pngFileName != null) {
                // Thêm provider vào danh sách chung
                Map<String, Object> provider = createProvider(glyph, pngFileName);
                allProviders.add(provider);
            }
        }

        // Tạo default.json duy nhất
        createDefaultFontJson(allProviders, new File(assetsDir, "font"));

        // Copy shader files
        copyShaderFiles(new File(assetsDir, "shaders/core"));

        plugin.getLogger().info("ResourcePack đã được tạo thành công tại: " + buildDir.getAbsolutePath());
    }

    private String processGlyph(GlyphDefinition glyph, File assetsDir) throws Exception {
        // Tìm file GIF
        File gifFile = new File(plugin.getDataFolder(), "animatedGlyph/gif/" + glyph.getFile());
        if (!gifFile.exists()) {
            plugin.getLogger().warning("❌ Không tìm thấy file GIF: " + gifFile.getAbsolutePath());
            return null;
        }

        try {
            // Chuyển đổi GIF thành PNG sprite sheet với frames configurable
            BufferedImage spriteSheet = GifToPngConverter.convertGifToPngSheet(
                    gifFile,
                    glyph.getDuration(),
                    glyph.getFrames()  // Sử dụng frames từ config
            );

            String pngFileName = glyph.getName() + ".png";
            File pngFile = new File(assetsDir, "textures/gif/" + pngFileName);
            GifToPngConverter.savePng(spriteSheet, pngFile);

            // Debug thông tin chi tiết
            int gridSize = GifToPngConverter.getGridSizeFromFrames(glyph.getFrames());
            plugin.getLogger().info("✅ Đã tạo sprite sheet: " + pngFileName);
            plugin.getLogger().info("   📏 Kích thước: " + spriteSheet.getWidth() + "x" + spriteSheet.getHeight());
            plugin.getLogger().info("   🎯 Grid: " + gridSize + "x" + gridSize + " (" + glyph.getFrames() + " frames)");
            plugin.getLogger().info("   📁 Đường dẫn: textures/gif/" + pngFileName);

            return pngFileName;

        } catch (Exception e) {
            plugin.getLogger().severe("❌ Lỗi chuyển đổi GIF " + glyph.getName() + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private Map<String, Object> createProvider(GlyphDefinition glyph, String pngFileName) {
        Map<String, Object> provider = new HashMap<>();
        provider.put("type", "bitmap");
        provider.put("file", "minecraft:gif/" + pngFileName);  // Đúng namespace và path
        provider.put("ascent", glyph.getAscent());
        provider.put("height", glyph.getHeight());
        provider.put("chars", glyph.getChars());
        return provider;
    }

    /**
     * Tạo file default.json duy nhất chứa tất cả providers
     */
    private void createDefaultFontJson(List<Map<String, Object>> providers, File fontDir) throws Exception {
        Map<String, Object> fontData = new HashMap<>();
        fontData.put("providers", providers);

        File defaultFontFile = new File(fontDir, "default.json");
        try (FileWriter writer = new FileWriter(defaultFontFile)) {
            gson.toJson(fontData, writer);
        }

        plugin.getLogger().info("Đã tạo default.json với " + providers.size() + " providers");
    }

    private void createPackMcmeta(File buildDir) throws Exception {
        Map<String, Object> packData = new HashMap<>();
        Map<String, Object> pack = new HashMap<>();
        pack.put("pack_format", 46);
        pack.put("description", "Animated Gif resourcepack");
        packData.put("pack", pack);

        File packFile = new File(buildDir, "pack.mcmeta");
        try (FileWriter writer = new FileWriter(packFile)) {
            gson.toJson(packData, writer);
        }
    }

    // ... (Các phương thức copyShaderFiles, deleteDirectory giữ nguyên như trước)

    private void copyShaderFiles(File shaderDir) throws Exception {
        copyShaderFile(shaderDir, "rendertype_text.fsh", getFragmentShaderContent());
        copyShaderFile(shaderDir, "rendertype_text.vsh", getVertexShaderContent());
        copyShaderFile(shaderDir, "rendertype_text.json", getShaderJsonContent());
    }

    private void copyShaderFile(File shaderDir, String fileName, String content) throws Exception {
        File shaderFile = new File(shaderDir, fileName);
        Files.write(shaderFile.toPath(), content.getBytes());
    }

    private String getFragmentShaderContent() {
        return "#version 150\n\n" +
                "#moj_import <fog.glsl>\n\n" +
                "uniform sampler2D Sampler0;\n\n" +
                "uniform vec4 ColorModulator;\n" +
                "uniform float FogStart;\n" +
                "uniform float FogEnd;\n" +
                "uniform vec4 FogColor;\n\n" +
                "in float vertexDistance;\n" +
                "in vec4 vertexColor;\n" +
                "in vec2 texCoord0;\n" +
                "in vec2 texCoord1;\n" +
                "in float isAnimated;\n\n" +
                "out vec4 fragColor;\n\n" +
                "void main() {\n" +
                "    vec4 color = texture(Sampler0, isAnimated > 0.5 ? texCoord1 : texCoord0) * vertexColor * ColorModulator;\n" +
                "    if (color.a < 0.1) {\n" +
                "        discard;\n" +
                "    }\n\n" +
                "    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);\n" +
                "}";
    }

    private String getVertexShaderContent() {
        return "// animated unicodes\n" +
                "// https://github.com/JNNGL/vanilla-shaders\n\n" +
                "#version 150\n\n" +
                "#moj_import <fog.glsl>\n\n" +
                "in vec3 Position;\n" +
                "in vec4 Color;\n" +
                "in vec2 UV0;\n" +
                "in ivec2 UV2;\n\n" +
                "uniform sampler2D Sampler0;\n" +
                "uniform sampler2D Sampler2;\n\n" +
                "uniform mat4 ModelViewMat;\n" +
                "uniform mat4 ProjMat;\n" +
                "uniform int FogShape;\n" +
                "uniform float GameTime;\n\n" +
                "out float vertexDistance;\n" +
                "out vec4 vertexColor;\n" +
                "out vec2 texCoord0;\n" +
                "out vec2 texCoord1;\n" +
                "out float isAnimated;\n\n" +
                "bool validateProperty2(vec4 data) {\n" +
                "    return ivec2(round(data.zw * 255.0)) == ivec2(75, 1);\n" +
                "}\n\n" +
                "bool decodeProperties0(in ivec2 coord, out ivec2 dim, out int frame_dim,\n" +
                "                       out int nframes, out float time) {\n" +
                "    vec4 magic = texelFetch(Sampler0, coord, 0);\n" +
                "    if (ivec4(round(magic * 255.0)) != ivec4(149, 213, 75, 1)) {\n" +
                "        return false;\n" +
                "    }\n\n" +
                "    vec4 dim_in = texelFetch(Sampler0, coord + ivec2(1, 0), 0);\n" +
                "    if (!validateProperty2(dim_in)) {\n" +
                "        return false;\n" +
                "    }\n\n" +
                "    dim = ivec2(round(dim_in.xy * 255.0));\n\n" +
                "    vec4 frame_data = texelFetch(Sampler0, coord + ivec2(2, 0), 0);\n" +
                "    if (!validateProperty2(frame_data)) {\n" +
                "        return false;\n" +
                "    }\n\n" +
                "    frame_dim = int(round(frame_data.x * 255.0));\n" +
                "    nframes = int(round(frame_data.y * 255.0));\n\n" +
                "    vec4 packed_time = texelFetch(Sampler0, coord + ivec2(3, 0), 0);\n" +
                "    if (!validateProperty2(packed_time)) {\n" +
                "        return false;\n" +
                "    }\n\n" +
                "    time = packed_time.x * 255.0 + packed_time.y;\n" +
                "    return true;\n" +
                "}\n\n" +
                "bool decodeProperties(in vec2 uv, out ivec2 dim, out int frame_dim, out int nframes,\n" +
                "                      out float time, out ivec2 size, out vec2 origin) {\n" +
                "    vec2 texSize = vec2(textureSize(Sampler0, 0));\n" +
                "    size = ivec2(texSize);\n\n" +
                "    ivec2 coord = ivec2(uv * texSize);\n" +
                "    if (decodeProperties0(coord, dim, frame_dim, nframes, time)) {\n" +
                "        origin = uv;\n" +
                "        return true;\n" +
                "    }\n\n" +
                "    vec4 uv_offset = texelFetch(Sampler0, coord, 0);\n" +
                "    if (!validateProperty2(uv_offset)) {\n" +
                "        return false;\n" +
                "    }\n\n" +
                "    ivec2 pointing = coord - ivec2(round(uv_offset.xy * 255.0));\n" +
                "    origin = vec2(pointing) / texSize;\n\n" +
                "    return decodeProperties0(pointing, dim, frame_dim, nframes, time);\n" +
                "}\n\n" +
                "void main() {\n" +
                "    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);\n\n" +
                "    isAnimated = 0.0;\n" +
                "    vec2 uv = UV0;\n\n" +
                "    ivec2 dim;\n" +
                "    int frame_dim;\n" +
                "    int nframes;\n" +
                "    float loop_time;\n" +
                "    ivec2 size;\n" +
                "    vec2 origin;\n" +
                "    if (decodeProperties(UV0, dim, frame_dim, nframes, loop_time, size, origin)) {\n" +
                "        isAnimated = 1.0;\n" +
                "        float time = fract(GameTime * 1200.0 / loop_time);\n" +
                "        int frame = int(time * nframes);\n" +
                "        int uframes = (dim.x - 2) / frame_dim;\n" +
                "        int u = frame % uframes;\n" +
                "        int v = frame / uframes;\n" +
                "        uv = vec2((uv.x - origin.x) / float(dim.x) * float(frame_dim) + origin.x, (uv.y - origin.y) / float(dim.y) * float(frame_dim) + origin.y);\n" +
                "        uv += (vec2(u, v) * vec2(frame_dim) + 1.0) / vec2(size);\n" +
                "    }\n\n" +
                "    vertexDistance = fog_distance(Position, FogShape);\n" +
                "    vertexColor = Color * texelFetch(Sampler2, UV2 / 16, 0);\n" +
                "    texCoord0 = UV0;\n" +
                "    texCoord1 = uv;\n" +
                "}";
    }

    private String getShaderJsonContent() {
        return "{\n" +
                "    \"vertex\": \"minecraft:core/rendertype_text\",\n" +
                "    \"fragment\": \"minecraft:core/rendertype_text\",\n" +
                "    \"attributes\": [\n" +
                "        \"Position\",\n" +
                "        \"Color\",\n" +
                "        \"UV0\",\n" +
                "        \"UV2\"\n" +
                "    ],\n" +
                "    \"samplers\": [\n" +
                "        { \"name\": \"Sampler0\" },\n" +
                "        { \"name\": \"Sampler2\" }\n" +
                "    ],\n" +
                "    \"uniforms\": [\n" +
                "        { \"name\": \"ModelViewMat\", \"type\": \"matrix4x4\", \"count\": 16, \"values\": [ 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0 ] },\n" +
                "        { \"name\": \"ProjMat\", \"type\": \"matrix4x4\", \"count\": 16, \"values\": [ 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0 ] },\n" +
                "        { \"name\": \"GameTime\", \"type\": \"float\", \"count\": 1, \"values\": [ 0.0 ] },\n" +
                "        { \"name\": \"ColorModulator\", \"type\": \"float\", \"count\": 4, \"values\": [ 1.0, 1.0, 1.0, 1.0 ] },\n" +
                "        { \"name\": \"FogStart\", \"type\": \"float\", \"count\": 1, \"values\": [ 0.0 ] },\n" +
                "        { \"name\": \"FogEnd\", \"type\": \"float\", \"count\": 1, \"values\": [ 1.0 ] },\n" +
                "        { \"name\": \"FogColor\", \"type\": \"float\", \"count\": 4, \"values\": [ 0.0, 0.0, 0.0, 0.0 ] },\n" +
                "        { \"name\": \"FogShape\", \"type\": \"int\", \"count\": 1, \"values\": [ 0 ] }\n" +
                "    ]\n" +
                "}";
    }

    private void deleteDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        directory.delete();
    }
}
