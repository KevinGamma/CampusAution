<template>
  <div>
    <!-- Welcome banner -->
    <el-alert
      v-if="isLoggedIn"
      :title="`欢迎回来，${currentUser.username}！浏览可直接购买的商品。`"
      type="success"
      :closable="true"
      show-icon
      class="welcome-alert"
    />

    <!-- Search / Filter / Sort bar -->
    <SearchFilterBar @search="handleSearch" />

    <!-- Header row -->
    <div class="page-header">
      <h2 class="page-title">直购市场</h2>
      <el-tag type="warning" size="large">{{ items.length }} 件在售</el-tag>
    </div>

    <!-- Grid -->
    <div v-loading="loading" element-loading-text="加载中…" class="grid-wrapper">
      <el-row :gutter="20">
        <el-col
          v-for="item in items"
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

      <el-empty
        v-if="!loading && items.length === 0"
        description="该条件下暂无可购商品"
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
const items   = ref([])
const loading = ref(false)

async function handleSearch(params) {
  loading.value = true
  try {
    items.value = await listActive({ ...params, saleType: 'DIRECT' })
    if (items.value.length === 0 && hasActiveFilter(params)) {
      ElMessage.info('没有符合条件的商品，请尝试调整筛选条件')
    }
  } catch {
    // global interceptor already showed the error
  } finally {
    loading.value = false
  }
}

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
