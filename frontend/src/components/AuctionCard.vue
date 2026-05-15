<template>
  <el-card class="auction-card" shadow="hover" @click="$emit('click')">
    <!-- Product thumbnail (shown only when at least one image is available) -->
    <div v-if="firstImage" class="card-thumb-wrap">
      <img :src="firstImage" :alt="auction.title" class="card-thumb" />
    </div>

    <template #header>
      <div class="card-head">
        <span class="card-title" :title="auction.title">{{ auction.title }}</span>
        <div class="card-badges">
          <el-tag v-if="isDirect" type="warning" size="small" effect="dark" class="mode-tag">直购</el-tag>
          <el-tag :type="statusType" size="small" effect="light">{{ STATUS_LABEL[auction.status] ?? auction.status }}</el-tag>
        </div>
      </div>
    </template>

    <div class="card-body">
      <!-- DIRECT sale: show fixed selling price + Buy Now indicator -->
      <template v-if="isDirect">
        <div class="info-row">
          <span class="label">售价</span>
          <span class="price direct-price">¥{{ auction.currentPrice }}</span>
        </div>
        <div class="info-row">
          <span class="buy-now-hint">点击立即购买</span>
        </div>
      </template>

      <!-- AUCTION: show bid activity + countdown -->
      <template v-else>
        <div class="info-row">
          <span class="label">当前最高价</span>
          <span v-if="auction.bidCount > 0" class="price">¥{{ auction.currentPrice }}</span>
          <span v-else class="no-bids">暂无出价</span>
        </div>
        <div class="info-row">
          <span class="label">剩余时间</span>
          <span class="countdown" :class="{ urgent: isUrgent }">{{ formatted }}</span>
        </div>
      </template>

      <div class="card-footer">
        <el-tag v-if="auction.category" size="small" type="info" effect="plain" class="category-tag">
          {{ auction.category }}
        </el-tag>
        <span v-if="auction.quantity != null" class="stock-label">库存：{{ auction.quantity }}</span>
      </div>
    </div>
  </el-card>
</template>

<script setup>
import { computed } from 'vue'
import { useCountdown } from '../composables/useCountdown.js'

const props = defineProps({
  auction: { type: Object, required: true }
})
defineEmits(['click'])

const isDirect   = computed(() => props.auction.saleType === 'DIRECT')
const firstImage = computed(() => {
  const urls = props.auction.imageUrls
  if (Array.isArray(urls) && urls.length) return urls[0]
  return null
})

const { isUrgent, formatted } = useCountdown(() => props.auction.endTime)

const STATUS_TYPE  = { ACTIVE: 'success', FINISHED: 'info', SOLD: 'warning', CANCELLED: 'danger' }
const STATUS_LABEL = { ACTIVE: '进行中', FINISHED: '已结束', SOLD: '已售出', CANCELLED: '已取消' }
const statusType   = computed(() => STATUS_TYPE[props.auction.status] ?? 'info')
</script>

<style scoped>
.auction-card { cursor: pointer; transition: transform .2s, box-shadow .2s; border-radius: 8px; overflow: hidden; }
.auction-card:hover { transform: translateY(-4px); box-shadow: 0 8px 24px rgba(0,0,0,.12); }

/* ── Thumbnail ── */
.card-thumb-wrap {
  margin: -20px -20px 0;   /* bleed to card edges */
  height: 160px;
  overflow: hidden;
  background: #f5f7fa;
}
.card-thumb {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.card-head  { display: flex; justify-content: space-between; align-items: center; gap: 8px; }
.card-badges { display: flex; gap: 4px; align-items: center; flex-shrink: 0; }
.mode-tag   { font-size: 11px; }
.card-title {
  font-weight: 600;
  font-size: 15px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 160px;
  color: #3d0000;
}

.card-body  { display: flex; flex-direction: column; gap: 10px; }
.info-row   { display: flex; justify-content: space-between; align-items: center; }
.label      { color: #909399; font-size: 13px; }

/* Auction bid price — SDU Gold */
.price      { font-weight: 700; font-size: 20px; color: #b8960f; }

/* Direct sale — campus store price label */
.direct-price {
  font-weight: 800;
  font-size: 20px;
  color: #800000;
  background: rgba(128,0,0,.07);
  border: 1px solid rgba(128,0,0,.2);
  border-radius: 4px;
  padding: 1px 8px;
  letter-spacing: .5px;
}

.no-bids      { font-style: italic; font-size: 13px; color: #c0c4cc; }
.buy-now-hint { font-size: 13px; color: #800000; font-weight: 600; }
.countdown    { font-family: 'Courier New', monospace; font-size: 14px; color: #606266; font-weight: 600; }
.countdown.urgent { color: #f56c6c; animation: pulse 1s ease-in-out infinite; }

.card-footer  { display: flex; align-items: center; justify-content: space-between; margin-top: 2px; }
.category-tag { flex-shrink: 0; }
.stock-label  { font-size: 12px; color: #909399; }

@keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: .5; } }
</style>
