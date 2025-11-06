package com.workassistant.image.impl;

import com.workassistant.image.BinarizeConfig;
import com.workassistant.image.Binarizer;
import com.workassistant.image.ImageUtils;

import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * Simple global thresholding binarizer.
 * Uses a fixed threshold value to convert grayscale to binary.
 */
public class GlobalBinarizer implements Binarizer {
    
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
        
        // Apply global threshold
        BufferedImage binary = applyThreshold(gray, config.getThreshold());
        
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
        return "Global";
    }
    
    /**
     * Apply a fixed threshold to create a binary image.
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
}
