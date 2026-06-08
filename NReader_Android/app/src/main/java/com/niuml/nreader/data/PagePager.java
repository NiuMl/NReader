package com.niuml.nreader.data;

import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.Log;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class PagePager {
    
    private static final String TAG = "PagePager";
    private static final int WINDOW_SIZE = 10 * 1024;

    public static class PageRange {
        public final int start;
        public final int end;
        
        public PageRange(int start, int end) {
            this.start = start;
            this.end = end;
        }
        
        public int length() {
            return end - start;
        }
    }

    private final TextPaint textPaint;
    private final int displayWidth;
    private final int displayHeight;

    public PagePager(TextView textView) {
        this.textPaint = new TextPaint(textView.getPaint());
        this.displayWidth = textView.getWidth() - 
            textView.getPaddingLeft() - textView.getPaddingRight();
        this.displayHeight = textView.getHeight() - 
            textView.getPaddingTop() - textView.getPaddingBottom();
    }

    public PagePager(TextPaint textPaint, int displayWidth, int displayHeight) {
        this.textPaint = new TextPaint(textPaint);
        this.displayWidth = displayWidth;
        this.displayHeight = displayHeight;
    }

    public List<PageRange> paginate(String chapterText) {
        return paginate(chapterText, displayWidth, displayHeight);
    }

    public static List<PageRange> paginate(String chapterText, int displayWidth, int displayHeight) {
        return paginate(chapterText, displayWidth, displayHeight, new TextPaint());
    }

    public static List<PageRange> paginate(String chapterText, int displayWidth, 
                                           int displayHeight, TextPaint textPaint) {
        List<PageRange> pages = new ArrayList<>();
        
        if (chapterText == null || chapterText.isEmpty()) {
            return pages;
        }
        
        if (displayHeight <= 0 || displayWidth <= 0) {
            pages.add(new PageRange(0, chapterText.length()));
            return pages;
        }
        
        int cursor = 0;
        int length = chapterText.length();
        
        while (cursor < length) {
            int endIndex = findPageBreak(chapterText, cursor, displayWidth, displayHeight, textPaint);
            
            if (endIndex <= cursor) {
                endIndex = cursor + Math.min(100, length - cursor);
            }
            
            pages.add(new PageRange(cursor, endIndex));
            cursor = endIndex;
        }
        
        Log.d(TAG, "Paginated " + pages.size() + " pages");
        return pages;
    }

    public static List<PageRange> paginateWithWindow(String chapterText, int displayWidth, 
                                                     int displayHeight, TextPaint textPaint) {
        List<PageRange> pages = new ArrayList<>();
        
        if (chapterText == null || chapterText.isEmpty()) {
            return pages;
        }
        
        if (displayHeight <= 0 || displayWidth <= 0) {
            pages.add(new PageRange(0, chapterText.length()));
            return pages;
        }
        
        int totalLength = chapterText.length();
        int cursor = 0;
        
        while (cursor < totalLength) {
            int windowEnd = Math.min(cursor + WINDOW_SIZE, totalLength);
            String windowText = chapterText.substring(cursor, windowEnd);
            
            int relativeBreak = findPageBreak(windowText, 0, displayWidth, displayHeight, textPaint);
            int absoluteBreak = cursor + relativeBreak;
            
            if (relativeBreak <= 0) {
                absoluteBreak = cursor + Math.min(100, totalLength - cursor);
            }
            
            pages.add(new PageRange(cursor, absoluteBreak));
            cursor = absoluteBreak;
        }
        
        return pages;
    }

    private static int findPageBreak(String text, int start, int displayWidth, 
                                     int displayHeight, TextPaint textPaint) {
        if (start >= text.length()) {
            return text.length();
        }
        
        String remainingText = text.substring(start);
        
        StaticLayout.Builder builder = StaticLayout.Builder.obtain(
            remainingText, 0, remainingText.length(), textPaint, displayWidth
        );
        
        StaticLayout layout = builder.build();
        
        int lineCount = layout.getLineCount();
        if (lineCount == 0) {
            return start;
        }
        
        int lastVisibleLine = -1;
        for (int i = 0; i < lineCount; i++) {
            float lineBottom = layout.getLineBottom(i);
            if (lineBottom <= displayHeight) {
                lastVisibleLine = i;
            } else {
                break;
            }
        }
        
        if (lastVisibleLine < 0) {
            int firstLineEnd = layout.getLineEnd(0);
            return start + firstLineEnd;
        }
        
        int lineEnd = layout.getLineEnd(lastVisibleLine);
        
        if (lastVisibleLine == lineCount - 1) {
            return start + lineEnd;
        }
        
        return start + lineEnd;
    }

    public int getDisplayWidth() {
        return displayWidth;
    }

    public int getDisplayHeight() {
        return displayHeight;
    }

    public TextPaint getTextPaint() {
        return textPaint;
    }
}
