<template>
  <div class="auth-root">

    <!-- Background ornament characters -->
    <span class="bg-char bg-char-1" aria-hidden="true">山</span>
    <span class="bg-char bg-char-2" aria-hidden="true">大</span>

    <div class="auth-card" :class="{ loaded }">

      <!-- ── Header ───────────────────────────────────────────────────── -->
      <div class="auth-header">
        <div class="sdu-seal">
          <span class="seal-char">山</span>
        </div>
        <h1 class="auth-title">山东大学</h1>
        <h2 class="auth-subtitle">统一身份认证系统</h2>
        <p class="auth-platform">山大闲置淘 · 软件园校区共享空间</p>
      </div>

      <!-- Divider -->
      <div class="auth-divider">
        <span class="divider-line"></span>
        <span class="divider-text">学生 / 管理员登录</span>
        <span class="divider-line"></span>
      </div>

      <!-- ── Form ─────────────────────────────────────────────────────── -->
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        size="large"
        class="auth-form"
      >
        <el-form-item label="用户名" prop="username">
          <el-input
            v-model="form.username"
            placeholder="请输入学号 / 管理员账号"
            clearable
          />
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
          class="btn-login"
          :loading="loading"
          @click="submit"
        >
          {{ loading ? '正在验证身份…' : '登 录' }}
        </el-button>
      </el-form>

      <!-- Register prompt -->
      <p class="register-prompt">
        还没有账号？
        <router-link to="/register" class="register-link">立即注册</router-link>
      </p>

      <!-- Footer note -->
      <p class="auth-footnote">
        软件学院软件工程专业实验项目演示版
        <span class="fn-sep">|</span>
        学无止境 气有浩然
      </p>

    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { loginApi } from '../api/user.js'
import { useAuth } from '../composables/useAuth.js'

const router             = useRouter()
const { login, isAdmin } = useAuth()

const formRef = ref(null)
const loading = ref(false)
const loaded  = ref(false)
const form    = ref({ username: '', password: '' })

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码',   trigger: 'blur' }]
}

onMounted(() => requestAnimationFrame(() => { loaded.value = true }))

const submit = async () => {
  try { await formRef.value.validate() } catch { return }

  loading.value = true
  try {
    const { token, user } = await loginApi(form.value.username, form.value.password)
    login(user, token)
    router.push(isAdmin.value ? { name: 'Admin' } : { name: 'Welcome' })
  } catch {
    // 401 already handled by the Axios interceptor (ElMessage.error)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
/* ── Root ── */
.auth-root {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(150deg, #1a0000 0%, #4a0000 45%, #800000 80%, #6b0000 100%);
  position: relative;
  overflow: hidden;
  padding: 24px;
}

/* Decorative bg ideograms */
.bg-char {
  position: absolute;
  font-size: 300px;
  font-weight: 900;
  color: rgba(255,255,255,.04);
  pointer-events: none;
  user-select: none;
  line-height: 1;
}
.bg-char-1 { top: -60px;  left: -40px; }
.bg-char-2 { bottom: -60px; right: -40px; }

/* ── Card ── */
.auth-card {
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 24px 80px rgba(0,0,0,.45), 0 2px 8px rgba(0,0,0,.2);
  width: 100%;
  max-width: 420px;
  padding: 40px 44px 32px;
  opacity: 0;
  transform: translateY(20px);
  transition: opacity .55s ease, transform .55s ease;
}
.auth-card.loaded { opacity: 1; transform: translateY(0); }

/* ── Header ── */
.auth-header { text-align: center; margin-bottom: 24px; }

.sdu-seal {
  width: 64px; height: 64px;
  border-radius: 50%;
  background: linear-gradient(135deg, #800000, #5a0000);
  border: 3px solid #D4AF37;
  display: flex; align-items: center; justify-content: center;
  margin: 0 auto 14px;
  box-shadow: 0 4px 16px rgba(128,0,0,.4);
}
.seal-char {
  color: #D4AF37;
  font-size: 28px;
  font-weight: 900;
}

.auth-title {
  margin: 0 0 2px;
  font-size: 22px;
  font-weight: 800;
  color: #800000;
  letter-spacing: 2px;
}
.auth-subtitle {
  margin: 0 0 6px;
  font-size: 14px;
  font-weight: 600;
  color: #606266;
  letter-spacing: 1px;
}
.auth-platform {
  margin: 0;
  font-size: 12px;
  color: #909399;
  letter-spacing: .5px;
}

/* ── Divider ── */
.auth-divider {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 0 0 20px;
}
.divider-line {
  flex: 1;
  height: 1px;
  background: linear-gradient(90deg, transparent, #e4e7ed, transparent);
}
.divider-text {
  font-size: 12px;
  color: #c0c4cc;
  white-space: nowrap;
  letter-spacing: .5px;
}

/* ── Form ── */
.auth-form { margin-bottom: 0; }

.btn-login {
  width: 100%;
  height: 46px;
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 3px;
  margin-top: 8px;
  background: linear-gradient(135deg, #800000, #a00000) !important;
  border-color: #800000 !important;
  border-radius: 8px !important;
  box-shadow: 0 4px 16px rgba(128,0,0,.35);
  transition: filter .2s, box-shadow .2s, transform .15s !important;
}
.btn-login:hover {
  filter: brightness(1.1);
  box-shadow: 0 6px 20px rgba(128,0,0,.5);
  transform: translateY(-1px);
}
.btn-login:active { transform: translateY(0); filter: brightness(.95); }

/* ── Register link ── */
.register-prompt {
  text-align: center;
  margin: 18px 0 0;
  font-size: 13px;
  color: #909399;
}
.register-link {
  color: #800000;
  font-weight: 600;
  text-decoration: none;
  margin-left: 4px;
}
.register-link:hover { text-decoration: underline; }

/* ── Footer note ── */
.auth-footnote {
  text-align: center;
  margin: 20px 0 0;
  font-size: 11px;
  color: #c0c4cc;
  letter-spacing: .3px;
  line-height: 1.6;
  border-top: 1px solid #f5f5f5;
  padding-top: 16px;
}
.fn-sep { margin: 0 6px; }
</style>
