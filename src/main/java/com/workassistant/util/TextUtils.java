package com.workassistant.util;

/**
 * Utility class for text processing operations
 */
public class TextUtils {
    
    /**
     * Regular expression pattern for splitting text on punctuation and whitespace
     * while preserving Unicode characters including CJK characters.
     * 
     * Pattern breakdown:
     * - \p{Punct}: ASCII punctuation marks
     * - \p{Space}: All whitespace characters
     * - \u3000-\u303F: CJK symbols and punctuation
     * - \uFF00-\uFFEF: Halfwidth and fullwidth forms (includes fullwidth punctuation)
     */
    public static final String PUNCTUATION_PATTERN = "[\\p{Punct}\\p{Space}\\u3000-\\u303F\\uFF00-\\uFFEF]+";
    
    /**
     * Check if a character codepoint is in one of the CJK (Chinese, Japanese, Korean) Unicode blocks
     * 
     * @param codepoint The Unicode codepoint to check (use int to support surrogate pairs)
     * @return true if the codepoint is a CJK character
     */
    public static boolean isCJKCharacter(int codepoint) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(codepoint);
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
