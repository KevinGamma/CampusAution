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
    { path: '/',               name: 'Welcome',        component: WelcomeView },
    // Student marketplace routes
    { path: '/auction-square', name: 'AuctionSquare',  component: HomeView,          meta: { requiresAuth: true } },
    { path: '/auctions/:id',   name: 'AuctionDetail',  component: AuctionDetail,     meta: { requiresAuth: true } },
    { path: '/direct-market',  name: 'DirectMarket',   component: DirectSaleView,    meta: { requiresAuth: true } },
    // Legacy /market alias kept for backwards compatibility
    { path: '/market',                                  redirect: { name: 'DirectMarket' } },
    { path: '/publish',        name: 'Publish',         component: CreateAuctionView, meta: { requiresAuth: true } },
    { path: '/profile',        name: 'Profile',         component: ProfileView,       meta: { requiresAuth: true } },
    // Public seller profile — no auth required
    { path: '/profile/:userId', name: 'SellerProfile', component: SellerProfileView },
    { path: '/login',           name: 'Login',          component: LoginView,         meta: { guestOnly: true } },
    { path: '/register',        name: 'Register',       component: RegisterView,      meta: { guestOnly: true } },
    { path: '/admin',           name: 'Admin',          component: AdminView,         meta: { requiresAuth: true, requiresAdmin: true } },
    { path: '/admin/dashboard',                         redirect: { name: 'Admin' } }
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
