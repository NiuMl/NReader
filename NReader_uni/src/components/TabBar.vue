<template>
  <view class="custom-tabbar">
    <view
      v-for="(item, index) in tabList"
      :key="index"
      class="tab-item"
      :class="{ active: currentIndex === index }"
      @click="switchTab(index)"
    >
      <text class="tab-icon">{{ item.icon }}</text>
      <text class="tab-text">{{ item.text }}</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { onShow } from '@dcloudio/uni-app'

const tabList = [
  { pagePath: '/pages/bookshelf/index', text: '书架', icon: '📚' },
  { pagePath: '/pages/library/index', text: '书库', icon: '📖' },
  { pagePath: '/pages/profile/index', text: '我的', icon: '👤' }
]

const currentIndex = ref(0)

function getCurrentPageIndex() {
  const pages = getCurrentPages()
  if (pages.length > 0) {
    const currentPage = pages[pages.length - 1]
    let route = '/' + currentPage.route
    if (route.endsWith('.vue')) {
      route = route.substring(0, route.length - 4)
    }
    const index = tabList.findIndex(item => item.pagePath === route || route.startsWith(item.pagePath))
    if (index !== -1) {
      currentIndex.value = index
    }
  }
}

function switchTab(index: number) {
  currentIndex.value = index
  uni.switchTab({
    url: tabList[index].pagePath
  })
}

onMounted(() => {
  getCurrentPageIndex()
})

onShow(() => {
  getCurrentPageIndex()
})
</script>

<style lang="scss" scoped>
.custom-tabbar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: 100rpx;
  background-color: #FFFFFF;
  display: flex;
  align-items: center;
  justify-content: space-around;
  box-shadow: 0 -2rpx 12rpx rgba(0, 0, 0, 0.04);
  padding-bottom: env(safe-area-inset-bottom);
  z-index: 999;
}

.tab-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 12rpx 0;
}

.tab-icon {
  font-size: 40rpx;
  margin-bottom: 8rpx;
}

.tab-text {
  font-size: 22rpx;
  color: #999999;
}

.tab-item.active {
  .tab-icon {
    transform: scale(1.1);
  }
  
  .tab-text {
    color: #4A90D9;
    font-weight: 500;
  }
}
</style>
