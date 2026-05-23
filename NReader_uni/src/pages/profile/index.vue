<template>
  <view class="container">
    <view class="profile-card">
      <view class="avatar" @click="changeNickname">
        <text class="avatar-icon">👤</text>
      </view>
      <view class="user-info">
        <text class="nickname">{{ nickname }}</text>
        <text class="desc">享受阅读的乐趣</text>
      </view>
    </view>

    <view class="menu-section">
      <view class="menu-item" @click="openSettings">
        <view class="menu-icon-wrapper">
          <text class="menu-icon">⚙️</text>
        </view>
        <view class="menu-content">
          <text class="menu-title">阅读偏好</text>
          <text class="menu-desc">翻页方式、字号、背景色</text>
        </view>
        <text class="menu-arrow">›</text>
      </view>

      <view class="menu-item" @click="clearCache">
        <view class="menu-icon-wrapper">
          <text class="menu-icon">🗑️</text>
        </view>
        <view class="menu-content">
          <text class="menu-title">数据管理</text>
          <text class="menu-desc">清除缓存数据</text>
        </view>
        <text class="menu-arrow">›</text>
      </view>

      <view class="menu-item" @click="showAbout">
        <view class="menu-icon-wrapper">
          <text class="menu-icon">ℹ️</text>
        </view>
        <view class="menu-content">
          <text class="menu-title">关于我们</text>
          <text class="menu-desc">版本号 v1.0.0</text>
        </view>
        <text class="menu-arrow">›</text>
      </view>
    </view>

    <view class="footer">
      <text class="footer-text">简阅 - 纯粹、无广告的私人小说阅读器</text>
    </view>

    <TabBar />
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getNickname, saveNickname, clearCache } from '@/utils/storage'
import TabBar from '@/components/TabBar.vue'

const nickname = ref(getNickname())

function changeNickname() {
  uni.showModal({
    title: '修改昵称',
    editable: true,
    placeholderText: '请输入昵称',
    success: (res) => {
      if (res.confirm && res.content) {
        nickname.value = res.content
        saveNickname(res.content)
        uni.showToast({ title: '修改成功', icon: 'success' })
      }
    }
  })
}

function openSettings() {
  uni.navigateTo({
    url: '/pages/settings/index'
  })
}

function clearCache() {
  uni.showModal({
    title: '清除缓存',
    content: '确定要清除所有缓存数据吗？此操作不可恢复。',
    success: (res) => {
      if (res.confirm) {
        clearCache()
        uni.showToast({ title: '清除成功', icon: 'success' })
      }
    }
  })
}

function showAbout() {
  uni.showModal({
    title: '关于简阅',
    content: '简阅 v1.0.0\n\n纯粹、无广告的私人小说阅读器\n本地阅读 + 局域网轻松传书',
    showCancel: false
  })
}

onMounted(() => {
  console.log('我的页面加载')
})
</script>

<style lang="scss" scoped>
.container {
  min-height: 100vh;
  background-color: #F7F8FA;
  padding-bottom: 120rpx;
}

.profile-card {
  display: flex;
  align-items: center;
  background: linear-gradient(135deg, #4A90D9 0%, #6BA3E0 100%);
  padding: 48rpx 32rpx;
  padding-top: calc(48rpx + var(--status-bar-height, 44px));
  margin: 24rpx;
  margin-top: calc(24rpx + var(--status-bar-height, 44px));
  border-radius: 24rpx;
}

.avatar {
  width: 120rpx;
  height: 120rpx;
  background-color: rgba(255, 255, 255, 0.2);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 24rpx;
}

.avatar-icon {
  font-size: 48rpx;
}

.user-info {
  flex: 1;
}

.nickname {
  font-size: 36rpx;
  font-weight: 600;
  color: #FFFFFF;
  display: block;
  margin-bottom: 8rpx;
}

.desc {
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.8);
}

.menu-section {
  margin: 24rpx;
  background-color: #FFFFFF;
  border-radius: 16rpx;
  overflow: hidden;
}

.menu-item {
  display: flex;
  align-items: center;
  padding: 32rpx;
  border-bottom: 2rpx solid #F7F8FA;
  
  &:last-child {
    border-bottom: none;
  }
  
  &:active {
    background-color: #F7F8FA;
  }
}

.menu-icon-wrapper {
  width: 64rpx;
  height: 64rpx;
  background-color: #F0F4F8;
  border-radius: 16rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 24rpx;
}

.menu-icon {
  font-size: 32rpx;
}

.menu-content {
  flex: 1;
}

.menu-title {
  font-size: 30rpx;
  color: #333333;
  display: block;
  margin-bottom: 8rpx;
}

.menu-desc {
  font-size: 24rpx;
  color: #999999;
}

.menu-arrow {
  font-size: 36rpx;
  color: #CCCCCC;
}

.footer {
  text-align: center;
  padding: 48rpx;
}

.footer-text {
  font-size: 24rpx;
  color: #999999;
}
</style>
