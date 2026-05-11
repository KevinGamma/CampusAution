<template>
  <div class="sfb-wrap">

    <!-- ── Row 1: keyword + sort ─────────────────────────────────────────── -->
    <div class="sfb-row sfb-row-top">
      <el-input
        v-model="filter.keyword"
        placeholder="搜索商品名称或描述…"
        :prefix-icon="Search"
        clearable
        class="sfb-search"
        @input="onKeyword"
        @clear="triggerSearch"
      />
      <el-select
        v-model="filter.sort"
        class="sfb-sort"
        @change="triggerSearch"
      >
        <el-option
          v-for="s in SORT_OPTIONS"
          :key="s.value"
          :label="s.label"
          :value="s.value"
        />
      </el-select>
    </div>

    <!-- ── Row 2: categories + price range + date range ──────────────────── -->
    <div class="sfb-row sfb-row-filters">

      <!-- Category pills -->
      <div class="sfb-cats">
        <button
          v-for="cat in CATEGORY_LIST"
          :key="cat.value"
          class="cat-pill"
          :class="{ active: filter.category === cat.value }"
          @click="toggleCategory(cat.value)"
        >
          {{ cat.label }}
        </button>
      </div>

      <!-- Price range -->
      <div class="sfb-price">
        <span class="sfb-label">价格</span>
        <el-input-number
          v-model="filter.minPrice"
          :min="0"
          :controls="false"
          placeholder="最低"
          class="sfb-price-input"
          @change="triggerSearch"
        />
        <span class="sfb-sep">—</span>
        <el-input-number
          v-model="filter.maxPrice"
          :min="0"
          :controls="false"
          placeholder="最高"
          class="sfb-price-input"
          @change="triggerSearch"
        />
      </div>

      <!-- Date range -->
      <el-date-picker
        v-model="filter.dateRange"
        type="daterange"
        unlink-panels
        range-separator="至"
        start-placeholder="结束日期从"
        end-placeholder="结束日期至"
        class="sfb-date"
        @change="triggerSearch"
      />
    </div>

    <!-- ── Row 3: active filter chips + reset ────────────────────────────── -->
    <div v-if="hasActiveFilters" class="sfb-row sfb-row-tags">
      <el-tag
        v-if="filter.keyword"
        closable
        size="small"
        @close="clearField('keyword')"
      >
        "{{ filter.keyword }}"
      </el-tag>
      <el-tag
        v-if="filter.category"
        type="success"
        closable
        size="small"
        @close="clearField('category')"
      >
        {{ categoryLabel(filter.category) }}
      </el-tag>
      <el-tag
        v-if="filter.minPrice != null || filter.maxPrice != null"
        type="warning"
        closable
        size="small"
        @close="clearPriceRange"
      >
        ¥{{ filter.minPrice ?? 0 }} — {{ filter.maxPrice != null ? '¥' + filter.maxPrice : '不限' }}
      </el-tag>
      <el-tag
        v-if="filter.dateRange"
        type="info"
        closable
        size="small"
        @close="clearField('dateRange')"
      >
        {{ fmtDate(filter.dateRange[0]) }} 至 {{ fmtDate(filter.dateRange[1]) }}
      </el-tag>
      <el-button size="small" text type="danger" @click="resetFilters">清除全部</el-button>
    </div>

  </div>
</template>

<script setup>
import { reactive, computed, onMounted } from 'vue'
import { Search } from '@element-plus/icons-vue'

const emit = defineEmits(['search'])

// ── Constants ────────────────────────────────────────────────────────────────

const CATEGORY_LIST = [
  { value: '',            label: '全部'   },
  { value: 'Electronics', label: '电子产品' },
  { value: 'Books',       label: '图书教材' },
  { value: 'Daily Use',   label: '生活用品' },
  { value: 'Sports',      label: '体育器材' },
  { value: 'Others',      label: '其他类'  },
]

const SORT_OPTIONS = [
  { value: 'id_DESC',           label: '最新发布'   },
  { value: 'currentPrice_ASC',  label: '价格从低到高' },
  { value: 'currentPrice_DESC', label: '价格从高到低' },
  { value: 'endTime_ASC',       label: '即将结束'   },
]

// ── State ────────────────────────────────────────────────────────────────────

const filter = reactive({
  keyword:   '',
  category:  '',        // '' = all categories
  minPrice:  undefined,
  maxPrice:  undefined,
  dateRange: null,      // [Date, Date] | null
  sort:      'id_DESC', // compound "sortBy_order"
})

// ── Computed ─────────────────────────────────────────────────────────────────

const hasActiveFilters = computed(() =>
  filter.keyword ||
  filter.category ||
  filter.minPrice  != null ||
  filter.maxPrice  != null ||
  filter.dateRange
)

// ── Helpers ──────────────────────────────────────────────────────────────────

function categoryLabel(val) {
  return CATEGORY_LIST.find(c => c.value === val)?.label ?? val
}

function fmtDate(d) {
  if (!d) return ''
  return new Date(d).toISOString().slice(0, 10)
}

// ── Debounce ─────────────────────────────────────────────────────────────────

let _debounceTimer = null
function onKeyword() {
  clearTimeout(_debounceTimer)
  _debounceTimer = setTimeout(triggerSearch, 300)
}

// ── Search emission ───────────────────────────────────────────────────────────

function triggerSearch() {
  const [sortBy, order] = filter.sort.split('_')
  const params = {}
  if (filter.keyword)           params.keyword   = filter.keyword.trim()
  if (filter.category)          params.category  = filter.category
  if (filter.minPrice  != null) params.minPrice  = filter.minPrice
  if (filter.maxPrice  != null) params.maxPrice  = filter.maxPrice
  if (filter.dateRange) {
    params.startDate = fmtDate(filter.dateRange[0])
    params.endDate   = fmtDate(filter.dateRange[1])
  }
  params.sortBy = sortBy
  params.order  = order
  emit('search', params)
}

// ── Interactions ─────────────────────────────────────────────────────────────

function toggleCategory(val) {
  filter.category = filter.category === val ? '' : val
  triggerSearch()
}

function clearField(key) {
  filter[key] = key === 'category' ? '' : (key === 'dateRange' ? null : '')
  triggerSearch()
}

function clearPriceRange() {
  filter.minPrice = undefined
  filter.maxPrice = undefined
  triggerSearch()
}

function resetFilters() {
  filter.keyword   = ''
  filter.category  = ''
  filter.minPrice  = undefined
  filter.maxPrice  = undefined
  filter.dateRange = null
  filter.sort      = 'id_DESC'
  triggerSearch()
}

// ── Init ─────────────────────────────────────────────────────────────────────

onMounted(() => triggerSearch())
</script>

<style scoped>
.sfb-wrap {
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 10px;
  padding: 14px 18px 10px;
  margin: 20px 0 0;
  box-shadow: 0 2px 8px rgba(0,0,0,.04);
}

/* ── Rows ─────────────────────────────────────────────────────────────────── */
.sfb-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}
.sfb-row + .sfb-row { margin-top: 10px; }

/* ── Row 1 ───────────────────────────────────────────────────────────────── */
.sfb-search { flex: 1 1 220px; }
.sfb-sort   { width: 150px; flex-shrink: 0; }

/* ── Row 2 ───────────────────────────────────────────────────────────────── */
.sfb-cats {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  flex: 1 1 auto;
}
.cat-pill {
  padding: 4px 14px;
  border-radius: 20px;
  border: 1px solid #dcdfe6;
  background: #fff;
  color: #606266;
  font-size: 13px;
  cursor: pointer;
  transition: all .18s;
  white-space: nowrap;
}
.cat-pill:hover  { border-color: #409eff; color: #409eff; }
.cat-pill.active { background: #409eff; border-color: #409eff; color: #fff; font-weight: 600; }

.sfb-price {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}
.sfb-label { font-size: 13px; color: #909399; white-space: nowrap; }
.sfb-price-input { width: 88px; }
.sfb-sep   { color: #c0c4cc; }

.sfb-date { width: 260px; flex-shrink: 0; }

/* ── Row 3 (tags) ────────────────────────────────────────────────────────── */
.sfb-row-tags { padding-top: 2px; border-top: 1px solid #f2f3f5; margin-top: 8px; }

/* ── Responsive ──────────────────────────────────────────────────────────── */
@media (max-width: 768px) {
  .sfb-sort  { width: 100%; }
  .sfb-date  { width: 100%; }
  .sfb-price { width: 100%; }
}
</style>
