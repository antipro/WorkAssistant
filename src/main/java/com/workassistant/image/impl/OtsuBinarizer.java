package com.workassistant.image.impl;

import com.workassistant.image.BinarizeConfig;
import com.workassistant.image.Binarizer;
import com.workassistant.image.ImageUtils;

import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * Pure Java implementation of Otsu's automatic thresholding method.
 * Computes the optimal threshold that minimizes intra-class variance.
 */
public class OtsuBinarizer implements Binarizer {
    
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
        
        // Calculate optimal threshold using Otsu's method
        int threshold = calculateOtsuThreshold(gray);
        
        // Apply threshold
        BufferedImage binary = applyThreshold(gray, threshold);
        
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
        return "Otsu";
    }
    
    /**
     * Calculate the optimal threshold using Otsu's method.
     * This method finds the threshold that minimizes the weighted within-class variance.
     */
    private int calculateOtsuThreshold(BufferedImage image) {
        // Calculate histogram
        int[] histogram = new int[256];
        byte[] pixels = ImageUtils.getPixels(image);
        
        for (byte pixel : pixels) {
            histogram[pixel & 0xFF]++;
        }
        
        int total = pixels.length;
        
        // Calculate sum of all intensities
        double sum = 0;
        for (int i = 0; i < 256; i++) {
            sum += i * histogram[i];
        }
        
        double sumB = 0;
        int wB = 0;
        int wF = 0;
        
        double maxVariance = 0;
        int threshold = 0;
        
        for (int t = 0; t < 256; t++) {
            wB += histogram[t]; // Weight background
            if (wB == 0) continue;
            
            wF = total - wB; // Weight foreground
            if (wF == 0) break;
            
            sumB += t * histogram[t];
            
            double mB = sumB / wB; // Mean background
            double mF = (sum - sumB) / wF; // Mean foreground
            
            // Calculate between-class variance
            double variance = wB * wF * (mB - mF) * (mB - mF);
            
            if (variance > maxVariance) {
                maxVariance = variance;
                threshold = t;
            }
        }
        
        return threshold;
    }
    
    /**
     * Apply a threshold to create a binary image.
     */
    private BufferedImage applyThreshold(BufferedImage gray, int threshold) {
        int width = gray.getWidth();
        int height = gray.getHeight();
        BufferedImage binary = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        
        byte[] inputPixels = ImageUtils.getPixels(gray);
        byte[] outputPixels = new byte[width * height];
        
        for (int i = 0; i < inputPixels.length; i++) {
            int pixel = inputPixels[i] & 0xFF;
            outputPixels[i] = (byte) (pixel >= threshold ? 255 : 0);
        }
        
        ImageUtils.setPixels(binary, outputPixels);
        return ImageUtils.toBinaryImage(binary);
    }
    
    /**
     * Get the threshold that would be used for the given image.
     * Useful for debugging and CLI output.
     */
    public int getThreshold(BufferedImage image) {
        BufferedImage gray = ImageUtils.toGrayscale(image);
        return calculateOtsuThreshold(gray);
    }
}
