<template>
  <div>
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
      <el-tag type="success" size="large">{{ auctions.length }} 件在拍</el-tag>
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
.welcome-alert { margin: 20px 0 0; border-radius: 8px; }
.page-header   { display: flex; align-items: center; gap: 14px; margin: 20px 0; }
.page-title    { margin: 0; font-size: 26px; font-weight: 700; color: #1d3557; }
.grid-wrapper  { min-height: 300px; }
.card-col      { margin-bottom: 20px; }
</style>
