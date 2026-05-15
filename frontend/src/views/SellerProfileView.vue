<template>
  <div class="seller-page">

    <!-- Loading skeleton -->
    <el-skeleton v-if="loading" :rows="5" animated style="max-width: 800px; margin: 40px auto;" />

    <!-- Profile not found -->
    <el-result
      v-else-if="!seller"
      icon="error"
      title="用户不存在"
      sub-title="该用户资料无法找到，可能已被删除。"
    >
      <template #extra>
        <el-button type="primary" @click="router.push('/')">返回首页</el-button>
      </template>
    </el-result>

    <!-- Profile content -->
    <template v-else>

      <!-- ── Hero card ─────────────────────────────────────────────────────── -->
      <el-card class="hero-card">
        <div class="hero-inner">
          <!-- Avatar -->
          <div class="hero-avatar-wrap">
            <img
              v-if="seller.avatarUrl"
              :src="seller.avatarUrl"
              class="hero-avatar"
              :alt="seller.username"
            />
            <div v-else class="hero-avatar-fallback">
              {{ (seller.username ?? '?')[0].toUpperCase() }}
            </div>
          </div>

          <!-- Info -->
          <div class="hero-info">
            <h2 class="hero-name">{{ seller.username }}</h2>

            <!-- Rating row -->
            <div class="hero-rating-row">
              <el-rate
                :model-value="seller.averageRating ?? 0"
                disabled
                show-score
                :colors="['#ff9900', '#ff9900', '#ff9900']"
                score-template="{value} 分"
              />
              <span class="review-count">
                {{ seller.reviewCount > 0
                  ? `（共 ${seller.reviewCount} 条评价）`
                  : '暂无评价' }}
              </span>
            </div>

            <!-- Bio -->
            <p v-if="seller.bio" class="hero-bio">{{ seller.bio }}</p>
            <p v-else class="hero-bio hero-bio--empty">该用户还没有填写简介。</p>

            <!-- Join date -->
            <p v-if="seller.createdAt" class="hero-joined">
              加入时间：{{ seller.createdAt.slice(0, 10) }}
            </p>
          </div>
        </div>
      </el-card>

      <!-- ── Tabs ──────────────────────────────────────────────────────────── -->
      <el-tabs v-model="activeTab" class="seller-tabs">

        <!-- ── Tab 1: Active listings ──────────────────────────────────────── -->
        <el-tab-pane name="listings">
          <template #label>
            <span>在售宝贝 <el-badge :value="auctions.length" :max="99" type="primary" /></span>
          </template>

          <div v-loading="loadingAuctions" class="grid-wrapper">
            <el-empty
              v-if="!loadingAuctions && auctions.length === 0"
              description="该卖家暂无在售商品"
              :image-size="140"
            />
            <el-row v-else :gutter="20">
              <el-col
                v-for="item in auctions"
                :key="item.id"
                :xs="24" :sm="12" :md="8" :lg="6"
                class="card-col"
              >
                <AuctionCard
                  :auction="item"
                  @click="router.push(`/auctions/${item.id}`)"
                />
              </el-col>
            </el-row>
          </div>
        </el-tab-pane>

        <!-- ── Tab 2: Reviews received ─────────────────────────────────────── -->
        <el-tab-pane name="reviews">
          <template #label>
            <span>用户评价 <el-badge :value="seller.reviewCount" :max="99" type="success" /></span>
          </template>

          <div v-loading="loadingReviews">
            <el-empty
              v-if="!loadingReviews && reviews.length === 0"
              description="该用户还没有收到任何评价"
              :image-size="140"
            />

            <div v-else class="review-list">
              <el-card
                v-for="r in reviews"
                :key="r.id"
                class="review-card"
                shadow="never"
              >
                <div class="review-header">
                  <!-- Reviewer avatar + name -->
                  <div class="reviewer-info">
                    <img
                      v-if="r.reviewerAvatar"
                      :src="r.reviewerAvatar"
                      class="reviewer-avatar"
                      :alt="r.reviewerName"
                    />
                    <div v-else class="reviewer-avatar-fallback">
                      {{ (r.reviewerName ?? '?')[0].toUpperCase() }}
                    </div>
                    <span class="reviewer-name">{{ r.reviewerName }}</span>
                  </div>

                  <!-- Rating + date -->
                  <div class="review-meta">
                    <el-rate :model-value="r.rating" disabled size="small" />
                    <span class="review-date">{{ r.createTime?.slice(0, 10) }}</span>
                  </div>
                </div>

                <p v-if="r.comment" class="review-comment">{{ r.comment }}</p>
                <p v-else class="review-comment review-comment--empty">（该用户未留下文字评价）</p>
              </el-card>
            </div>
          </div>
        </el-tab-pane>

      </el-tabs>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getSellerProfile, getSellerReviews, getSellerAuctions } from '../api/review.js'
import AuctionCard from '../components/AuctionCard.vue'

const route  = useRoute()
const router = useRouter()
const userId = route.params.userId

const loading        = ref(false)
const loadingAuctions = ref(false)
const loadingReviews  = ref(false)

const seller   = ref(null)
const auctions = ref([])
const reviews  = ref([])
const activeTab = ref('listings')

async function loadAll() {
  loading.value = true
  try {
    seller.value = await getSellerProfile(userId)
  } catch {
    // global interceptor shows the error; seller stays null → "not found" UI
  } finally {
    loading.value = false
  }

  if (!seller.value) return

  // Load listings and reviews in parallel
  loadingAuctions.value = true
  loadingReviews.value  = true

  getSellerAuctions(userId)
    .then(data => { auctions.value = data })
    .catch(() => {})
    .finally(() => { loadingAuctions.value = false })

  getSellerReviews(userId)
    .then(data => { reviews.value = data })
    .catch(() => {})
    .finally(() => { loadingReviews.value = false })
}

onMounted(loadAll)
</script>

<style scoped>
.seller-page { padding-top: 8px; }

/* ── Hero card ── */
.hero-card   { border-radius: 10px; margin-bottom: 24px; }
.hero-inner  { display: flex; gap: 24px; align-items: flex-start; flex-wrap: wrap; }

.hero-avatar-wrap {
  flex-shrink: 0;
  width: 100px;
  height: 100px;
  border-radius: 50%;
  overflow: hidden;
  border: 3px solid #e4e7ed;
  background: #f5f7fa;
}
.hero-avatar         { width: 100%; height: 100%; object-fit: cover; display: block; }
.hero-avatar-fallback {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 42px;
  font-weight: 700;
  color: #909399;
}

.hero-info        { flex: 1 1 280px; }
.hero-name        { margin: 0 0 8px; font-size: 24px; font-weight: 700; color: #1d3557; }
.hero-rating-row  { display: flex; align-items: center; gap: 8px; margin-bottom: 10px; }
.review-count     { font-size: 13px; color: #909399; }
.hero-bio         { margin: 0 0 8px; color: #606266; line-height: 1.7; white-space: pre-wrap; }
.hero-bio--empty  { color: #c0c4cc; font-style: italic; }
.hero-joined      { margin: 0; font-size: 12px; color: #c0c4cc; }

/* ── Tabs ── */
.seller-tabs  { margin-top: 4px; }
.grid-wrapper { min-height: 200px; }
.card-col     { margin-bottom: 20px; }

/* ── Review list ── */
.review-list { display: flex; flex-direction: column; gap: 12px; }
.review-card { border-radius: 8px; }

.review-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 10px;
}

.reviewer-info {
  display: flex;
  align-items: center;
  gap: 10px;
}
.reviewer-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid #e4e7ed;
}
.reviewer-avatar-fallback {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #e4e7ed;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 700;
  color: #909399;
  flex-shrink: 0;
}
.reviewer-name  { font-weight: 600; color: #303133; font-size: 14px; }

.review-meta   { display: flex; align-items: center; gap: 10px; }
.review-date   { font-size: 12px; color: #c0c4cc; }

.review-comment         { margin: 0; color: #606266; line-height: 1.7; }
.review-comment--empty  { color: #c0c4cc; font-style: italic; }
</style>
