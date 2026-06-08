package com.niuml.nreader.data;

import android.content.Context;
import android.text.TextPaint;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReadingManager {
    
    private static final String TAG = "ReadingManager";
    
    public interface OnTotalPagesCalculatedListener {
        void onTotalPagesCalculated(int totalPages);
    }

    private final File bookFile;
    private final String bookId;
    private final TextPaint textPaint;
    private final int displayWidth;
    private final int displayHeight;
    private final int fontSize;
    private final PaginationCacheManager cacheManager;
    
    private List<ChapterManager.Chapter> chapters;
    private Map<Integer, List<PagePager.PageRange>> chapterPages;
    private List<Integer> chapterFirstPageNumbers;
    
    private int currentChapterIndex = 0;
    private int currentPageInChapter = 0;
    private int totalPages = 0;
    private boolean isFullyCalculated = false;
    
    private final MutableLiveData<Integer> totalPagesLiveData = new MutableLiveData<>();
    private final MutableLiveData<ReadingProgress> progressLiveData = new MutableLiveData<>();
    private final MutableLiveData<ChapterManager.Chapter> currentChapterLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isReadyLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isFullyCalculatedLiveData = new MutableLiveData<>(false);
    
    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    
    private OnTotalPagesCalculatedListener listener;
    private Charset detectedCharset = StandardCharsets.UTF_8;
    private PaginationCacheManager.ReadingProgress savedProgress = null;

    public ReadingManager(Context context, File bookFile, TextPaint textPaint, 
                         int displayWidth, int displayHeight) {
        this.bookFile = bookFile;
        this.bookId = bookFile.getAbsolutePath();
        this.textPaint = textPaint;
        this.displayWidth = displayWidth;
        this.displayHeight = displayHeight;
        this.fontSize = (int) (textPaint.getTextSize() / textPaint.density);
        this.cacheManager = new PaginationCacheManager(context);
        this.chapters = new ArrayList<>();
        this.chapterPages = new HashMap<>();
        this.chapterFirstPageNumbers = new ArrayList<>();
    }

    public void loadBook() {
        executor.submit(() -> {
            try {
                savedProgress = cacheManager.loadReadingProgress(bookId);
                Log.d(TAG, "Loaded saved progress: " + (savedProgress != null ? 
                    "chapter " + savedProgress.chapterIndex + ", page " + savedProgress.pageInChapter : "none"));
                
                boolean hasValidCache = cacheManager.hasValidCache(bookId, fontSize, displayWidth, displayHeight);
                
                if (hasValidCache) {
                    Log.d(TAG, "INSTANT OPEN from cache!");
                    loadFromCache();
                    restoreReadingProgress();
                    isFullyCalculated = true;
                    isFullyCalculatedLiveData.postValue(true);
                    notifyReady();
                } else {
                    Log.d(TAG, "First time - Ultra fast load");
                    ultraFastLoad();
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Failed to load book", e);
                fastLoadFallback();
                notifyReady();
            }
        });
    }

    private void loadFromCache() {
        chapters = cacheManager.loadChapters(bookId);
        totalPages = cacheManager.loadTotalPages(bookId);
        
        if (chapters == null) {
            chapters = new ArrayList<>();
        }
        
        for (int i = 0; i < chapters.size(); i++) {
            List<PagePager.PageRange> pages = cacheManager.loadPages(bookId, i);
            if (pages != null) {
                chapterPages.put(i, pages);
            }
        }
        
        calculateChapterFirstPageNumbers();
    }

    private void restoreReadingProgress() {
        if (savedProgress != null && savedProgress.chapterIndex < chapters.size()) {
            currentChapterIndex = savedProgress.chapterIndex;
            
            List<PagePager.PageRange> pages = chapterPages.get(currentChapterIndex);
            if (pages != null && savedProgress.pageInChapter < pages.size()) {
                currentPageInChapter = savedProgress.pageInChapter;
            } else {
                currentPageInChapter = 0;
            }
            
            Log.d(TAG, "Restored to chapter " + currentChapterIndex + ", page " + currentPageInChapter);
        }
    }

    private void ultraFastLoad() throws Exception {
        detectedCharset = ChapterManager.detectFileEncoding(bookFile);
        
        chapters = ChapterManager.parseChapters(bookFile, detectedCharset);
        cacheManager.saveChapters(bookId, chapters);
        
        if (chapters.isEmpty()) {
            chapters.add(new ChapterManager.Chapter("全书", 0, bookFile.length()));
        }
        
        int targetChapter = savedProgress != null ? savedProgress.chapterIndex : 0;
        if (targetChapter >= chapters.size()) {
            targetChapter = 0;
        }
        
        ChapterManager.Chapter targetChapterObj = chapters.get(targetChapter);
        String chapterText = ChapterManager.readChapterContent(bookFile, targetChapterObj, detectedCharset);
        
        List<PagePager.PageRange> targetPages = PagePager.paginate(
            chapterText, displayWidth, displayHeight, textPaint
        );
        chapterPages.put(targetChapter, targetPages);
        
        if (targetChapter > 0) {
            ChapterManager.Chapter prevChapter = chapters.get(targetChapter - 1);
            String prevText = ChapterManager.readChapterContent(bookFile, prevChapter, detectedCharset);
            List<PagePager.PageRange> prevPages = PagePager.paginate(
                prevText, displayWidth, displayHeight, textPaint
            );
            chapterPages.put(targetChapter - 1, prevPages);
        }
        
        if (targetChapter < chapters.size() - 1) {
            ChapterManager.Chapter nextChapter = chapters.get(targetChapter + 1);
            String nextText = ChapterManager.readChapterContent(bookFile, nextChapter, detectedCharset);
            List<PagePager.PageRange> nextPages = PagePager.paginate(
                nextText, displayWidth, displayHeight, textPaint
            );
            chapterPages.put(targetChapter + 1, nextPages);
        }
        
        totalPages = estimateTotalPages();
        calculateChapterFirstPageNumbers();
        
        currentChapterIndex = targetChapter;
        if (savedProgress != null) {
            List<PagePager.PageRange> pages = chapterPages.get(targetChapter);
            if (pages != null && savedProgress.pageInChapter < pages.size()) {
                currentPageInChapter = savedProgress.pageInChapter;
            } else {
                currentPageInChapter = 0;
            }
        }
        
        notifyReady();
        
        backgroundCalculateAllPages();
    }

    private void backgroundCalculateAllPages() {
        executor.submit(() -> {
            try {
                calculateAllPages();
                saveToCache();
                
                isFullyCalculated = true;
                isFullyCalculatedLiveData.postValue(true);
                
                if (listener != null) {
                    listener.onTotalPagesCalculated(totalPages);
                }
                totalPagesLiveData.postValue(totalPages);
                updateProgress();
                
                Log.d(TAG, "Background calculation complete: " + totalPages + " pages");
                
            } catch (Exception e) {
                Log.e(TAG, "Background calculation failed", e);
            }
        });
    }

    private int estimateTotalPages() {
        long totalChars = 0;
        for (ChapterManager.Chapter chapter : chapters) {
            totalChars += chapter.length;
        }
        
        int charsPerPage = 500;
        return (int) (totalChars / charsPerPage) + 1;
    }

    private void fastLoadFallback() {
        try {
            chapters = ChapterManager.parseChapters(bookFile, StandardCharsets.UTF_8);
        } catch (Exception e) {
            chapters = new ArrayList<>();
            chapters.add(new ChapterManager.Chapter("全书", 0, bookFile.length()));
        }
        
        totalPages = estimateTotalPages();
    }

    private void saveToCache() {
        cacheManager.saveTotalPages(bookId, totalPages);
        cacheManager.saveDisplayConfig(bookId, fontSize, displayWidth, displayHeight);
        
        for (int i = 0; i < chapters.size(); i++) {
            List<PagePager.PageRange> pages = chapterPages.get(i);
            if (pages != null) {
                cacheManager.savePages(bookId, i, pages);
            }
        }
    }

    public void saveCurrentProgress() {
        cacheManager.saveReadingProgress(bookId, currentChapterIndex, currentPageInChapter, getCurrentGlobalPage());
        Log.d(TAG, "Saved progress: chapter " + currentChapterIndex + ", page " + currentPageInChapter);
    }

    private void notifyReady() {
        totalPagesLiveData.postValue(totalPages);
        
        if (!chapters.isEmpty()) {
            currentChapterLiveData.postValue(chapters.get(currentChapterIndex));
            updateProgress();
        }
        
        isReadyLiveData.postValue(true);
    }

    public void calculateAllPages() {
        totalPages = 0;
        
        for (int i = 0; i < chapters.size(); i++) {
            if (chapterPages.containsKey(i)) {
                totalPages += chapterPages.get(i).size();
                continue;
            }
            
            ChapterManager.Chapter chapter = chapters.get(i);
            
            try {
                String chapterText = ChapterManager.readChapterContent(
                    bookFile, chapter, detectedCharset
                );
                
                List<PagePager.PageRange> pages;
                if (chapter.length > 500 * 1024) {
                    pages = PagePager.paginateWithWindow(chapterText, displayWidth, displayHeight, textPaint);
                } else {
                    pages = PagePager.paginate(chapterText, displayWidth, displayHeight, textPaint);
                }
                
                chapterPages.put(i, pages);
                totalPages += pages.size();
                
            } catch (Exception e) {
                Log.e(TAG, "Failed to paginate chapter " + i, e);
                chapterPages.put(i, new ArrayList<>());
            }
        }
        
        calculateChapterFirstPageNumbers();
    }

    private void calculateChapterFirstPageNumbers() {
        chapterFirstPageNumbers.clear();
        int accumulated = 0;
        for (int i = 0; i < chapters.size(); i++) {
            chapterFirstPageNumbers.add(accumulated + 1);
            List<PagePager.PageRange> pages = chapterPages.get(i);
            accumulated += pages != null ? pages.size() : estimateChapterPages(i);
        }
    }

    private int estimateChapterPages(int chapterIndex) {
        if (chapterPages.containsKey(chapterIndex)) {
            return chapterPages.get(chapterIndex).size();
        }
        
        ChapterManager.Chapter chapter = chapters.get(chapterIndex);
        return (int) (chapter.length / 500) + 1;
    }

    public void goToChapter(int chapterIndex) {
        if (chapterIndex >= 0 && chapterIndex < chapters.size()) {
            currentChapterIndex = chapterIndex;
            currentPageInChapter = 0;
            
            loadChapterPagesIfNeeded(chapterIndex);
            currentChapterLiveData.postValue(chapters.get(currentChapterIndex));
            updateProgress();
            saveCurrentProgress();
        }
    }

    private void loadChapterPagesIfNeeded(int chapterIndex) {
        if (chapterPages.containsKey(chapterIndex)) {
            return;
        }
        
        executor.submit(() -> {
            try {
                ChapterManager.Chapter chapter = chapters.get(chapterIndex);
                String chapterText = ChapterManager.readChapterContent(
                    bookFile, chapter, detectedCharset
                );
                
                List<PagePager.PageRange> pages = PagePager.paginate(
                    chapterText, displayWidth, displayHeight, textPaint
                );
                chapterPages.put(chapterIndex, pages);
                calculateChapterFirstPageNumbers();
                
            } catch (Exception e) {
                Log.e(TAG, "Failed to load chapter " + chapterIndex, e);
            }
        });
    }

    public void nextPage() {
        List<PagePager.PageRange> currentPages = chapterPages.get(currentChapterIndex);
        
        if (currentPages != null && !currentPages.isEmpty() && currentPageInChapter < currentPages.size() - 1) {
            currentPageInChapter++;
        } else if (currentChapterIndex < chapters.size() - 1) {
            currentChapterIndex++;
            currentPageInChapter = 0;
            loadChapterPagesIfNeeded(currentChapterIndex);
            currentChapterLiveData.postValue(chapters.get(currentChapterIndex));
        }
        
        updateProgress();
        saveCurrentProgress();
    }

    public void prevPage() {
        if (currentPageInChapter > 0) {
            currentPageInChapter--;
        } else if (currentChapterIndex > 0) {
            currentChapterIndex--;
            loadChapterPagesIfNeeded(currentChapterIndex);
            
            List<PagePager.PageRange> prevPages = chapterPages.get(currentChapterIndex);
            currentPageInChapter = prevPages != null && !prevPages.isEmpty() ? prevPages.size() - 1 : 0;
            
            currentChapterLiveData.postValue(chapters.get(currentChapterIndex));
        }
        
        updateProgress();
        saveCurrentProgress();
    }

    public void goToGlobalPage(int globalPage) {
        int accumulatedPages = 0;
        
        for (int i = 0; i < chapters.size(); i++) {
            int chapterPageCount = getChapterPageCount(i);
            
            if (accumulatedPages + chapterPageCount >= globalPage) {
                currentChapterIndex = i;
                currentPageInChapter = globalPage - accumulatedPages - 1;
                
                if (currentPageInChapter < 0) currentPageInChapter = 0;
                
                loadChapterPagesIfNeeded(currentChapterIndex);
                currentChapterLiveData.postValue(chapters.get(currentChapterIndex));
                updateProgress();
                saveCurrentProgress();
                return;
            }
            
            accumulatedPages += chapterPageCount;
        }
        
        if (!chapters.isEmpty()) {
            currentChapterIndex = chapters.size() - 1;
            loadChapterPagesIfNeeded(currentChapterIndex);
            
            int lastPageCount = getChapterPageCount(currentChapterIndex);
            currentPageInChapter = lastPageCount > 0 ? lastPageCount - 1 : 0;
            
            currentChapterLiveData.postValue(chapters.get(currentChapterIndex));
            updateProgress();
            saveCurrentProgress();
        }
    }

    private int getChapterPageCount(int chapterIndex) {
        List<PagePager.PageRange> pages = chapterPages.get(chapterIndex);
        if (pages != null && !pages.isEmpty()) {
            return pages.size();
        }
        
        ChapterManager.Chapter chapter = chapters.get(chapterIndex);
        return (int) (chapter.length / 500) + 1;
    }

    public String getCurrentPageText() {
        List<PagePager.PageRange> pages = chapterPages.get(currentChapterIndex);
        
        if (pages == null || pages.isEmpty()) {
            return loadCurrentChapterContent();
        }
        
        try {
            ChapterManager.Chapter chapter = chapters.get(currentChapterIndex);
            String chapterText = ChapterManager.readChapterContent(
                bookFile, chapter, detectedCharset
            );
            
            PagePager.PageRange pageRange = pages.get(Math.min(currentPageInChapter, pages.size() - 1));
            return chapterText.substring(pageRange.start, pageRange.end);
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to get current page text", e);
            return loadCurrentChapterContent();
        }
    }

    private String loadCurrentChapterContent() {
        try {
            ChapterManager.Chapter chapter = chapters.get(currentChapterIndex);
            String chapterText = ChapterManager.readChapterContent(
                bookFile, chapter, detectedCharset
            );
            
            if (chapterText.length() > 2000) {
                return chapterText.substring(0, 2000);
            }
            return chapterText;
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to load chapter content", e);
            return "";
        }
    }

    public int getCurrentChapterPage() {
        return currentPageInChapter + 1;
    }

    public int getCurrentChapterTotalPages() {
        return getChapterPageCount(currentChapterIndex);
    }

    public int getCurrentGlobalPage() {
        if (currentChapterIndex >= chapterFirstPageNumbers.size()) {
            return 1;
        }
        return chapterFirstPageNumbers.get(currentChapterIndex) + currentPageInChapter;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public boolean isFullyCalculated() {
        return isFullyCalculated;
    }

    public ChapterManager.Chapter getCurrentChapter() {
        return chapters.isEmpty() ? null : chapters.get(currentChapterIndex);
    }

    public List<ChapterManager.Chapter> getChapters() {
        return chapters;
    }

    private void updateProgress() {
        ReadingProgress progress = new ReadingProgress(
            currentChapterIndex,
            currentPageInChapter,
            getCurrentChapterTotalPages(),
            getCurrentGlobalPage(),
            totalPages,
            isFullyCalculated
        );
        progressLiveData.postValue(progress);
    }

    public LiveData<Integer> getTotalPagesLiveData() {
        return totalPagesLiveData;
    }

    public LiveData<ReadingProgress> getProgressLiveData() {
        return progressLiveData;
    }

    public LiveData<ChapterManager.Chapter> getCurrentChapterLiveData() {
        return currentChapterLiveData;
    }

    public LiveData<Boolean> getIsReadyLiveData() {
        return isReadyLiveData;
    }

    public LiveData<Boolean> getIsFullyCalculatedLiveData() {
        return isFullyCalculatedLiveData;
    }

    public void setOnTotalPagesCalculatedListener(OnTotalPagesCalculatedListener listener) {
        this.listener = listener;
    }

    public void destroy() {
        saveCurrentProgress();
        executor.shutdown();
    }

    public static class ReadingProgress {
        public final int chapterIndex;
        public final int pageInChapter;
        public final int chapterTotalPages;
        public final int globalPage;
        public final int totalPages;
        public final boolean isFullyCalculated;
        
        public ReadingProgress(int chapterIndex, int pageInChapter, int chapterTotalPages,
                              int globalPage, int totalPages, boolean isFullyCalculated) {
            this.chapterIndex = chapterIndex;
            this.pageInChapter = pageInChapter;
            this.chapterTotalPages = chapterTotalPages;
            this.globalPage = globalPage;
            this.totalPages = totalPages;
            this.isFullyCalculated = isFullyCalculated;
        }
        
        public String getDisplayText() {
            String pagesText = isFullyCalculated ? 
                String.format("本章 %d/%d 页 · 全书 %d/%d 页",
                    pageInChapter + 1, chapterTotalPages, globalPage, totalPages) :
                String.format("本章 %d/%d 页 · 全书 %d/~%d 页",
                    pageInChapter + 1, chapterTotalPages, globalPage, totalPages);
            
            return pagesText;
        }
    }
}
