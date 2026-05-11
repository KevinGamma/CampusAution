import axios from 'axios'
import { ElMessage, ElNotification } from 'element-plus'

const http = axios.create({ timeout: 10_000 })

// ── Request interceptor ────────────────────────────────────────────────────
// Attach the JWT from localStorage so every API call is authenticated.
// The token is read fresh on every request so it stays current after login.
http.interceptors.request.use(config => {
  const token = localStorage.getItem('ca_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// ── Response interceptor ───────────────────────────────────────────────────
// Success path: unwrap the Axios envelope and return the server's Result<T>
//               so callers receive { code, msg, data } directly.
// Error path:   map each HTTP status to an appropriate Element Plus alert
//               so every component gets global error feedback for free.
http.interceptors.response.use(
  response => response.data,

  error => {
    const status = error.response?.status
    const msg    = error.response?.data?.msg ?? '发生未知错误，请稍后重试'

    switch (status) {
      case 401:
        ElMessage({ type: 'error', message: msg, duration: 4000 })
        break
      case 403:
        ElMessage({ type: 'error', message: msg, duration: 4000 })
        break
      case 409:
        ElMessage({ type: 'warning', message: msg, duration: 4000 })
        break
      case 400:
        if (error.response?.data?.msg === 'Insufficient balance') {
          ElMessage({ type: 'error', message: '余额不足，请充值后再试！', duration: 4000 })
        } else {
          ElMessage({ type: 'error', message: msg, duration: 3000 })
        }
        break
      case 404:
        ElMessage({ type: 'error', message: '请求的资源不存在', duration: 3000 })
        break
      case 500:
        ElNotification({ title: '服务器错误', message: msg, type: 'error', duration: 5000 })
        break
      default:
        ElMessage({ type: 'error', message: msg, duration: 3000 })
    }

    return Promise.reject(error)
  }
)

export default http
