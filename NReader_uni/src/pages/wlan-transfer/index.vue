<template>
  <view class="container">
    <view class="custom-nav">
      <view class="nav-back" @click="goBack">
        <text class="nav-back-icon">‹</text>
      </view>
      <text class="nav-title">WLAN传书</text>
      <view class="nav-placeholder"></view>
    </view>
    <view class="transfer-header">
      <text class="header-title">📶 连接同一WiFi</text>
      <text class="header-desc">确保手机和电脑连接到同一个局域网</text>
    </view>

    <view class="transfer-content">

      <view class="address-section">
        <text class="address-label">访问地址</text>
        <view class="address-box" @click="copyAddress">
          <text class="address-text">{{ transferAddress }}</text>
          <text class="copy-icon">📋</text>
        </view>
        <text class="address-hint">在电脑浏览器中打开此地址</text>
      </view>

      <view class="status-section">
        <view class="status-indicator" :class="statusClass"></view>
        <text class="status-text">{{ statusText }}</text>
      </view>

      <view v-if="isTransferring" class="progress-section">
        <text class="progress-filename">{{ currentFile }}</text>
        <view class="progress-bar">
          <view class="progress-fill" :style="{ width: `${transferProgress}%` }"></view>
        </view>
        <text class="progress-percent">{{ transferProgress }}%</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useBookStore } from '@/stores/bookStore'
import type { Book } from '@/types'
import { generateBookId, formatFileName, getFileExtension } from '@/utils/bookParser'

const bookStore = useBookStore()

const transferAddress = ref('')
const status = ref<'waiting' | 'transferring' | 'completed'>('waiting')
const transferProgress = ref(0)
const currentFile = ref('')

const statusClass = computed(() => {
  const map: Record<string, string> = {
    waiting: 'status-waiting',
    transferring: 'status-transferring',
    completed: 'status-completed'
  }
  return map[status.value]
})

const statusText = computed(() => {
  const map: Record<string, string> = {
    waiting: '等待连接...',
    transferring: '正在传输...',
    completed: '接收完成'
  }
  return map[status.value]
})

const isTransferring = computed(() => status.value === 'transferring')

function goBack() {
  uni.navigateBack()
}

function copyAddress() {
  if (!transferAddress.value) return
  
  if (navigator.clipboard) {
    navigator.clipboard.writeText(transferAddress.value).then(() => {
      uni.showToast({ title: '已复制', icon: 'success' })
    })
  } else {
    uni.showToast({ title: '复制失败', icon: 'none' })
  }
}

function startServer() {
  if (typeof window !== 'undefined') {
    transferAddress.value = 'http://localhost:8080'
    status.value = 'waiting'
    
    window.addEventListener('message', handleFileMessage)
    
    const uploadUrl = createUploadPage()
    setTimeout(() => {
      window.open(uploadUrl, '_blank')
    }, 500)
  } else {
    transferAddress.value = 'http://192.168.1.105:8080'
    status.value = 'waiting'
    uni.showToast({ title: '服务已启动', icon: 'success' })
  }
}

function handleFileMessage(e: MessageEvent) {
  if (e.data && e.data.type === 'file_selected') {
    const fileData = e.data.fileData
    if (fileData) {
      currentFile.value = fileData.name
      status.value = 'transferring'
      transferProgress.value = 0
      
      let progress = 0
      const interval = setInterval(() => {
        progress += Math.random() * 15
        if (progress >= 90) {
          progress = 90
          clearInterval(interval)
        }
        transferProgress.value = Math.round(progress)
      }, 200)
      
      setTimeout(() => {
        clearInterval(interval)
        transferProgress.value = 100
        
        const book: Book = {
          id: generateBookId(),
          title: formatFileName(fileData.name),
          author: '未知作者',
          cover: '',
          filePath: URL.createObjectURL(new Blob([fileData.content], { type: 'text/plain' })),
          format: 'txt',
          progress: 0,
          lastReadTime: new Date().toISOString()
        }
        
        localStorage.setItem(`book_content_${book.id}`, fileData.content)
        bookStore.addBook(book)
        
        status.value = 'completed'
        
        setTimeout(() => {
          uni.navigateBack()
        }, 3000)
      }, 1500)
    }
  }
}

function createUploadPage(): string {
  const htmlContent = `
<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>简阅 - WLAN传书</title>
  <style>
    * { margin: 0; padding: 0; box-sizing: border-box; }
    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: #F7F8FA; min-height: 100vh; display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 40px; }
    .upload-area { width: 100%; max-width: 500px; height: 300px; border: 3px dashed #4A90D9; border-radius: 20px; display: flex; flex-direction: column; align-items: center; justify-content: center; background: #FFFFFF; cursor: pointer; transition: all 0.3s; }
    .upload-area:hover, .upload-area.dragover { border-color: #6BA3E0; background: #F0F7FF; }
    .upload-icon { font-size: 64px; margin-bottom: 16px; }
    .upload-title { font-size: 20px; color: #333; margin-bottom: 8px; }
    .upload-desc { font-size: 14px; color: #999; }
    input[type="file"] { display: none; }
    .progress-bar { width: 100%; max-width: 500px; height: 8px; background: #E8E8E8; border-radius: 4px; margin-top: 24px; overflow: hidden; }
    .progress-fill { height: 100%; background: #4A90D9; border-radius: 4px; transition: width 0.3s; }
    .progress-text { font-size: 14px; color: #999; margin-top: 8px; }
    .success { color: #52C41A; font-size: 18px; margin-top: 16px; }
  </style>
</head>
<body>
  <div class="upload-area" id="uploadArea">
    <div class="upload-icon">📤</div>
    <div class="upload-title">拖拽文件到此处</div>
    <div class="upload-desc">或点击选择文件（支持txt/epub格式）</div>
  </div>
  <input type="file" id="fileInput" accept=".txt,.epub">
  <div class="progress-bar" id="progressBar" style="display: none;">
    <div class="progress-fill" id="progressFill"></div>
  </div>
  <div class="progress-text" id="progressText"></div>
  <div class="success" id="successMsg" style="display: none;">传输完成！书籍已添加到书架</div>
  
  <script>
    const uploadArea = document.getElementById('uploadArea');
    const fileInput = document.getElementById('fileInput');
    const progressBar = document.getElementById('progressBar');
    const progressFill = document.getElementById('progressFill');
    const progressText = document.getElementById('progressText');
    const successMsg = document.getElementById('successMsg');
    
    uploadArea.addEventListener('click', () => fileInput.click());
    
    uploadArea.addEventListener('dragover', (e) => { e.preventDefault(); uploadArea.classList.add('dragover'); });
    uploadArea.addEventListener('dragleave', () => uploadArea.classList.remove('dragover'));
    
    uploadArea.addEventListener('drop', (e) => {
      e.preventDefault();
      uploadArea.classList.remove('dragover');
      const file = e.dataTransfer.files[0];
      if (file) handleFile(file);
    });
    
    fileInput.addEventListener('change', (e) => {
      const file = (e.target).files[0];
      if (file) handleFile(file);
    });
    
    function handleFile(file) {
      progressBar.style.display = 'block';
      successMsg.style.display = 'none';
      progressFill.style.width = '0%';
      progressText.textContent = '正在上传...';
      
      const reader = new FileReader();
      reader.onload = () => {
        progressFill.style.width = '100%';
        progressText.textContent = '上传完成！';
        successMsg.style.display = 'block';
        
        if (window.opener) {
          window.opener.postMessage({
            type: 'file_selected',
            fileData: { name: file.name, content: reader.result }
          }, '*');
        }
      };
      
      let progress = 0;
      const interval = setInterval(() => {
        progress += Math.random() * 15;
        if (progress >= 90) { progress = 90; clearInterval(interval); }
        progressFill.style.width = progress + '%';
      }, 200);
      
      reader.onloadend = () => { clearInterval(interval); progressFill.style.width = '100%'; };
      reader.readAsText(file, 'utf-8');
    }
  <\/script>
</body>
</html>
  `
  
  const blob = new Blob([htmlContent], { type: 'text/html' })
  return URL.createObjectURL(blob)
}

onMounted(() => {
  startServer()
})

onUnmounted(() => {
  status.value = 'waiting'
  window.removeEventListener('message', handleFileMessage)
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

.transfer-header {
  padding: 32rpx;
  padding-top: calc(88rpx + var(--status-bar-height, 44px) + 32rpx);
  background-color: #FFFFFF;
}

.header-title {
  font-size: 32rpx;
  font-weight: 500;
  color: #333333;
  display: block;
  margin-bottom: 12rpx;
}

.header-desc {
  font-size: 26rpx;
  color: #999999;
}

.transfer-content {
  padding: 32rpx;
}

.guide-section {
  text-align: center;
  margin-bottom: 48rpx;
}

.guide-title {
  font-size: 32rpx;
  font-weight: 500;
  color: #333333;
  display: block;
  margin-bottom: 12rpx;
}

.guide-desc {
  font-size: 26rpx;
  color: #999999;
}

.address-section {
  background-color: #FFFFFF;
  border-radius: 16rpx;
  padding: 32rpx;
  margin-bottom: 48rpx;
}

.address-label {
  font-size: 26rpx;
  color: #999999;
  display: block;
  margin-bottom: 16rpx;
}

.address-box {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24rpx;
  background-color: #F7F8FA;
  border-radius: 12rpx;
  margin-bottom: 16rpx;
}

.address-text {
  font-size: 30rpx;
  color: #4A90D9;
  font-family: 'Courier New', monospace;
  word-break: break-all;
  flex: 1;
}

.copy-icon {
  font-size: 32rpx;
  margin-left: 16rpx;
}

.address-hint {
  font-size: 24rpx;
  color: #999999;
}

.status-section {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 48rpx;
}

.status-indicator {
  width: 16rpx;
  height: 16rpx;
  border-radius: 50%;
  margin-right: 16rpx;
  
  &.status-waiting {
    background-color: #FAAD14;
    animation: pulse 1.5s infinite;
  }
  
  &.status-transferring {
    background-color: #4A90D9;
    animation: pulse 1s infinite;
  }
  
  &.status-completed {
    background-color: #52C41A;
  }
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.status-text {
  font-size: 30rpx;
  color: #333333;
}

.progress-section {
  background-color: #FFFFFF;
  border-radius: 16rpx;
  padding: 32rpx;
}

.progress-filename {
  font-size: 28rpx;
  color: #333333;
  display: block;
  margin-bottom: 20rpx;
  word-break: break-all;
}

.progress-bar {
  height: 12rpx;
  background-color: #E8E8E8;
  border-radius: 6rpx;
  overflow: hidden;
  margin-bottom: 12rpx;
}

.progress-fill {
  height: 100%;
  background-color: #4A90D9;
  border-radius: 6rpx;
  transition: width 0.3s;
}

.progress-percent {
  font-size: 26rpx;
  color: #999999;
  display: block;
  text-align: right;
}
</style>
