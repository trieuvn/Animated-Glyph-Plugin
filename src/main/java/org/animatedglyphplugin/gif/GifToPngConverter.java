package org.animatedglyphplugin.gif;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GifToPngConverter {

    private static String pythonScriptPath = null;

    /**
     * Tạo script Python và lưu vào temp directory
     */
    private static String getPythonScript(File pluginDataFolder) throws IOException {
        if (pythonScriptPath != null) {
            return pythonScriptPath;
        }

        File scriptsDir = new File(pluginDataFolder, "scripts");
        if (!scriptsDir.exists()) {
            scriptsDir.mkdirs();
        }

        File pythonScript = new File(scriptsDir, "gif_converter.py");

        // Ghi script Python vào file
        String scriptContent = getPythonScriptContent();
        try (FileWriter writer = new FileWriter(pythonScript)) {
            writer.write(scriptContent);
        }

        pythonScriptPath = pythonScript.getAbsolutePath();
        return pythonScriptPath;
    }

    /**
     * Sử dụng script Python để chuyển đổi GIF
     */
    public static boolean convertGifToPngUsingPython(File gifFile, double animationSeconds, File outputPng, File pluginDataFolder) throws Exception {
        String scriptPath = getPythonScript(pluginDataFolder);

        // Tạo thư mục output nếu chưa tồn tại
        if (!outputPng.getParentFile().exists()) {
            outputPng.getParentFile().mkdirs();
        }

        // Xây dựng command để chạy Python
        String[] command = {
                "python3",
                scriptPath,
                gifFile.getAbsolutePath(),
                String.valueOf(animationSeconds),
                outputPng.getAbsolutePath()
        };

        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(pluginDataFolder);
            pb.redirectErrorStream(true);

            Process process = pb.start();

            // Đọc output từ Python script
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();
            String outputStr = output.toString().trim();

            if (exitCode == 0 && outputStr.startsWith("SUCCESS:")) {
                return true;
            } else {
                throw new RuntimeException("Python script failed: " + outputStr);
            }

        } catch (IOException | InterruptedException e) {
            // Thử với "python" thay vì "python3"
            command[0] = "python";
            try {
                ProcessBuilder pb = new ProcessBuilder(command);
                pb.directory(pluginDataFolder);
                pb.redirectErrorStream(true);

                Process process = pb.start();

                StringBuilder output = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
                    }
                }

                int exitCode = process.waitFor();
                String outputStr = output.toString().trim();

                if (exitCode == 0 && outputStr.startsWith("SUCCESS:")) {
                    return true;
                } else {
                    throw new RuntimeException("Python script failed with 'python': " + outputStr);
                }

            } catch (Exception e2) {
                throw new RuntimeException("Could not run Python script. Make sure Python is installed with PIL/Pillow: " + e2.getMessage());
            }
        }
    }

    private static String getPythonScriptContent() {
        return "#!/usr/bin/python3\n\n" +
                "import math\n" +
                "import sys\n" +
                "import os\n" +
                "from PIL import Image\n\n" +
                "def build_frames(gif_path, animation_seconds, output_path):\n" +
                "    try:\n" +
                "        gif = Image.open(gif_path)\n" +
                "        num_frames = gif.n_frames\n" +
                "        frame_width, frame_height = gif.size\n" +
                "        frames_per_line = math.ceil(math.sqrt(num_frames))\n" +
                "        frame_dim = max(frame_width, frame_height)\n" +
                "        width = frames_per_line * frame_dim + 2\n" +
                "        height = frames_per_line * frame_dim + 2\n" +
                "        \n" +
                "        frames = Image.new(\"RGBA\", (width, height), color=(0, 0, 0, 0))\n" +
                "        \n" +
                "        for i in range(num_frames):\n" +
                "            gif.seek(i)\n" +
                "            frame = gif.copy()\n" +
                "            aligned_frame = Image.new(\"RGBA\", (frame_dim, frame_dim), color=(0, 0, 0, 0))\n" +
                "            aligned_frame.paste(frame, (0, 0))\n" +
                "            frames.paste(aligned_frame,\n" +
                "                         ((i % frames_per_line) * frame_dim + 1, \n" +
                "                          math.floor(i / frames_per_line) * frame_dim + 1),\n" +
                "                         aligned_frame)\n" +
                "        \n" +
                "        frames.putpixel((0, 0), (149, 213, 75, 1))\n" +
                "        frames.putpixel((1, 0), (width, height, 75, 1))\n" +
                "        frames.putpixel((2, 0), (frame_dim, num_frames, 75, 1))\n" +
                "        s = math.floor(animation_seconds)\n" +
                "        frames.putpixel((3, 0), (s, math.floor((animation_seconds - s) * 255.0), 75, 1))\n" +
                "        frames.putpixel((width - 1, 0), (width - 1, 0, 75, 1))\n" +
                "        frames.putpixel((0, height - 1), (0, height - 1, 75, 1))\n" +
                "        frames.putpixel((width - 1, height - 1), (width - 1, height - 1, 75, 1))\n" +
                "        \n" +
                "        frames.save(output_path, format='PNG')\n" +
                "        print(f\"SUCCESS: {output_path}\")\n" +
                "        return True\n" +
                "        \n" +
                "    except Exception as e:\n" +
                "        print(f\"ERROR: {str(e)}\")\n" +
                "        return False\n\n" +
                "if __name__ == \"__main__\":\n" +
                "    if len(sys.argv) != 4:\n" +
                "        print(\"Usage: python gif_converter.py <input_gif> <duration> <output_png>\")\n" +
                "        sys.exit(1)\n" +
                "    \n" +
                "    gif_path = sys.argv[1]\n" +
                "    duration = float(sys.argv[2])\n" +
                "    output_path = sys.argv[3]\n" +
                "    \n" +
                "    if not os.path.exists(gif_path):\n" +
                "        print(f\"ERROR: Input file not found: {gif_path}\")\n" +
                "        sys.exit(1)\n" +
                "        \n" +
                "    success = build_frames(gif_path, duration, output_path)\n" +
                "    sys.exit(0 if success else 1)";
    }
}
