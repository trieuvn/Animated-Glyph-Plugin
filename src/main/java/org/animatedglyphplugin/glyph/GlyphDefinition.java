package org.animatedglyphplugin.glyph;

import java.util.List;

public class GlyphDefinition {
    private String name;
    private String file;
    private int ascent;
    private int height;
    private List<String> chars;
    private double duration;

    public GlyphDefinition(String name, String file, int ascent, int height, List<String> chars, double duration) {
        this.name = name;
        this.file = file;
        this.ascent = ascent;
        this.height = height;
        this.chars = chars;
        this.duration = duration;
    }

    // Getters và Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getFile() { return file; }
    public void setFile(String file) { this.file = file; }

    public int getAscent() { return ascent; }
    public void setAscent(int ascent) { this.ascent = ascent; }

    public int getHeight() { return height; }
    public void setHeight(int height) { this.height = height; }

    public List<String> getChars() { return chars; }
    public void setChars(List<String> chars) { this.chars = chars; }

    public double getDuration() { return duration; }
    public void setDuration(double duration) { this.duration = duration; }
}
