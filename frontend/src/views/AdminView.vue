<template>
  <div v-loading.fullscreen.lock="seeding" element-loading-text="正在生成模拟数据…">

    <!-- ── SDU Identity Stripe ────────────────────────────────────────────────── -->
    <div class="sdu-stripe">
      <span class="sdu-emblem">山东大学 · 校园集市</span>
      <span class="sdu-stripe-motto">学无止境，气有浩然</span>
    </div>

    <!-- ── Admin header ──────────────────────────────────────────────────────── -->
    <div class="admin-hero">
      <div>
        <h2 class="admin-title">山东大学校园集市数据管理与审计中心</h2>
        <p class="admin-sub">Shandong University Campus Marketplace — Software Campus Audit Console</p>
      </div>
      <el-tag effect="dark" size="large" class="mode-tag">管理员模式</el-tag>
    </div>

    <!-- ── Stats row ─────────────────────────────────────────────────────────── -->
    <el-row :gutter="16" class="stats-row">
      <el-col :span="6" v-for="s in stats" :key="s.label">
        <el-card class="stat-card" shadow="never" :body-style="{ padding: '20px' }">
          <div class="stat-value" :style="{ color: s.color }">{{ s.value }}</div>
          <div class="stat-label">{{ s.label }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- ── Auction table ──────────────────────────────────────────────────────── -->
    <el-card class="table-card">
      <template #header>
        <div class="table-hd">
          <span class="table-title">全部商品</span>
          <el-button size="small" :loading="loading" @click="loadAuctions">
            刷新
          </el-button>
        </div>
      </template>

      <el-table
        :data="auctions"
        v-loading="loading"
        stripe
        border
        style="width: 100%"
        :header-cell-style="{ background: '#f5f7fa', color: '#606266', fontWeight: '600' }"
      >
        <el-table-column prop="id"           label="编号"     width="70"  align="center" />
        <el-table-column prop="title"        label="商品名称"  min-width="200" show-overflow-tooltip />
        <el-table-column prop="category"     label="分类"     width="110" align="center">
          <template #default="{ row }">
            <el-tag size="small" type="info" effect="plain">{{ row.category ?? '—' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="quantity"     label="数量"     width="70"  align="center" />
        <el-table-column prop="creatorId"    label="发布者"   width="90"  align="center">
          <template #default="{ row }">
            <el-tag size="small" type="info">#{{ row.creatorId }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="currentPrice" label="价格"     width="110" align="right">
          <template #default="{ row }">
            <span class="price-cell">¥{{ row.currentPrice }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status"       label="状态"     width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="STATUS_TYPE[row.status]" size="small" effect="light">
              {{ STATUS_LABEL[row.status] ?? row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="endTime"      label="结束时间"  width="175">
          <template #default="{ row }">{{ row.endTime?.replace('T', ' ') }}</template>
        </el-table-column>
        <el-table-column label="操作" width="140" align="center" fixed="right">
          <template #default="{ row }">
            <el-popconfirm
              :title="`确认强制下架「${row.title}」及其所有出价？`"
              confirm-button-text="确认删除"
              confirm-button-type="danger"
              cancel-button-text="取消"
              width="280"
              @confirm="forceDelete(row.id)"
            >
              <template #reference>
                <el-button type="danger" size="small" plain>强制下架</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- ── System Tools ───────────────────────────────────────────────────────── -->
    <el-card class="tools-card">
      <template #header>
        <div class="table-hd">
          <span class="table-title">系统工具</span>
          <el-tag type="warning" size="small" effect="plain">仅管理员</el-tag>
        </div>
      </template>

      <div class="tools-body">
        <p class="tools-desc">
          批量生成学生账户及商品列表，用于压力测试或演示。所有数据遵循现有外键约束。
        </p>

        <!-- Row 1: user/auction params -->
        <div class="seed-controls">
          <div class="seed-field">
            <span class="seed-label">创建用户数</span>
            <el-input-number
              v-model="seedForm.userCount"
              :min="1" :max="200" :precision="0"
              size="default" style="width: 150px"
            />
          </div>

          <div class="seed-field">
            <span class="seed-label">每用户拍卖数</span>
            <el-input-number
              v-model="seedForm.auctionsPerUser"
              :min="1" :max="50" :precision="0"
              size="default" style="width: 150px"
            />
          </div>

          <div class="seed-field">
            <span class="seed-label">直购商品数量</span>
            <el-input-number
              v-model="seedForm.directSaleCount"
              :min="0" :max="500" :precision="0"
              size="default" style="width: 150px"
            />
          </div>

          <div class="seed-field seed-summary">
            <span class="seed-label">预计生成总数</span>
            <strong class="seed-total">
              {{ seedTotal }} 条商品 + {{ seedForm.userCount }} 名用户
            </strong>
          </div>
        </div>

        <!-- Row 2: action buttons -->
        <div class="seed-actions">
          <div class="seed-btn-group">
            <el-button
              type="primary"
              :loading="seeding"
              @click="runSeed(false)"
            >
              初始化模拟数据
            </el-button>
            <span class="btn-hint">仅生成拍卖（忽略直购数量）</span>
          </div>

          <div class="seed-btn-group">
            <el-button
              type="warning"
              :loading="seeding"
              @click="runSeed(true)"
            >
              生成混合市场数据
            </el-button>
            <span class="btn-hint">同时生成拍卖与直购商品</span>
          </div>
        </div>
      </div>
    </el-card>

  </div>
</template>

<script setup>
import { ref, computed, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { deleteAuction } from '../api/auction.js'
import { listAll, seedData } from '../api/admin.js'

const STATUS_TYPE  = { ACTIVE: 'success', FINISHED: 'info', SOLD: 'warning', CANCELLED: 'danger' }
const STATUS_LABEL = { ACTIVE: '进行中', FINISHED: '已结束', SOLD: '已售出', CANCELLED: '已取消' }

const auctions = ref([])
const loading  = ref(false)
const seeding  = ref(false)

const seedForm = reactive({ userCount: 10, auctionsPerUser: 5, directSaleCount: 20 })
const seedTotal = computed(() => seedForm.userCount * seedForm.auctionsPerUser + seedForm.directSaleCount)

const stats = computed(() => {
  const all      = auctions.value
  const byStatus = s => all.filter(a => a.status === s).length
  return [
    { label: '商品总数', value: all.length,            color: '#303133' },
    { label: '进行中',   value: byStatus('ACTIVE'),    color: '#67c23a' },
    { label: '已售出',   value: byStatus('SOLD'),      color: '#e6a23c' },
    { label: '已取消',   value: byStatus('CANCELLED'), color: '#f56c6c' },
  ]
})

const loadAuctions = async () => {
  loading.value = true
  try {
    auctions.value = await listAll()
  } catch {
    // interceptor already notified the user
  } finally {
    loading.value = false
  }
}

const forceDelete = async (id) => {
  try {
    await deleteAuction(id)
    ElMessage.success('拍卖已成功强制下架')
    auctions.value = auctions.value.filter(a => a.id !== id)
  } catch {
    // interceptor handles messaging
  }
}

const runSeed = async (mixed = false) => {
  seeding.value = true
  try {
    const directSaleCount = mixed ? seedForm.directSaleCount : 0
    const result = await seedData(seedForm.userCount, seedForm.auctionsPerUser, 30, directSaleCount)
    const label = mixed ? '混合市场数据' : '模拟数据'
    ElMessage.success(
      `${label} 完成：已创建 ${result.usersCreated} 名用户和 ${result.auctionsCreated} 条商品。`
    )
    await loadAuctions()
  } catch {
    // interceptor handles messaging
  } finally {
    seeding.value = false
  }
}

onMounted(loadAuctions)
</script>

<style scoped>
/* ── SDU Identity Stripe ── */
.sdu-stripe {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #800000;
  border-radius: 8px 8px 0 0;
  padding: 8px 20px;
  margin: 20px 0 0;
}
.sdu-emblem {
  color: #fff;
  font-size: 14px;
  font-weight: 700;
  letter-spacing: 2px;
}
.sdu-stripe-motto {
  color: rgba(255, 255, 255, .75);
  font-size: 12px;
  font-style: italic;
  letter-spacing: 2px;
}

/* ── Admin hero ── */
.admin-hero {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fff;
  border: 1px solid rgba(128, 0, 0, .2);
  border-top: none;
  border-radius: 0 0 8px 8px;
  padding: 20px 24px;
  margin: 0 0 20px;
}
.admin-title { margin: 0; font-size: 20px; font-weight: 700; color: #800000; }
.admin-sub   { margin: 4px 0 0; color: #909399; font-size: 13px; }
.mode-tag {
  font-size: 13px;
  padding: 6px 14px;
  letter-spacing: 1px;
  background: #800000;
  border-color: #800000;
  color: #fff;
  flex-shrink: 0;
}

.stats-row  { margin-bottom: 20px; }
.stat-card  { border-radius: 8px; text-align: center; }
.stat-value { font-size: 32px; font-weight: 700; line-height: 1.2; }
.stat-label { font-size: 13px; color: #909399; margin-top: 6px; }

.table-card  { border-radius: 8px; }
.table-hd    { display: flex; justify-content: space-between; align-items: center; }
.table-title { font-size: 16px; font-weight: 600; color: #303133; }
.price-cell  { font-weight: 600; color: #e6a23c; }

.tools-card { border-radius: 8px; margin-top: 20px; }

.tools-desc {
  margin: 0 0 20px;
  color: #606266;
  font-size: 14px;
  line-height: 1.6;
}

.seed-controls {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  gap: 24px;
}

.seed-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.seed-label {
  font-size: 13px;
  color: #909399;
  font-weight: 500;
}

.seed-total {
  font-size: 15px;
  color: #303133;
}

.seed-summary { justify-content: flex-end; }

.seed-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 24px;
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid #f0f0f0;
}

.seed-btn-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.btn-hint {
  font-size: 12px;
  color: #c0c4cc;
}
</style>
