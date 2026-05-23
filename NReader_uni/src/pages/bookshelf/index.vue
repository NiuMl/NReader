<template>
  <view class="container">
    <view class="bookshelf-header">
      <text class="header-title">我的书架</text>
      <view class="add-btn" @click="showAddMenu = true">
        <text class="add-icon">+</text>
      </view>
    </view>

    <view v-if="books.length === 0" class="empty-state">
      <view class="empty-icon">📚</view>
      <text class="empty-title">书架空空如也</text>
      <text class="empty-desc">点击右上角 + 号添加书籍</text>
    </view>

    <scroll-view v-else class="books-grid" scroll-y>
      <view class="grid-wrapper">
        <view
          v-for="book in books"
          :key="book.id"
          class="book-card"
          @click="openReader(book)"
          @longpress="showDeleteConfirm(book)"
        >
          <view class="book-cover">
            <text class="cover-placeholder">📖</text>
          </view>
          <text class="book-title">{{ book.title }}</text>
          <text class="book-progress">已读 {{ book.progress }}%</text>
        </view>
      </view>
    </scroll-view>

    <view v-if="showAddMenu" class="menu-mask" @click="showAddMenu = false">
      <view class="menu-popover" @click.stop>
        <view class="menu-item" @click="importFromLocal">
          <text class="menu-icon">📁</text>
          <text class="menu-text">本机导入</text>
        </view>
        <view class="menu-item" @click="openWlanTransfer">
          <text class="menu-icon">📶</text>
          <text class="menu-text">WLAN传书</text>
        </view>
      </view>
    </view>

    <TabBar />
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useBookStore } from '@/stores/bookStore'
import type { Book } from '@/types'
import { generateBookId, formatFileName, getFileExtension } from '@/utils/bookParser'
import TabBar from '@/components/TabBar.vue'

const bookStore = useBookStore()
const showAddMenu = ref(false)

const books = computed(() => bookStore.sortedBookshelf)

function openReader(book: Book) {
  bookStore.setCurrentBook(book)
  uni.navigateTo({
    url: `/pages/reader/index?bookId=${book.id}`
  })
}

function showDeleteConfirm(book: Book) {
  uni.showModal({
    title: '删除确认',
    content: `确定要删除《${book.title}》吗？`,
    success: (res) => {
      if (res.confirm) {
        bookStore.removeBook(book.id)
        uni.showToast({ title: '删除成功', icon: 'success' })
      }
    }
  })
}

function importFromLocal() {
    showAddMenu.value = false
    if (typeof window !== 'undefined') {
      const input = document.createElement('input')
      input.type = 'file'
      input.accept = '.txt,.epub'
      input.onchange = (e: Event) => {
        const file = (e.target as HTMLInputElement).files?.[0]
        if (file) {
          handleFile(file)
        }
      }
      input.click()
    } else {
      uni.chooseMessageFile({
        count: 1,
        type: 'file',
        success: (res) => {
          const file = res.tempFiles[0]
          if (file) {
            const extension = getFileExtension(file.name)
            if (extension === 'txt' || extension === 'epub') {
              const book: Book = {
                id: generateBookId(),
                title: formatFileName(file.name),
                author: '未知作者',
                cover: '',
                filePath: file.path,
                format: extension as 'txt' | 'epub',
                progress: 0,
                lastReadTime: new Date().toISOString()
              }
              bookStore.addBook(book)
              uni.showToast({ title: '导入成功', icon: 'success' })
            } else {
              uni.showToast({ title: '仅支持txt/epub格式', icon: 'none' })
            }
          }
        },
        fail: () => {
          uni.showToast({ title: '选择文件失败', icon: 'none' })
        }
      })
    }
  }

function handleFile(file: File) {
  const extension = getFileExtension(file.name)
  if (extension !== 'txt' && extension !== 'epub') {
    uni.showToast({ title: '仅支持txt/epub格式', icon: 'none' })
    return
  }

  const reader = new FileReader()
  reader.onload = (e) => {
    const content = e.target?.result as string
    const book: Book = {
      id: generateBookId(),
      title: formatFileName(file.name),
      author: '未知作者',
      cover: '',
      filePath: URL.createObjectURL(file),
      format: extension as 'txt' | 'epub',
      progress: 0,
      lastReadTime: new Date().toISOString()
    }
    localStorage.setItem(`book_content_${book.id}`, content)
    bookStore.addBook(book)
    uni.showToast({ title: '导入成功', icon: 'success' })
  }
  reader.readAsText(file, 'utf-8')
}

function openWlanTransfer() {
  showAddMenu.value = false
  uni.navigateTo({
    url: '/pages/wlan-transfer/index'
  })
}

onMounted(() => {
  console.log('书架页面加载')
})
</script>

<style lang="scss" scoped>
.container {
  min-height: 100vh;
  background-color: #F7F8FA;
  padding-bottom: 120rpx;
}

.bookshelf-header {
  padding: 32rpx;
  padding-top: calc(32rpx + var(--status-bar-height, 44px));
  background-color: #FFFFFF;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-title {
  font-size: 36rpx;
  font-weight: 600;
  color: #333333;
}

.add-btn {
  width: 72rpx;
  height: 72rpx;
  background-color: #4A90D9;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.add-icon {
  color: #FFFFFF;
  font-size: 40rpx;
  font-weight: 300;
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

.books-grid {
  height: calc(100vh - 200rpx);
}

.grid-wrapper {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  padding: 24rpx;
  gap: 24rpx;
}

.book-card {
  background-color: #FFFFFF;
  border-radius: 16rpx;
  padding: 20rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);
}

.book-cover {
  width: 100%;
  aspect-ratio: 3/4;
  background-color: #F7F8FA;
  border-radius: 8rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16rpx;
}

.cover-placeholder {
  font-size: 48rpx;
}

.book-title {
  font-size: 24rpx;
  color: #333333;
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-bottom: 8rpx;
}

.book-progress {
  font-size: 22rpx;
  color: #999999;
}

.menu-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: flex-start;
  justify-content: flex-end;
  padding: 120rpx 32rpx;
  z-index: 100;
}

.menu-popover {
  background-color: #FFFFFF;
  border-radius: 16rpx;
  padding: 16rpx;
  min-width: 240rpx;
}

.menu-item {
  display: flex;
  align-items: center;
  padding: 24rpx 32rpx;
  border-radius: 12rpx;
  
  &:active {
    background-color: #F7F8FA;
  }
}

.menu-icon {
  font-size: 36rpx;
  margin-right: 16rpx;
}

.menu-text {
  font-size: 28rpx;
  color: #333333;
}
</style>
