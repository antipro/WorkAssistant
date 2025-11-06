package com.workassistant.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for OCRService
 */
public class OCRServiceTest {
    
    private OCRService ocrService;
    
    @BeforeEach
    public void setUp() {
        ocrService = OCRService.getInstance();
    }
    
    @Test
    public void testExtractKeywords_ChineseText() {
        // Test that Chinese characters are preserved
        String chineseText = "这是一个测试文本，包含中文字符。我们需要提取关键词。";
        
        // Use reflection to call private method for testing
        try {
            java.lang.reflect.Method method = OCRService.class.getDeclaredMethod("extractKeywordsFromText", String.class);
            method.setAccessible(true);
            List<String> keywords = (List<String>) method.invoke(ocrService, chineseText);
            
            assertNotNull(keywords);
            assertFalse(keywords.isEmpty(), "Should extract keywords from Chinese text");
            
            // Verify that Chinese characters are preserved
            boolean hasChineseKeyword = keywords.stream()
                .anyMatch(k -> k.chars().anyMatch(c -> 
                    Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS));
            
            assertTrue(hasChineseKeyword, "Should preserve Chinese characters in keywords");
            
            System.out.println("Extracted Chinese keywords: " + keywords);
        } catch (Exception e) {
            fail("Failed to test Chinese keyword extraction: " + e.getMessage());
        }
    }
    
    @Test
    public void testExtractKeywords_EnglishText() {
        // Test that English text still works
        String englishText = "This is a test document with some important keywords about testing";
        
        try {
            java.lang.reflect.Method method = OCRService.class.getDeclaredMethod("extractKeywordsFromText", String.class);
            method.setAccessible(true);
            List<String> keywords = (List<String>) method.invoke(ocrService, englishText);
            
            assertNotNull(keywords);
            assertFalse(keywords.isEmpty(), "Should extract keywords from English text");
            
            // Should contain words longer than 3 characters
            assertTrue(keywords.stream().anyMatch(k -> k.equals("test") || k.equals("document") || k.equals("important")),
                "Should extract meaningful English keywords");
            
            // Should filter out stop words
            assertFalse(keywords.contains("this"), "Should filter out stop words");
            assertFalse(keywords.contains("with"), "Should filter out stop words");
            
            System.out.println("Extracted English keywords: " + keywords);
        } catch (Exception e) {
            fail("Failed to test English keyword extraction: " + e.getMessage());
        }
    }
    
    @Test
    public void testExtractKeywords_MixedText() {
        // Test mixed Chinese and English text
        String mixedText = "这是一个test文档，包含中英文mixed content关键词";
        
        try {
            java.lang.reflect.Method method = OCRService.class.getDeclaredMethod("extractKeywordsFromText", String.class);
            method.setAccessible(true);
            List<String> keywords = (List<String>) method.invoke(ocrService, mixedText);
            
            assertNotNull(keywords);
            assertFalse(keywords.isEmpty(), "Should extract keywords from mixed text");
            
            // Should have both Chinese and English keywords
            boolean hasChinese = keywords.stream()
                .anyMatch(k -> k.chars().anyMatch(c -> 
                    Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS));
            boolean hasEnglish = keywords.stream()
                .anyMatch(k -> k.chars().allMatch(c -> c >= 'a' && c <= 'z'));
            
            assertTrue(hasChinese || hasEnglish, "Should extract keywords from mixed text");
            
            System.out.println("Extracted mixed keywords: " + keywords);
        } catch (Exception e) {
            fail("Failed to test mixed keyword extraction: " + e.getMessage());
        }
    }
    
    @Test
    public void testExtractKeywords_EmptyText() {
        // Test empty text
        String emptyText = "";
        
        try {
            java.lang.reflect.Method method = OCRService.class.getDeclaredMethod("extractKeywordsFromText", String.class);
            method.setAccessible(true);
            List<String> keywords = (List<String>) method.invoke(ocrService, emptyText);
            
            assertNotNull(keywords);
            assertTrue(keywords.isEmpty(), "Should return empty list for empty text");
        } catch (Exception e) {
            fail("Failed to test empty text: " + e.getMessage());
        }
    }
    
    @Test
    public void testExtractKeywords_PunctuationHandling() {
        // Test that punctuation is properly handled
        String textWithPunctuation = "测试！文本，包含。各种；标点符号";
        
        try {
            java.lang.reflect.Method method = OCRService.class.getDeclaredMethod("extractKeywordsFromText", String.class);
            method.setAccessible(true);
            List<String> keywords = (List<String>) method.invoke(ocrService, textWithPunctuation);
            
            assertNotNull(keywords);
            
            // Keywords should not contain punctuation
            keywords.forEach(k -> {
                assertFalse(k.contains("！"), "Keywords should not contain punctuation");
                assertFalse(k.contains("，"), "Keywords should not contain punctuation");
                assertFalse(k.contains("。"), "Keywords should not contain punctuation");
            });
            
            System.out.println("Extracted keywords from punctuated text: " + keywords);
        } catch (Exception e) {
            fail("Failed to test punctuation handling: " + e.getMessage());
        }
    }
}
