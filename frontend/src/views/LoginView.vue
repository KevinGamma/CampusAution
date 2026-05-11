<template>
  <div class="login-wrap">
    <el-card class="login-card" shadow="always">
      <template #header>
        <div class="login-header">
          <div class="login-logo">🎓</div>
          <h2 class="login-title">校园拍卖</h2>
          <p class="login-sub">登录以继续</p>
          <p class="login-tagline">校园二手好物，尽在这里。</p>
        </div>
      </template>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" size="large">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" clearable />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            show-password
            @keyup.enter="submit"
          />
        </el-form-item>

        <el-button
          type="primary"
          class="btn-submit"
          :loading="loading"
          @click="submit"
        >
          登录
        </el-button>
      </el-form>

      <div class="register-prompt">
        还没有账号？
        <router-link to="/register" class="register-link">立即注册</router-link>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { loginApi } from '../api/user.js'
import { useAuth } from '../composables/useAuth.js'

const router              = useRouter()
const { login, isAdmin }  = useAuth()

const formRef = ref(null)
const loading = ref(false)
const form    = ref({ username: '', password: '' })

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const submit = async () => {
  try { await formRef.value.validate() } catch { return }

  loading.value = true
  try {
    const { token, user } = await loginApi(form.value.username, form.value.password)
    login(user, token)
    router.push(isAdmin.value ? '/admin/dashboard' : '/')
  } catch {
    // 401 already handled by the Axios interceptor (ElMessage.error)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-wrap {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1d3557 0%, #457b9d 55%, #a8dadc 100%);
}
.login-card    { width: 420px; border-radius: 16px; }
.login-header  { text-align: center; padding: 12px 0 4px; }
.login-logo    { font-size: 36px; line-height: 1; margin-bottom: 10px; }
.login-title   { margin: 0 0 4px; font-size: 26px; font-weight: 700; color: #1d3557; }
.login-sub     { margin: 0 0 4px; color: #606266; font-size: 14px; font-weight: 500; }
.login-tagline { margin: 0; color: #909399; font-size: 12px; }
.btn-submit      { width: 100%; margin-top: 8px; font-size: 16px; height: 44px; }
.register-prompt { text-align: center; margin-top: 16px; font-size: 13px; color: #606266; }
.register-link   { color: #457b9d; font-weight: 600; text-decoration: none; margin-left: 4px; }
.register-link:hover { text-decoration: underline; }
</style>
