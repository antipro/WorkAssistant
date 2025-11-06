package com.workassistant.service;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for OCR (Optical Character Recognition) using Tesseract
 */
public class OCRService {
    private static final Logger logger = LoggerFactory.getLogger(OCRService.class);
    private static OCRService instance;
    private final Tesseract tesseract;
    private boolean available;

    private OCRService() {
        this.tesseract = new Tesseract();
        this.available = initializeTesseract();
    }

    public static synchronized OCRService getInstance() {
        if (instance == null) {
            instance = new OCRService();
        }
        return instance;
    }

    private boolean initializeTesseract() {
        try {
            // Try to set Tesseract data path - common locations
            String[] possiblePaths = {
                "/usr/share/tesseract-ocr/4.00/tessdata",
                "/usr/share/tesseract-ocr/5.00/tessdata",
                "/usr/local/share/tessdata",
                "/opt/homebrew/share/tessdata",
                System.getenv("TESSDATA_PREFIX"),
                "tessdata"
            };

            for (String path : possiblePaths) {
                if (path != null && new File(path).exists()) {
                    tesseract.setDatapath(path);
                    logger.info("Tesseract data path set to: {}", path);
                    break;
                }
            }

            // Set language to English by default
            tesseract.setLanguage("eng");
            logger.info("OCR service initialized successfully");
            return true;
        } catch (Exception e) {
            logger.warn("Failed to initialize Tesseract OCR: {}. OCR features will be disabled.", e.getMessage());
            return false;
        }
    }

    public boolean isAvailable() {
        return available;
    }

    /**
     * Extract text from an image file
     */
    public String extractText(File imageFile) {
        if (!available) {
            logger.warn("OCR service is not available");
            return "";
        }

        try {
            String text = tesseract.doOCR(imageFile);
            return text != null ? text.trim() : "";
        } catch (TesseractException e) {
            logger.error("Failed to extract text from image: {}", imageFile.getName(), e);
            return "";
        }
    }

    /**
     * Extract text from a BufferedImage
     */
    public String extractText(BufferedImage image) {
        if (!available) {
            logger.warn("OCR service is not available");
            return "";
        }

        try {
            String text = tesseract.doOCR(image);
            return text != null ? text.trim() : "";
        } catch (TesseractException e) {
            logger.error("Failed to extract text from BufferedImage", e);
            return "";
        }
    }

    /**
     * Extract keywords from an image file
     * Returns a list of significant words (longer than 3 characters)
     */
    public List<String> extractKeywords(File imageFile) {
        String text = extractText(imageFile);
        return extractKeywordsFromText(text);
    }

    /**
     * Extract keywords from a BufferedImage
     */
    public List<String> extractKeywords(BufferedImage image) {
        String text = extractText(image);
        return extractKeywordsFromText(text);
    }

    /**
     * Extract keywords from text
     * Filters out common words and short words
     */
    private List<String> extractKeywordsFromText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new ArrayList<>();
        }

        // Split by whitespace and punctuation
        String[] words = text.toLowerCase()
                .replaceAll("[^a-zA-Z0-9\\s]", " ")
                .split("\\s+");

        // Common English stop words to filter out
        List<String> stopWords = Arrays.asList(
            "the", "and", "for", "are", "but", "not", "you", "all", "can", "her", 
            "was", "one", "our", "out", "day", "get", "has", "him", "his", "how",
            "man", "new", "now", "old", "see", "two", "way", "who", "boy", "did",
            "its", "let", "put", "say", "she", "too", "use", "that", "this", "with"
        );

        return Arrays.stream(words)
                .filter(word -> word.length() > 3)  // Only words longer than 3 chars
                .filter(word -> !stopWords.contains(word))
                .distinct()
                .limit(20)  // Limit to 20 keywords
                .collect(Collectors.toList());
    }
}
