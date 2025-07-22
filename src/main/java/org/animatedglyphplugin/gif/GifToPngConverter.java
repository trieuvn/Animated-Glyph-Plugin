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

    private static final int FRAME_SIZE = 40;  // Kích thước mỗi frame (có thể config sau)

    // Các giá trị frames được phép (2x2, 3x3, 4x4, 5x5, 6x6, 7x7, 8x8, 9x9, 10x10)
    private static final int[] ALLOWED_FRAMES = {4, 9, 16, 25, 36, 49, 64, 81, 100};

    /**
     * Phương thức chính với configurable frames
     */
    public static BufferedImage convertGifToPngSheet(File gifFile, double animationSeconds, int configFrames) throws IOException {
        // Validate frames input
        int gridSize = validateAndGetGridSize(configFrames);

        List<BufferedImage> frames = loadAndProcessGifFrames(gifFile);
        if (frames.isEmpty()) {
            throw new IOException("Không thể đọc frames từ file GIF: " + gifFile.getName());
        }

        return buildConfigurableSpriteSheet(frames, animationSeconds, gridSize, configFrames);
    }

    /**
     * Phương thức backward compatible (mặc định 4x4)
     */
    public static BufferedImage convertGifToPngSheet(File gifFile, double animationSeconds) throws IOException {
        return convertGifToPngSheet(gifFile, animationSeconds, 16); // Mặc định 4x4
    }

    /**
     * Validate số frames và trả về grid size
     */
    private static int validateAndGetGridSize(int frames) throws IllegalArgumentException {
        for (int allowedFrames : ALLOWED_FRAMES) {
            if (frames == allowedFrames) {
                return (int) Math.sqrt(frames);
            }
        }

        StringBuilder allowedList = new StringBuilder();
        for (int i = 0; i < ALLOWED_FRAMES.length; i++) {
            allowedList.append(ALLOWED_FRAMES[i]);
            if (i < ALLOWED_FRAMES.length - 1) {
                allowedList.append(", ");
            }
        }

        throw new IllegalArgumentException(
                "Số frames không hợp lệ: " + frames + ". " +
                        "Chỉ cho phép các giá trị: " + allowedList.toString() + " " +
                        "(tương ứng 2x2, 3x3, 4x4, 5x5, 6x6, 7x7, 8x8, 9x9, 10x10)"
        );
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

        return originalFrames;
    }

    /**
     * Xử lý frames với số lượng configurable
     */
    private static List<BufferedImage> processFramesToConfigurableSize(List<BufferedImage> originalFrames, int targetFrameCount) {
        List<BufferedImage> processedFrames = new ArrayList<>();

        // Resize tất cả frames về FRAME_SIZE x FRAME_SIZE
        for (BufferedImage originalFrame : originalFrames) {
            BufferedImage resizedFrame = resizeFrame(originalFrame, FRAME_SIZE, FRAME_SIZE);
            processedFrames.add(resizedFrame);
        }

        // Điều chỉnh số lượng frames theo target
        if (processedFrames.size() < targetFrameCount) {
            // Thiếu frame: lặp lại frames có sẵn theo pattern
            while (processedFrames.size() < targetFrameCount) {
                int sourceIndex = (processedFrames.size() - originalFrames.size()) % originalFrames.size();
                BufferedImage frameToClone = processedFrames.get(sourceIndex);
                processedFrames.add(duplicateFrame(frameToClone));
            }
        } else if (processedFrames.size() > targetFrameCount) {
            // Dư frame: cắt bớt hoặc sample đều
            if (processedFrames.size() <= targetFrameCount * 2) {
                // Nếu không quá nhiều, chỉ cắt bớt
                processedFrames = processedFrames.subList(0, targetFrameCount);
            } else {
                // Nếu quá nhiều, sample đều
                List<BufferedImage> sampledFrames = new ArrayList<>();
                for (int i = 0; i < targetFrameCount; i++) {
                    int sourceIndex = (i * processedFrames.size()) / targetFrameCount;
                    sampledFrames.add(processedFrames.get(sourceIndex));
                }
                processedFrames = sampledFrames;
            }
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
     * Tạo sprite sheet với grid size configurable
     */
    private static BufferedImage buildConfigurableSpriteSheet(List<BufferedImage> originalFrames, double animationSeconds, int gridSize, int totalFrames) {
        // Process frames theo số lượng cần thiết
        List<BufferedImage> frames = processFramesToConfigurableSize(originalFrames, totalFrames);

        // Kích thước sprite sheet: gridSize * FRAME_SIZE + 2 (border 1 pixel mỗi bên)
        int sheetWidth = gridSize * FRAME_SIZE + 2;
        int sheetHeight = gridSize * FRAME_SIZE + 2;

        BufferedImage spriteSheet = new BufferedImage(sheetWidth, sheetHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = spriteSheet.createGraphics();

        // Tạo nền trong suốt hoàn toàn
        g2d.setComposite(AlphaComposite.Clear);
        g2d.fillRect(0, 0, sheetWidth, sheetHeight);
        g2d.setComposite(AlphaComposite.SrcOver);

        // Dán từng frame vào vị trí đúng trong grid
        for (int i = 0; i < totalFrames && i < frames.size(); i++) {
            BufferedImage frame = frames.get(i);

            // Tính tọa độ trong grid
            int gridX = i % gridSize;
            int gridY = i / gridSize;

            // Vị trí thực tế trên sprite sheet (cộng thêm border 1 pixel)
            int x = gridX * FRAME_SIZE + 1;
            int y = gridY * FRAME_SIZE + 1;

            g2d.drawImage(frame, x, y, null);
        }
        g2d.dispose();

        // Ghi metadata pixels theo đúng format shader mong đợi
        writeMetadataPixels(spriteSheet, sheetWidth, sheetHeight, animationSeconds, totalFrames);

        return spriteSheet;
    }

    /**
     * Ghi metadata vào các pixel đặc biệt theo format shader (updated)
     */
    private static void writeMetadataPixels(BufferedImage image, int sheetWidth, int sheetHeight, double animationSeconds, int totalFrames) {
        // Pixel (0,0): Magic number
        image.setRGB(0, 0, packRGBA(149, 213, 75, 1));

        // Pixel (1,0): Kích thước sprite sheet
        image.setRGB(1, 0, packRGBA(sheetWidth & 0xFF, sheetHeight & 0xFF, 75, 1));

        // Pixel (2,0): Frame dimension và số frames (cập nhật với totalFrames configurable)
        image.setRGB(2, 0, packRGBA(FRAME_SIZE & 0xFF, totalFrames & 0xFF, 75, 1));

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

    /**
     * Utility method để check frames có hợp lệ không
     */
    public static boolean isValidFrameCount(int frames) {
        for (int allowedFrames : ALLOWED_FRAMES) {
            if (frames == allowedFrames) {
                return true;
            }
        }
        return false;
    }

    /**
     * Utility method để lấy grid size từ frame count
     */
    public static int getGridSizeFromFrames(int frames) throws IllegalArgumentException {
        return validateAndGetGridSize(frames);
    }
}
