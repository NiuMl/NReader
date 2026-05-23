<template>
  <view class="container">
    <view class="custom-nav">
      <view class="nav-back" @click="goBack">
        <text class="nav-back-icon">‹</text>
      </view>
      <text class="nav-title">阅读设置</text>
      <view class="nav-placeholder"></view>
    </view>
    <view class="settings-header"></view>

    <view class="settings-content">
      <view class="setting-section">
        <text class="section-title">翻页方式</text>
        <view class="options-row">
          <view
            v-for="mode in pageModeOptions"
            :key="mode.value"
            class="option-card"
            :class="{ active: readingSettings.pageMode === mode.value }"
            @click="updateSetting('pageMode', mode.value)"
          >
            <text class="option-icon">{{ mode.icon }}</text>
            <text class="option-text">{{ mode.label }}</text>
          </view>
        </view>
      </view>

      <view class="setting-section">
        <text class="section-title">字号大小</text>
        <view class="options-row">
          <view
            v-for="size in fontSizeOptions"
            :key="size.value"
            class="option-card"
            :class="{ active: readingSettings.fontSize === size.value }"
            @click="updateSetting('fontSize', size.value)"
          >
            <text class="option-text" :class="size.value">{{ size.label }}</text>
          </view>
        </view>
      </view>

      <view class="setting-section">
        <text class="section-title">行间距</text>
        <view class="options-row">
          <view
            v-for="spacing in lineSpacingOptions"
            :key="spacing.value"
            class="option-card"
            :class="{ active: readingSettings.lineSpacing === spacing.value }"
            @click="updateSetting('lineSpacing', spacing.value)"
          >
            <text class="option-text">{{ spacing.label }}</text>
          </view>
        </view>
      </view>

      <view class="setting-section">
        <text class="section-title">背景颜色</text>
        <view class="options-row">
          <view
            v-for="bg in backgroundColorOptions"
            :key="bg.value"
            class="option-card bg-card"
            :class="{ active: readingSettings.backgroundColor === bg.value }"
            :style="{ backgroundColor: bg.color, borderColor: readingSettings.backgroundColor === bg.value ? '#4A90D9' : 'transparent' }"
            @click="updateSetting('backgroundColor', bg.value)"
          >
            <text class="option-text">{{ bg.label }}</text>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useBookStore } from '@/stores/bookStore'
import type { ReadingSettings } from '@/types'

const bookStore = useBookStore()

const readingSettings = computed(() => bookStore.readingSettings)

const pageModeOptions = [
  { value: 'scroll', label: '上下滚动', icon: '⬆️' },
  { value: 'slide', label: '左右滑动', icon: '➡️' },
  { value: 'click', label: '点击翻页', icon: '👆' }
]

const fontSizeOptions = [
  { value: 'small', label: '小' },
  { value: 'medium', label: '中' },
  { value: 'large', label: '大' },
  { value: 'xlarge', label: '超大' }
]

const lineSpacingOptions = [
  { value: 'compact', label: '紧凑' },
  { value: 'normal', label: '适中' },
  { value: 'relaxed', label: '宽松' }
]

const backgroundColorOptions = [
  { value: 'white', label: '白色', color: '#FFFFFF' },
  { value: 'cream', label: '米黄', color: '#FDF6E3' },
  { value: 'dark', label: '深色', color: '#2C2C2C' }
]

function goBack() {
  uni.navigateBack()
}

function updateSetting(key: keyof ReadingSettings, value: string) {
  bookStore.updateSettings({ [key]: value })
}

onMounted(() => {
  console.log('设置页面加载')
})
</script>

<style lang="scss" scoped>
.container {
  min-height: 100vh;
  background-color: #F7F8FA;
}

.custom-nav {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 88rpx;
  padding-top: var(--status-bar-height, 44px);
  background-color: #FFFFFF;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-left: 24rpx;
  padding-right: 24rpx;
  z-index: 100;
}

.nav-back {
  width: 64rpx;
  height: 64rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.nav-back-icon {
  font-size: 48rpx;
  color: #333333;
}

.nav-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #333333;
}

.nav-placeholder {
  width: 64rpx;
}

.settings-header {
  height: calc(88rpx + var(--status-bar-height, 44px));
}

.settings-content {
  padding: 24rpx;
}

.setting-section {
  background-color: #FFFFFF;
  border-radius: 16rpx;
  padding: 32rpx;
  margin-bottom: 24rpx;
}

.section-title {
  font-size: 28rpx;
  color: #999999;
  display: block;
  margin-bottom: 24rpx;
}

.options-row {
  display: flex;
  flex-wrap: wrap;
  gap: 20rpx;
}

.option-card {
  flex: 1;
  min-width: calc(33.333% - 14rpx);
  padding: 24rpx;
  background-color: #F7F8FA;
  border-radius: 12rpx;
  text-align: center;
  border: 2rpx solid transparent;
  transition: all 0.3s;
  
  &.active {
    background-color: #E8F0FE;
    border-color: #4A90D9;
  }
}

.option-icon {
  font-size: 36rpx;
  display: block;
  margin-bottom: 8rpx;
}

.option-text {
  font-size: 26rpx;
  color: #333333;
  
  &.small { font-size: 22rpx; }
  &.xlarge { font-size: 32rpx; }
  
  .active & {
    color: #4A90D9;
  }
}

.bg-card {
  min-width: calc(33.333% - 14rpx);
  
  .option-text {
    color: #666666;
    
    .active & {
      color: #4A90D9;
      font-weight: 500;
    }
  }
}
</style>
