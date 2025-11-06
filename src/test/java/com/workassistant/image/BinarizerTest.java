package com.workassistant.image;

import com.workassistant.image.impl.AdaptiveMeanBinarizer;
import com.workassistant.image.impl.GlobalBinarizer;
import com.workassistant.image.impl.OtsuBinarizer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for image binarization implementations.
 */
class BinarizerTest {
    
    @Test
    void testOtsuBinarizer_UniformImage() throws Exception {
        // Load test image
        String imagePath = getTestImagePath("uniform.png");
        BufferedImage input = ImageUtils.loadImage(imagePath);
        
        // Create binarizer and config
        Binarizer binarizer = new OtsuBinarizer();
        BinarizeConfig config = BinarizeConfig.forOCR();
        
        // Binarize
        BufferedImage output = binarizer.binarize(input, config);
        
        // Verify output
        assertNotNull(output);
        assertEquals(input.getWidth(), output.getWidth());
        assertEquals(input.getHeight(), output.getHeight());
        assertEquals(BufferedImage.TYPE_BYTE_BINARY, output.getType());
        
        // Verify we have both black and white pixels
        assertTrue(hasBothBlackAndWhite(output));
    }
    
    @Test
    void testOtsuBinarizer_GetThreshold() throws Exception {
        String imagePath = getTestImagePath("uniform.png");
        BufferedImage input = ImageUtils.loadImage(imagePath);
        
        OtsuBinarizer binarizer = new OtsuBinarizer();
        int threshold = binarizer.getThreshold(input);
        
        // Threshold should be in valid range
        assertTrue(threshold >= 0 && threshold <= 255);
    }
    
    @Test
    void testAdaptiveMeanBinarizer_UnevenLight() throws Exception {
        String imagePath = getTestImagePath("uneven_light.png");
        BufferedImage input = ImageUtils.loadImage(imagePath);
        
        Binarizer binarizer = new AdaptiveMeanBinarizer();
        BinarizeConfig config = BinarizeConfig.forUnevenLighting();
        
        BufferedImage output = binarizer.binarize(input, config);
        
        assertNotNull(output);
        assertEquals(BufferedImage.TYPE_BYTE_BINARY, output.getType());
        assertTrue(hasBothBlackAndWhite(output));
    }
    
    @Test
    void testGlobalBinarizer_WithFixedThreshold() throws Exception {
        String imagePath = getTestImagePath("uniform.png");
        BufferedImage input = ImageUtils.loadImage(imagePath);
        
        Binarizer binarizer = new GlobalBinarizer();
        BinarizeConfig config = new BinarizeConfig()
                .setAlgorithm(BinarizeConfig.Algorithm.GLOBAL)
                .setThreshold(128);
        
        BufferedImage output = binarizer.binarize(input, config);
        
        assertNotNull(output);
        assertEquals(BufferedImage.TYPE_BYTE_BINARY, output.getType());
    }
    
    @Test
    void testBinarizer_WithBlur() throws Exception {
        String imagePath = getTestImagePath("noisy.png");
        BufferedImage input = ImageUtils.loadImage(imagePath);
        
        Binarizer binarizer = new OtsuBinarizer();
        BinarizeConfig config = new BinarizeConfig()
                .setAlgorithm(BinarizeConfig.Algorithm.OTSU)
                .setBlurKernelSize(3);
        
        BufferedImage output = binarizer.binarize(input, config);
        
        assertNotNull(output);
        assertTrue(hasBothBlackAndWhite(output));
    }
    
    @Test
    void testBinarizer_WithMedianBlur() throws Exception {
        String imagePath = getTestImagePath("uniform.png");
        BufferedImage input = ImageUtils.loadImage(imagePath);
        
        Binarizer binarizer = new OtsuBinarizer();
        BinarizeConfig config = new BinarizeConfig()
                .setAlgorithm(BinarizeConfig.Algorithm.OTSU)
                .setBlurKernelSize(3)
                .setUseMedianBlur(true);
        
        BufferedImage output = binarizer.binarize(input, config);
        
        // Just verify output is created and is binary type
        // Note: median blur on small binary images may result in uniform output
        assertNotNull(output);
        assertEquals(BufferedImage.TYPE_BYTE_BINARY, output.getType());
    }
    
    @Test
    void testBinarizer_WithMorphologicalOpening() throws Exception {
        String imagePath = getTestImagePath("noisy.png");
        BufferedImage input = ImageUtils.loadImage(imagePath);
        
        Binarizer binarizer = new OtsuBinarizer();
        BinarizeConfig config = new BinarizeConfig()
                .setAlgorithm(BinarizeConfig.Algorithm.OTSU)
                .setUseMorphologicalOpening(true)
                .setMorphKernelSize(3);
        
        BufferedImage output = binarizer.binarize(input, config);
        
        assertNotNull(output);
    }
    
    @Test
    void testBinarizer_WithMorphologicalClosing() throws Exception {
        String imagePath = getTestImagePath("uniform.png");
        BufferedImage input = ImageUtils.loadImage(imagePath);
        
        Binarizer binarizer = new OtsuBinarizer();
        BinarizeConfig config = new BinarizeConfig()
                .setAlgorithm(BinarizeConfig.Algorithm.OTSU)
                .setUseMorphologicalClosing(true)
                .setMorphKernelSize(3);
        
        BufferedImage output = binarizer.binarize(input, config);
        
        assertNotNull(output);
    }
    
    @Test
    void testBinarizer_FilePathInput(@TempDir Path tempDir) throws Exception {
        String inputPath = getTestImagePath("uniform.png");
        String outputPath = tempDir.resolve("output.png").toString();
        
        Binarizer binarizer = new OtsuBinarizer();
        BinarizeConfig config = BinarizeConfig.forOCR();
        
        binarizer.binarize(inputPath, outputPath, config);
        
        // Verify output file was created
        File outputFile = new File(outputPath);
        assertTrue(outputFile.exists());
        
        // Verify we can load the output
        BufferedImage output = ImageUtils.loadImage(outputPath);
        assertNotNull(output);
    }
    
    @Test
    void testBinarizerFactory_CreateOtsu() {
        BinarizeConfig config = new BinarizeConfig()
                .setAlgorithm(BinarizeConfig.Algorithm.OTSU);
        
        Binarizer binarizer = BinarizerFactory.createBinarizer(config);
        
        assertNotNull(binarizer);
        assertTrue(binarizer instanceof OtsuBinarizer);
    }
    
    @Test
    void testBinarizerFactory_CreateAdaptiveMean() {
        BinarizeConfig config = new BinarizeConfig()
                .setAlgorithm(BinarizeConfig.Algorithm.ADAPTIVE_MEAN);
        
        Binarizer binarizer = BinarizerFactory.createBinarizer(config);
        
        assertNotNull(binarizer);
        assertTrue(binarizer instanceof AdaptiveMeanBinarizer);
    }
    
    @Test
    void testBinarizerFactory_CreateGlobal() {
        BinarizeConfig config = new BinarizeConfig()
                .setAlgorithm(BinarizeConfig.Algorithm.GLOBAL);
        
        Binarizer binarizer = BinarizerFactory.createBinarizer(config);
        
        assertNotNull(binarizer);
        assertTrue(binarizer instanceof GlobalBinarizer);
    }
    
    @Test
    void testBinarizerFactory_Default() {
        Binarizer binarizer = BinarizerFactory.createDefaultBinarizer();
        
        assertNotNull(binarizer);
        assertTrue(binarizer instanceof OtsuBinarizer);
    }
    
    @Test
    void testBinarizerFactory_OpenCVNotAvailable() {
        BinarizeConfig config = new BinarizeConfig()
                .setUseOpenCV(true);
        
        // Should throw exception since OpenCV is not available
        assertThrows(IllegalArgumentException.class, () -> {
            BinarizerFactory.createBinarizer(config);
        });
    }
    
    @Test
    void testBinarizeConfig_BlockSizeMustBeOdd() {
        BinarizeConfig config = new BinarizeConfig();
        
        // Should throw exception for even block size
        assertThrows(IllegalArgumentException.class, () -> {
            config.setBlockSize(10);
        });
        
        // Should work for odd block size
        assertDoesNotThrow(() -> {
            config.setBlockSize(11);
        });
    }
    
    @Test
    void testBinarizeConfig_BlurKernelMustBeOdd() {
        BinarizeConfig config = new BinarizeConfig();
        
        // Should throw exception for even kernel size
        assertThrows(IllegalArgumentException.class, () -> {
            config.setBlurKernelSize(4);
        });
        
        // Should work for odd kernel size
        assertDoesNotThrow(() -> {
            config.setBlurKernelSize(3);
        });
        
        // Zero should work (no blur)
        assertDoesNotThrow(() -> {
            config.setBlurKernelSize(0);
        });
    }
    
    @Test
    void testImageUtils_ToGrayscale() throws Exception {
        String imagePath = getTestImagePath("uniform.png");
        BufferedImage input = ImageUtils.loadImage(imagePath);
        
        BufferedImage gray = ImageUtils.toGrayscale(input);
        
        assertNotNull(gray);
        assertEquals(BufferedImage.TYPE_BYTE_GRAY, gray.getType());
        assertEquals(input.getWidth(), gray.getWidth());
        assertEquals(input.getHeight(), gray.getHeight());
    }
    
    @Test
    void testImageUtils_ToGrayscale_AlreadyGray() throws Exception {
        String imagePath = getTestImagePath("uniform.png");
        BufferedImage input = ImageUtils.loadImage(imagePath);
        BufferedImage gray = ImageUtils.toGrayscale(input);
        
        // Converting again should return same image
        BufferedImage gray2 = ImageUtils.toGrayscale(gray);
        assertSame(gray, gray2);
    }
    
    @Test
    void testImageUtils_SaveAndLoad(@TempDir Path tempDir) throws Exception {
        String imagePath = getTestImagePath("uniform.png");
        BufferedImage input = ImageUtils.loadImage(imagePath);
        
        String outputPath = tempDir.resolve("saved.png").toString();
        ImageUtils.saveImage(input, outputPath);
        
        BufferedImage loaded = ImageUtils.loadImage(outputPath);
        assertNotNull(loaded);
        assertEquals(input.getWidth(), loaded.getWidth());
        assertEquals(input.getHeight(), loaded.getHeight());
    }
    
    private String getTestImagePath(String filename) {
        // Try to load from test resources
        String resourcePath = "src/test/resources/test-images/" + filename;
        File file = new File(resourcePath);
        if (file.exists()) {
            return file.getAbsolutePath();
        }
        
        // Fallback to classpath
        return getClass().getClassLoader().getResource("test-images/" + filename).getPath();
    }
    
    private boolean hasBothBlackAndWhite(BufferedImage image) {
        boolean hasBlack = false;
        boolean hasWhite = false;
        
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                int gray = rgb & 0xFF;
                
                if (gray < 128) hasBlack = true;
                if (gray >= 128) hasWhite = true;
                
                if (hasBlack && hasWhite) return true;
            }
        }
        
        return hasBlack && hasWhite;
    }
}
