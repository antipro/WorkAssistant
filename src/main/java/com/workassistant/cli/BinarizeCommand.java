package com.workassistant.cli;

import com.workassistant.image.BinarizeConfig;
import com.workassistant.image.Binarizer;
import com.workassistant.image.BinarizerFactory;
import com.workassistant.image.ImageUtils;
import com.workassistant.image.impl.OtsuBinarizer;

import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Command-line utility for image binarization.
 * 
 * Usage examples:
 *   java com.workassistant.cli.BinarizeCommand input.jpg output.png
 *   java com.workassistant.cli.BinarizeCommand input.jpg output.png --algorithm OTSU --blur 3
 *   java com.workassistant.cli.BinarizeCommand input.jpg output.png --algorithm ADAPTIVE_MEAN --block-size 11 --c 2
 *   java com.workassistant.cli.BinarizeCommand input.jpg output.png --algorithm GLOBAL --threshold 128
 */
public class BinarizeCommand {
    
    public static void main(String[] args) {
        if (args.length < 2) {
            printUsage();
            System.exit(1);
        }
        
        String inputPath = args[0];
        String outputPath = args[1];
        
        // Parse configuration from command line arguments
        BinarizeConfig config = parseConfig(args);
        
        try {
            System.out.println("Binarizing image...");
            System.out.println("  Input: " + inputPath);
            System.out.println("  Output: " + outputPath);
            System.out.println("  Algorithm: " + config.getAlgorithm());
            
            // Create binarizer
            Binarizer binarizer = BinarizerFactory.createBinarizer(config);
            
            // Load input image
            BufferedImage input = ImageUtils.loadImage(inputPath);
            System.out.println("  Input size: " + input.getWidth() + "x" + input.getHeight());
            
            // Show threshold if using Otsu
            if (config.getAlgorithm() == BinarizeConfig.Algorithm.OTSU && binarizer instanceof OtsuBinarizer) {
                BufferedImage gray = ImageUtils.toGrayscale(input);
                int threshold = ((OtsuBinarizer) binarizer).getThreshold(gray);
                System.out.println("  Calculated Otsu threshold: " + threshold);
            }
            
            // Binarize
            long startTime = System.currentTimeMillis();
            BufferedImage output = binarizer.binarize(input, config);
            long elapsed = System.currentTimeMillis() - startTime;
            
            // Save output
            ImageUtils.saveImage(output, outputPath);
            
            System.out.println("  Output size: " + output.getWidth() + "x" + output.getHeight());
            System.out.println("  Processing time: " + elapsed + "ms");
            System.out.println("Done!");
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    private static BinarizeConfig parseConfig(String[] args) {
        BinarizeConfig config = new BinarizeConfig();
        
        for (int i = 2; i < args.length; i++) {
            String arg = args[i];
            
            if (arg.equals("--algorithm") && i + 1 < args.length) {
                config.setAlgorithm(BinarizeConfig.Algorithm.valueOf(args[++i]));
            } else if (arg.equals("--threshold") && i + 1 < args.length) {
                config.setThreshold(Integer.parseInt(args[++i]));
            } else if (arg.equals("--block-size") && i + 1 < args.length) {
                config.setBlockSize(Integer.parseInt(args[++i]));
            } else if (arg.equals("--c") && i + 1 < args.length) {
                config.setC(Double.parseDouble(args[++i]));
            } else if (arg.equals("--blur") && i + 1 < args.length) {
                config.setBlurKernelSize(Integer.parseInt(args[++i]));
            } else if (arg.equals("--median-blur")) {
                config.setUseMedianBlur(true);
            } else if (arg.equals("--morph-open")) {
                config.setUseMorphologicalOpening(true);
            } else if (arg.equals("--morph-close")) {
                config.setUseMorphologicalClosing(true);
            } else if (arg.equals("--morph-kernel") && i + 1 < args.length) {
                config.setMorphKernelSize(Integer.parseInt(args[++i]));
            } else if (arg.equals("--opencv")) {
                config.setUseOpenCV(true);
            } else if (arg.equals("--help")) {
                printUsage();
                System.exit(0);
            }
        }
        
        return config;
    }
    
    private static void printUsage() {
        System.out.println("Image Binarization CLI");
        System.out.println();
        System.out.println("Usage: java com.workassistant.cli.BinarizeCommand <input> <output> [options]");
        System.out.println();
        System.out.println("Arguments:");
        System.out.println("  <input>              Path to input image file");
        System.out.println("  <output>             Path to output image file");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --algorithm ALGO     Binarization algorithm:");
        System.out.println("                       GLOBAL, OTSU (default), ADAPTIVE_MEAN, ADAPTIVE_GAUSSIAN");
        System.out.println("  --threshold N        Threshold value for GLOBAL algorithm (0-255, default: 128)");
        System.out.println("  --block-size N       Window size for adaptive methods (odd number, default: 11)");
        System.out.println("  --c VALUE            Constant subtracted from mean (default: 2.0)");
        System.out.println("  --blur N             Apply blur with kernel size N (odd number, 0 = no blur)");
        System.out.println("  --median-blur        Use median blur instead of Gaussian");
        System.out.println("  --morph-open         Apply morphological opening (remove noise)");
        System.out.println("  --morph-close        Apply morphological closing (fill holes)");
        System.out.println("  --morph-kernel N     Kernel size for morphological operations (default: 3)");
        System.out.println("  --opencv             Use OpenCV implementation (requires OpenCV dependency)");
        System.out.println("  --help               Show this help message");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  # Use Otsu's method with light blur (recommended for OCR)");
        System.out.println("  java com.workassistant.cli.BinarizeCommand input.jpg output.png --blur 3");
        System.out.println();
        System.out.println("  # Use adaptive thresholding for uneven lighting");
        System.out.println("  java com.workassistant.cli.BinarizeCommand input.jpg output.png --algorithm ADAPTIVE_MEAN --block-size 11 --c 2");
        System.out.println();
        System.out.println("  # Use global thresholding with fixed threshold");
        System.out.println("  java com.workassistant.cli.BinarizeCommand input.jpg output.png --algorithm GLOBAL --threshold 128");
    }
}
