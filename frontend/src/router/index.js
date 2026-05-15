import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import WelcomeView        from '../views/WelcomeView.vue'
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
    // Public entrance — accessible to all roles (no requiresAuth)
    { path: '/',                    component: WelcomeView },
    // Student marketplace routes
    { path: '/auction-square',      component: HomeView,          meta: { requiresAuth: true } },
    { path: '/auctions/:id',        component: AuctionDetail,     meta: { requiresAuth: true } },
    { path: '/direct-market',       component: DirectSaleView,    meta: { requiresAuth: true } },
    // Legacy /market alias kept for backwards compatibility
    { path: '/market',              redirect: '/direct-market' },
    { path: '/publish',             component: CreateAuctionView, meta: { requiresAuth: true } },
    { path: '/profile',             component: ProfileView,       meta: { requiresAuth: true } },
    // Public seller profile — no auth required
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

  // Redirect authenticated users away from /login or /register back to welcome
  if (to.meta.guestOnly && user) return '/'

  // All protected routes require a logged-in user
  if (to.meta.requiresAuth && !user) return '/login'

  // Admin-only routes: students get bounced with a Chinese 403 message
  if (to.meta.requiresAdmin && user?.role !== 'ADMIN') {
    ElMessage.error('权限不足，无法访问管理中心')
    return '/'
  }
})

export default router
