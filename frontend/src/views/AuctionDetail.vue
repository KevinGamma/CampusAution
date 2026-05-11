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

        <el-descriptions-item label="卖家编号">
          #{{ auction.creatorId }}
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

            <el-form-item>
              <el-button
                type="primary"
                :loading="submitting"
                :disabled="!isLoggedIn || isOwner || auction.status !== 'ACTIVE' || remaining <= 0"
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
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getAuction, placeBid as apiBid, deleteAuction, buyDirect } from '../api/auction.js'
import { useCountdown } from '../composables/useCountdown.js'
import { useAuth } from '../composables/useAuth.js'

const route     = useRoute()
const router    = useRouter()
const auctionId = route.params.id

const { currentUser, isLoggedIn, isAdmin, isStudent } = useAuth()

const auction    = ref(null)
const loading    = ref(false)
const refreshing = ref(false)
const submitting = ref(false)
const deleting   = ref(false)
const buying     = ref(false)
const formRef    = ref(null)
const form       = ref({ amount: null })

const isDirect = computed(() => auction.value?.saleType === 'DIRECT')

const isOwner = computed(() =>
  isStudent.value && currentUser.value?.id === auction.value?.creatorId
)

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
    }
  ]
}

const reload = async (flag) => {
  flag.value = true
  try {
    auction.value = await getAuction(auctionId)
  } catch {
    // global interceptor handles messaging
  } finally {
    flag.value = false
  }
}

onMounted(() => reload(loading))

const submitBid = async () => {
  try { await formRef.value.validate() } catch { return }

  submitting.value = true
  try {
    await apiBid(auctionId, currentUser.value.id, form.value.amount)
    ElMessage({ type: 'success', message: '出价成功！', duration: 3000 })
    form.value.amount = null
    await reload(refreshing)
  } catch {
    // 403 / 409 / 500 already surfaced by http.js interceptor
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

const submitBuy = async () => {
  buying.value = true
  try {
    await buyDirect(auctionId)
    ElMessage({ type: 'success', message: '购买成功！商品已归您所有。', duration: 3500 })
    router.push('/')
  } catch {
    // 400 "Insufficient balance" → http.js interceptor shows 余额不足…
  } finally {
    buying.value = false
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
