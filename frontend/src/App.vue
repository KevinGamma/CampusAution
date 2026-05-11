<template>
  <el-container class="layout">
    <el-header v-if="route.path !== '/login' && route.path !== '/register'" class="navbar">
      <!-- Brand -->
      <router-link to="/" class="brand">校园拍卖</router-link>

      <!-- Right-side nav items -->
      <nav class="nav-right">
        <!-- ── Admin experience: management links only ── -->
        <template v-if="isAdmin">
          <router-link to="/admin/dashboard" class="nav-link admin-link">
            管理后台
          </router-link>
        </template>

        <!-- ── Student experience: marketplace + personal links ── -->
        <template v-if="isStudent">
          <router-link to="/" class="nav-link market-link auction-link">拍卖广场</router-link>
          <router-link to="/market" class="nav-link market-link direct-link">直购市场</router-link>
          <router-link to="/publish" class="nav-link publish-link">发布商品</router-link>
          <router-link to="/profile" class="nav-link account-link">个人中心</router-link>
        </template>

        <!-- ── Guest: market browsing links only ── -->
        <template v-if="!isLoggedIn">
          <router-link to="/" class="nav-link market-link auction-link">拍卖广场</router-link>
          <router-link to="/market" class="nav-link market-link direct-link">直购市场</router-link>
        </template>

        <!-- ── Authenticated: balance chip + user chip + logout ── -->
        <template v-if="isLoggedIn">
          <el-tag v-if="isStudent" type="success" effect="plain" class="balance-chip">
            ¥{{ currentUser.balance?.toFixed(2) ?? '0.00' }}
          </el-tag>
          <el-tag
            :type="isAdmin ? 'danger' : 'success'"
            effect="dark"
            class="user-chip"
          >
            {{ currentUser.username }}
            <span class="role-badge">{{ isAdmin ? '管理员' : '学生' }}</span>
          </el-tag>
          <el-button size="small" plain class="logout-btn" @click="handleLogout">
            退出登录
          </el-button>
        </template>

        <!-- ── Guest: login link ── -->
        <router-link v-if="!isLoggedIn" to="/login" class="nav-link login-link">
          登录
        </router-link>
      </nav>
    </el-header>

    <el-main :class="{ 'no-pad': route.path === '/login' || route.path === '/register' }">
      <div v-if="route.path !== '/login' && route.path !== '/register'" class="page-content">
        <router-view />
      </div>
      <router-view v-else />
    </el-main>
  </el-container>
</template>

<script setup>
import { useRouter, useRoute } from 'vue-router'
import { useAuth } from './composables/useAuth.js'

const router = useRouter()
const route  = useRoute()
const { currentUser, isLoggedIn, isAdmin, isStudent, logout } = useAuth()

const handleLogout = () => {
  logout()
  router.push('/login')
}
</script>

<style>
*, *::before, *::after { box-sizing: border-box; }
body { margin: 0; background: #f0f2f5; font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; }
.layout  { min-height: 100vh; }
.navbar  {
  background: #1d3557;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 28px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, .2);
}
.brand {
  color: #fff;
  font-size: 20px;
  font-weight: 700;
  text-decoration: none;
  letter-spacing: .4px;
  flex-shrink: 0;
}
.nav-right {
  display: flex;
  align-items: center;
  gap: 16px;
}
.nav-link {
  color: rgba(255, 255, 255, .85);
  text-decoration: none;
  font-size: 14px;
  font-weight: 500;
  transition: color .2s;
}
.nav-link:hover { color: #fff; }
.admin-link {
  background: rgba(245, 108, 108, .25);
  border: 1px solid rgba(245, 108, 108, .5);
  border-radius: 4px;
  padding: 4px 12px;
  font-size: 13px;
  letter-spacing: .3px;
}
.admin-link:hover { background: rgba(245, 108, 108, .4); color: #fff; }
.login-link {
  background: rgba(255, 255, 255, .15);
  border: 1px solid rgba(255, 255, 255, .35);
  border-radius: 4px;
  padding: 4px 14px;
}
.login-link:hover { background: rgba(255, 255, 255, .25); }
.publish-link {
  background: rgba(64, 158, 255, .2);
  border: 1px solid rgba(64, 158, 255, .5);
  border-radius: 4px;
  padding: 4px 12px;
  font-size: 13px;
}
.publish-link:hover { background: rgba(64, 158, 255, .35); color: #fff; }
.account-link {
  background: rgba(103, 194, 58, .2);
  border: 1px solid rgba(103, 194, 58, .5);
  border-radius: 4px;
  padding: 4px 12px;
  font-size: 13px;
}
.account-link:hover { background: rgba(103, 194, 58, .35); color: #fff; }
.market-link {
  border-radius: 4px;
  padding: 4px 12px;
  font-size: 13px;
}
.auction-link {
  background: rgba(64, 158, 255, .15);
  border: 1px solid rgba(64, 158, 255, .4);
}
.auction-link:hover, .auction-link.router-link-active { background: rgba(64, 158, 255, .35); color: #fff; }
.direct-link {
  background: rgba(230, 162, 60, .15);
  border: 1px solid rgba(230, 162, 60, .4);
}
.direct-link:hover, .direct-link.router-link-active { background: rgba(230, 162, 60, .35); color: #fff; }
.balance-chip { cursor: default; font-weight: 600; font-size: 13px; }
.user-chip  { cursor: default; }
.role-badge { margin-left: 6px; font-size: 11px; opacity: .75; }
.logout-btn { border-color: rgba(255,255,255,.4); color: #fff; background: transparent; }
.logout-btn:hover { border-color: #fff; background: rgba(255,255,255,.1); }
.no-pad { padding: 0; }
.page-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 8px 0 40px;
}
</style>
