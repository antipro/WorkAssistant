package com.workassistant.util;

import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Utility class for converting SVG content to PNG format.
 * Supports both headless/server environments and desktop environments.
 * Uses Apache Batik transcoder for SVG rendering.
 */
public class SVGConverter {
    private static final Logger logger = LoggerFactory.getLogger(SVGConverter.class);
    
    private static final String SVG_MIME_TYPE = "image/svg+xml";
    private static final String TEXT_XML_MIME_TYPE = "text/xml";
    private static final String TEXT_SVG_MIME_TYPE = "text/svg";
    private static final String APPLICATION_XML_MIME_TYPE = "application/xml";
    
    private static final int DEFAULT_WIDTH = 800;
    private static final int DEFAULT_HEIGHT = 600;
    
    /**
     * Private constructor to prevent instantiation of utility class
     */
    private SVGConverter() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
    
    /**
     * Checks if the given MIME type represents SVG content
     * 
     * @param mimeType The MIME type to check
     * @return true if the MIME type is SVG-related, false otherwise
     */
    public static boolean isSVGMimeType(String mimeType) {
        if (mimeType == null || mimeType.isEmpty()) {
            return false;
        }
        
        String normalizedType = mimeType.toLowerCase().trim();
        return normalizedType.equals(SVG_MIME_TYPE) ||
               normalizedType.equals(TEXT_SVG_MIME_TYPE) ||
               normalizedType.startsWith(SVG_MIME_TYPE + ";") ||
               normalizedType.startsWith(TEXT_SVG_MIME_TYPE + ";");
    }
    
    /**
     * Checks if the given content contains SVG data by inspecting the content itself
     * 
     * @param content The content to check
     * @param mimeType The MIME type of the content (can be text/xml or application/xml)
     * @return true if the content appears to contain SVG data, false otherwise
     */
    public static boolean isSVGContent(String content, String mimeType) {
        if (content == null || content.isEmpty()) {
            return false;
        }
        
        // Check MIME type first
        if (isSVGMimeType(mimeType)) {
            return true;
        }
        
        // For text/xml or application/xml, check if content contains SVG markers
        if (mimeType != null && (mimeType.toLowerCase().contains("xml"))) {
            String normalized = content.trim().toLowerCase();
            return normalized.contains("<svg") || normalized.contains("xmlns=\"http://www.w3.org/2000/svg\"");
        }
        
        return false;
    }
    
    /**
     * Converts SVG content to PNG format
     * 
     * @param svgContent The SVG content as a string
     * @return Base64-encoded PNG image data with data URI prefix (data:image/png;base64,...)
     * @throws IOException if an I/O error occurs during conversion
     * @throws TranscoderException if SVG transcoding fails
     */
    public static String convertSVGToPNG(String svgContent) throws IOException, TranscoderException {
        return convertSVGToPNG(svgContent, DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }
    
    /**
     * Converts SVG content to PNG format with specified dimensions
     * 
     * @param svgContent The SVG content as a string
     * @param width The desired width of the PNG image
     * @param height The desired height of the PNG image
     * @return Base64-encoded PNG image data with data URI prefix (data:image/png;base64,...)
     * @throws IOException if an I/O error occurs during conversion
     * @throws TranscoderException if SVG transcoding fails
     */
    public static String convertSVGToPNG(String svgContent, int width, int height) throws IOException, TranscoderException {
        if (svgContent == null || svgContent.trim().isEmpty()) {
            throw new IllegalArgumentException("SVG content cannot be null or empty");
        }
        
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Width and height must be positive values");
        }
        
        logger.debug("Converting SVG to PNG with dimensions: {}x{}", width, height);
        
        try {
            // Create PNG transcoder
            PNGTranscoder transcoder = new PNGTranscoder();
            
            // Set transcoding hints for dimensions
            transcoder.addTranscodingHint(PNGTranscoder.KEY_WIDTH, (float) width);
            transcoder.addTranscodingHint(PNGTranscoder.KEY_HEIGHT, (float) height);
            
            // Create input from SVG string
            TranscoderInput input = new TranscoderInput(new StringReader(svgContent));
            
            // Create output stream for PNG
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            TranscoderOutput output = new TranscoderOutput(outputStream);
            
            // Perform the transcoding
            transcoder.transcode(input, output);
            
            // Convert to base64 data URI
            byte[] pngData = outputStream.toByteArray();
            String base64Data = Base64.getEncoder().encodeToString(pngData);
            String dataUri = "data:image/png;base64," + base64Data;
            
            logger.debug("Successfully converted SVG to PNG ({} bytes)", pngData.length);
            
            return dataUri;
            
        } catch (TranscoderException e) {
            logger.error("Failed to transcode SVG to PNG", e);
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error during SVG to PNG conversion", e);
            throw new IOException("Failed to convert SVG to PNG", e);
        }
    }
    
    /**
     * Converts SVG content from a byte array to PNG format
     * 
     * @param svgBytes The SVG content as a byte array
     * @return Base64-encoded PNG image data with data URI prefix (data:image/png;base64,...)
     * @throws IOException if an I/O error occurs during conversion
     * @throws TranscoderException if SVG transcoding fails
     */
    public static String convertSVGToPNG(byte[] svgBytes) throws IOException, TranscoderException {
        if (svgBytes == null || svgBytes.length == 0) {
            throw new IllegalArgumentException("SVG bytes cannot be null or empty");
        }
        
        String svgContent = new String(svgBytes, StandardCharsets.UTF_8);
        return convertSVGToPNG(svgContent);
    }
    
    /**
     * Converts SVG content from an input stream to PNG format
     * 
     * @param svgInputStream The SVG content as an input stream
     * @return Base64-encoded PNG image data with data URI prefix (data:image/png;base64,...)
     * @throws IOException if an I/O error occurs during conversion
     * @throws TranscoderException if SVG transcoding fails
     */
    public static String convertSVGToPNG(InputStream svgInputStream) throws IOException, TranscoderException {
        if (svgInputStream == null) {
            throw new IllegalArgumentException("SVG input stream cannot be null");
        }
        
        byte[] svgBytes = svgInputStream.readAllBytes();
        return convertSVGToPNG(svgBytes);
    }
    
    /**
     * Extracts dimensions from SVG content if specified in the root element
     * Returns an array with [width, height], or [DEFAULT_WIDTH, DEFAULT_HEIGHT] if not found
     * 
     * @param svgContent The SVG content as a string
     * @return Array containing [width, height]
     */
    public static int[] extractSVGDimensions(String svgContent) {
        int[] defaultDimensions = {DEFAULT_WIDTH, DEFAULT_HEIGHT};
        
        if (svgContent == null || svgContent.isEmpty()) {
            return defaultDimensions;
        }
        
        try {
            // Look for width and height attributes in the SVG tag
            String normalized = svgContent.toLowerCase();
            int svgStart = normalized.indexOf("<svg");
            if (svgStart == -1) {
                return defaultDimensions;
            }
            
            int svgEnd = normalized.indexOf(">", svgStart);
            if (svgEnd == -1) {
                return defaultDimensions;
            }
            
            String svgTag = svgContent.substring(svgStart, svgEnd);
            
            // Extract width
            int width = extractDimensionValue(svgTag, "width");
            int height = extractDimensionValue(svgTag, "height");
            
            if (width > 0 && height > 0) {
                return new int[]{width, height};
            }
            
        } catch (Exception e) {
            logger.warn("Failed to extract SVG dimensions, using defaults", e);
        }
        
        return defaultDimensions;
    }
    
    /**
     * Helper method to extract a numeric dimension value from an attribute
     */
    private static int extractDimensionValue(String svgTag, String attribute) {
        try {
            String pattern = attribute + "=\"";
            int start = svgTag.indexOf(pattern);
            if (start == -1) {
                return -1;
            }
            start += pattern.length();
            
            int end = svgTag.indexOf("\"", start);
            if (end == -1) {
                return -1;
            }
            
            String value = svgTag.substring(start, end);
            // Remove units like px, pt, etc.
            value = value.replaceAll("[^0-9.]", "");
            
            if (value.isEmpty()) {
                return -1;
            }
            
            return (int) Double.parseDouble(value);
            
        } catch (Exception e) {
            return -1;
        }
    }
}
