# Animated Glyph Plugin

A powerful Paper/Spigot plugin for Minecraft 1.21.4 that automatically converts GIF animations into animated Unicode characters using core shaders and resource packs.

## Features
- **Automatic GIF to PNG Conversion**: Converts animated GIFs into sprite sheets with embedded metadata
- **Configurable Grid Sizes**: Support for 2x2, 3x3, 4x4, 5x5, 6x6, 7x7, 8x8, 9x9, and 10x10 frame layouts
- **Core Shader Integration**: Uses Minecraft's core shader system for smooth animations
- **Resource Pack Generation**: Automatically creates complete resource packs
- **Unicode Character Mapping**: Maps animations to custom Unicode characters
- **Hot Reload**: Update animations without restarting the server

## Requirements
- **Minecraft Version**: 1.21.4
- **Server Software**: Paper (recommended) or Spigot
- **Client**: Vanilla Minecraft (no mods required)

## Installation
1. Download the latest release from the releases page
2. Place the `.jar` file in your server's `plugins/` folder
3. Start your server
4. The plugin will automatically create the required directory structure

## Quick Start
After installation, the plugin creates example files in the `plugins/AnimatedGlyph/animatedGlyph/` directory:

```
animatedGlyph/
├── config.yml              # Plugin configuration
├── gif/
│   └── fire.gif            # Example animated GIF
├── glyph/
│   └── example.yml         # Glyph definition
└── build/                  # Generated resource pack
```

## Configuration

### Main Config (`animatedGlyph/config.yml`)
```yaml
debug-level: 1                    # Debug verbosity (0-2)
default-duration: 2.0             # Default animation duration in seconds
max-texture-size: 4096            # Maximum texture size
pack-description: "Animated Unicode Plugin - Generated ResourcePack"
```

### Glyph Configuration (`animatedGlyph/glyph/*.yml`)
Create YAML files in the `glyph/` directory to define your animated characters:
```yaml
name: fire                        # Internal name
file: fire.gif                    # GIF file name (in gif/ folder)
ascent: 11                        # Font ascent
height: 15                        # Font height
duration: 2.0                     # Animation loop duration
frames: 16                        # Number of frames (4, 9, 16, 25, 36, 49, 64, 81, 100)
chars: ["🔥"]                     # Unicode characters to map (leave empty for auto-generation)
```

## Supported Frame Counts
The plugin supports the following frame counts (perfect squares only):

| Frames | Grid Size | Use Case                |
|--------|-----------|-------------------------|
| 4      | 2x2       | Simple animations       |
| 9      | 3x3       | Basic animations        |
| 16     | 4x4       | Standard animations     |
| 25     | 5x5       | Detailed animations     |
| 36     | 6x6       | Complex animations      |
| 49     | 7x7       | High-detail animations  |
| 64     | 8x8       | Very detailed animations|
| 81     | 9x9       | Professional animations |
| 100    | 10x10     | Maximum detail animations|

## Commands

| Command                   | Permission             | Description                     |
|---------------------------|------------------------|---------------------------------|
| `/animatedglyph reload`   | animatedglyph.reload   | Regenerate the resource pack    |
| `/animatedglyph debug`    | animatedglyph.reload   | Show debug information          |
| `/animatedglyph structure`| animatedglyph.reload   | Display directory structure     |

## Creating Your First Animation
1. Add a GIF file to `animatedGlyph/gif/` (e.g., `myanimation.gif`)
2. Create a glyph definition in `animatedGlyph/glyph/myanimation.yml`:
```yaml
name: myanimation
file: myanimation.gif
ascent: 8
height: 12
duration: 1.5
frames: 16
chars: ["⚡"]
```
3. Reload the plugin:
```
/animatedglyph reload
```
4. Distribute the resource pack from `animatedGlyph/build/` to your players
5. Use the character in chat, signs, books, etc.: ⚡

## Resource Pack Distribution
The generated resource pack is located in `animatedGlyph/build/`. You can:
1. Zip the contents and upload to a file hosting service
2. Configure your server to automatically provide the resource pack
3. Share directly with players for manual installation

## Advanced Usage

### Custom Unicode Ranges
To avoid conflicts, the plugin uses Private Use Area Unicode characters (U+E000-U+F8FF) for auto-generated mappings.

### Performance Considerations
- **GIF Size**: Larger GIFs take more processing time
- **Frame Count**: Higher frame counts use more memory
- **Texture Resolution**: Keep individual frames reasonable (40x40 recommended)

### Multiple Animations
You can create multiple animated characters by adding more GIF files and corresponding YAML configurations. Each will be processed into the same resource pack.
