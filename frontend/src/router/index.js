import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import HomeView           from '../views/HomeView.vue'
import AuctionDetail      from '../views/AuctionDetail.vue'
import LoginView          from '../views/LoginView.vue'
import RegisterView       from '../views/RegisterView.vue'
import AdminView          from '../views/AdminView.vue'
import CreateAuctionView  from '../views/CreateAuctionView.vue'
import ProfileView        from '../views/ProfileView.vue'
import DirectSaleView     from '../views/DirectSaleView.vue'
import SellerProfileView  from '../views/SellerProfileView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/',                    component: HomeView,          meta: { requiresAuth: true } },
    { path: '/auctions/:id',        component: AuctionDetail,     meta: { requiresAuth: true } },
    { path: '/market',              component: DirectSaleView,    meta: { requiresAuth: true } },
    { path: '/publish',             component: CreateAuctionView, meta: { requiresAuth: true } },
    { path: '/profile',             component: ProfileView,       meta: { requiresAuth: true } },
    // Public seller profile — /profile/:userId (no auth required)
    { path: '/profile/:userId',     component: SellerProfileView },
    { path: '/login',               component: LoginView,         meta: { guestOnly: true } },
    { path: '/register',            component: RegisterView,      meta: { guestOnly: true } },
    { path: '/admin',               component: AdminView,         meta: { requiresAuth: true, requiresAdmin: true } },
    { path: '/admin/dashboard',     redirect: '/admin' }
  ]
})

// ── Navigation guard ───────────────────────────────────────────────────────
// Reads localStorage directly (no Vue reactivity) so the guard works before
// any component has called useAuth().
router.beforeEach(to => {
  const raw  = localStorage.getItem('ca_user')
  const user = raw ? JSON.parse(raw) : null

  // Redirect authenticated users away from /login or /register
  if (to.meta.guestOnly && user) {
    return user.role === 'ADMIN' ? '/admin' : '/'
  }

  // Root URL: admins go straight to their dashboard, not the student market
  if (to.path === '/' && user?.role === 'ADMIN') return '/admin'

  // All protected routes require a logged-in user
  if (to.meta.requiresAuth && !user) return '/login'

  // Admin-only routes require the ADMIN role
  if (to.meta.requiresAdmin && user?.role !== 'ADMIN') {
    ElMessage.error('权限不足，无法访问管理中心')
    return '/'
  }
})

export default router
