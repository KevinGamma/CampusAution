<template>
  <div>
    <!-- SDU Hero Banner -->
    <div class="sdu-hero">
      <div class="sdu-hero-content">
        <h1 class="sdu-platform-name">山大闲置淘</h1>
        <p class="sdu-campus-label">软件园校区共享空间</p>
        <p class="sdu-motto">学无止境，气有浩然</p>
      </div>
    </div>

    <!-- Welcome banner -->
    <el-alert
      v-if="isLoggedIn"
      :title="`欢迎回来，${currentUser.username}！浏览并参与竞拍。`"
      type="success"
      :closable="true"
      show-icon
      class="welcome-alert"
    />

    <!-- Search / Filter / Sort bar -->
    <SearchFilterBar @search="handleSearch" />

    <!-- Header row -->
    <div class="page-header">
      <h2 class="page-title">拍卖广场</h2>
      <el-tag size="large" class="sdu-count-tag">{{ auctions.length }} 件在拍</el-tag>
    </div>

    <!-- Grid -->
    <div v-loading="loading" element-loading-text="加载中…" class="grid-wrapper">
      <el-row :gutter="20">
        <el-col
          v-for="auction in auctions"
          :key="auction.id"
          :xs="24" :sm="12" :md="8" :lg="6"
          class="card-col"
        >
          <AuctionCard
            :auction="auction"
            @click="router.push(`/auctions/${auction.id}`)"
          />
        </el-col>
      </el-row>

      <el-empty
        v-if="!loading && auctions.length === 0"
        description="该条件下暂无进行中的拍卖"
        :image-size="160"
      />
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { listActive } from '../api/auction.js'
import { useAuth } from '../composables/useAuth.js'
import AuctionCard from '../components/AuctionCard.vue'
import SearchFilterBar from '../components/SearchFilterBar.vue'

const router = useRouter()
const { currentUser, isLoggedIn } = useAuth()
const auctions = ref([])
const loading  = ref(false)

// Called by SearchFilterBar on every filter change (debounced for keyword)
async function handleSearch(params) {
  loading.value = true
  try {
    auctions.value = await listActive({ ...params, saleType: 'AUCTION' })
    if (auctions.value.length === 0 && hasActiveFilter(params)) {
      ElMessage.info('没有符合条件的拍卖商品，请尝试调整筛选条件')
    }
  } catch {
    // global interceptor already showed the error
  } finally {
    loading.value = false
  }
}

// Returns true when the user has set any filter beyond the default sort
function hasActiveFilter(params) {
  return !!(
    params.keyword ||
    params.category ||
    params.minPrice != null ||
    params.maxPrice != null ||
    params.startDate ||
    params.endDate
  )
}
</script>

<style scoped>
/* ── SDU Hero Banner ── */
.sdu-hero {
  background: linear-gradient(135deg, #800000 0%, #a00000 60%, #6b0000 100%);
  border-radius: 10px;
  padding: 32px 40px;
  margin: 20px 0 0;
  position: relative;
  overflow: hidden;
}
.sdu-hero::before {
  content: '山东大学';
  position: absolute;
  right: 32px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 72px;
  font-weight: 900;
  color: rgba(255, 255, 255, .06);
  letter-spacing: 4px;
  pointer-events: none;
  user-select: none;
}
.sdu-hero-content { position: relative; z-index: 1; }
.sdu-platform-name {
  margin: 0;
  font-size: 32px;
  font-weight: 800;
  color: #fff;
  letter-spacing: 2px;
}
.sdu-campus-label {
  margin: 6px 0 0;
  font-size: 14px;
  color: rgba(255, 255, 255, .75);
  letter-spacing: 1px;
}
.sdu-motto {
  margin: 12px 0 0;
  font-size: 15px;
  color: rgba(255, 255, 255, .9);
  font-style: italic;
  letter-spacing: 3px;
  border-left: 3px solid rgba(255, 255, 255, .5);
  padding-left: 12px;
}

/* ── Page chrome ── */
.welcome-alert { margin: 16px 0 0; border-radius: 8px; }
.page-header   { display: flex; align-items: center; gap: 14px; margin: 20px 0; }
.page-title    { margin: 0; font-size: 26px; font-weight: 700; color: #800000; }
.sdu-count-tag {
  --el-tag-bg-color: rgba(128, 0, 0, .1);
  --el-tag-border-color: rgba(128, 0, 0, .3);
  --el-tag-text-color: #800000;
}
.grid-wrapper  { min-height: 300px; }
.card-col      { margin-bottom: 20px; }
</style>
