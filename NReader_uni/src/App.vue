<script setup lang="ts">
import { onLaunch, onShow, onHide } from '@dcloudio/uni-app'
import { clearBookshelf } from '@/utils/storage'

onLaunch(() => {
  console.log('App Launch')
  // 清空原有书架数据，使用本地小说列表
  clearBookshelf()
})

onShow(() => {
  console.log('App Show')
})

onHide(() => {
  console.log('App Hide')
})
</script>

<style lang="scss">
@use './styles/variables.scss' as *;

page {
  background-color: $bg-color;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  color: $text-primary;
  font-size: 28rpx;
  line-height: 1.5;
}

.container {
  min-height: 100vh;
  background-color: $bg-color;
}

.card {
  background-color: $card-color;
  border-radius: 16rpx;
  padding: 32rpx;
  margin: 24rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);
}

.btn-primary {
  background-color: $primary-color;
  color: #FFFFFF;
  border-radius: 40rpx;
  padding: 20rpx 48rpx;
  font-size: 28rpx;
  text-align: center;
}

.btn-secondary {
  background-color: $bg-color;
  color: $text-primary;
  border: 2rpx solid $border-color;
  border-radius: 40rpx;
  padding: 20rpx 48rpx;
  font-size: 28rpx;
  text-align: center;
}
</style>
