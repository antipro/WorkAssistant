package com.workassistant.image;

/**
 * Configuration for image binarization operations.
 * Contains algorithm selection and all parameters needed for different binarization methods.
 */
public class BinarizeConfig {
    
    /**
     * Binarization algorithm types
     */
    public enum Algorithm {
        /** Global thresholding with manual threshold */
        GLOBAL,
        /** Otsu's automatic global thresholding */
        OTSU,
        /** Adaptive mean thresholding */
        ADAPTIVE_MEAN,
        /** Adaptive Gaussian thresholding */
        ADAPTIVE_GAUSSIAN,
        /** OpenCV-backed implementation (requires OpenCV dependency) */
        OPENCV
    }
    
    private Algorithm algorithm = Algorithm.OTSU;
    private int threshold = 128; // For GLOBAL algorithm
    private int blockSize = 11; // Window size for adaptive methods (must be odd)
    private double C = 2.0; // Constant subtracted from mean/weighted mean
    private int blurKernelSize = 0; // 0 means no blur, else must be odd
    private boolean useMedianBlur = false; // If true, use median blur; else Gaussian
    private boolean useMorphologicalOpening = false;
    private boolean useMorphologicalClosing = false;
    private int morphKernelSize = 3;
    private boolean useOpenCV = false;
    
    public BinarizeConfig() {
    }
    
    public Algorithm getAlgorithm() {
        return algorithm;
    }
    
    public BinarizeConfig setAlgorithm(Algorithm algorithm) {
        this.algorithm = algorithm;
        return this;
    }
    
    public int getThreshold() {
        return threshold;
    }
    
    public BinarizeConfig setThreshold(int threshold) {
        this.threshold = threshold;
        return this;
    }
    
    public int getBlockSize() {
        return blockSize;
    }
    
    public BinarizeConfig setBlockSize(int blockSize) {
        if (blockSize % 2 == 0) {
            throw new IllegalArgumentException("Block size must be odd");
        }
        this.blockSize = blockSize;
        return this;
    }
    
    public double getC() {
        return C;
    }
    
    public BinarizeConfig setC(double c) {
        this.C = c;
        return this;
    }
    
    public int getBlurKernelSize() {
        return blurKernelSize;
    }
    
    public BinarizeConfig setBlurKernelSize(int blurKernelSize) {
        if (blurKernelSize > 0 && blurKernelSize % 2 == 0) {
            throw new IllegalArgumentException("Blur kernel size must be odd");
        }
        this.blurKernelSize = blurKernelSize;
        return this;
    }
    
    public boolean isUseMedianBlur() {
        return useMedianBlur;
    }
    
    public BinarizeConfig setUseMedianBlur(boolean useMedianBlur) {
        this.useMedianBlur = useMedianBlur;
        return this;
    }
    
    public boolean isUseMorphologicalOpening() {
        return useMorphologicalOpening;
    }
    
    public BinarizeConfig setUseMorphologicalOpening(boolean useMorphologicalOpening) {
        this.useMorphologicalOpening = useMorphologicalOpening;
        return this;
    }
    
    public boolean isUseMorphologicalClosing() {
        return useMorphologicalClosing;
    }
    
    public BinarizeConfig setUseMorphologicalClosing(boolean useMorphologicalClosing) {
        this.useMorphologicalClosing = useMorphologicalClosing;
        return this;
    }
    
    public int getMorphKernelSize() {
        return morphKernelSize;
    }
    
    public BinarizeConfig setMorphKernelSize(int morphKernelSize) {
        this.morphKernelSize = morphKernelSize;
        return this;
    }
    
    public boolean isUseOpenCV() {
        return useOpenCV;
    }
    
    public BinarizeConfig setUseOpenCV(boolean useOpenCV) {
        this.useOpenCV = useOpenCV;
        return this;
    }
    
    /**
     * Create a default configuration for OCR preprocessing.
     * Uses Otsu's method with light Gaussian blur.
     */
    public static BinarizeConfig forOCR() {
        return new BinarizeConfig()
                .setAlgorithm(Algorithm.OTSU)
                .setBlurKernelSize(3)
                .setUseMedianBlur(false);
    }
    
    /**
     * Create a configuration for images with uneven lighting.
     * Uses adaptive mean thresholding.
     */
    public static BinarizeConfig forUnevenLighting() {
        return new BinarizeConfig()
                .setAlgorithm(Algorithm.ADAPTIVE_MEAN)
                .setBlockSize(11)
                .setC(2.0)
                .setBlurKernelSize(3);
    }
}
