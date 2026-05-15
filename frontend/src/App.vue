<template>
  <el-container class="layout">

    <!-- ── Navbar (hidden on welcome / login / register) ────────────────── -->
    <el-header
      v-if="showChrome"
      class="navbar"
    >
      <!-- Brand -->
      <router-link to="/" class="brand">
        <span class="brand-main">山大闲置淘</span>
        <span class="brand-sub">· 软件园校区</span>
        <span class="brand-ver">v2.0</span>
      </router-link>

      <!-- Right-side nav items -->
      <nav class="nav-right">
        <!-- Admin -->
        <template v-if="isAdmin">
          <router-link to="/admin" class="nav-link admin-link">管理后台</router-link>
        </template>

        <!-- Student -->
        <template v-if="isStudent">
          <router-link to="/auction-square" class="nav-link market-link auction-link">拍卖广场</router-link>
          <router-link to="/direct-market"  class="nav-link market-link direct-link">直购市场</router-link>
          <router-link to="/publish"        class="nav-link publish-link">发布商品</router-link>
          <router-link to="/profile"        class="nav-link account-link">个人中心</router-link>
        </template>

        <!-- Guest -->
        <template v-if="!isLoggedIn">
          <router-link to="/login" class="nav-link guest-link">登录</router-link>
        </template>

        <!-- Authenticated: balance + user chip + logout -->
        <template v-if="isLoggedIn">
          <span v-if="isStudent" class="balance-chip">
            ¥{{ currentUser.balance?.toFixed(2) ?? '0.00' }}
          </span>
          <router-link to="/profile" class="user-chip-link">
            <div class="user-chip-inner">
              <img
                v-if="currentUser.avatarUrl"
                :src="currentUser.avatarUrl"
                class="nav-avatar"
                :alt="currentUser.username"
              />
              <div v-else class="nav-avatar-fallback">
                {{ (currentUser.username ?? '?')[0].toUpperCase() }}
              </div>
              <span class="nav-username">{{ currentUser.username }}</span>
              <span class="role-badge">{{ isAdmin ? '管理员' : '学生' }}</span>
            </div>
          </router-link>
          <el-button size="small" plain class="logout-btn" @click="handleLogout">
            退出登录
          </el-button>
        </template>
      </nav>
    </el-header>

    <!-- ── Main content ──────────────────────────────────────────────────── -->
    <el-main :class="{ 'no-pad': !showChrome }">
      <div v-if="showChrome" class="page-content">
        <router-view />
      </div>
      <router-view v-else />
    </el-main>

    <!-- ── Global footer (same pages as navbar) ──────────────────────────── -->
    <el-footer v-if="showChrome" class="site-footer" height="auto">
      © 2026 山东大学软件学院 · 软件园校区共享空间
      <span class="footer-sep">|</span>
      技术支持：软件工程专业 伽马团队
      <span class="footer-sep">|</span>
      <em class="footer-motto">学无止境，气有浩然</em>
    </el-footer>

  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuth } from './composables/useAuth.js'

const router = useRouter()
const route  = useRoute()
const { currentUser, isLoggedIn, isAdmin, isStudent, logout } = useAuth()

// Show the persistent chrome (navbar + footer) on all app pages except the
// full-bleed welcome, login, and register screens.
const showChrome = computed(() =>
  route.path !== '/' && route.path !== '/login' && route.path !== '/register'
)

const handleLogout = () => {
  logout()
  router.push({ name: 'Login' })
}
</script>

<style>
/* ── Reset & base ── */
*, *::before, *::after { box-sizing: border-box; }
body {
  margin: 0;
  font-family: 'PingFang SC', 'Helvetica Neue', Helvetica, Arial, sans-serif;
}
.layout { min-height: 100vh; display: flex; flex-direction: column; }

/* ── Navbar ── */
.navbar {
  background: linear-gradient(90deg, #5a0000 0%, #800000 50%, #6b0000 100%);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 28px;
  box-shadow: 0 2px 12px rgba(0,0,0,.3);
  flex-shrink: 0;
}

/* ── Brand ── */
.brand {
  display: flex;
  align-items: baseline;
  gap: 4px;
  text-decoration: none;
  flex-shrink: 0;
}
.brand-main {
  color: #fff;
  font-size: 18px;
  font-weight: 800;
  letter-spacing: .5px;
}
.brand-sub {
  color: rgba(255,255,255,.65);
  font-size: 13px;
  font-weight: 500;
  letter-spacing: .3px;
}
.brand-ver {
  background: #D4AF37;
  color: #3d2800;
  font-size: 10px;
  font-weight: 700;
  padding: 1px 5px;
  border-radius: 3px;
  letter-spacing: .5px;
  margin-left: 2px;
  align-self: center;
}

/* ── Nav right ── */
.nav-right {
  display: flex;
  align-items: center;
  gap: 14px;
}
.nav-link {
  color: rgba(255,255,255,.85);
  text-decoration: none;
  font-size: 13px;
  font-weight: 500;
  border-radius: 4px;
  padding: 4px 12px;
  transition: background .18s, color .18s, border-color .18s;
}
.nav-link:hover { color: #fff; }

/* Active state: gold underline glow */
.nav-link.router-link-active {
  color: #D4AF37 !important;
  border-bottom: 2px solid #D4AF37;
}

.admin-link {
  background: rgba(212,175,55,.15);
  border: 1px solid rgba(212,175,55,.4);
  color: #D4AF37;
}
.admin-link:hover { background: rgba(212,175,55,.28); color: #f0d060; }
.admin-link.router-link-active { color: #f0d060 !important; border-bottom-color: #f0d060; }

.guest-link {
  background: rgba(255,255,255,.12);
  border: 1px solid rgba(255,255,255,.3);
}
.guest-link:hover { background: rgba(255,255,255,.22); }

.publish-link {
  background: rgba(255,255,255,.1);
  border: 1px solid rgba(255,255,255,.25);
}
.publish-link:hover { background: rgba(255,255,255,.2); color: #fff; }

.account-link {
  background: rgba(212,175,55,.1);
  border: 1px solid rgba(212,175,55,.3);
}
.account-link:hover { background: rgba(212,175,55,.22); color: #D4AF37; }

.market-link {
  background: rgba(255,255,255,.08);
  border: 1px solid rgba(255,255,255,.2);
}
.auction-link:hover,
.auction-link.router-link-active { background: rgba(255,255,255,.2); color: #fff; }
.direct-link:hover,
.direct-link.router-link-active  { background: rgba(212,175,55,.2); color: #D4AF37 !important; border-bottom-color: #D4AF37; }

/* Balance chip */
.balance-chip {
  background: rgba(212,175,55,.18);
  border: 1px solid rgba(212,175,55,.4);
  color: #D4AF37;
  font-size: 13px;
  font-weight: 700;
  padding: 3px 10px;
  border-radius: 12px;
}

/* User chip */
.user-chip-link {
  text-decoration: none;
  border-radius: 20px;
  border: 1px solid rgba(255,255,255,.25);
  padding: 3px 10px 3px 4px;
  display: flex;
  align-items: center;
  transition: background .2s;
}
.user-chip-link:hover { background: rgba(255,255,255,.1); }
.user-chip-inner { display: flex; align-items: center; gap: 6px; }
.nav-avatar {
  width: 26px; height: 26px;
  border-radius: 50%; object-fit: cover;
  border: 1px solid rgba(212,175,55,.5);
}
.nav-avatar-fallback {
  width: 26px; height: 26px;
  border-radius: 50%;
  background: rgba(212,175,55,.25);
  display: flex; align-items: center; justify-content: center;
  font-size: 13px; font-weight: 700; color: #D4AF37;
  flex-shrink: 0;
}
.nav-username { color: #fff; font-size: 13px; font-weight: 500; }
.role-badge   { color: rgba(212,175,55,.8); font-size: 11px; }

.logout-btn {
  border-color: rgba(255,255,255,.35) !important;
  color: rgba(255,255,255,.85) !important;
  background: transparent !important;
}
.logout-btn:hover {
  border-color: #D4AF37 !important;
  color: #D4AF37 !important;
  background: rgba(212,175,55,.1) !important;
}

/* ── Page content wrapper ── */
.no-pad { padding: 0; }
.page-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 8px 0 40px;
}

/* ── Site footer ── */
.site-footer {
  background: #3d0000;
  color: rgba(255,255,255,.5);
  font-size: 12px;
  text-align: center;
  padding: 14px 24px !important;
  letter-spacing: .5px;
  line-height: 1.8;
  flex-shrink: 0;
}
.footer-sep   { margin: 0 10px; opacity: .4; }
.footer-motto { font-style: italic; color: rgba(212,175,55,.6); }
</style>
