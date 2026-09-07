<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const title = computed(() => (typeof route.meta.title === 'string' ? route.meta.title : '该页面'))
</script>

<template>
  <el-card>
    <el-empty :description="`「${title}」为骨架占位页：接口契约见 docs/茶饮SaaS-接口需求.md，M3 按模块补齐`" />
  </el-card>
</template>
