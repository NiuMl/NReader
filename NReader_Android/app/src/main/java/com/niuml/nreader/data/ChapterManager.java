package com.niuml.nreader.data;

import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChapterManager {
    
    private static final String TAG = "ChapterManager";
    
    private static final Pattern CHINESE_CHAPTER_PATTERN = Pattern.compile(
        "^\\s*[第]([一二三四五六七八九十百千\\d]+)[章回部卷节篇集][\\s　]*.*$"
    );
    
    private static final Pattern ENGLISH_CHAPTER_PATTERN = Pattern.compile(
        "^\\s*[Cc]hapter\\s+([\\d]+)[.:\\s].*$"
    );
    
    private static final Pattern ROMAN_CHAPTER_PATTERN = Pattern.compile(
        "^\\s*([IVXLCDM]+)[.:\\s].*$"
    );

    private static final byte[] UTF_8_BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
    private static final byte[] UTF_16LE_BOM = {(byte) 0xFF, (byte) 0xFE};
    private static final byte[] UTF_16BE_BOM = {(byte) 0xFE, (byte) 0xFF};

    public static class Chapter {
        public final String title;
        public final long startOffset;
        public final long length;
        
        public Chapter(String title, long startOffset, long length) {
            this.title = title;
            this.startOffset = startOffset;
            this.length = length;
        }
        
        public long getEndOffset() {
            return startOffset + length;
        }
    }

    public static List<Chapter> parseChapters(File file) throws IOException {
        Charset charset = detectFileEncoding(file);
        Log.d(TAG, "Detected encoding: " + charset.name());
        return parseChapters(file, charset);
    }

    public static List<Chapter> parseChapters(File file, Charset charset) throws IOException {
        List<Chapter> chapters = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), charset))) {
            
            String line;
            long currentOffset = 0;
            long chapterStartOffset = 0;
            String lastChapterTitle = null;
            byte[] lineSeparator = "\n".getBytes(charset);
            
            while ((line = reader.readLine()) != null) {
                String trimmedLine = line.trim();
                
                if (isChapterTitle(trimmedLine)) {
                    if (lastChapterTitle != null && chapterStartOffset < currentOffset) {
                        chapters.add(new Chapter(
                            lastChapterTitle,
                            chapterStartOffset,
                            currentOffset - chapterStartOffset
                        ));
                    }
                    
                    lastChapterTitle = trimmedLine;
                    chapterStartOffset = currentOffset;
                }
                
                currentOffset += line.getBytes(charset).length + lineSeparator.length;
            }
            
            if (lastChapterTitle != null && chapterStartOffset < currentOffset) {
                chapters.add(new Chapter(
                    lastChapterTitle,
                    chapterStartOffset,
                    currentOffset - chapterStartOffset - lineSeparator.length
                ));
            }
        }
        
        if (chapters.isEmpty()) {
            long fileLength = file.length();
            chapters.add(new Chapter("全书", 0, fileLength));
        }
        
        Log.d(TAG, "Parsed " + chapters.size() + " chapters");
        return chapters;
    }

    public static Charset detectFileEncoding(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int bytesRead = fis.read(buffer);
            
            if (bytesRead >= 3 && buffer[0] == UTF_8_BOM[0] && 
                buffer[1] == UTF_8_BOM[1] && buffer[2] == UTF_8_BOM[2]) {
                return StandardCharsets.UTF_8;
            }

            if (bytesRead >= 2) {
                if (buffer[0] == UTF_16LE_BOM[0] && buffer[1] == UTF_16LE_BOM[1]) {
                    return Charset.forName("UTF-16LE");
                }
                if (buffer[0] == UTF_16BE_BOM[0] && buffer[1] == UTF_16BE_BOM[1]) {
                    return Charset.forName("UTF-16BE");
                }
            }

            if (isValidUtf8(buffer, bytesRead)) {
                return StandardCharsets.UTF_8;
            }

            float highByteRatio = calculateHighByteRatio(buffer, bytesRead);
            if (highByteRatio > 0.3) {
                return Charset.forName("GBK");
            }

            if (highByteRatio > 0.1) {
                if (detectShiftJisPattern(buffer, bytesRead)) {
                    return Charset.forName("Shift-JIS");
                }
                return Charset.forName("GB18030");
            }

            return StandardCharsets.UTF_8;
        }
    }

    private static boolean isValidUtf8(byte[] bytes, int length) {
        int sampleSize = Math.min(length, 8192);
        int i = 0;
        while (i < sampleSize) {
            int byteValue = bytes[i] & 0xFF;
            
            if (byteValue < 0x80) {
                i++;
            } else if (byteValue < 0xE0) {
                if (i + 1 >= sampleSize) return true;
                if ((bytes[i + 1] & 0xC0) != 0x80) return false;
                i += 2;
            } else if (byteValue < 0xF0) {
                if (i + 2 >= sampleSize) return true;
                if ((bytes[i + 1] & 0xC0) != 0x80) return false;
                if ((bytes[i + 2] & 0xC0) != 0x80) return false;
                i += 3;
            } else if (byteValue < 0xF8) {
                if (i + 3 >= sampleSize) return true;
                if ((bytes[i + 1] & 0xC0) != 0x80) return false;
                if ((bytes[i + 2] & 0xC0) != 0x80) return false;
                if ((bytes[i + 3] & 0xC0) != 0x80) return false;
                i += 4;
            } else {
                return false;
            }
        }
        return true;
    }

    private static float calculateHighByteRatio(byte[] bytes, int length) {
        int sampleSize = Math.min(length, 1024);
        int highByteCount = 0;
        for (int i = 0; i < sampleSize; i++) {
            if ((bytes[i] & 0xFF) >= 0x80) {
                highByteCount++;
            }
        }
        return (float) highByteCount / sampleSize;
    }

    private static boolean detectShiftJisPattern(byte[] bytes, int length) {
        int sampleSize = Math.min(length - 1, 1024);
        for (int i = 0; i < sampleSize; i++) {
            int b1 = bytes[i] & 0xFF;
            int b2 = bytes[i + 1] & 0xFF;
            
            if (((b1 >= 0x81 && b1 <= 0x9F) || (b1 >= 0xE0 && b1 <= 0xFC)) &&
                ((b2 >= 0x40 && b2 <= 0x7E) || (b2 >= 0x80 && b2 <= 0xFC))) {
                return true;
            }
        }
        return false;
    }

    public static String readChapterContent(File file, Chapter chapter, Charset charset) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            fis.getChannel().position(chapter.startOffset);
            
            byte[] buffer = new byte[(int) Math.min(chapter.length, 1024 * 1024)];
            int bytesRead = fis.read(buffer);
            
            if (bytesRead > 0) {
                return new String(buffer, 0, bytesRead, charset);
            }
            return "";
        }
    }

    public static String readChapterContentWithWindow(File file, Chapter chapter, int windowSize, 
                                                      Charset charset) throws IOException {
        long totalLength = chapter.length;
        long startOffset = chapter.startOffset;
        StringBuilder content = new StringBuilder();
        
        try (FileInputStream fis = new FileInputStream(file)) {
            fis.getChannel().position(startOffset);
            
            byte[] buffer = new byte[Math.min(windowSize, (int) totalLength)];
            long remaining = totalLength;
            
            while (remaining > 0) {
                int bytesToRead = (int) Math.min(buffer.length, remaining);
                int bytesRead = fis.read(buffer, 0, bytesToRead);
                
                if (bytesRead <= 0) break;
                
                content.append(new String(buffer, 0, bytesRead, charset));
                remaining -= bytesRead;
            }
        }
        
        return content.toString();
    }

    private static boolean isChapterTitle(String line) {
        if (line == null || line.isEmpty()) return false;
        
        Matcher chineseMatcher = CHINESE_CHAPTER_PATTERN.matcher(line);
        if (chineseMatcher.matches()) {
            return true;
        }
        
        Matcher englishMatcher = ENGLISH_CHAPTER_PATTERN.matcher(line);
        if (englishMatcher.matches()) {
            return true;
        }
        
        Matcher romanMatcher = ROMAN_CHAPTER_PATTERN.matcher(line);
        if (romanMatcher.matches()) {
            String roman = romanMatcher.group(1);
            int romanValue = parseRomanNumeral(roman);
            return romanValue > 0 && romanValue <= 100;
        }
        
        return false;
    }

    private static int parseRomanNumeral(String roman) {
        int result = 0;
        int[] values = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
        String[] symbols = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};
        
        for (int i = 0; i < symbols.length; i++) {
            while (roman.startsWith(symbols[i])) {
                result += values[i];
                roman = roman.substring(symbols[i].length());
            }
        }
        
        return result;
    }
}
