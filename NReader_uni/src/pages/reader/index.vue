<template>
  <view class="reader-container" :class="backgroundClass" @click="handleReaderClick" @touchstart="handleTouchStart" @touchmove="handleTouchMove" @touchend="handleTouchEnd">
    <view class="toolbar-top" :class="{ visible: showToolbar }">
      <view class="toolbar-left" @click="goBack">
        <text class="toolbar-icon">‹</text>
      </view>
      <text class="toolbar-title">{{ currentBook?.title }}</text>
      <view class="toolbar-right" @click="showSettingsPanel = true">
        <text class="toolbar-icon">⚙️</text>
      </view>
    </view>

    <view v-if="isLoadingContent" class="loading-state">
      <text class="loading-text">加载中...</text>
    </view>

    <view v-show="!isLoadingContent" class="canvas-wrapper">
      <canvas
        id="readerCanvas"
        canvas-id="readerCanvas"
        class="reader-canvas"
      />
    </view>

    <view class="toolbar-bottom">
      <text class="progress-text" @click.stop="openGotoPage">{{ isNaN(currentPage) ? 1 : currentPage }} / {{ isNaN(totalPages) ? 1 : totalPages }}</text>
      <view class="status-info">
        <text class="status-text">{{ currentTime }}</text>
        <text class="status-text">{{ batteryLevel }}%</text>
      </view>
    </view>

    <view v-if="showSettingsPanel" class="settings-mask" @click="showSettingsPanel = false">
      <view class="settings-panel" @click.stop>
        <view class="panel-header">
          <text class="panel-title">阅读设置</text>
          <view class="panel-close" @click="showSettingsPanel = false">
            <text>✕</text>
          </view>
        </view>

        <view class="setting-item">
          <text class="setting-label">字号</text>
          <view class="slider-container">
            <text class="slider-value">{{ fontSizeValue }}px</text>
            <slider
              class="font-size-slider"
              :value="fontSizeValue"
              :min="13"
              :max="48"
              :step="1"
              activeColor="#4A90D9"
              backgroundColor="#E8E8E8"
              block-size="24"
              @change="onFontSizeChange"
            />
            <view class="slider-labels">
              <text class="slider-label">小</text>
              <text class="slider-label">大</text>
            </view>
          </view>
        </view>

        <view class="setting-item">
          <text class="setting-label">行间距</text>
          <view class="setting-options">
            <view
              v-for="spacing in lineSpacingOptions"
              :key="spacing.value"
              class="setting-option"
              :class="{ active: readingSettings.lineSpacing === spacing.value }"
              @click="updateSetting('lineSpacing', spacing.value)"
            >
              <text>{{ spacing.label }}</text>
            </view>
          </view>
        </view>

        <view class="setting-item">
          <text class="setting-label">背景色</text>
          <view class="setting-options">
            <view
              v-for="bg in backgroundColorOptions"
              :key="bg.value"
              class="setting-option bg-option"
              :class="{ active: readingSettings.backgroundColor === bg.value }"
              :style="{ backgroundColor: bg.color }"
              @click="updateSetting('backgroundColor', bg.value)"
            ></view>
          </view>
        </view>

        <view class="setting-item">
          <text class="setting-label">翻页效果</text>
          <view class="setting-options">
            <view
              v-for="effect in pageEffectOptions"
              :key="effect.value"
              class="setting-option"
              :class="{ active: readingSettings.pageEffect === effect.value }"
              @click="updateSetting('pageEffect', effect.value)"
            >
              <text>{{ effect.label }}</text>
            </view>
          </view>
        </view>
      </view>
    </view>

    <view v-if="showGotoPage" class="settings-mask" @click="cancelGotoPage">
      <view class="settings-panel" @click.stop>
        <view class="panel-header">
          <text class="panel-title">跳转页面</text>
          <view class="panel-close" @click="cancelGotoPage">
            <text>✕</text>
          </view>
        </view>

        <view class="setting-item">
          <text class="setting-label">跳转到</text>
          <view class="goto-container">
            <input
              v-model.number="gotoPageValue"
              type="number"
              class="goto-input"
              :min="1"
              :max="isNaN(totalPages) ? 1 : totalPages"
            />
            <text class="goto-separator"> / {{ isNaN(totalPages) ? 1 : totalPages }}</text>
          </view>
        </view>

        <view class="setting-item">
          <slider
            :value="gotoPageValue"
            :min="1"
            :max="isNaN(totalPages) ? 1 : totalPages"
            @change="handleSliderChange"
            activeColor="#4A90D9"
            backgroundColor="#E8E8E8"
            blockSize="24"
          />
        </view>

        <view class="panel-footer">
          <view class="panel-button cancel-button" @click="cancelGotoPage">
            <text>取消</text>
          </view>
          <view class="panel-button confirm-button" @click="confirmGotoPage">
            <text>确认</text>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { onLoad, onReady } from '@dcloudio/uni-app'
import { useBookStore } from '@/stores/bookStore'
import type { ReadingSettings, Book } from '@/types'
import { parseTxtContent } from '@/utils/bookParser'
import { libraryBooks } from '@/static/data/library'
import { generateNovelContent } from '@/utils/contentGenerator'
import { fetchNovelContent } from '@/api/novel'

const bookStore = useBookStore()

const showToolbar = ref(false)
const showSettingsPanel = ref(false)
const showGotoPage = ref(false)
const gotoPageValue = ref(1)
const content = ref<string[]>([])
const currentPage = ref(1)
const totalPages = ref(1)
const isLoadingContent = ref(false)
const touchStartX = ref(0)
const touchMoveX = ref(0)
const touchStartY = ref(0)
const touchMoveY = ref(0)

const systemInfo = uni.getSystemInfoSync()
const screenWidth = systemInfo.windowWidth
const savedFontSize = bookStore.readingSettings?.fontSize
const fontSizeValue = ref(typeof savedFontSize === 'number' && savedFontSize > 0 ? savedFontSize : 19)

const canvasWidth = ref(screenWidth)
const canvasHeight = ref(systemInfo.windowHeight)

let canvasContext: any = null
let targetSavePage = 0
let canvasElement: HTMLCanvasElement | null = null

const lineSpacingOptions = [
  { value: 'compact', label: '紧凑' },
  { value: 'normal', label: '适中' },
  { value: 'relaxed', label: '宽松' }
]

const backgroundColorOptions = [
  { value: 'white', color: '#FFFFFF' },
  { value: 'cream', color: '#FDF6E3' },
  { value: 'dark', color: '#2C2C2C' }
]

const pageEffectOptions = [
  { value: 'none', label: '无效果' },
  { value: 'flip', label: '模拟翻页' },
  { value: 'slideUpDown', label: '上下滑动' }
]

const currentBook = computed(() => bookStore.currentBook)
const readingSettings = computed(() => bookStore.readingSettings)

const backgroundClass = computed(() => {
  return `bg-${readingSettings.value.backgroundColor}`
})

const progress = computed(() => {
  if (totalPages.value === 0) return 0
  return Math.round((currentPage.value / totalPages.value) * 100)
})

const currentTime = ref('')
const batteryLevel = ref(100)

function updateTime() {
  const now = new Date()
  const hours = now.getHours().toString().padStart(2, '0')
  const minutes = now.getMinutes().toString().padStart(2, '0')
  currentTime.value = `${hours}:${minutes}`
}

function getBatteryInfo() {
  if (typeof uni.getBatteryInfo === 'function') {
    uni.getBatteryInfo({
      success: (res: any) => {
        batteryLevel.value = res.level
      }
    })
  }
}

const lineSpacingMap: Record<string, number> = {
  'compact': 1.4,
  'normal': 1.8,
  'relaxed': 2.2
}

const textColor = computed(() => {
  return readingSettings.value.backgroundColor === 'dark' ? '#CCCCCC' : '#333333'
})

const bgColor = computed(() => {
  const colorMap: Record<string, string> = {
    white: '#FFFFFF',
    cream: '#FDF6E3',
    dark: '#2C2C2C'
  }
  return colorMap[readingSettings.value.backgroundColor] || '#FFFFFF'
})

function getCanvasConfig() {
  const systemInfo = uni.getSystemInfoSync()
  const screenWidth = systemInfo.windowWidth
  const screenHeight = systemInfo.windowHeight
  const pixelRatio = systemInfo.pixelRatio || 1

  const rpxToPx = screenWidth / 750
  const horizontalPaddingLeft = 24 * rpxToPx
  const horizontalPaddingRight = 48 * rpxToPx
  const verticalPaddingTop = 20 * rpxToPx
  const verticalPaddingBottom = 60 * rpxToPx

  const fontSize = typeof fontSizeValue.value === 'number' && fontSizeValue.value > 0 ? fontSizeValue.value : 19
  
  const availableWidth = screenWidth - horizontalPaddingLeft - horizontalPaddingRight - fontSize * 1
  const availableHeight = screenHeight - verticalPaddingTop - verticalPaddingBottom

  const lineSpacing = lineSpacingMap[readingSettings.value.lineSpacing] || 1.8
  const lineHeight = fontSize * lineSpacing

  const maxCharsPerLine = availableWidth > 0 ? Math.floor(availableWidth / (fontSize * 0.9)) : 20
  const linesPerPage = availableHeight > 0 && lineHeight > 0 ? Math.floor(availableHeight / lineHeight) : 20

  return {
    screenWidth,
    screenHeight,
    pixelRatio,
    horizontalPaddingLeft,
    horizontalPaddingRight,
    verticalPaddingTop,
    verticalPaddingBottom,
    availableWidth,
    availableHeight,
    fontSize,
    lineSpacing,
    lineHeight,
    maxCharsPerLine,
    linesPerPage
  }
}

function wrapText(text: string, maxCharsPerLine: number): string[] {
  const lines: string[] = []
  if (!text || text.length === 0) return lines

  let currentLine = ''
  for (let i = 0; i < text.length; i++) {
    const char = text[i]
    const testLine = currentLine + char

    if (testLine.length > maxCharsPerLine) {
      if (currentLine) {
        lines.push(currentLine)
        currentLine = char
      } else {
        lines.push(char)
        currentLine = ''
      }
    } else {
      currentLine = testLine
    }
  }

  if (currentLine) {
    lines.push(currentLine)
  }

  return lines
}

function getAllTextLines(): string[] {
  const config = getCanvasConfig()
  const allLines: string[] = []

  for (const paragraph of content.value) {
    const wrappedLines = wrapText(paragraph, config.maxCharsPerLine)
    allLines.push(...wrappedLines)
  }

  return allLines
}

function drawPage() {
  if (!canvasContext || content.value.length === 0 || currentPage.value <= 0) {
    return
  }

  const config = getCanvasConfig()

  if (canvasContext.setFillStyle) {
    canvasContext.setFillStyle(bgColor.value)
    canvasContext.fillRect(0, 0, config.screenWidth, config.screenHeight)

    canvasContext.setFillStyle(textColor.value)
    canvasContext.setFontSize(config.fontSize)

    const allLines = getAllTextLines()
    const pageSize = config.linesPerPage

    const start = (currentPage.value - 1) * pageSize
    const end = Math.min(start + pageSize, allLines.length)
    const pageLines = allLines.slice(start, end)

    let y = config.verticalPaddingTop + config.lineHeight
    const textX = config.horizontalPaddingLeft

    for (let i = 0; i < pageLines.length; i++) {
      const line = pageLines[i]
      const currentY = y + i * config.lineHeight
      if (currentY > config.screenHeight - config.verticalPaddingBottom) {
        break
      }
      canvasContext.fillText(line, textX, currentY)
    }

    canvasContext.draw(false)
  } else {
    canvasContext.fillStyle = bgColor.value
    canvasContext.fillRect(0, 0, config.screenWidth * config.pixelRatio, config.screenHeight * config.pixelRatio)

    canvasContext.fillStyle = textColor.value
    canvasContext.font = `${config.fontSize * config.pixelRatio}px sans-serif`

    const allLines = getAllTextLines()
    const pageSize = config.linesPerPage

    const start = (currentPage.value - 1) * pageSize
    const end = Math.min(start + pageSize, allLines.length)
    const pageLines = allLines.slice(start, end)

    let y = (config.verticalPaddingTop + config.lineHeight) * config.pixelRatio
    const textX = config.horizontalPaddingLeft * config.pixelRatio
    const lineHeight = config.lineHeight * config.pixelRatio

    for (let i = 0; i < pageLines.length; i++) {
      const line = pageLines[i]
      const currentY = y + i * lineHeight
      if (currentY > config.screenHeight * config.pixelRatio - config.verticalPaddingBottom * config.pixelRatio) {
        break
      }
      canvasContext.fillText(line, textX, currentY)
    }
  }
}

function initCanvas() {
  const systemInfo = uni.getSystemInfoSync()
  const newWidth = systemInfo.windowWidth
  const newHeight = systemInfo.windowHeight
  
  canvasWidth.value = newWidth
  canvasHeight.value = newHeight

  canvasContext = null

  if (typeof window !== 'undefined' && typeof document !== 'undefined') {
    canvasElement = document.getElementById('readerCanvas') as HTMLCanvasElement
    if (canvasElement && typeof canvasElement.getContext === 'function') {
      const pixelRatio = systemInfo.pixelRatio || window.devicePixelRatio || 1
      canvasElement.width = newWidth * pixelRatio
      canvasElement.height = newHeight * pixelRatio
      canvasElement.style.width = `${newWidth}px`
      canvasElement.style.height = `${newHeight}px`
      canvasContext = canvasElement.getContext('2d')
    }
  }
  
  if (!canvasContext) {
    try {
      canvasContext = uni.createCanvasContext('readerCanvas')
    } catch (e) {
      console.error('Failed to create canvas context:', e)
    }
  }
  
  if (content.value.length > 0 && currentPage.value > 0 && canvasContext) {
    recalculateTotalPages()
    drawPage()
  }
}

function recalculateTotalPages() {
  if (content.value.length === 0) {
    totalPages.value = 1
    return
  }

  const config = getCanvasConfig()
  const allLines = getAllTextLines()
  const linesPerPage = config.linesPerPage > 0 ? config.linesPerPage : 20
  totalPages.value = Math.max(1, Math.ceil(allLines.length / linesPerPage))
}

function onFontSizeChange(e: any) {
  fontSizeValue.value = e.detail.value
  bookStore.updateSettings({ fontSize: e.detail.value })
  recalculateTotalPages()
  drawPage()
}

watch(fontSizeValue, () => {
  recalculateTotalPages()
  drawPage()
})

watch(() => readingSettings.value.lineSpacing, () => {
  recalculateTotalPages()
  drawPage()
})

watch(() => readingSettings.value.backgroundColor, () => {
  drawPage()
})

watch(content, () => {
  if (content.value.length > 0 && currentPage.value > 0 && canvasContext) {
    recalculateTotalPages()
    drawPage()
  }
}, { deep: true })

function openGotoPage() {
  gotoPageValue.value = currentPage.value
  showGotoPage.value = true
}

function cancelGotoPage() {
  showGotoPage.value = false
}

function confirmGotoPage() {
  const maxPage = isNaN(totalPages.value) ? 1 : totalPages.value
  let targetPage = gotoPageValue.value
  
  if (typeof targetPage !== 'number' || targetPage < 1) {
    targetPage = 1
  }
  if (targetPage > maxPage) {
    targetPage = maxPage
  }
  
  currentPage.value = targetPage
  targetSavePage = targetPage
  drawPage()
  showGotoPage.value = false
}

function handleSliderChange(e: any) {
  gotoPageValue.value = e.detail.value
}

function toggleToolbar() {
  showToolbar.value = !showToolbar.value
}

function goBack() {
  const savePage = currentPage.value > 0 ? currentPage.value : targetSavePage
  if (currentBook.value && savePage > 0) {
    bookStore.updateCurrentPage(currentBook.value.id, savePage)
  }
  bookStore.updateProgress(currentBook.value?.id || '', progress.value)
  uni.navigateBack()
}

function updateSetting(key: keyof ReadingSettings, value: string) {
  bookStore.updateSettings({ [key]: value })
}

async function loadBookContent() {
  if (!currentBook.value) {
    return
  }

  const bookId = currentBook.value.id
  const storedPage = bookStore.getCurrentPage(bookId)
  
  await new Promise(resolve => setTimeout(resolve, 50))
  
  if (!canvasContext) {
    initCanvas()
  }
  
  if (!canvasContext) {
    setTimeout(() => {
      loadBookContent()
    }, 100)
    return
  }

  const filePath = currentBook.value.filePath

  if (filePath.startsWith('blob:')) {
    fetch(filePath)
      .then(res => res.text())
      .then(text => {
        content.value = parseTxtContent(text)
        recalculateTotalPages()
        
        const targetPage = Math.min(totalPages.value, Math.max(1, storedPage))
        targetSavePage = targetPage
        currentPage.value = targetPage
        drawPage()
      })
  } else if (filePath.startsWith('library://') || bookId.startsWith('local_')) {
    isLoadingContent.value = true
    try {
      const novelContent = await fetchNovelContent(bookId)
      if (novelContent && novelContent.content) {
        content.value = parseTxtContent(novelContent.content)
      } else {
        content.value = generateMockContent(currentBook.value.title)
      }
    } catch (error) {
      content.value = generateMockContent(currentBook.value.title)
    } finally {
      isLoadingContent.value = false
    }
    recalculateTotalPages()
    
    const targetPage = Math.min(totalPages.value, Math.max(1, storedPage))
    targetSavePage = targetPage
    currentPage.value = targetPage
    drawPage()
  } else {
    isLoadingContent.value = true
    try {
      if (typeof window !== 'undefined') {
        const response = await fetch(filePath)
        const text = await response.text()
        content.value = parseTxtContent(text)
      } else {
        const res = await uni.getFileSystemManager().readFile({
          filePath: filePath,
          encoding: 'utf8'
        })
        content.value = parseTxtContent(res.data as string)
      }
    } catch (error) {
      content.value = generateMockContent(currentBook.value.title)
    } finally {
      isLoadingContent.value = false
    }
    recalculateTotalPages()
    
    const targetPage = Math.min(totalPages.value, Math.max(1, storedPage))
    targetSavePage = targetPage
    currentPage.value = targetPage
    drawPage()
  }
}

function generateMockContent(title: string): string[] {
  return generateNovelContent(title)
}

onLoad((options: any) => {
  if (options.bookId) {
    const bookId = options.bookId as string
    let book = bookStore.bookshelf.find(b => b.id === bookId)
    if (!book) {
      const libraryBook = libraryBooks.find(b => b.id === bookId)
      if (libraryBook) {
        book = {
          id: libraryBook.id,
          title: libraryBook.title,
          author: libraryBook.author,
          cover: libraryBook.cover,
          filePath: `library://${libraryBook.id}`,
          format: 'txt' as const,
          progress: 0,
          lastReadTime: new Date().toISOString()
        }
      }
    }
    if (book) {
      bookStore.setCurrentBook(book)
      loadBookContent()
    }
  }
})

onReady(() => {
  initCanvas()
  
  if (content.value.length > 0 && currentPage.value > 0) {
    recalculateTotalPages()
    drawPage()
  }
})

function handleTouchStart(e: any) {
  touchStartX.value = e.touches[0].clientX
  touchMoveX.value = e.touches[0].clientX
  touchStartY.value = e.touches[0].clientY
  touchMoveY.value = e.touches[0].clientY
}

function handleTouchMove(e: any) {
  if (showSettingsPanel.value || showGotoPage.value) return
  touchMoveX.value = e.touches[0].clientX
  touchMoveY.value = e.touches[0].clientY
}

function handleReaderClick(e: any) {
  if (showSettingsPanel.value || showGotoPage.value) return

  const pageEffect = readingSettings.value?.pageEffect || 'none'
  
  if (pageEffect === 'slideUpDown') {
    toggleToolbar()
    return
  }

  let touchX = 0
  if (e.touches && e.touches.length > 0) {
    touchX = e.touches[0].clientX
  } else if (e.changedTouches && e.changedTouches.length > 0) {
    touchX = e.changedTouches[0].clientX
  } else if (typeof e.clientX === 'number') {
    touchX = e.clientX
  } else if (e.detail && typeof e.detail.x === 'number') {
    touchX = e.detail.x
  }

  const config = getCanvasConfig()
  const thirdWidth = config.screenWidth / 3

  if (touchX < thirdWidth) {
    prevPage()
  } else if (touchX > config.screenWidth - thirdWidth) {
    nextPage()
  } else {
    toggleToolbar()
  }
}

function prevPage() {
  if (currentPage.value > 1) {
    const targetPage = currentPage.value - 1
    targetSavePage = targetPage
    showToolbar.value = false
    currentPage.value = targetPage
    drawPage()
  }
}

function nextPage() {
  if (currentPage.value < totalPages.value) {
    const targetPage = currentPage.value + 1
    targetSavePage = targetPage
    showToolbar.value = false
    currentPage.value = targetPage
    drawPage()
  }
}

function handleTouchEnd(e: any) {
  if (showSettingsPanel.value || showGotoPage.value) return

  const touchEndX = e.changedTouches[0].clientX
  const touchEndY = e.changedTouches[0].clientY
  const diffX = touchStartX.value - touchEndX
  const diffY = touchStartY.value - touchEndY

  const pageEffect = readingSettings.value?.pageEffect || 'none'

  if (pageEffect === 'slideUpDown') {
    if (Math.abs(diffY) > 50) {
      if (diffY > 0) {
        nextPage()
      } else {
        prevPage()
      }
    }
  } else {
    if (Math.abs(diffX) > 50) {
      if (diffX > 0) {
        nextPage()
      } else {
        prevPage()
      }
    }
  }
}

onMounted(() => {
  updateTime()
  getBatteryInfo()
  initCanvas()

  if (content.value.length > 0 && currentPage.value > 0) {
    recalculateTotalPages()
    drawPage()
  }

  setInterval(updateTime, 1000)

  uni.onWindowResize(() => {
    getBatteryInfo()
    initCanvas()
  })

  uni.onBackPress(() => {
    const savePage = currentPage.value > 0 ? currentPage.value : targetSavePage
    if (currentBook.value && savePage > 0) {
      bookStore.updateCurrentPage(currentBook.value.id, savePage)
    }
    bookStore.updateProgress(currentBook.value?.id || '', progress.value)
    return false
  })

  if (typeof window !== 'undefined') {
    window.addEventListener('resize', () => {
      initCanvas()
    })
  }
})

onUnmounted(() => {
  if (typeof window !== 'undefined') {
    window.removeEventListener('resize', initCanvas)
  }
})
</script>

<style lang="scss" scoped>
.reader-container {
  min-height: 100vh;
  position: relative;
  overflow: hidden;

  &.bg-white {
    background-color: #FFFFFF;
  }

  &.bg-cream {
    background-color: #FDF6E3;
  }

  &.bg-dark {
    background-color: #2C2C2C;
    .toolbar-top, .toolbar-bottom { background-color: rgba(0,0,0,0.8); }
    .toolbar-title { color: #FFFFFF; }
    .progress-text { color: #FFFFFF; }
  }
}

.toolbar-top {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 60rpx 32rpx 24rpx;
  background-color: rgba(255,255,255,0.8);
  z-index: 10;
  transition: transform 0.3s ease-out, opacity 0.3s ease-out;
  transform: translateY(-100%);
  opacity: 0;
}

.toolbar-top.visible {
  transform: translateY(0);
  opacity: 1;
}

.toolbar-left, .toolbar-right {
  width: 64rpx;
  height: 64rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.toolbar-icon {
  font-size: 48rpx;
}

.toolbar-title {
  font-size: 30rpx;
  font-weight: bold;
}

.loading-state {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100vh;
}

.loading-text {
  font-size: 28rpx;
  color: #999999;
}

.canvas-wrapper {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  width: 100vw;
  height: 100vh;
  overflow: hidden;
}

.reader-canvas {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  max-width: 100vw;
  max-height: 100vh;
}

.toolbar-bottom {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 16rpx 32rpx;
  background-color: rgba(255,255,255,0.8);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.progress-text {
  font-size: 24rpx;
  color: #666666;
}

.status-info {
  display: flex;
  gap: 24rpx;
}

.status-text {
  font-size: 22rpx;
  color: #999999;
}

.settings-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0,0,0,0.5);
  display: flex;
  align-items: flex-end;
  z-index: 100;
}

.settings-panel {
  width: 100%;
  background-color: #FFFFFF;
  border-radius: 24rpx 24rpx 0 0;
  padding: 32rpx;
  padding-bottom: calc(32rpx + env(safe-area-inset-bottom));
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 32rpx;
}

.panel-title {
  font-size: 32rpx;
  font-weight: bold;
}

.panel-close {
  width: 48rpx;
  height: 48rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.setting-item {
  margin-bottom: 32rpx;
}

.setting-label {
  font-size: 28rpx;
  margin-bottom: 16rpx;
  display: block;
}

.slider-container {
  padding: 0 16rpx;
}

.slider-value {
  font-size: 24rpx;
  color: #4A90D9;
  margin-bottom: 16rpx;
  display: block;
}

.slider-labels {
  display: flex;
  justify-content: space-between;
  margin-top: 16rpx;
}

.slider-label {
  font-size: 22rpx;
  color: #999999;
}

.setting-options {
  display: flex;
  gap: 24rpx;
}

.setting-option {
  flex: 1;
  padding: 16rpx;
  text-align: center;
  background-color: #F5F5F5;
  border-radius: 8rpx;
  font-size: 26rpx;

  &.active {
    background-color: #4A90D9;
    color: #FFFFFF;
  }

  &.bg-option {
    height: 48rpx;
    border-radius: 50%;
    padding: 0;
  }
}

.goto-container {
  display: flex;
  align-items: center;
  padding: 0 16rpx;
}

.goto-input {
  flex: 1;
  height: 80rpx;
  font-size: 32rpx;
  border: 2rpx solid #E8E8E8;
  border-radius: 8rpx;
  padding: 0 16rpx;
  text-align: center;
  background-color: #FFFFFF;
}

.goto-separator {
  font-size: 32rpx;
  color: #666666;
  margin-left: 16rpx;
}

.panel-footer {
  display: flex;
  gap: 24rpx;
  margin-top: 32rpx;
}

.panel-button {
  flex: 1;
  height: 88rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12rpx;
  font-size: 28rpx;

  &.cancel-button {
    background-color: #F5F5F5;
    color: #666666;
  }

  &.confirm-button {
    background-color: #4A90D9;
    color: #FFFFFF;
  }
}

.progress-text {
  cursor: pointer;
}
</style>
