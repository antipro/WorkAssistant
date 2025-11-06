package com.workassistant.image;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 * Utility methods for image processing operations.
 */
public class ImageUtils {
    
    /**
     * Convert an image to grayscale.
     * Uses optimized raster access for TYPE_BYTE_GRAY images.
     * 
     * @param input the input image
     * @return grayscale image of TYPE_BYTE_GRAY
     */
    public static BufferedImage toGrayscale(BufferedImage input) {
        if (input.getType() == BufferedImage.TYPE_BYTE_GRAY) {
            return input;
        }
        
        BufferedImage gray = new BufferedImage(
            input.getWidth(), 
            input.getHeight(), 
            BufferedImage.TYPE_BYTE_GRAY
        );
        
        Graphics2D g = gray.createGraphics();
        g.drawImage(input, 0, 0, null);
        g.dispose();
        
        return gray;
    }
    
    /**
     * Apply Gaussian blur to an image.
     * 
     * @param input the input image
     * @param kernelSize the size of the blur kernel (must be odd)
     * @return blurred image
     */
    public static BufferedImage applyGaussianBlur(BufferedImage input, int kernelSize) {
        if (kernelSize <= 1) {
            return input;
        }
        
        int width = input.getWidth();
        int height = input.getHeight();
        BufferedImage output = new BufferedImage(width, height, input.getType());
        
        // Generate Gaussian kernel
        double[][] kernel = generateGaussianKernel(kernelSize);
        
        // Apply convolution
        int radius = kernelSize / 2;
        byte[] inputPixels = getPixels(input);
        byte[] outputPixels = new byte[width * height];
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double sum = 0;
                
                for (int ky = -radius; ky <= radius; ky++) {
                    for (int kx = -radius; kx <= radius; kx++) {
                        int px = clamp(x + kx, 0, width - 1);
                        int py = clamp(y + ky, 0, height - 1);
                        int pixel = inputPixels[py * width + px] & 0xFF;
                        sum += pixel * kernel[ky + radius][kx + radius];
                    }
                }
                
                outputPixels[y * width + x] = (byte) clamp((int) sum, 0, 255);
            }
        }
        
        setPixels(output, outputPixels);
        return output;
    }
    
    /**
     * Apply median blur to an image.
     * 
     * @param input the input image
     * @param kernelSize the size of the blur kernel (must be odd)
     * @return blurred image
     */
    public static BufferedImage applyMedianBlur(BufferedImage input, int kernelSize) {
        if (kernelSize <= 1) {
            return input;
        }
        
        int width = input.getWidth();
        int height = input.getHeight();
        BufferedImage output = new BufferedImage(width, height, input.getType());
        
        int radius = kernelSize / 2;
        byte[] inputPixels = getPixels(input);
        byte[] outputPixels = new byte[width * height];
        int[] window = new int[kernelSize * kernelSize];
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int idx = 0;
                
                for (int ky = -radius; ky <= radius; ky++) {
                    for (int kx = -radius; kx <= radius; kx++) {
                        int px = clamp(x + kx, 0, width - 1);
                        int py = clamp(y + ky, 0, height - 1);
                        window[idx++] = inputPixels[py * width + px] & 0xFF;
                    }
                }
                
                Arrays.sort(window, 0, idx);
                outputPixels[y * width + x] = (byte) window[idx / 2];
            }
        }
        
        setPixels(output, outputPixels);
        return output;
    }
    
    /**
     * Apply morphological erosion (shrinks white regions).
     */
    public static BufferedImage erode(BufferedImage input, int kernelSize) {
        int width = input.getWidth();
        int height = input.getHeight();
        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);
        
        int radius = kernelSize / 2;
        byte[] inputPixels = getPixels(input);
        byte[] outputPixels = new byte[width * height];
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                boolean allWhite = true;
                
                for (int ky = -radius; ky <= radius && allWhite; ky++) {
                    for (int kx = -radius; kx <= radius && allWhite; kx++) {
                        int px = clamp(x + kx, 0, width - 1);
                        int py = clamp(y + ky, 0, height - 1);
                        if ((inputPixels[py * width + px] & 0xFF) < 128) {
                            allWhite = false;
                        }
                    }
                }
                
                outputPixels[y * width + x] = allWhite ? (byte) 255 : 0;
            }
        }
        
        setPixels(output, outputPixels);
        return output;
    }
    
    /**
     * Apply morphological dilation (expands white regions).
     */
    public static BufferedImage dilate(BufferedImage input, int kernelSize) {
        int width = input.getWidth();
        int height = input.getHeight();
        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);
        
        int radius = kernelSize / 2;
        byte[] inputPixels = getPixels(input);
        byte[] outputPixels = new byte[width * height];
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                boolean anyWhite = false;
                
                for (int ky = -radius; ky <= radius && !anyWhite; ky++) {
                    for (int kx = -radius; kx <= radius && !anyWhite; kx++) {
                        int px = clamp(x + kx, 0, width - 1);
                        int py = clamp(y + ky, 0, height - 1);
                        if ((inputPixels[py * width + px] & 0xFF) >= 128) {
                            anyWhite = true;
                        }
                    }
                }
                
                outputPixels[y * width + x] = anyWhite ? (byte) 255 : 0;
            }
        }
        
        setPixels(output, outputPixels);
        return output;
    }
    
    /**
     * Apply morphological opening (erosion followed by dilation).
     * Removes small white regions (noise).
     */
    public static BufferedImage open(BufferedImage input, int kernelSize) {
        return dilate(erode(input, kernelSize), kernelSize);
    }
    
    /**
     * Apply morphological closing (dilation followed by erosion).
     * Fills small black regions (holes).
     */
    public static BufferedImage close(BufferedImage input, int kernelSize) {
        return erode(dilate(input, kernelSize), kernelSize);
    }
    
    /**
     * Convert image to binary (black and white only).
     */
    public static BufferedImage toBinaryImage(BufferedImage input) {
        if (input.getType() == BufferedImage.TYPE_BYTE_BINARY) {
            return input;
        }
        
        int width = input.getWidth();
        int height = input.getHeight();
        BufferedImage binary = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);
        
        Graphics2D g = binary.createGraphics();
        g.drawImage(input, 0, 0, null);
        g.dispose();
        
        return binary;
    }
    
    /**
     * Load an image from a file.
     */
    public static BufferedImage loadImage(String path) throws IOException {
        File file = new File(path);
        BufferedImage image = ImageIO.read(file);
        if (image == null) {
            throw new IOException("Failed to read image from: " + path);
        }
        return image;
    }
    
    /**
     * Save an image to a file.
     */
    public static void saveImage(BufferedImage image, String path) throws IOException {
        File file = new File(path);
        String format = getFormatFromPath(path);
        if (!ImageIO.write(image, format, file)) {
            throw new IOException("Failed to write image to: " + path);
        }
    }
    
    /**
     * Get pixel data from an image as a byte array.
     */
    public static byte[] getPixels(BufferedImage image) {
        BufferedImage gray = toGrayscale(image);
        DataBuffer buffer = gray.getRaster().getDataBuffer();
        if (buffer instanceof DataBufferByte) {
            return ((DataBufferByte) buffer).getData();
        }
        
        // Fallback for other buffer types
        int width = image.getWidth();
        int height = image.getHeight();
        byte[] pixels = new byte[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = gray.getRGB(x, y);
                pixels[y * width + x] = (byte) (rgb & 0xFF);
            }
        }
        return pixels;
    }
    
    /**
     * Set pixel data in an image from a byte array.
     */
    public static void setPixels(BufferedImage image, byte[] pixels) {
        DataBuffer buffer = image.getRaster().getDataBuffer();
        if (buffer instanceof DataBufferByte) {
            byte[] data = ((DataBufferByte) buffer).getData();
            System.arraycopy(pixels, 0, data, 0, Math.min(pixels.length, data.length));
        } else {
            // Fallback for other buffer types
            int width = image.getWidth();
            int height = image.getHeight();
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int idx = y * width + x;
                    if (idx < pixels.length) {
                        int gray = pixels[idx] & 0xFF;
                        int rgb = (gray << 16) | (gray << 8) | gray;
                        image.setRGB(x, y, rgb);
                    }
                }
            }
        }
    }
    
    private static double[][] generateGaussianKernel(int size) {
        double[][] kernel = new double[size][size];
        double sigma = size / 6.0;
        double sum = 0;
        int center = size / 2;
        
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int dx = x - center;
                int dy = y - center;
                kernel[y][x] = Math.exp(-(dx * dx + dy * dy) / (2 * sigma * sigma));
                sum += kernel[y][x];
            }
        }
        
        // Normalize
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                kernel[y][x] /= sum;
            }
        }
        
        return kernel;
    }
    
    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
    
    private static String getFormatFromPath(String path) {
        int dotIndex = path.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < path.length() - 1) {
            return path.substring(dotIndex + 1).toLowerCase();
        }
        return "png"; // Default format
    }
}
