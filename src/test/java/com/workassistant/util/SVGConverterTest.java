package com.workassistant.util;

import org.apache.batik.transcoder.TranscoderException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SVGConverter utility class
 */
public class SVGConverterTest {
    
    // Simple valid SVG content for testing
    private static final String VALID_SVG = 
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
        "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"100\" height=\"100\">" +
        "  <rect x=\"10\" y=\"10\" width=\"80\" height=\"80\" fill=\"blue\"/>" +
        "</svg>";
    
    private static final String SVG_WITHOUT_DIMENSIONS = 
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
        "<svg xmlns=\"http://www.w3.org/2000/svg\">" +
        "  <circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"red\"/>" +
        "</svg>";
    
    private static final String SVG_WITH_VIEWBOX = 
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
        "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 200 200\">" +
        "  <circle cx=\"100\" cy=\"100\" r=\"80\" fill=\"green\"/>" +
        "</svg>";
    
    @Test
    public void testIsSVGMimeType_WithImageSvgXml() {
        assertTrue(SVGConverter.isSVGMimeType("image/svg+xml"));
        assertTrue(SVGConverter.isSVGMimeType("IMAGE/SVG+XML"));
        assertTrue(SVGConverter.isSVGMimeType("image/svg+xml;charset=utf-8"));
    }
    
    @Test
    public void testIsSVGMimeType_WithTextSvg() {
        assertTrue(SVGConverter.isSVGMimeType("text/svg"));
        assertTrue(SVGConverter.isSVGMimeType("TEXT/SVG"));
        assertTrue(SVGConverter.isSVGMimeType("text/svg;charset=utf-8"));
    }
    
    @Test
    public void testIsSVGMimeType_WithNonSVGTypes() {
        assertFalse(SVGConverter.isSVGMimeType("image/png"));
        assertFalse(SVGConverter.isSVGMimeType("image/jpeg"));
        assertFalse(SVGConverter.isSVGMimeType("text/plain"));
        assertFalse(SVGConverter.isSVGMimeType("text/html"));
        assertFalse(SVGConverter.isSVGMimeType("application/json"));
    }
    
    @Test
    public void testIsSVGMimeType_WithNullOrEmpty() {
        assertFalse(SVGConverter.isSVGMimeType(null));
        assertFalse(SVGConverter.isSVGMimeType(""));
        assertFalse(SVGConverter.isSVGMimeType("   "));
    }
    
    @Test
    public void testIsSVGContent_WithSVGMimeType() {
        assertTrue(SVGConverter.isSVGContent(VALID_SVG, "image/svg+xml"));
        assertTrue(SVGConverter.isSVGContent(VALID_SVG, "text/svg"));
    }
    
    @Test
    public void testIsSVGContent_WithXMLMimeTypeAndSVGContent() {
        assertTrue(SVGConverter.isSVGContent(VALID_SVG, "text/xml"));
        assertTrue(SVGConverter.isSVGContent(VALID_SVG, "application/xml"));
    }
    
    @Test
    public void testIsSVGContent_WithXMLMimeTypeButNoSVGContent() {
        String xmlContent = "<?xml version=\"1.0\"?><root><item>test</item></root>";
        assertFalse(SVGConverter.isSVGContent(xmlContent, "text/xml"));
        assertFalse(SVGConverter.isSVGContent(xmlContent, "application/xml"));
    }
    
    @Test
    public void testIsSVGContent_WithSVGTagVariations() {
        String svgLowercase = "<svg xmlns=\"http://www.w3.org/2000/svg\"></svg>";
        String svgUppercase = "<SVG xmlns=\"http://www.w3.org/2000/svg\"></SVG>";
        String svgMixedCase = "<Svg xmlns=\"http://www.w3.org/2000/svg\"></Svg>";
        
        assertTrue(SVGConverter.isSVGContent(svgLowercase, "text/xml"));
        assertTrue(SVGConverter.isSVGContent(svgUppercase, "text/xml"));
        assertTrue(SVGConverter.isSVGContent(svgMixedCase, "text/xml"));
    }
    
    @Test
    public void testIsSVGContent_WithNullOrEmpty() {
        assertFalse(SVGConverter.isSVGContent(null, "text/xml"));
        assertFalse(SVGConverter.isSVGContent("", "text/xml"));
        assertFalse(SVGConverter.isSVGContent("   ", "text/xml"));
    }
    
    @Test
    public void testConvertSVGToPNG_WithValidSVG() throws IOException, TranscoderException {
        String result = SVGConverter.convertSVGToPNG(VALID_SVG);
        
        assertNotNull(result);
        assertTrue(result.startsWith("data:image/png;base64,"));
        
        // Extract base64 part and verify it's not empty
        String base64Part = result.substring("data:image/png;base64,".length());
        assertFalse(base64Part.isEmpty());
        assertTrue(base64Part.length() > 100); // PNG data should be substantial
    }
    
    @Test
    public void testConvertSVGToPNG_WithCustomDimensions() throws IOException, TranscoderException {
        String result = SVGConverter.convertSVGToPNG(VALID_SVG, 200, 200);
        
        assertNotNull(result);
        assertTrue(result.startsWith("data:image/png;base64,"));
    }
    
    @Test
    public void testConvertSVGToPNG_WithSVGWithoutDimensions() throws IOException, TranscoderException {
        String result = SVGConverter.convertSVGToPNG(SVG_WITHOUT_DIMENSIONS);
        
        assertNotNull(result);
        assertTrue(result.startsWith("data:image/png;base64,"));
    }
    
    @Test
    public void testConvertSVGToPNG_WithSVGWithViewBox() throws IOException, TranscoderException {
        String result = SVGConverter.convertSVGToPNG(SVG_WITH_VIEWBOX);
        
        assertNotNull(result);
        assertTrue(result.startsWith("data:image/png;base64,"));
    }
    
    @Test
    public void testConvertSVGToPNG_WithNullContent() {
        assertThrows(IllegalArgumentException.class, () -> {
            SVGConverter.convertSVGToPNG((String) null);
        });
    }
    
    @Test
    public void testConvertSVGToPNG_WithEmptyContent() {
        assertThrows(IllegalArgumentException.class, () -> {
            SVGConverter.convertSVGToPNG("");
        });
    }
    
    @Test
    public void testConvertSVGToPNG_WithInvalidDimensions() {
        assertThrows(IllegalArgumentException.class, () -> {
            SVGConverter.convertSVGToPNG(VALID_SVG, 0, 100);
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            SVGConverter.convertSVGToPNG(VALID_SVG, 100, -1);
        });
    }
    
    @Test
    public void testConvertSVGToPNG_WithInvalidSVG() {
        String invalidSVG = "This is not valid SVG content";
        
        assertThrows(TranscoderException.class, () -> {
            SVGConverter.convertSVGToPNG(invalidSVG);
        });
    }
    
    @Test
    public void testConvertSVGToPNG_FromByteArray() throws IOException, TranscoderException {
        byte[] svgBytes = VALID_SVG.getBytes(StandardCharsets.UTF_8);
        String result = SVGConverter.convertSVGToPNG(svgBytes);
        
        assertNotNull(result);
        assertTrue(result.startsWith("data:image/png;base64,"));
    }
    
    @Test
    public void testConvertSVGToPNG_FromByteArray_WithNull() {
        assertThrows(IllegalArgumentException.class, () -> {
            SVGConverter.convertSVGToPNG((byte[]) null);
        });
    }
    
    @Test
    public void testConvertSVGToPNG_FromByteArray_WithEmpty() {
        assertThrows(IllegalArgumentException.class, () -> {
            SVGConverter.convertSVGToPNG(new byte[0]);
        });
    }
    
    @Test
    public void testConvertSVGToPNG_FromInputStream() throws IOException, TranscoderException {
        InputStream inputStream = new ByteArrayInputStream(VALID_SVG.getBytes(StandardCharsets.UTF_8));
        String result = SVGConverter.convertSVGToPNG(inputStream);
        
        assertNotNull(result);
        assertTrue(result.startsWith("data:image/png;base64,"));
    }
    
    @Test
    public void testConvertSVGToPNG_FromInputStream_WithNull() {
        assertThrows(IllegalArgumentException.class, () -> {
            SVGConverter.convertSVGToPNG((InputStream) null);
        });
    }
    
    @Test
    public void testExtractSVGDimensions_WithValidDimensions() {
        int[] dimensions = SVGConverter.extractSVGDimensions(VALID_SVG);
        
        assertNotNull(dimensions);
        assertEquals(2, dimensions.length);
        assertEquals(100, dimensions[0]);
        assertEquals(100, dimensions[1]);
    }
    
    @Test
    public void testExtractSVGDimensions_WithoutDimensions() {
        int[] dimensions = SVGConverter.extractSVGDimensions(SVG_WITHOUT_DIMENSIONS);
        
        assertNotNull(dimensions);
        assertEquals(2, dimensions.length);
        assertEquals(800, dimensions[0]); // Default width
        assertEquals(600, dimensions[1]); // Default height
    }
    
    @Test
    public void testExtractSVGDimensions_WithViewBox() {
        int[] dimensions = SVGConverter.extractSVGDimensions(SVG_WITH_VIEWBOX);
        
        assertNotNull(dimensions);
        assertEquals(2, dimensions.length);
        // Should return defaults since viewBox is not extracted as width/height
        assertEquals(800, dimensions[0]);
        assertEquals(600, dimensions[1]);
    }
    
    @Test
    public void testExtractSVGDimensions_WithUnitsInDimensions() {
        String svgWithUnits = 
            "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"150px\" height=\"200pt\">" +
            "</svg>";
        
        int[] dimensions = SVGConverter.extractSVGDimensions(svgWithUnits);
        
        assertNotNull(dimensions);
        assertEquals(2, dimensions.length);
        assertEquals(150, dimensions[0]);
        assertEquals(200, dimensions[1]);
    }
    
    @Test
    public void testExtractSVGDimensions_WithNullOrEmpty() {
        int[] dimensions1 = SVGConverter.extractSVGDimensions(null);
        assertNotNull(dimensions1);
        assertEquals(800, dimensions1[0]);
        assertEquals(600, dimensions1[1]);
        
        int[] dimensions2 = SVGConverter.extractSVGDimensions("");
        assertNotNull(dimensions2);
        assertEquals(800, dimensions2[0]);
        assertEquals(600, dimensions2[1]);
    }
    
    @Test
    public void testConstructor_CannotBeInstantiated() throws Exception {
        // Use reflection to test private constructor
        java.lang.reflect.Constructor<SVGConverter> constructor = 
            SVGConverter.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        
        try {
            constructor.newInstance();
            fail("Expected UnsupportedOperationException to be thrown");
        } catch (java.lang.reflect.InvocationTargetException e) {
            // The InvocationTargetException wraps the actual exception
            assertTrue(e.getCause() instanceof UnsupportedOperationException);
            assertEquals("Utility class cannot be instantiated", e.getCause().getMessage());
        }
    }
}
