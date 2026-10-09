<script setup lang="ts">
/** 订单状态标签（颜色映射 format.ts ORDER_STATUS） */

import { computed } from 'vue';
import { ORDER_STATUS } from '@/utils/format';

const props = defineProps<{ status: number }>();
const cls = computed(() => ORDER_STATUS[props.status]?.color ?? 'gray');
</script>

<template>
  <text class="tag" :class="cls">{{ ORDER_STATUS[status]?.text ?? '未知' }}</text>
</template>

<style scoped lang="scss">
.tag {
  font-size: 12px;
  padding: 2px 10px;
  border-radius: 99px;
  font-weight: 600;
}
.tag.amber {
  color: var(--amber-deep);
  background: var(--amber-soft);
}
.tag.green {
  color: var(--green);
  background: var(--green-soft);
}
.tag.red {
  color: var(--red);
  background: rgba(194, 84, 47, 0.12);
}
.tag.gray {
  color: var(--ink2);
  background: #f1ece2;
}
</style>
