package com.workassistant.util;

/**
 * Utility class for text processing operations
 */
public class TextUtils {
    
    /**
     * Check if a character is in one of the CJK (Chinese, Japanese, Korean) Unicode blocks
     * 
     * @param c The character to check
     * @return true if the character is a CJK character
     */
    public static boolean isCJKCharacter(int c) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(c);
        return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS ||
               block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A ||
               block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B ||
               block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS;
    }
    
    /**
     * Check if a string contains any CJK characters
     * 
     * @param text The text to check
     * @return true if the text contains at least one CJK character
     */
    public static boolean containsCJKCharacters(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        return text.chars().anyMatch(TextUtils::isCJKCharacter);
    }
}
