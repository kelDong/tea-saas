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
    <div class="login-panel">
      <div class="login-brand">
        <div class="brand-logo">🍵</div>
        <div>
          <div class="brand-name">茶饮 SaaS 管理后台</div>
          <div class="brand-sub">连锁加盟 · 一站式经营</div>
        </div>
      </div>
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
      <div class="login-foot">茶饮 SaaS · 山茶制造</div>
    </div>
  </div>
</template>

<style scoped>
/* 深茶棕渐变画布 + 琥珀微光 */
.login-page {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  background:
    radial-gradient(600px 320px at 70% 18%, rgba(217, 123, 41, 0.18), transparent 70%),
    linear-gradient(135deg, #3a2b1e 0%, #2e251b 100%);
}

.login-panel {
  width: 400px;
  padding: 32px 32px 24px;
  background: var(--ts-paper);
  border-radius: var(--ts-radius-dialog);
  box-shadow: 0 16px 48px rgba(42, 32, 22, 0.35);
}

.login-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 26px;
}

.brand-logo {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: var(--ts-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  flex-shrink: 0;
}

.brand-name {
  font-size: 18px;
  font-weight: 700;
  color: var(--ts-ink);
  line-height: 1.3;
}

.brand-sub {
  font-size: 12px;
  color: var(--ts-ink-2);
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
  border-radius: 6px;
}

.login-submit {
  width: 100%;
}

.login-foot {
  margin-top: 4px;
  text-align: center;
  font-size: 12px;
  color: var(--ts-ink-3);
}
</style>
