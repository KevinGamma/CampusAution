<template>
  <div class="welcome-root">
    <!-- ── Hero ──────────────────────────────────────────────────────────── -->
    <div class="hero" :class="{ visible: show }">
      <!-- Watermark ideogram -->
      <span class="hero-watermark" aria-hidden="true">山</span>

      <div class="hero-inner">
        <!-- School identity -->
        <p class="hero-school">山东大学 · 软件学院</p>

        <!-- Platform name -->
        <h1 class="hero-title">山大闲置淘</h1>
        <p class="hero-subtitle">软件园校区闲置资源共享空间</p>

        <!-- Motto -->
        <div class="motto-wrap">
          <span class="motto-bar"></span>
          <p class="motto-text">学无止境，气有浩然</p>
          <span class="motto-bar"></span>
        </div>
      </div>
    </div>

    <!-- ── Entrance Card ──────────────────────────────────────────────────── -->
    <div class="card-zone" :class="{ visible: show }">
      <div class="entrance-card">
        <!-- Not logged in -->
        <template v-if="!isLoggedIn">
          <p class="card-label">欢迎来到山大闲置淘</p>
          <p class="card-hint">请登录以开始浏览和交易</p>
          <router-link to="/login" class="btn btn-primary">
            有卡号？即刻登录
          </router-link>
          <router-link to="/register" class="btn btn-ghost">
            新用户注册
          </router-link>
        </template>

        <!-- Student -->
        <template v-else-if="isStudent">
          <p class="card-label">欢迎回来，{{ currentUser.username }}</p>
          <p class="card-hint">选择你想前往的市场</p>
          <div class="btn-grid">
            <router-link to="/auction-square" class="btn btn-primary btn-block">
              <span class="btn-icon">🔨</span>
              <span class="btn-text">
                <strong>前往拍卖广场</strong>
                <small>参与竞拍，赢取心仪好物</small>
              </span>
            </router-link>
            <router-link to="/direct-market" class="btn btn-gold btn-block">
              <span class="btn-icon">🛒</span>
              <span class="btn-text">
                <strong>进入直购市场</strong>
                <small>直接购买，无需等待</small>
              </span>
            </router-link>
          </div>
        </template>

        <!-- Admin -->
        <template v-else-if="isAdmin">
          <p class="card-label">管理员，您好</p>
          <p class="card-hint">进入数据管理与审计系统</p>
          <router-link to="/admin" class="btn btn-admin btn-block">
            <span class="btn-icon">⚙️</span>
            <span class="btn-text">
              <strong>进入数据管理与审计中心</strong>
              <small>山东大学校园集市管理后台</small>
            </span>
          </router-link>
        </template>
      </div>
    </div>

    <!-- ── Footer strip ───────────────────────────────────────────────────── -->
    <footer class="welcome-footer" :class="{ visible: show }">
      山东大学软件学院 · 校园集市共享平台 &copy; {{ new Date().getFullYear() }}
    </footer>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useAuth } from '../composables/useAuth.js'

const { currentUser, isLoggedIn, isStudent, isAdmin } = useAuth()

const show = ref(false)
onMounted(() => {
  // Tiny delay lets the CSS transition fire after the element is in the DOM
  requestAnimationFrame(() => { show.value = true })
})
</script>

<style scoped>
/* ── Root ── */
.welcome-root {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  background: linear-gradient(160deg, #1a0000 0%, #3d0000 40%, #800000 75%, #a33000 100%);
  position: relative;
  overflow: hidden;
}

/* ── Subtle radial glow behind hero ── */
.welcome-root::before {
  content: '';
  position: absolute;
  inset: 0;
  background: radial-gradient(ellipse 70% 55% at 50% 38%, rgba(255,200,100,.08) 0%, transparent 70%);
  pointer-events: none;
}

/* ── Hero ── */
.hero {
  position: relative;
  width: 100%;
  text-align: center;
  padding: 80px 24px 40px;
  opacity: 0;
  transform: translateY(-24px);
  transition: opacity .7s ease, transform .7s ease;
}
.hero.visible { opacity: 1; transform: translateY(0); }

.hero-watermark {
  position: absolute;
  top: 10px;
  left: 50%;
  transform: translateX(-50%);
  font-size: 340px;
  font-weight: 900;
  color: rgba(255,255,255,.04);
  line-height: 1;
  pointer-events: none;
  user-select: none;
  letter-spacing: -10px;
}

.hero-inner { position: relative; z-index: 1; }

.hero-school {
  margin: 0 0 18px;
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 4px;
  color: rgba(255,210,120,.75);
  text-transform: uppercase;
}

.hero-title {
  margin: 0;
  font-size: clamp(48px, 8vw, 80px);
  font-weight: 900;
  color: #fff;
  letter-spacing: 6px;
  text-shadow: 0 4px 32px rgba(0,0,0,.4);
}

.hero-subtitle {
  margin: 14px 0 0;
  font-size: clamp(14px, 2.2vw, 18px);
  color: rgba(255,255,255,.7);
  letter-spacing: 3px;
}

/* Motto */
.motto-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 18px;
  margin-top: 36px;
}
.motto-bar {
  flex: 0 0 48px;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(255,210,100,.6), transparent);
}
.motto-text {
  margin: 0;
  font-size: clamp(16px, 2.5vw, 22px);
  font-weight: 700;
  color: rgba(255,210,100,.95);
  letter-spacing: 5px;
  font-style: italic;
  white-space: nowrap;
}

/* ── Entrance Card ── */
.card-zone {
  flex: 1;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding: 8px 24px 48px;
  width: 100%;
  opacity: 0;
  transform: translateY(24px);
  transition: opacity .7s ease .2s, transform .7s ease .2s;
}
.card-zone.visible { opacity: 1; transform: translateY(0); }

.entrance-card {
  background: rgba(255,255,255,.97);
  border-radius: 16px;
  box-shadow: 0 24px 64px rgba(0,0,0,.35), 0 2px 8px rgba(0,0,0,.2);
  padding: 40px 48px;
  width: 100%;
  max-width: 520px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  text-align: center;
}

.card-label {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: #800000;
}
.card-hint {
  margin: 0;
  font-size: 14px;
  color: #909399;
}

/* ── Buttons ── */
.btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  width: 100%;
  padding: 14px 20px;
  border-radius: 10px;
  text-decoration: none;
  font-size: 15px;
  font-weight: 600;
  transition: filter .18s, transform .18s, box-shadow .18s;
  cursor: pointer;
  border: none;
}
.btn:hover {
  filter: brightness(1.08);
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0,0,0,.18);
}
.btn:active { transform: translateY(0); filter: brightness(.96); }

.btn-primary {
  background: #800000;
  color: #fff;
  box-shadow: 0 2px 8px rgba(128,0,0,.4);
}
.btn-ghost {
  background: transparent;
  color: #800000;
  border: 2px solid rgba(128,0,0,.35);
}
.btn-ghost:hover { background: rgba(128,0,0,.06); }

.btn-gold {
  background: linear-gradient(135deg, #b8860b 0%, #daa520 100%);
  color: #fff;
  box-shadow: 0 2px 8px rgba(184,134,11,.4);
}
.btn-admin {
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
  color: #fff;
  box-shadow: 0 2px 8px rgba(0,0,0,.4);
}

.btn-block {
  justify-content: flex-start;
  text-align: left;
}

.btn-grid { display: flex; flex-direction: column; gap: 12px; width: 100%; }

.btn-icon { font-size: 22px; flex-shrink: 0; }
.btn-text { display: flex; flex-direction: column; gap: 2px; }
.btn-text strong { font-size: 15px; }
.btn-text small { font-size: 12px; opacity: .8; font-weight: 400; }

/* ── Footer ── */
.welcome-footer {
  padding: 18px 24px;
  font-size: 12px;
  color: rgba(255,255,255,.4);
  letter-spacing: 1px;
  text-align: center;
  opacity: 0;
  transition: opacity .7s ease .4s;
}
.welcome-footer.visible { opacity: 1; }

/* ── Responsive ── */
@media (max-width: 480px) {
  .entrance-card { padding: 32px 24px; }
  .hero { padding: 60px 16px 32px; }
  .motto-bar { flex: 0 0 24px; }
}
</style>
