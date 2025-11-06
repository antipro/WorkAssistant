package com.workassistant.service;

import com.workassistant.util.TextUtils;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
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

            // Set language to Simplified Chinese by default, and also accept English and Traditional Chinese
            tesseract.setLanguage("chi_sim+eng+chi_tra");
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

        // Skip OCR for vector formats like SVG
        String name = imageFile.getName().toLowerCase();
        if (name.endsWith(".svg") || name.endsWith(".svg+xml") || name.endsWith(".svgz")) {
            logger.info("Skipping OCR for SVG/vector image: {}", imageFile.getName());
            return "";
        }

        try {
            String text = tesseract.doOCR(imageFile);
            return text != null ? text.trim() : "";
        } catch (TesseractException e) {
            // Handle known ImageIO/JPEG parsing issue: attempt to sanitize and retry
            Throwable cause = e.getCause();
            boolean isJpegMetadataError = false;
            if (cause != null && cause instanceof javax.imageio.IIOException) {
                String msg = cause.getMessage();
                if (msg != null && msg.toLowerCase().contains("jfif app0")) {
                    isJpegMetadataError = true;
                }
            }

            logger.warn("Initial OCR failed for {}: {}. jpegMetadataError={}", imageFile.getName(), e.getMessage(), isJpegMetadataError);

            if (isJpegMetadataError) {
                try {
                    byte[] bytes = Files.readAllBytes(imageFile.toPath());

                    // First try to read as-is from bytes
                    BufferedImage img = null;
                    try (ByteArrayInputStream in = new ByteArrayInputStream(bytes)) {
                        img = ImageIO.read(in);
                    } catch (IOException ioe) {
                        logger.warn("ImageIO read failed on raw bytes for {}: {}", imageFile.getName(), ioe.getMessage());
                    }

                    // If that didn't work, try trimming to first SOS/SOI..EOI JPEG markers
                    if (img == null) {
                        int start = indexOf(bytes, new byte[]{(byte)0xFF, (byte)0xD8});
                        int end = lastIndexOf(bytes, new byte[]{(byte)0xFF, (byte)0xD9});
                        if (start >= 0 && end > start) {
                            byte[] trimmed = java.util.Arrays.copyOfRange(bytes, start, end + 2);
                            try (ByteArrayInputStream in2 = new ByteArrayInputStream(trimmed)) {
                                img = ImageIO.read(in2);
                            } catch (IOException ioe) {
                                logger.warn("ImageIO read failed on trimmed bytes for {}: {}", imageFile.getName(), ioe.getMessage());
                            }
                        }
                    }

                    if (img != null) {
                        // Re-encode sanitized image to PNG and OCR from file to avoid JPEG metadata quirks
                        File tmp = null;
                        try {
                            tmp = File.createTempFile("ocr-sanitized-", ".png");
                            ImageIO.write(img, "png", tmp);
                            String text2 = tesseract.doOCR(tmp);
                            return text2 != null ? text2.trim() : "";
                        } catch (Exception t2) {
                            logger.error("Retry OCR failed for sanitized image {}", imageFile.getName(), t2);
                        } finally {
                            if (tmp != null && tmp.exists()) {
                                try { tmp.delete(); } catch (Exception ignore) {}
                            }
                        }
                    } else {
                        logger.warn("Unable to decode image bytes for {} after sanitization attempts", imageFile.getName());
                    }
                } catch (Exception ex) {
                    logger.error("Error while attempting to sanitize image for OCR: {}", ex.getMessage());
                }
            }

            // Final fallback: log and return empty string
            logger.error("Failed to extract text from image: {}", imageFile.getName(), e);
            return "";
        }
    }

    // Helper: find first occurrence of a byte sequence
    private int indexOf(byte[] data, byte[] pattern) {
        outer:
        for (int i = 0; i < data.length - pattern.length + 1; i++) {
            for (int j = 0; j < pattern.length; j++) {
                if (data[i + j] != pattern[j]) continue outer;
            }
            return i;
        }
        return -1;
    }

    // Helper: find last occurrence of a byte sequence
    private int lastIndexOf(byte[] data, byte[] pattern) {
        outer:
        for (int i = data.length - pattern.length; i >= 0; i--) {
            for (int j = 0; j < pattern.length; j++) {
                if (data[i + j] != pattern[j]) continue outer;
            }
            return i;
        }
        return -1;
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

        // Split by punctuation and whitespace while preserving Unicode characters (including Chinese)
        String[] words = text
                .replaceAll(TextUtils.PUNCTUATION_PATTERN, " ")
                .trim()
                .split("\\s+");

        // Common English stop words to filter out
        List<String> stopWords = Arrays.asList(
            "the", "and", "for", "are", "but", "not", "you", "all", "can", "her", 
            "was", "one", "our", "out", "day", "get", "has", "him", "his", "how",
            "man", "new", "now", "old", "see", "two", "way", "who", "boy", "did",
            "its", "let", "put", "say", "she", "too", "use", "that", "this", "with"
        );

        return Arrays.stream(words)
                .filter(word -> !word.isEmpty())
                .filter(word -> {
                    // For Chinese/CJK characters: keep words with at least 2 characters
                    // For English/Latin: keep words longer than 3 chars
                    boolean hasCJK = TextUtils.containsCJKCharacters(word);
                    if (hasCJK) {
                        return word.length() >= 2;  // Chinese words can be meaningful with 2 chars
                    } else {
                        return word.length() > 3;  // English words need more than 3 chars
                    }
                })
                .filter(word -> !stopWords.contains(word.toLowerCase()))
                .distinct()
                .limit(20)  // Limit to 20 keywords
                .collect(Collectors.toList());
    }
}
