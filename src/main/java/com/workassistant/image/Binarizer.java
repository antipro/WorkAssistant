package com.workassistant.image;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Interface for image binarization operations.
 * Implementations convert grayscale or color images to black-and-white binary images.
 */
public interface Binarizer {
    
    /**
     * Binarize an image using the specified configuration.
     * 
     * @param input the input image (will be converted to grayscale if needed)
     * @param config the binarization configuration
     * @return the binarized image
     */
    BufferedImage binarize(BufferedImage input, BinarizeConfig config);
    
    /**
     * Binarize an image file and return the result.
     * 
     * @param inputPath path to the input image file
     * @param config the binarization configuration
     * @return the binarized image
     * @throws IOException if the file cannot be read
     */
    BufferedImage binarize(String inputPath, BinarizeConfig config) throws IOException;
    
    /**
     * Binarize an image file and save the result.
     * 
     * @param inputPath path to the input image file
     * @param outputPath path to save the binarized image
     * @param config the binarization configuration
     * @throws IOException if the file cannot be read or written
     */
    void binarize(String inputPath, String outputPath, BinarizeConfig config) throws IOException;
    
    /**
     * Get the name of this binarizer implementation.
     */
    String getName();
}
