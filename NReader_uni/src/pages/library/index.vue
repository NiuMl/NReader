<template>
  <view class="container">
    <view class="library-header">
      <view class="header-left">
        <text class="header-title">书库</text>
        <input 
          v-model="searchText" 
          class="search-input" 
          placeholder="搜索..."
          @confirm="handleSearch"
          @input="handleSearchInput"
        />
      </view>
      <view class="search-btn" @click="handleSearch">
        <text class="search-icon">🔍</text>
      </view>
    </view>

    <view v-if="isLoading && !hasMore" class="empty-state">
      <text class="empty-title">加载中...</text>
    </view>

    <view v-else-if="libraryBooks.length === 0 && !isLoading" class="empty-state">
      <view class="empty-icon">📚</view>
      <text class="empty-title">书库为空</text>
      <text class="empty-desc">请确保 NReader_py 服务正在运行</text>
    </view>

    <scroll-view 
      v-else 
      class="books-list" 
      scroll-y 
      @scrolltolower="loadMore"
      :refresher-enabled="true"
      :refresher-triggered="isRefreshing"
      @refresherrefresh="refreshList"
    >
      <view
        v-for="book in libraryBooks"
        :key="book.id"
        class="book-item"
      >
        <view class="book-info" @click="showBookDetail(book)">
          <view class="book-cover">
            <text class="cover-placeholder">📖</text>
          </view>
          <view class="book-content">
            <text class="book-title">{{ book.title }}</text>
            <text class="book-author">{{ book.author }}</text>
          </view>
        </view>
        <view
          class="action-btn"
          :class="{ 'disabled': isInShelf(book.id) }"
          @click="handleAddToShelf(book)"
        >
          <text class="btn-text">{{ isInShelf(book.id) ? '已在书架' : '+加入书架' }}</text>
        </view>
      </view>

      <view v-if="isLoadingMore" class="loading-more">
        <text>加载更多...</text>
      </view>

      <view v-if="!hasMore && libraryBooks.length > 0" class="no-more">
        <text>— 已加载全部 —</text>
      </view>
    </scroll-view>

    <TabBar />
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useBookStore } from '@/stores/bookStore'
import type { LibraryBook, Book } from '@/types'
import { fetchNovels, type FetchNovelsResponse } from '@/api/novel'
import TabBar from '@/components/TabBar.vue'

const bookStore = useBookStore()
const libraryBooks = ref<LibraryBook[]>([])
const isLoading = ref(false)
const isLoadingMore = ref(false)
const isRefreshing = ref(false)
const hasMore = ref(true)
const currentPage = ref(1)
const totalCount = ref(0)
const pageSize = 10
const searchText = ref('')

async function loadLibraryBooks(page: number = 1, refresh: boolean = false) {
  if (refresh) {
    isLoading.value = true
    isRefreshing.value = true
  } else {
    isLoadingMore.value = true
  }
  
  try {
    const response: FetchNovelsResponse = await fetchNovels(page, pageSize, searchText.value)
    
    if (refresh) {
      libraryBooks.value = response.novels
    } else {
      libraryBooks.value = [...libraryBooks.value, ...response.novels]
    }
    
    totalCount.value = response.total
    currentPage.value = response.page
    hasMore.value = libraryBooks.value.length < totalCount.value
  } catch (error) {
    console.error('Failed to load library books:', error)
    uni.showToast({ title: '加载书库失败', icon: 'none' })
  } finally {
    isLoading.value = false
    isLoadingMore.value = false
    isRefreshing.value = false
  }
}

function handleSearchInput() {
}

function handleSearch() {
  currentPage.value = 1
  hasMore.value = true
  loadLibraryBooks(1, true)
}

function loadMore() {
  if (!hasMore.value || isLoading.value || isLoadingMore.value) return
  loadLibraryBooks(currentPage.value + 1)
}

function refreshList() {
  currentPage.value = 1
  hasMore.value = true
  loadLibraryBooks(1, true)
}

function isInShelf(bookId: string): boolean {
  return bookStore.checkBookInShelf(bookId)
}

function handleAddToShelf(book: LibraryBook) {
  if (isInShelf(book.id)) {
    return
  }

  const newBook: Book = {
    id: book.id,
    title: book.title,
    author: book.author,
    cover: book.cover,
    filePath: book.filePath || '',
    format: 'txt' as const,
    progress: 0,
    lastReadTime: new Date().toISOString()
  }

  bookStore.addBook(newBook)
  uni.showToast({ title: '已加入书架', icon: 'success' })
}

function showBookDetail(book: LibraryBook) {
  let targetBook = bookStore.bookshelf.find(b => b.id === book.id)

  if (!targetBook) {
    targetBook = {
      id: book.id,
      title: book.title,
      author: book.author,
      cover: book.cover,
      filePath: book.filePath || '',
      format: 'txt' as const,
      progress: 0,
      lastReadTime: new Date().toISOString()
    }
  }

  bookStore.setCurrentBook(targetBook)
  uni.navigateTo({
    url: `/pages/reader/index?bookId=${targetBook.id}`
  })
}

onMounted(() => {
  console.log('书库页面加载')
  loadLibraryBooks(1, true)
})
</script>

<style lang="scss" scoped>
.container {
  min-height: 100vh;
  background-color: #F7F8FA;
  padding-bottom: 120rpx;
}

.library-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 32rpx;
  padding-top: calc(32rpx + var(--status-bar-height, 44px));
  background-color: #FFFFFF;
}

.header-left {
  flex: 1;
  display: flex;
  align-items: center;
  margin-right: 24rpx;
}

.header-title {
  font-size: 36rpx;
  font-weight: 600;
  color: #333333;
  margin-right: 24rpx;
}

.search-input {
  flex: 1;
  height: 64rpx;
  background-color: #F5F5F5;
  border-radius: 32rpx;
  padding: 0 24rpx;
  font-size: 26rpx;
}

.search-btn {
  width: 64rpx;
  height: 64rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.search-icon {
  font-size: 36rpx;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 120rpx 48rpx;
}

.empty-icon {
  font-size: 120rpx;
  margin-bottom: 32rpx;
}

.empty-title {
  font-size: 32rpx;
  color: #333333;
  margin-bottom: 16rpx;
}

.empty-desc {
  font-size: 26rpx;
  color: #999999;
}

.books-list {
  height: calc(100vh - 260rpx);
  padding: 24rpx;
}

.book-item {
  display: flex;
  align-items: center;
  background-color: #FFFFFF;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 24rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);
}

.book-info {
  flex: 1;
  display: flex;
  align-items: center;
  margin-right: 24rpx;
  max-width: 420rpx;
}

.book-cover {
  width: 120rpx;
  height: 160rpx;
  background-color: #F7F8FA;
  border-radius: 8rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 24rpx;
}

.cover-placeholder {
  font-size: 48rpx;
}

.book-content {
  flex: 1;
}

.book-title {
  font-size: 28rpx;
  color: #333333;
  display: block;
  margin-bottom: 8rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.book-author {
  font-size: 24rpx;
  color: #999999;
}

.action-btn {
  margin-left: 24rpx;
  padding: 16rpx 32rpx;
  background-color: #4A90D9;
  border-radius: 8rpx;
  
  &.disabled {
    background-color: #E8F4E8;
  }
}

.btn-text {
  font-size: 24rpx;
  color: #FFFFFF;
  
  .disabled & {
    color: #4CAF50;
  }
}

.loading-more {
  text-align: center;
  padding: 24rpx;
  font-size: 26rpx;
  color: #999999;
}

.no-more {
  text-align: center;
  padding: 16rpx 24rpx;
  font-size: 24rpx;
  color: #CCCCCC;
  line-height: 1.5;
}
</style>