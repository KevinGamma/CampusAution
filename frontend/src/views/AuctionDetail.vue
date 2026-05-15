<template>
  <div>
    <!-- Back navigation -->
    <el-page-header class="page-header" @back="router.push('/')">
      <template #content>
        <span class="page-title">拍卖详情</span>
      </template>
    </el-page-header>

    <!-- Loading skeleton -->
    <el-skeleton v-if="loading" :rows="6" animated />

    <!-- Main content -->
    <el-card v-else-if="auction" class="detail-card">
      <!-- ── Header ── -->
      <template #header>
        <div class="detail-head">
          <h2 class="auction-title">{{ auction.title }}</h2>
          <el-tag :type="statusType" size="large" effect="plain">{{ STATUS_LABEL[auction.status] ?? auction.status }}</el-tag>
        </div>
      </template>

      <!-- ── Product image carousel ── -->
      <el-carousel
        v-if="auction.imageUrls && auction.imageUrls.length"
        :interval="4000"
        :height="carouselHeight"
        indicator-position="outside"
        class="image-carousel"
      >
        <el-carousel-item
          v-for="(url, idx) in auction.imageUrls"
          :key="idx"
        >
          <img :src="url" :alt="`商品图片 ${idx + 1}`" class="carousel-img" />
        </el-carousel-item>
      </el-carousel>

      <!-- ── Description ── -->
      <p class="description">{{ auction.description || '暂无描述。' }}</p>

      <!-- ── Key metrics ── -->
      <el-descriptions :column="2" border class="metrics">
        <el-descriptions-item :label="isDirect ? '售价' : '起拍价'">
          ¥{{ auction.startPrice }}
        </el-descriptions-item>

        <template v-if="isDirect">
          <el-descriptions-item label="商品类型">
            <el-tag type="warning" effect="dark" size="small">直购</el-tag>
          </el-descriptions-item>
        </template>
        <template v-else>
          <el-descriptions-item label="当前最高价">
            <template v-if="auction.bidCount > 0">
              <span class="current-price">¥{{ auction.currentPrice }}</span>
            </template>
            <template v-else>
              <span class="no-bids-hint">暂无出价，快来抢占先机！</span>
            </template>
          </el-descriptions-item>

          <el-descriptions-item label="剩余时间">
            <span class="countdown-display" :class="{ urgent: isUrgent }">{{ formatted }}</span>
          </el-descriptions-item>
        </template>

        <el-descriptions-item label="卖家" :span="2">
          <div
            class="seller-chip"
            @click="router.push(`/profile/${auction.creatorId}`)"
          >
            <img
              v-if="sellerInfo?.avatarUrl"
              :src="sellerInfo.avatarUrl"
              class="seller-avatar"
              :alt="sellerInfo.username"
            />
            <div v-else class="seller-avatar-fallback">
              {{ (sellerInfo?.username ?? String(auction.creatorId))[0].toUpperCase() }}
            </div>
            <div class="seller-text">
              <span class="seller-name">
                {{ sellerInfo ? sellerInfo.username : `用户 #${auction.creatorId}` }}
              </span>
              <el-rate
                v-if="sellerInfo?.averageRating"
                :model-value="sellerInfo.averageRating"
                disabled
                size="small"
                class="seller-rate"
              />
              <span v-if="sellerInfo?.reviewCount" class="seller-review-count">
                {{ sellerInfo.reviewCount }} 条评价
              </span>
            </div>
            <el-icon class="seller-arrow"><ArrowRight /></el-icon>
          </div>
        </el-descriptions-item>
      </el-descriptions>

      <!-- ══════════════════════════════════════════════════════════════════════
           Action panel — exactly ONE of three mutually exclusive blocks shown.
           Priority: ADMIN > STUDENT owner > bidder / guest
      ══════════════════════════════════════════════════════════════════════ -->

      <!-- ── 1. Admin: force-remove any auction ── -->
      <template v-if="isAdmin">
        <el-divider content-position="left">管理操作</el-divider>
        <div class="action-panel admin-panel">
          <el-alert
            title="管理员账户：禁止参与交易，您是系统监管者。"
            type="warning"
            :closable="false"
            show-icon
            class="panel-alert"
          />
          <el-popconfirm
            :title="`确认强制下架「${auction.title}」及其所有出价记录？`"
            confirm-button-text="确认删除"
            confirm-button-type="danger"
            cancel-button-text="取消"
            width="300"
            @confirm="adminDelete"
          >
            <template #reference>
              <el-button type="danger" size="large" :loading="deleting" class="action-btn">
                强制下架
              </el-button>
            </template>
          </el-popconfirm>
        </div>
      </template>

      <!-- ── 2. Student owner: cancel own auction ── -->
      <template v-else-if="isStudent && isOwner">
        <el-divider content-position="left">管理我的拍卖</el-divider>
        <div class="action-panel owner-panel">
          <el-alert
            title="这是您发布的拍卖，您可随时取消。"
            type="warning"
            :closable="false"
            show-icon
            class="panel-alert"
          />
          <el-popconfirm
            title="确认取消该拍卖？此操作无法撤回。"
            confirm-button-text="确认取消"
            confirm-button-type="warning"
            cancel-button-text="保留"
            width="280"
            @confirm="ownerDelete"
          >
            <template #reference>
              <el-button type="warning" size="large" :loading="deleting" class="action-btn">
                取消我的拍卖
              </el-button>
            </template>
          </el-popconfirm>

          <template v-if="auction.saleType === 'AUCTION' && auction.status === 'ACTIVE'">
            <el-divider />
            <el-alert
              title="您可一键成交当前最高出价。"
              type="info"
              :closable="false"
              show-icon
              class="panel-alert"
            />
            <el-popconfirm
              :title="`确认以当前最高价成交「${auction.title}」？`"
              confirm-button-text="确认成交"
              confirm-button-type="primary"
              cancel-button-text="取消"
              width="300"
              @confirm="submitAcceptHighest"
            >
              <template #reference>
                <el-button type="primary" size="large" :loading="accepting" class="action-btn">
                  接受当前最高价并成交
                </el-button>
              </template>
            </el-popconfirm>
          </template>
        </div>
      </template>

      <!-- ── 3. Bidder / buyer (non-owner student) or unauthenticated guest ── -->
      <template v-else>
        <!-- ── 3a. DIRECT sale: Buy Now panel ── -->
        <template v-if="isDirect">
          <el-divider content-position="left">立即购买</el-divider>
          <div class="action-panel buy-panel">
            <el-alert
              v-if="!isLoggedIn"
              title="请先登录后再购买。"
              type="info"
              :closable="false"
              show-icon
              class="panel-alert"
            />
            <el-alert
              v-else-if="auction.status !== 'ACTIVE'"
              :title="`该商品已不可购买（${STATUS_LABEL[auction.status] ?? auction.status}）。`"
              type="warning"
              :closable="false"
              show-icon
              class="panel-alert"
            />
            <template v-else>
              <div class="buy-price-display">
                <span class="buy-price-label">价格</span>
                <span class="buy-price-value">¥{{ auction.currentPrice }}</span>
              </div>
              <el-popconfirm
                :title="`确认以 ¥${auction.currentPrice} 购买「${auction.title}」？`"
                confirm-button-text="确认购买"
                confirm-button-type="success"
                cancel-button-text="取消"
                width="320"
                @confirm="submitBuy"
              >
                <template #reference>
                  <el-button type="success" size="large" :loading="buying" class="action-btn">
                    立即购买 — ¥{{ auction.currentPrice }}
                  </el-button>
                </template>
              </el-popconfirm>
            </template>
          </div>
        </template>

        <!-- ── 3b. AUCTION: Place a bid ── -->
        <template v-else>
          <el-divider content-position="left">参与竞拍</el-divider>

          <el-form
            ref="formRef"
            :model="form"
            :rules="rules"
            label-width="140px"
            class="bid-form"
            status-icon
          >
            <el-form-item label="出价金额（¥）" prop="amount">
              <el-input-number
                v-model="form.amount"
                :precision="2"
                :step="1"
                :min="0.01"
                placeholder="0.00"
                style="width: 200px"
              />
              <span class="field-hint">须高于 ¥{{ auction.currentPrice }}</span>
            </el-form-item>

            <el-form-item v-if="isLoggedIn && !isOwner" label=" ">
              <span class="field-hint">
                您的账户余额：<strong :style="{ color: overBalance ? '#f56c6c' : '#67c23a' }">¥{{ Number(currentUser?.balance ?? 0).toFixed(2) }}</strong>
                <span v-if="overBalance" style="color:#f56c6c; margin-left:8px;">出价金额不能超过您的账户余额</span>
              </span>
            </el-form-item>

            <el-form-item>
              <el-button
                type="primary"
                :loading="submitting"
                :disabled="!isLoggedIn || isOwner || auction.status !== 'ACTIVE' || remaining <= 0 || overBalance"
                @click="submitBid"
              >
                提交出价
              </el-button>
              <el-button :loading="refreshing" @click="reload(refreshing)">
                刷新
              </el-button>
            </el-form-item>
          </el-form>
        </template>
      </template>

    </el-card>
  </div>

  <!-- Transaction breakdown dialog -->
  <el-dialog
    v-model="txDialogVisible"
    title="交易成功"
    width="400px"
    :close-on-click-modal="false"
    @closed="router.push('/')"
  >
    <template v-if="txResult">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="商品">{{ txResult.title }}</el-descriptions-item>
        <el-descriptions-item label="成交价">¥{{ txResult.amount.toFixed(2) }}</el-descriptions-item>
        <el-descriptions-item label="平台手续费 (5%)">
          <span style="color: #e6a23c">¥{{ txResult.fee.toFixed(2) }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="卖家实际到账">
          <span style="color: #67c23a">¥{{ txResult.sellerReceives.toFixed(2) }}</span>
        </el-descriptions-item>
      </el-descriptions>
      <p style="margin-top: 12px; color: #909399; font-size: 13px;">点击关闭后将返回首页。</p>
    </template>
    <template #footer>
      <el-button type="primary" @click="txDialogVisible = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowRight } from '@element-plus/icons-vue'
import { getAuction, placeBid as apiBid, deleteAuction, buyDirect, acceptCurrentHighest } from '../api/auction.js'
import { getSellerProfile } from '../api/review.js'
import { useCountdown } from '../composables/useCountdown.js'
import { useAuth } from '../composables/useAuth.js'

const route     = useRoute()
const router    = useRouter()
const auctionId = route.params.id

const { currentUser, isLoggedIn, isAdmin, isStudent, updateBalance } = useAuth()

const auction         = ref(null)
const loading         = ref(false)
const refreshing      = ref(false)
const submitting      = ref(false)
const deleting        = ref(false)
const buying          = ref(false)
const accepting       = ref(false)
const txDialogVisible = ref(false)
const txResult        = ref(null)
const formRef         = ref(null)
const form            = ref({ amount: null })

const isDirect   = computed(() => auction.value?.saleType === 'DIRECT')
const sellerInfo = ref(null)   // populated after auction load

// Responsive carousel height
const carouselHeight = ref(window.innerWidth < 600 ? '220px' : '380px')
const onResize = () => { carouselHeight.value = window.innerWidth < 600 ? '220px' : '380px' }
onMounted(() => window.addEventListener('resize', onResize))
onUnmounted(() => window.removeEventListener('resize', onResize))

const isOwner = computed(() =>
  isStudent.value && currentUser.value?.id === auction.value?.creatorId
)

const overBalance = computed(() => {
  const amt = Number(form.value.amount)
  const bal = Number(currentUser.value?.balance ?? 0)
  return Number.isFinite(amt) && amt > 0 && amt > bal
})

const { remaining, isUrgent, formatted } = useCountdown(() => auction.value?.endTime)

const STATUS_TYPE  = { ACTIVE: 'success', FINISHED: 'info', SOLD: 'warning', CANCELLED: 'danger' }
const STATUS_LABEL = { ACTIVE: '进行中', FINISHED: '已结束', SOLD: '已售出', CANCELLED: '已取消' }
const statusType   = computed(() => STATUS_TYPE[auction.value?.status] ?? 'info')

const rules = {
  amount: [
    { required: true, message: '请输入出价金额', trigger: 'blur' },
    {
      validator(_, value, callback) {
        if (auction.value && value <= auction.value.currentPrice) {
          callback(new Error(`出价须高于 ¥${auction.value.currentPrice}`))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    },
    {
      validator(_, value, callback) {
        if (currentUser.value && value > currentUser.value.balance) {
          callback(new Error('出价金额不能超过您的账户余额'))
        } else {
          callback()
        }
      },
      trigger: ['blur', 'change']
    }
  ]
}

const reload = async (flag) => {
  flag.value = true
  try {
    auction.value = await getAuction(auctionId)
    // Load seller profile in the background after auction arrives
    if (auction.value?.creatorId) {
      getSellerProfile(auction.value.creatorId)
        .then(p => { sellerInfo.value = p })
        .catch(() => {})
    }
  } catch {
    // global interceptor handles messaging
  } finally {
    flag.value = false
  }
}

onMounted(() => reload(loading))

const submitBid = async () => {
  try { await formRef.value.validate() } catch { return }

  if (currentUser.value && form.value.amount > currentUser.value.balance) {
    ElMessage({ type: 'error', message: '出价金额不能超过您的账户余额', duration: 3000 })
    return
  }

  submitting.value = true
  try {
    await apiBid(auctionId, currentUser.value.id, form.value.amount)
    ElMessage({ type: 'success', message: '出价成功！', duration: 3000 })
    form.value.amount = null
    await reload(refreshing)
  } catch {
    // 403 / 409 / 400 already surfaced by http.js interceptor
  } finally {
    submitting.value = false
  }
}

const runDelete = async (successMsg) => {
  deleting.value = true
  try {
    await deleteAuction(auctionId)
    ElMessage({ type: 'success', message: successMsg, duration: 3000 })
    router.push('/')
  } catch {
    // interceptor already notified the user
  } finally {
    deleting.value = false
  }
}

const adminDelete = () => runDelete('管理员已强制下架该拍卖。')
const ownerDelete = () => runDelete('您的拍卖已取消。')

function showTxDialog(order, auctionTitle) {
  const amount = Number(order.amount)
  txResult.value = {
    amount,
    fee: +(amount * 0.05).toFixed(2),
    sellerReceives: +(amount * 0.95).toFixed(2),
    title: auctionTitle
  }
  txDialogVisible.value = true
}

const submitBuy = async () => {
  buying.value = true
  try {
    const order = await buyDirect(auctionId)
    updateBalance(Number(currentUser.value?.balance ?? 0) - Number(order.amount))
    showTxDialog(order, auction.value.title)
    await reload(refreshing)
  } catch {
    // 400 "Insufficient balance" → http.js interceptor shows 余额不足…
  } finally {
    buying.value = false
  }
}

const submitAcceptHighest = async () => {
  accepting.value = true
  try {
    const order = await acceptCurrentHighest(auctionId)
    const sellerReceives = +(Number(order.amount) * 0.95).toFixed(2)
    updateBalance(Number(currentUser.value?.balance ?? 0) + sellerReceives)
    showTxDialog(order, auction.value.title)
    await reload(refreshing)
  } catch {
    // interceptor handles error display
  } finally {
    accepting.value = false
  }
}
</script>

<style scoped>
.page-header    { margin: 20px 0 24px; }
.page-title     { font-size: 18px; font-weight: 600; color: #303133; }
.detail-card    { border-radius: 10px; }
.detail-head    { display: flex; justify-content: space-between; align-items: center; gap: 12px; }
.auction-title  { margin: 0; font-size: 22px; font-weight: 700; color: #1d3557; }
.description    { color: #606266; line-height: 1.75; margin: 0 0 22px; }

/* ── Image carousel ── */
.image-carousel { margin-bottom: 20px; border-radius: 8px; overflow: hidden; }
.carousel-img   {
  width: 100%;
  height: 100%;
  object-fit: contain;
  background: #f5f5f5;
}

/* ── Seller chip ── */
.seller-chip {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 6px 10px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  cursor: pointer;
  transition: background .18s, border-color .18s;
  max-width: 320px;
}
.seller-chip:hover { background: #f5f7fa; border-color: #409eff; }

.seller-avatar {
  width: 36px; height: 36px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
  border: 1px solid #e4e7ed;
}
.seller-avatar-fallback {
  width: 36px; height: 36px;
  border-radius: 50%;
  background: #e4e7ed;
  display: flex; align-items: center; justify-content: center;
  font-size: 16px; font-weight: 700; color: #909399;
  flex-shrink: 0;
}
.seller-text   { display: flex; flex-direction: column; gap: 2px; }
.seller-name   { font-weight: 600; font-size: 14px; color: #303133; }
.seller-rate   { margin: 0; }
.seller-review-count { font-size: 11px; color: #909399; }
.seller-arrow  { color: #c0c4cc; margin-left: auto; font-size: 14px; }
.metrics        { margin-bottom: 8px; }
.current-price  { font-size: 22px; font-weight: 700; color: #e6a23c; }
.countdown-display {
  font-family: 'Courier New', monospace;
  font-size: 20px;
  font-weight: 700;
  color: #303133;
}
.countdown-display.urgent { color: #f56c6c; animation: pulse 1s ease-in-out infinite; }

/* ── Action panels ── */
.action-panel { display: flex; flex-direction: column; gap: 14px; margin-top: 12px; max-width: 560px; }
.panel-alert  { border-radius: 6px; }
.action-btn   { width: 220px; }

.bid-form      { margin-top: 16px; max-width: 520px; }
.field-hint    { margin-left: 14px; font-size: 12px; color: #909399; }
.no-bids-hint  { font-style: italic; color: #909399; font-size: 14px; }

.buy-panel { max-width: 400px; }
.buy-price-display { display: flex; align-items: baseline; gap: 12px; }
.buy-price-label { font-size: 14px; color: #909399; }
.buy-price-value { font-size: 32px; font-weight: 700; color: #67c23a; }

@keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: .45; } }
</style>
