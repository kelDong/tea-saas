<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'

import { authApi } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'
import type { CaptchaVO } from '@/types/api'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const captcha = ref<CaptchaVO | null>(null)

const form = reactive({
  username: '',
  password: '',
  captchaCode: '',
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

onMounted(refreshCaptcha)

/** 验证码一期可选：拉取失败（如本地未起 Redis）不阻塞账号密码登录 */
async function refreshCaptcha() {
  try {
    captcha.value = await authApi.captcha()
  } catch {
    captcha.value = null
  }
}

async function handleLogin() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }
  loading.value = true
  try {
    await auth.login({
      username: form.username,
      password: form.password,
      captchaKey: captcha.value?.key,
      captchaCode: captcha.value ? form.captchaCode || undefined : undefined,
    })
    ElMessage.success('登录成功')
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
    router.push(redirect)
  } catch {
    // 错误提示已由 request.ts 统一弹出；刷新验证码重试
    refreshCaptcha()
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <el-card class="login-card">
      <h2 class="login-title">茶饮 SaaS 管理后台</h2>
      <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="handleLogin">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" autocomplete="username" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            show-password
            autocomplete="current-password"
          />
        </el-form-item>
        <el-form-item v-if="captcha">
          <div class="login-captcha">
            <el-input v-model="form.captchaCode" placeholder="验证码" maxlength="4" />
            <img
              v-if="captcha"
              class="login-captcha-img"
              :src="captcha.image"
              alt="验证码"
              title="点击刷新"
              @click="refreshCaptcha"
            />
          </div>
        </el-form-item>
        <el-form-item>
          <el-button class="login-submit" type="primary" :loading="loading" @click="handleLogin">
            登 录
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.login-page {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  background: linear-gradient(135deg, #1f3b73 0%, #0f1e3d 100%);
}

.login-card {
  width: 400px;
  border-radius: 8px;
}

.login-title {
  margin: 8px 0 24px;
  text-align: center;
  color: #303133;
}

.login-captcha {
  display: flex;
  gap: 8px;
  width: 100%;
}

.login-captcha-img {
  height: 40px;
  width: 120px;
  cursor: pointer;
  border-radius: 4px;
}

.login-submit {
  width: 100%;
}
</style>
