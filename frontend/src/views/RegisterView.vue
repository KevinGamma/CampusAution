<template>
  <div class="register-wrap">
    <el-card class="register-card" shadow="always">
      <template #header>
        <div class="register-header">
          <div class="register-logo">🎓</div>
          <h2 class="register-title">注册账号</h2>
          <p class="register-sub">校园拍卖</p>
        </div>
      </template>

      <el-alert
        title="您正在注册学生账号。"
        description="管理员账号由系统预置，无法在此注册。"
        type="info"
        :closable="false"
        show-icon
        class="role-notice"
      />

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
          />
        </el-form-item>

        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            placeholder="请再次输入密码"
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
          注册学生账号
        </el-button>
      </el-form>

      <div class="login-prompt">
        已有账号？
        <router-link to="/login" class="login-link">立即登录</router-link>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { registerApi } from '../api/user.js'

const router  = useRouter()
const formRef = ref(null)
const loading = ref(false)
const form    = ref({ username: '', password: '', confirmPassword: '' })

const validateConfirm = (_, value, callback) => {
  if (!value) {
    callback(new Error('请确认密码'))
  } else if (value !== form.value.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 50, message: '用户名须为 3–50 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, validator: validateConfirm, trigger: 'blur' }
  ]
}

const submit = async () => {
  try { await formRef.value.validate() } catch { return }

  loading.value = true
  try {
    await registerApi(form.value.username, form.value.password)
    ElMessage.success('注册成功！请登录。')
    router.push('/login')
  } catch {
    // 409 "Username already exists" is shown automatically by the Axios interceptor
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-wrap {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1d3557 0%, #457b9d 55%, #a8dadc 100%);
}
.register-card   { width: 440px; border-radius: 16px; }
.register-header { text-align: center; padding: 12px 0 4px; }
.register-logo   { font-size: 36px; line-height: 1; margin-bottom: 10px; }
.register-title  { margin: 0 0 4px; font-size: 26px; font-weight: 700; color: #1d3557; }
.register-sub    { margin: 0; color: #909399; font-size: 14px; }
.role-notice     { margin-bottom: 20px; }
.btn-submit      { width: 100%; margin-top: 8px; font-size: 16px; height: 44px; }
.login-prompt    { text-align: center; margin-top: 16px; font-size: 13px; color: #606266; }
.login-link      { color: #457b9d; font-weight: 600; text-decoration: none; margin-left: 4px; }
.login-link:hover { text-decoration: underline; }
</style>
