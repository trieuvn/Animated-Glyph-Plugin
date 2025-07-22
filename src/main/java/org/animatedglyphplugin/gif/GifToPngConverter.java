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

    private static final int FRAME_SIZE = 40;  // Cố định 40x40 pixel
    private static final int GRID_SIZE = 4;    // Luôn 4x4 grid
    private static final int TOTAL_FRAMES = 16; // 4x4 = 16 frames

    public static BufferedImage convertGifToPngSheet(File gifFile, double animationSeconds) throws IOException {
        List<BufferedImage> frames = loadAndProcessGifFrames(gifFile);
        if (frames.isEmpty()) {
            throw new IOException("Không thể đọc frames từ file GIF: " + gifFile.getName());
        }

        return buildSprite4x4Sheet(frames, animationSeconds);
    }

    private static List<BufferedImage> loadAndProcessGifFrames(File gifFile) throws IOException {
        List<BufferedImage> originalFrames = new ArrayList<>();

        try (ImageInputStream iis = ImageIO.createImageInputStream(gifFile)) {
            Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("gif");
            if (!readers.hasNext()) {
                throw new IOException("Không tìm thấy GIF reader");
            }

            ImageReader reader = readers.next();
            reader.setInput(iis);

            int frameCount = reader.getNumImages(true);
            for (int i = 0; i < frameCount; i++) {
                BufferedImage frame = reader.read(i);
                originalFrames.add(frame);
            }
            reader.dispose();
        }

        return processFramesToFixedSize(originalFrames);
    }

    /**
     * Xử lý frames: resize về 40x40 và điều chỉnh số lượng để có đúng 16 frames
     */
    private static List<BufferedImage> processFramesToFixedSize(List<BufferedImage> originalFrames) {
        List<BufferedImage> processedFrames = new ArrayList<>();

        // Resize tất cả frames về 40x40
        for (BufferedImage originalFrame : originalFrames) {
            BufferedImage resizedFrame = resizeFrame(originalFrame, FRAME_SIZE, FRAME_SIZE);
            processedFrames.add(resizedFrame);
        }

        // Điều chỉnh số lượng frames để có đúng 16 frames
        if (processedFrames.size() < TOTAL_FRAMES) {
            // Thiếu frame: lặp lại frame cuối
            BufferedImage lastFrame = processedFrames.get(processedFrames.size() - 1);
            while (processedFrames.size() < TOTAL_FRAMES) {
                processedFrames.add(duplicateFrame(lastFrame));
            }
        } else if (processedFrames.size() > TOTAL_FRAMES) {
            // Dư frame: cắt bớt chỉ lấy 16 frame đầu
            processedFrames = processedFrames.subList(0, TOTAL_FRAMES);
        }

        return processedFrames;
    }

    /**
     * Resize frame về kích thước cố định với chất lượng cao
     */
    private static BufferedImage resizeFrame(BufferedImage originalFrame, int targetWidth, int targetHeight) {
        BufferedImage resizedFrame = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = resizedFrame.createGraphics();

        // Cài đặt rendering hints để có chất lượng tốt nhất
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Xóa nền và vẽ frame đã resize
        g2d.setComposite(AlphaComposite.Clear);
        g2d.fillRect(0, 0, targetWidth, targetHeight);
        g2d.setComposite(AlphaComposite.SrcOver);
        g2d.drawImage(originalFrame, 0, 0, targetWidth, targetHeight, null);
        g2d.dispose();

        return resizedFrame;
    }

    /**
     * Tạo bản copy của frame
     */
    private static BufferedImage duplicateFrame(BufferedImage sourceFrame) {
        BufferedImage duplicatedFrame = new BufferedImage(
                sourceFrame.getWidth(),
                sourceFrame.getHeight(),
                BufferedImage.TYPE_INT_ARGB
        );
        Graphics2D g2d = duplicatedFrame.createGraphics();
        g2d.drawImage(sourceFrame, 0, 0, null);
        g2d.dispose();
        return duplicatedFrame;
    }

    /**
     * Tạo sprite sheet 4x4 với kích thước cố định
     */
    private static BufferedImage buildSprite4x4Sheet(List<BufferedImage> frames, double animationSeconds) {
        // Kích thước sprite sheet: 4*40 + 2 = 162x162 (với border 1 pixel mỗi bên)
        int sheetWidth = GRID_SIZE * FRAME_SIZE + 2;
        int sheetHeight = GRID_SIZE * FRAME_SIZE + 2;

        BufferedImage spriteSheet = new BufferedImage(sheetWidth, sheetHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = spriteSheet.createGraphics();

        // Tạo nền trong suốt hoàn toàn
        g2d.setComposite(AlphaComposite.Clear);
        g2d.fillRect(0, 0, sheetWidth, sheetHeight);
        g2d.setComposite(AlphaComposite.SrcOver);

        // Dán từng frame vào vị trí đúng trong grid 4x4
        for (int i = 0; i < TOTAL_FRAMES && i < frames.size(); i++) {
            BufferedImage frame = frames.get(i);

            // Tính tọa độ trong grid 4x4
            int gridX = i % GRID_SIZE;
            int gridY = i / GRID_SIZE;

            // Vị trí thực tế trên sprite sheet (cộng thêm border 1 pixel)
            int x = gridX * FRAME_SIZE + 1;
            int y = gridY * FRAME_SIZE + 1;

            g2d.drawImage(frame, x, y, null);
        }
        g2d.dispose();

        // Ghi metadata pixels theo đúng format shader mong đợi
        writeMetadataPixels(spriteSheet, sheetWidth, sheetHeight, animationSeconds);

        return spriteSheet;
    }

    /**
     * Ghi metadata vào các pixel đặc biệt theo format shader
     */
    private static void writeMetadataPixels(BufferedImage image, int sheetWidth, int sheetHeight, double animationSeconds) {
        // Pixel (0,0): Magic number
        image.setRGB(0, 0, packRGBA(149, 213, 75, 1));

        // Pixel (1,0): Kích thước sprite sheet
        image.setRGB(1, 0, packRGBA(sheetWidth & 0xFF, sheetHeight & 0xFF, 75, 1));

        // Pixel (2,0): Frame dimension và số frames
        image.setRGB(2, 0, packRGBA(FRAME_SIZE & 0xFF, TOTAL_FRAMES & 0xFF, 75, 1));

        // Pixel (3,0): Thời gian animation
        int seconds = (int) Math.floor(animationSeconds);
        int fraction = (int) Math.floor((animationSeconds - seconds) * 255.0);
        image.setRGB(3, 0, packRGBA(seconds & 0xFF, fraction & 0xFF, 75, 1));

        // Corner markers
        image.setRGB(sheetWidth - 1, 0, packRGBA((sheetWidth - 1) & 0xFF, 0, 75, 1));
        image.setRGB(0, sheetHeight - 1, packRGBA(0, (sheetHeight - 1) & 0xFF, 75, 1));
        image.setRGB(sheetWidth - 1, sheetHeight - 1, packRGBA((sheetWidth - 1) & 0xFF, (sheetHeight - 1) & 0xFF, 75, 1));
    }

    /**
     * Đóng gói RGBA values thành int
     */
    private static int packRGBA(int r, int g, int b, int a) {
        return ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    public static void savePng(BufferedImage image, File outputFile) throws IOException {
        if (!outputFile.getParentFile().exists()) {
            outputFile.getParentFile().mkdirs();
        }
        ImageIO.write(image, "PNG", outputFile);
    }
}
