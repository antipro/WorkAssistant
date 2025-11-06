package com.workassistant.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for TextUtils
 */
public class TextUtilsTest {
    
    @Test
    public void testIsCJKCharacter_ChineseCharacter() {
        // Test Chinese characters
        assertTrue(TextUtils.isCJKCharacter('中'), "Chinese character should be detected as CJK");
        assertTrue(TextUtils.isCJKCharacter('文'), "Chinese character should be detected as CJK");
        assertTrue(TextUtils.isCJKCharacter('测'), "Chinese character should be detected as CJK");
        assertTrue(TextUtils.isCJKCharacter('试'), "Chinese character should be detected as CJK");
    }
    
    @Test
    public void testIsCJKCharacter_EnglishCharacter() {
        // Test English characters
        assertFalse(TextUtils.isCJKCharacter('a'), "English character should not be detected as CJK");
        assertFalse(TextUtils.isCJKCharacter('Z'), "English character should not be detected as CJK");
        assertFalse(TextUtils.isCJKCharacter('1'), "Number should not be detected as CJK");
    }
    
    @Test
    public void testIsCJKCharacter_SpecialCharacter() {
        // Test special characters
        assertFalse(TextUtils.isCJKCharacter('!'), "Exclamation mark should not be detected as CJK");
        assertFalse(TextUtils.isCJKCharacter('.'), "Period should not be detected as CJK");
        assertFalse(TextUtils.isCJKCharacter(' '), "Space should not be detected as CJK");
    }
    
    @Test
    public void testContainsCJKCharacters_PureChinese() {
        // Test pure Chinese text
        assertTrue(TextUtils.containsCJKCharacters("中文测试"), "Pure Chinese text should contain CJK characters");
        assertTrue(TextUtils.containsCJKCharacters("这是一个测试"), "Pure Chinese text should contain CJK characters");
    }
    
    @Test
    public void testContainsCJKCharacters_PureEnglish() {
        // Test pure English text
        assertFalse(TextUtils.containsCJKCharacters("English text"), "Pure English text should not contain CJK characters");
        assertFalse(TextUtils.containsCJKCharacters("test123"), "English text with numbers should not contain CJK characters");
    }
    
    @Test
    public void testContainsCJKCharacters_MixedText() {
        // Test mixed Chinese and English text
        assertTrue(TextUtils.containsCJKCharacters("中文English"), "Mixed text should contain CJK characters");
        assertTrue(TextUtils.containsCJKCharacters("test测试"), "Mixed text should contain CJK characters");
        assertTrue(TextUtils.containsCJKCharacters("这是test"), "Mixed text should contain CJK characters");
    }
    
    @Test
    public void testContainsCJKCharacters_EmptyText() {
        // Test empty text
        assertFalse(TextUtils.containsCJKCharacters(""), "Empty text should not contain CJK characters");
        assertFalse(TextUtils.containsCJKCharacters(null), "Null text should not contain CJK characters");
    }
    
    @Test
    public void testContainsCJKCharacters_WithPunctuation() {
        // Test text with punctuation
        assertTrue(TextUtils.containsCJKCharacters("中文！测试。"), "Chinese with punctuation should contain CJK characters");
        assertFalse(TextUtils.containsCJKCharacters("English! Test."), "English with punctuation should not contain CJK characters");
    }
}
