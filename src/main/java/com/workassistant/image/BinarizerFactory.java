package com.workassistant.image;

import com.workassistant.image.impl.AdaptiveMeanBinarizer;
import com.workassistant.image.impl.GlobalBinarizer;
import com.workassistant.image.impl.OtsuBinarizer;

/**
 * Factory for creating binarizer instances based on configuration.
 */
public class BinarizerFactory {
    
    /**
     * Create a binarizer based on the configuration algorithm.
     * 
     * @param config the binarization configuration
     * @return appropriate binarizer instance
     * @throws IllegalArgumentException if algorithm is not supported
     */
    public static Binarizer createBinarizer(BinarizeConfig config) {
        if (config.isUseOpenCV()) {
            try {
                // Try to load OpenCV binarizer if requested
                Class<?> clazz = Class.forName("com.workassistant.image.impl.OpenCVBinarizer");
                return (Binarizer) clazz.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new IllegalArgumentException(
                    "OpenCV binarizer requested but not available. " +
                    "Please add OpenCV dependency to your project.", e);
            }
        }
        
        switch (config.getAlgorithm()) {
            case GLOBAL:
                return new GlobalBinarizer();
            case OTSU:
                return new OtsuBinarizer();
            case ADAPTIVE_MEAN:
            case ADAPTIVE_GAUSSIAN:
                // Use adaptive mean for both (Gaussian would require different kernel)
                return new AdaptiveMeanBinarizer();
            case OPENCV:
                throw new IllegalArgumentException(
                    "OpenCV algorithm selected but useOpenCV flag is false. " +
                    "Set config.setUseOpenCV(true) to use OpenCV.");
            default:
                throw new IllegalArgumentException("Unsupported algorithm: " + config.getAlgorithm());
        }
    }
    
    /**
     * Create a binarizer with default configuration.
     */
    public static Binarizer createDefaultBinarizer() {
        return new OtsuBinarizer();
    }
}
