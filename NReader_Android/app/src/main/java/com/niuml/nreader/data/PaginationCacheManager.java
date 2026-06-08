package com.niuml.nreader.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PaginationCacheManager {
    
    private static final String TAG = "PaginationCacheManager";
    private static final String PREFS_NAME = "reader_pagination_cache";
    private static final String KEY_CHAPTERS = "chapters_%s";
    private static final String KEY_PAGES = "pages_%s_%d";
    private static final String KEY_TOTAL_PAGES = "total_pages_%s";
    private static final String KEY_FONT_SIZE = "font_size_%s";
    private static final String KEY_DISPLAY_DIMENSIONS = "dimensions_%s";
    private static final String KEY_READING_PROGRESS = "progress_%s";
    private static final String KEY_LAST_OPEN_TIME = "last_open_%s";
    
    private final SharedPreferences prefs;
    private final Gson gson;
    
    public PaginationCacheManager(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
    }

    public boolean hasCachedChapters(String bookId) {
        return prefs.contains(String.format(KEY_CHAPTERS, bookId));
    }

    public boolean hasCachedPages(String bookId, int fontSize, int width, int height) {
        return prefs.contains(String.format(KEY_TOTAL_PAGES, bookId)) &&
               !isDisplayConfigChanged(bookId, fontSize, width, height);
    }

    public void saveChapters(String bookId, List<ChapterManager.Chapter> chapters) {
        String key = String.format(KEY_CHAPTERS, bookId);
        String json = gson.toJson(chapters);
        prefs.edit().putString(key, json).apply();
        Log.d(TAG, "Saved chapters for book: " + bookId);
    }

    public List<ChapterManager.Chapter> loadChapters(String bookId) {
        String key = String.format(KEY_CHAPTERS, bookId);
        String json = prefs.getString(key, null);
        if (json != null) {
            Type type = new TypeToken<ArrayList<ChapterManager.Chapter>>() {}.getType();
            return gson.fromJson(json, type);
        }
        return null;
    }

    public void savePages(String bookId, int chapterIndex, List<PagePager.PageRange> pages) {
        String key = String.format(KEY_PAGES, bookId, chapterIndex);
        String json = gson.toJson(pages);
        prefs.edit().putString(key, json).apply();
    }

    public List<PagePager.PageRange> loadPages(String bookId, int chapterIndex) {
        String key = String.format(KEY_PAGES, bookId, chapterIndex);
        String json = prefs.getString(key, null);
        if (json != null) {
            Type type = new TypeToken<ArrayList<PagePager.PageRange>>() {}.getType();
            return gson.fromJson(json, type);
        }
        return null;
    }

    public void saveTotalPages(String bookId, int totalPages) {
        String key = String.format(KEY_TOTAL_PAGES, bookId);
        prefs.edit().putInt(key, totalPages).apply();
    }

    public int loadTotalPages(String bookId) {
        String key = String.format(KEY_TOTAL_PAGES, bookId);
        return prefs.getInt(key, 0);
    }

    public void saveDisplayConfig(String bookId, int fontSize, int width, int height) {
        String fontSizeKey = String.format(KEY_FONT_SIZE, bookId);
        String dimensionsKey = String.format(KEY_DISPLAY_DIMENSIONS, bookId);
        prefs.edit()
            .putInt(fontSizeKey, fontSize)
            .putString(dimensionsKey, width + "x" + height)
            .apply();
    }

    public boolean isDisplayConfigChanged(String bookId, int fontSize, int width, int height) {
        String fontSizeKey = String.format(KEY_FONT_SIZE, bookId);
        String dimensionsKey = String.format(KEY_DISPLAY_DIMENSIONS, bookId);
        
        int savedFontSize = prefs.getInt(fontSizeKey, -1);
        String savedDimensions = prefs.getString(dimensionsKey, "");
        
        return savedFontSize != fontSize || !savedDimensions.equals(width + "x" + height);
    }

    public void saveReadingProgress(String bookId, int chapterIndex, int pageInChapter, int globalPage) {
        ReadingProgress progress = new ReadingProgress(chapterIndex, pageInChapter, globalPage, System.currentTimeMillis());
        String key = String.format(KEY_READING_PROGRESS, bookId);
        String json = gson.toJson(progress);
        prefs.edit().putString(key, json).apply();
        Log.d(TAG, "Saved reading progress for book: " + bookId);
    }

    public ReadingProgress loadReadingProgress(String bookId) {
        String key = String.format(KEY_READING_PROGRESS, bookId);
        String json = prefs.getString(key, null);
        if (json != null) {
            return gson.fromJson(json, ReadingProgress.class);
        }
        return null;
    }

    public void saveLastOpenTime(String bookId, long timestamp) {
        String key = String.format(KEY_LAST_OPEN_TIME, bookId);
        prefs.edit().putLong(key, timestamp).apply();
    }

    public long loadLastOpenTime(String bookId) {
        String key = String.format(KEY_LAST_OPEN_TIME, bookId);
        return prefs.getLong(key, 0);
    }

    public boolean hasValidCache(String bookId, int fontSize, int width, int height) {
        return hasCachedChapters(bookId) && 
               prefs.contains(String.format(KEY_TOTAL_PAGES, bookId)) &&
               !isDisplayConfigChanged(bookId, fontSize, width, height);
    }

    public void clearCache(String bookId) {
        SharedPreferences.Editor editor = prefs.edit();
        
        editor.remove(String.format(KEY_CHAPTERS, bookId));
        editor.remove(String.format(KEY_TOTAL_PAGES, bookId));
        editor.remove(String.format(KEY_FONT_SIZE, bookId));
        editor.remove(String.format(KEY_DISPLAY_DIMENSIONS, bookId));
        editor.remove(String.format(KEY_READING_PROGRESS, bookId));
        editor.remove(String.format(KEY_LAST_OPEN_TIME, bookId));
        
        for (int i = 0; i < 1000; i++) {
            String key = String.format(KEY_PAGES, bookId, i);
            if (!prefs.contains(key)) break;
            editor.remove(key);
        }
        
        editor.apply();
        Log.d(TAG, "Cleared cache for book: " + bookId);
    }

    public void clearAllCache() {
        prefs.edit().clear().apply();
        Log.d(TAG, "Cleared all cache");
    }

    public static class ReadingProgress {
        public int chapterIndex;
        public int pageInChapter;
        public int globalPage;
        public long timestamp;
        
        public ReadingProgress() {}
        
        public ReadingProgress(int chapterIndex, int pageInChapter, int globalPage, long timestamp) {
            this.chapterIndex = chapterIndex;
            this.pageInChapter = pageInChapter;
            this.globalPage = globalPage;
            this.timestamp = timestamp;
        }
    }
}
