<script setup lang="ts">
/** 空态（列表三态之一，工程规范 §11） */

defineProps<{ text?: string }>();
</script>

<template>
  <view class="empty-wrap">
    <view class="ico">🫗</view>
    <view class="txt">{{ text ?? '暂无数据' }}</view>
  </view>
</template>

<style scoped lang="scss">
.empty-wrap {
  text-align: center;
  padding: 48px 0;
}
.ico {
  font-size: 36px;
  opacity: 0.5;
}
.txt {
  color: var(--ink2);
  font-size: 13px;
  margin-top: 10px;
}
</style>
