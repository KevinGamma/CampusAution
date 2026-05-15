<template>
  <div class="profile-page">

    <!-- Page header with avatar -->
    <div class="profile-hero">
      <div class="hero-left">
        <!-- Avatar -->
        <el-upload
          :show-file-list="false"
          :auto-upload="false"
          accept="image/jpeg,image/png,image/gif,image/webp"
          :on-change="onAvatarChange"
        >
          <div class="avatar-wrap">
            <img
              v-if="currentUser?.avatarUrl"
              :src="currentUser.avatarUrl"
              class="avatar-img"
              :alt="currentUser.username"
            />
            <div v-else class="avatar-placeholder">
              {{ (currentUser?.username ?? '?')[0].toUpperCase() }}
            </div>
            <div class="avatar-overlay">
              <el-icon><Camera /></el-icon>
              <span>换头像</span>
            </div>
          </div>
        </el-upload>

        <div>
          <h2 class="profile-title">{{ currentUser?.username }}</h2>
          <p class="profile-sub">{{ currentUser?.role === 'ADMIN' ? '管理员' : '学生用户' }}</p>
        </div>
      </div>

      <div class="balance-section">
        <el-tag type="success" effect="dark" size="large">
          账户余额：¥{{ currentUser?.balance?.toFixed(2) ?? '0.00' }}
        </el-tag>
        <el-button size="small" type="success" plain @click="rechargeDialog.visible = true">
          充值
        </el-button>
      </div>
    </div>

    <!-- Tabs -->
    <el-tabs v-model="activeTab" class="profile-tabs">

      <!-- ── Profile info tab ─────────────────────────────────────────────── -->
      <el-tab-pane label="个人信息" name="info">
        <el-card class="info-card">
          <el-form label-position="top" style="max-width: 480px">
            <el-form-item label="个人简介">
              <el-input
                v-model="bioInput"
                type="textarea"
                :rows="4"
                placeholder="写几句话介绍自己吧…"
                maxlength="200"
                show-word-limit
              />
            </el-form-item>
            <el-form-item>
              <el-button
                type="primary"
                :loading="savingProfile"
                @click="doSaveProfile"
              >
                保存简介
              </el-button>
            </el-form-item>
          </el-form>

          <el-divider />

          <div class="current-bio" v-if="currentUser?.bio">
            <p class="bio-label">当前简介</p>
            <p class="bio-text">{{ currentUser.bio }}</p>
          </div>
        </el-card>
      </el-tab-pane>

      <!-- ── My Auctions tab ──────────────────────────────────────────────── -->
      <el-tab-pane label="我发布的" name="auctions">
        <div v-loading="loadingItems">
          <el-empty
            v-if="!loadingItems && myItems.length === 0"
            description="您还没有发布任何商品"
            :image-size="140"
          />

          <el-table
            v-else
            :data="myItems"
            stripe
            border
            style="width: 100%"
            :header-cell-style="{ background: '#f5f7fa', color: '#606266', fontWeight: '600' }"
          >
            <el-table-column prop="id"    label="编号"    width="70" align="center" />
            <el-table-column prop="title" label="商品名称" min-width="200" show-overflow-tooltip />
            <el-table-column prop="category" label="分类" width="120" align="center">
              <template #default="{ row }">
                <el-tag size="small" type="info" effect="plain">{{ row.category ?? '—' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="currentPrice" label="当前价格" width="130" align="right">
              <template #default="{ row }">
                <span class="price-cell">¥{{ row.currentPrice }}</span>
              </template>
            </el-table-column>
            <el-table-column label="实际收益" width="165" align="right">
              <template #header>
                <el-tooltip
                  content="实际收益 = 成交价 - 5% 平台手续费"
                  placement="top"
                  effect="light"
                >
                  <span style="cursor: default">
                    实际收益
                    <el-icon style="vertical-align: middle; color: #909399; font-size: 13px"><InfoFilled /></el-icon>
                  </span>
                </el-tooltip>
              </template>
              <template #default="{ row }">
                <span v-if="row.status === 'SOLD' && row.actualEarnings != null" class="earnings-cell">
                  ¥{{ Number(row.actualEarnings).toFixed(2) }}
                </span>
                <span v-else class="no-action">—</span>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="120" align="center">
              <template #default="{ row }">
                <el-tag :type="STATUS_TYPE[row.status]" size="small" effect="light">
                  {{ STATUS_LABEL[row.status] ?? row.status }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="endTime" label="结束时间" width="170">
              <template #default="{ row }">{{ row.endTime?.replace('T', ' ') }}</template>
            </el-table-column>
            <el-table-column label="操作" width="260" align="center" fixed="right">
              <template #default="{ row }">
                <template v-if="row.status === 'ACTIVE'">
                  <el-button
                    v-if="row.saleType === 'AUCTION'"
                    size="small"
                    type="primary"
                    plain
                    @click="openBidsDialog(row)"
                  >
                    查看出价
                  </el-button>
                  <el-button
                    size="small"
                    type="warning"
                    plain
                    @click="openEditDialog(row)"
                  >
                    编辑
                  </el-button>
                  <el-popconfirm
                    :title="`确认下架「${row.title}」？`"
                    confirm-button-text="确认下架"
                    confirm-button-type="danger"
                    cancel-button-text="保留"
                    width="240"
                    @confirm="doCancelItem(row.id)"
                  >
                    <template #reference>
                      <el-button size="small" type="danger" plain :loading="cancelling === row.id">
                        下架
                      </el-button>
                    </template>
                  </el-popconfirm>
                </template>
                <span v-else class="no-action">—</span>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <!-- ── My Bids tab ──────────────────────────────────────────────────── -->
      <el-tab-pane label="我参与的" name="bids">
        <div v-loading="loadingBids">
          <el-empty
            v-if="!loadingBids && myBids.length === 0"
            description="您还没有参与任何竞拍"
            :image-size="140"
          />

          <el-table
            v-else
            :data="myBids"
            stripe
            border
            style="width: 100%"
            :header-cell-style="{ background: '#f5f7fa', color: '#606266', fontWeight: '600' }"
          >
            <el-table-column label="商品名称" min-width="200" show-overflow-tooltip>
              <template #default="{ row }">
                <el-link type="primary" @click="router.push(`/auctions/${row.auction.id}`)">
                  {{ row.auction.title }}
                </el-link>
              </template>
            </el-table-column>
            <el-table-column label="分类" width="120" align="center">
              <template #default="{ row }">
                <el-tag size="small" type="info" effect="plain">{{ row.auction.category ?? '—' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="我的最高出价" width="145" align="right">
              <template #default="{ row }">
                <span class="my-bid-cell">¥{{ row.myHighestBid }}</span>
              </template>
            </el-table-column>
            <el-table-column label="当前价格" width="130" align="right">
              <template #default="{ row }">
                <span class="price-cell">¥{{ row.auction.currentPrice }}</span>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="120" align="center">
              <template #default="{ row }">
                <el-tag :type="STATUS_TYPE[row.auction.status]" size="small" effect="light">
                  {{ STATUS_LABEL[row.auction.status] ?? row.auction.status }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="结束时间" width="170">
              <template #default="{ row }">{{ row.auction.endTime?.replace('T', ' ') }}</template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <!-- ── My Purchases tab ────────────────────────────────────────────── -->
      <el-tab-pane label="我买到的" name="purchases">
        <div v-loading="loadingPurchases">
          <el-empty
            v-if="!loadingPurchases && myPurchases.length === 0"
            description="暂无购买记录"
            :image-size="140"
          />

          <el-table
            v-else
            :data="myPurchases"
            stripe
            border
            style="width: 100%"
            :header-cell-style="{ background: '#f5f7fa', color: '#606266', fontWeight: '600' }"
          >
            <el-table-column label="商品名称" min-width="180" show-overflow-tooltip>
              <template #default="{ row }">
                <el-link type="primary" @click="router.push(`/auctions/${row.auction.id}`)">
                  {{ row.auction.title }}
                </el-link>
              </template>
            </el-table-column>
            <el-table-column label="卖家" width="100" align="center">
              <template #default="{ row }">
                <el-link type="info" @click="router.push(`/profile/${row.sellerId}`)">
                  #{{ row.sellerId }}
                </el-link>
              </template>
            </el-table-column>
            <el-table-column label="实付金额" width="120" align="right">
              <template #default="{ row }">
                <span class="price-cell">¥{{ row.paidAmount }}</span>
              </template>
            </el-table-column>
            <el-table-column label="购买时间" width="160">
              <template #default="{ row }">{{ row.purchasedAt?.replace('T', ' ') }}</template>
            </el-table-column>
            <el-table-column label="评价" width="120" align="center" fixed="right">
              <template #default="{ row }">
                <span v-if="reviewedOrderIds.has(row.orderId)" class="reviewed-tag">✓ 已评价</span>
                <el-button
                  v-else
                  size="small"
                  type="success"
                  plain
                  @click="openReviewDialog(row)"
                >
                  评价交易
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

    </el-tabs>

    <!-- ── Bids dialog ──────────────────────────────────────────────────────── -->
    <el-dialog
      v-model="bidsDialog.visible"
      :title="`「${bidsDialog.auction?.title}」的出价记录`"
      width="680px"
      destroy-on-close
    >
      <div v-loading="bidsDialog.loading">
        <el-empty
          v-if="!bidsDialog.loading && bidsDialog.bids.length === 0"
          description="该拍卖暂无出价"
          :image-size="100"
        />

        <el-table
          v-else
          :data="bidsDialog.bids"
          stripe
          border
          size="small"
          style="width: 100%"
        >
          <el-table-column prop="id"       label="出价编号"   width="80"  align="center" />
          <el-table-column prop="bidderId" label="竞拍者"   width="100" align="center">
            <template #default="{ row }">
              <el-tag size="small" type="info">#{{ row.bidderId }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="amount" label="出价金额" width="130" align="right">
            <template #default="{ row }">
              <strong class="bid-amount">¥{{ row.amount }}</strong>
            </template>
          </el-table-column>
          <el-table-column prop="timestamp" label="出价时间" min-width="160">
            <template #default="{ row }">{{ row.timestamp?.replace('T', ' ') }}</template>
          </el-table-column>
          <el-table-column label="操作" width="150" align="center" fixed="right">
            <template #default="{ row }">
              <el-popconfirm
                :title="`确认接受竞拍者 #${row.bidderId} 的 ¥${row.amount} 出价？`"
                confirm-button-text="接受"
                confirm-button-type="success"
                cancel-button-text="取消"
                width="280"
                @confirm="doAcceptBid(bidsDialog.auction.id, row.id)"
              >
                <template #reference>
                  <el-button
                    type="success"
                    size="small"
                    :loading="accepting === row.id"
                  >
                    接受此出价
                  </el-button>
                </template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <template #footer>
        <el-button @click="bidsDialog.visible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- ── Edit listing dialog ────────────────────────────────────────────── -->
    <EditAuctionDialog
      v-model="editDialog.visible"
      :auction="editDialog.auction"
      @updated="fetchMyItems"
    />

    <!-- ── Review dialog ─────────────────────────────────────────────────── -->
    <el-dialog
      v-model="reviewDialog.visible"
      title="评价此次交易"
      width="460px"
      destroy-on-close
    >
      <div class="review-body">
        <p class="review-item-name">「{{ reviewDialog.purchaseRow?.auction?.title }}」</p>

        <el-form label-position="top">
          <el-form-item label="综合评分">
            <el-rate
              v-model="reviewDialog.rating"
              :texts="['非常差', '较差', '一般', '满意', '非常满意']"
              show-text
              size="large"
            />
          </el-form-item>
          <el-form-item label="文字评价（可选）">
            <el-input
              v-model="reviewDialog.comment"
              type="textarea"
              :rows="4"
              placeholder="描述一下您对本次交易的感受…"
              maxlength="500"
              show-word-limit
            />
          </el-form-item>
        </el-form>
      </div>

      <template #footer>
        <el-button @click="reviewDialog.visible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="reviewDialog.loading"
          :disabled="!reviewDialog.rating"
          @click="doSubmitReview"
        >
          提交评价
        </el-button>
      </template>
    </el-dialog>

    <!-- ── Recharge dialog ─────────────────────────────────────────────────── -->
    <el-dialog
      v-model="rechargeDialog.visible"
      title="账户充值"
      width="420px"
      destroy-on-close
    >
      <div class="recharge-body">
        <p class="recharge-label">选择充值金额：</p>
        <div class="preset-amounts">
          <el-button
            v-for="amt in PRESET_AMOUNTS"
            :key="amt"
            :type="rechargeDialog.amount === amt ? 'primary' : 'default'"
            @click="rechargeDialog.amount = amt"
          >
            ¥{{ amt }}
          </el-button>
        </div>
        <el-divider>或自定义金额</el-divider>
        <el-input-number
          v-model="rechargeDialog.amount"
          :min="1"
          :max="99999"
          :precision="2"
          placeholder="输入自定义金额"
          style="width: 100%"
          controls-position="right"
        />
      </div>

      <template #footer>
        <el-button @click="rechargeDialog.visible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="rechargeDialog.loading"
          :disabled="!rechargeDialog.amount"
          @click="doRecharge"
        >
          确认支付
        </el-button>
      </template>
    </el-dialog>

  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Camera, InfoFilled } from '@element-plus/icons-vue'
import { useAuth } from '../composables/useAuth.js'
import { listMyItems, listMyBids, getAuctionBids, acceptBid, listPurchased, cancelAuction } from '../api/auction.js'
import { rechargeApi, updateProfileApi } from '../api/user.js'
import { uploadImage } from '../api/upload.js'
import { createReview } from '../api/review.js'
import EditAuctionDialog from '../components/EditAuctionDialog.vue'

const router = useRouter()
const { currentUser, updateBalance, updateUser } = useAuth()

const STATUS_TYPE  = { ACTIVE: 'success', FINISHED: 'info', SOLD: 'warning', CANCELLED: 'danger' }
const STATUS_LABEL = { ACTIVE: '进行中', FINISHED: '已结束', SOLD: '已售出', CANCELLED: '已取消' }
const PRESET_AMOUNTS = [100, 500, 1000]

const activeTab       = ref('info')
const bioInput        = ref(currentUser.value?.bio ?? '')
const savingProfile   = ref(false)
const loadingItems    = ref(false)
const loadingBids     = ref(false)
const loadingPurchases = ref(false)
const myItems         = ref([])
const myBids          = ref([])
const myPurchases     = ref([])
const accepting       = ref(null)
const cancelling      = ref(null)

const editDialog = reactive({
  visible: false,
  auction: null,
})

const bidsDialog = reactive({
  visible: false,
  loading: false,
  auction: null,
  bids:    [],
})

const rechargeDialog = reactive({
  visible: false,
  loading: false,
  amount:  null,
})

/** Tracks orderIds the user has already reviewed in this session (avoids re-fetch). */
const reviewedOrderIds = ref(new Set())

const reviewDialog = reactive({
  visible:      false,
  loading:      false,
  rating:       0,
  comment:      '',
  purchaseRow:  null,  // the PurchasedItem row being reviewed
})

function openReviewDialog(row) {
  reviewDialog.purchaseRow = row
  reviewDialog.rating      = 0
  reviewDialog.comment     = ''
  reviewDialog.visible     = true
}

async function doSubmitReview() {
  if (!reviewDialog.rating) {
    ElMessage.warning('请先选择评分')
    return
  }
  reviewDialog.loading = true
  try {
    await createReview({
      orderId: reviewDialog.purchaseRow.orderId,
      rating:  reviewDialog.rating,
      comment: reviewDialog.comment.trim() || null,
    })
    // Mark this order as reviewed so the button disappears immediately
    reviewedOrderIds.value = new Set([...reviewedOrderIds.value, reviewDialog.purchaseRow.orderId])
    reviewDialog.visible = false
    ElMessage.success('评价已提交，感谢您的反馈！')
  } catch {
    // 409 "already reviewed" or other errors → interceptor already showed the message
  } finally {
    reviewDialog.loading = false
  }
}

/** Called when user picks a new avatar file from the el-upload widget. */
async function onAvatarChange(uploadFile) {
  const file = uploadFile.raw
  if (!file) return
  try {
    const avatarUrl = await uploadImage(file)
    const updatedUser = await updateProfileApi({ avatarUrl })
    updateUser(updatedUser)
    ElMessage.success('头像已更新！')
  } catch {
    // interceptor already notified the user
  }
}

/** Saves the bio text the user typed. */
async function doSaveProfile() {
  if (!bioInput.value.trim()) {
    ElMessage.warning('简介不能为空')
    return
  }
  savingProfile.value = true
  try {
    const updatedUser = await updateProfileApi({ bio: bioInput.value.trim() })
    updateUser(updatedUser)
    ElMessage.success('个人简介已保存！')
  } catch {
    // interceptor already notified the user
  } finally {
    savingProfile.value = false
  }
}

async function fetchMyItems() {
  loadingItems.value = true
  try {
    myItems.value = await listMyItems()
  } catch {
    // interceptor already showed the error
  } finally {
    loadingItems.value = false
  }
}

async function fetchMyBids() {
  loadingBids.value = true
  try {
    myBids.value = await listMyBids()
  } catch {
    // interceptor already showed the error
  } finally {
    loadingBids.value = false
  }
}

async function fetchMyPurchases() {
  loadingPurchases.value = true
  try {
    myPurchases.value = await listPurchased()
  } catch {
    // interceptor already showed the error
  } finally {
    loadingPurchases.value = false
  }
}

async function openBidsDialog(auction) {
  bidsDialog.auction  = auction
  bidsDialog.bids     = []
  bidsDialog.visible  = true
  bidsDialog.loading  = true
  try {
    bidsDialog.bids = await getAuctionBids(auction.id)
  } catch {
    // interceptor handles messaging
  } finally {
    bidsDialog.loading = false
  }
}

async function doAcceptBid(auctionId, bidId) {
  accepting.value = bidId
  // Capture bid amount before the dialog data may change
  const acceptedBid = bidsDialog.bids.find(b => b.id === bidId)
  try {
    await acceptBid(auctionId, bidId)
    // Sync the seller's displayed balance immediately (credit the accepted amount)
    if (acceptedBid) {
      updateBalance(Number(currentUser.value?.balance ?? 0) + Number(acceptedBid.amount))
    }
    ElMessage.success('出价已接受，拍卖已成功成交！')
    bidsDialog.visible = false
    await fetchMyItems()
  } catch {
    // 400 "Insufficient balance" → http.js interceptor shows 余额不足…
    // other errors → also handled by interceptor
  } finally {
    accepting.value = null
  }
}

function openEditDialog(auction) {
  editDialog.auction = auction
  editDialog.visible = true
}

async function doCancelItem(auctionId) {
  cancelling.value = auctionId
  try {
    await cancelAuction(auctionId)
    ElMessage.success('商品已成功下架。')
    await fetchMyItems()
  } catch {
    // interceptor handles messaging
  } finally {
    cancelling.value = null
  }
}

async function doRecharge() {
  if (!rechargeDialog.amount) return
  rechargeDialog.loading = true
  try {
    const updatedUser = await rechargeApi(rechargeDialog.amount)
    updateBalance(updatedUser.balance)
    rechargeDialog.visible = false
    rechargeDialog.amount  = null
    ElMessage.success(`充值成功！当前余额 ¥${updatedUser.balance.toFixed(2)}`)
  } catch {
    // interceptor handles error messaging
  } finally {
    rechargeDialog.loading = false
  }
}

onMounted(() => {
  fetchMyItems()
  fetchMyBids()
  fetchMyPurchases()
})
</script>

<style scoped>
.profile-page { padding-top: 8px; }

.profile-hero {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 16px 0 24px;
  gap: 16px;
  flex-wrap: wrap;
}
.hero-left {
  display: flex;
  align-items: center;
  gap: 18px;
}

/* ── Avatar ── */
.avatar-wrap {
  position: relative;
  width: 72px;
  height: 72px;
  border-radius: 50%;
  overflow: hidden;
  cursor: pointer;
  flex-shrink: 0;
  background: #e4e7ed;
  border: 2px solid #dcdfe6;
}
.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.avatar-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  font-weight: 700;
  color: #909399;
  background: #e4e7ed;
  user-select: none;
}
.avatar-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0,0,0,.45);
  color: #fff;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  font-size: 11px;
  opacity: 0;
  transition: opacity .2s;
}
.avatar-wrap:hover .avatar-overlay { opacity: 1; }

.profile-title { margin: 0; font-size: 24px; font-weight: 700; color: #1d3557; }
.profile-sub   { margin: 4px 0 0; color: #909399; font-size: 14px; }

.balance-section {
  display: flex;
  align-items: center;
  gap: 10px;
}

/* ── Profile info tab ── */
.info-card { border-radius: 8px; }
.current-bio { margin-top: 4px; }
.bio-label { margin: 0 0 6px; font-size: 13px; color: #909399; }
.bio-text  { margin: 0; color: #303133; line-height: 1.7; white-space: pre-wrap; }

.profile-tabs { margin-top: 4px; }

.price-cell    { font-weight: 600; color: #e6a23c; }
.earnings-cell { font-weight: 700; color: #67c23a; }
.my-bid-cell   { font-weight: 600; color: #409eff; }
.bid-amount    { color: #67c23a; }
.no-action     { color: #c0c4cc; font-size: 13px; }

.recharge-body  { padding: 8px 0; }
.recharge-label { margin: 0 0 12px; color: #606266; font-size: 14px; }
.preset-amounts { display: flex; gap: 12px; margin-bottom: 4px; }
.preset-amounts .el-button { flex: 1; font-size: 15px; font-weight: 600; }

/* ── Review dialog ── */
.review-body        { padding: 4px 0 8px; }
.review-item-name   {
  margin: 0 0 16px;
  font-weight: 600;
  color: #1d3557;
  font-size: 15px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.reviewed-tag { font-size: 12px; color: #67c23a; font-weight: 600; }
</style>
