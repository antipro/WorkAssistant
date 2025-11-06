package com.workassistant.image.impl;

import com.workassistant.image.BinarizeConfig;
import com.workassistant.image.Binarizer;
import com.workassistant.image.ImageUtils;

import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * Pure Java implementation of adaptive mean thresholding using integral images.
 * Efficient implementation suitable for uneven lighting conditions.
 */
public class AdaptiveMeanBinarizer implements Binarizer {
    
    @Override
    public BufferedImage binarize(BufferedImage input, BinarizeConfig config) {
        // Convert to grayscale
        BufferedImage gray = ImageUtils.toGrayscale(input);
        
        // Apply blur if configured
        if (config.getBlurKernelSize() > 0) {
            if (config.isUseMedianBlur()) {
                gray = ImageUtils.applyMedianBlur(gray, config.getBlurKernelSize());
            } else {
                gray = ImageUtils.applyGaussianBlur(gray, config.getBlurKernelSize());
            }
        }
        
        // Apply adaptive mean thresholding
        BufferedImage binary = applyAdaptiveThreshold(gray, config.getBlockSize(), config.getC());
        
        // Apply morphological operations if configured
        if (config.isUseMorphologicalOpening()) {
            binary = ImageUtils.open(binary, config.getMorphKernelSize());
        }
        if (config.isUseMorphologicalClosing()) {
            binary = ImageUtils.close(binary, config.getMorphKernelSize());
        }
        
        return binary;
    }
    
    @Override
    public BufferedImage binarize(String inputPath, BinarizeConfig config) throws IOException {
        BufferedImage input = ImageUtils.loadImage(inputPath);
        return binarize(input, config);
    }
    
    @Override
    public void binarize(String inputPath, String outputPath, BinarizeConfig config) throws IOException {
        BufferedImage result = binarize(inputPath, config);
        ImageUtils.saveImage(result, outputPath);
    }
    
    @Override
    public String getName() {
        return "AdaptiveMean";
    }
    
    /**
     * Apply adaptive mean thresholding using integral image for efficiency.
     * Each pixel is compared to the mean of its neighborhood.
     */
    private BufferedImage applyAdaptiveThreshold(BufferedImage gray, int blockSize, double C) {
        int width = gray.getWidth();
        int height = gray.getHeight();
        
        byte[] pixels = ImageUtils.getPixels(gray);
        
        // Build integral image for fast mean calculation
        long[][] integral = buildIntegralImage(pixels, width, height);
        
        // Apply adaptive threshold
        BufferedImage binary = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        byte[] outputPixels = new byte[width * height];
        
        int radius = blockSize / 2;
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // Calculate bounds of the local window
                int x1 = Math.max(0, x - radius);
                int y1 = Math.max(0, y - radius);
                int x2 = Math.min(width - 1, x + radius);
                int y2 = Math.min(height - 1, y + radius);
                
                // Calculate mean using integral image
                int count = (x2 - x1 + 1) * (y2 - y1 + 1);
                long sum = getIntegralSum(integral, x1, y1, x2, y2);
                double mean = (double) sum / count;
                
                // Apply threshold
                int pixel = pixels[y * width + x] & 0xFF;
                outputPixels[y * width + x] = (byte) (pixel >= (mean - C) ? 255 : 0);
            }
        }
        
        ImageUtils.setPixels(binary, outputPixels);
        return ImageUtils.toBinaryImage(binary);
    }
    
    /**
     * Build an integral image (summed area table) for fast region sum calculation.
     * integral[y][x] = sum of all pixels from (0,0) to (x,y) inclusive.
     */
    private long[][] buildIntegralImage(byte[] pixels, int width, int height) {
        long[][] integral = new long[height + 1][width + 1];
        
        for (int y = 1; y <= height; y++) {
            long rowSum = 0;
            for (int x = 1; x <= width; x++) {
                int pixel = pixels[(y - 1) * width + (x - 1)] & 0xFF;
                rowSum += pixel;
                integral[y][x] = integral[y - 1][x] + rowSum;
            }
        }
        
        return integral;
    }
    
    /**
     * Calculate sum of pixels in a rectangle using the integral image.
     * This operation is O(1) regardless of rectangle size.
     */
    private long getIntegralSum(long[][] integral, int x1, int y1, int x2, int y2) {
        // Convert to 1-based indices for integral image
        x1++; y1++; x2++; y2++;
        
        return integral[y2][x2] 
             - integral[y1 - 1][x2]
             - integral[y2][x1 - 1]
             + integral[y1 - 1][x1 - 1];
    }
}
