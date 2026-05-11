<template>
  <div class="publish-page">
    <el-card class="publish-card">
      <template #header>
        <div class="card-header">
          <el-page-header @back="router.push('/')" title="返回" content="发布商品" />
        </div>
      </template>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        size="large"
        class="publish-form"
      >
        <!-- Sale type selector -->
        <el-form-item label="发布类型" prop="saleType">
          <el-radio-group v-model="form.saleType" size="large">
            <el-radio-button value="AUCTION">竞拍</el-radio-button>
            <el-radio-button value="DIRECT">直购</el-radio-button>
          </el-radio-group>
          <div class="type-hint">
            <span v-if="form.saleType === 'AUCTION'">买家通过竞价参与，最高价者赢得拍卖。</span>
            <span v-else>买家可按固定价格立即购买。</span>
          </div>
        </el-form-item>

        <el-form-item label="商品名称" prop="title">
          <el-input
            v-model="form.title"
            placeholder="请输入清晰的商品名称"
            clearable
          />
        </el-form-item>

        <el-form-item label="详细描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="4"
            placeholder="请描述商品状态、特征及其他相关信息"
          />
        </el-form-item>

        <el-row :gutter="24">
          <el-col :xs="24" :sm="12">
            <el-form-item :label="form.saleType === 'DIRECT' ? '售价（¥）' : '起拍价（¥）'" prop="startPrice">
              <el-input-number
                v-model="form.startPrice"
                :min="0.01"
                :precision="2"
                :step="1"
                placeholder="0.00"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>

          <el-col :xs="24" :sm="12">
            <el-form-item label="数量" prop="quantity">
              <el-input-number
                v-model="form.quantity"
                :min="1"
                :precision="0"
                :step="1"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="24">
          <el-col :xs="24" :sm="12">
            <el-form-item label="分类" prop="category">
              <el-select v-model="form.category" placeholder="请选择分类" style="width: 100%">
                <el-option
                  v-for="cat in CATEGORIES"
                  :key="cat.value"
                  :label="cat.label"
                  :value="cat.value"
                />
              </el-select>
            </el-form-item>
          </el-col>

          <el-col v-if="form.saleType === 'AUCTION'" :xs="24" :sm="12">
            <el-form-item label="拍卖截止时间" prop="endTime">
              <el-date-picker
                v-model="form.endTime"
                type="datetime"
                placeholder="请选择截止日期和时间"
                :disabled-date="isPastDate"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item class="submit-row">
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            class="btn-submit"
            @click="submit"
          >
            {{ form.saleType === 'DIRECT' ? '发布直购商品' : '发布拍卖' }}
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createAuction } from '../api/auction.js'

const CATEGORIES = [
  { label: '电子产品', value: 'Electronics' },
  { label: '图书教材', value: 'Books' },
  { label: '生活用品', value: 'Daily Use' },
  { label: '体育器材', value: 'Sports' },
  { label: '其他类',   value: 'Others' },
]

const router  = useRouter()
const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  title:       '',
  description: '',
  startPrice:  null,
  quantity:    1,
  category:    '',
  endTime:     null,
  saleType:    'AUCTION',
})

const isPastDate = (date) => date < new Date(new Date().setHours(0, 0, 0, 0))

const rules = {
  title: [
    { required: true, message: '请输入商品名称', trigger: 'blur' },
  ],
  description: [
    { required: true, message: '请输入详细描述', trigger: 'blur' },
  ],
  startPrice: [
    { required: true, message: '请输入起拍价', trigger: 'blur' },
    {
      validator: (_, value, callback) => {
        if (value == null || value <= 0) {
          callback(new Error('价格须大于 0'))
        } else {
          callback()
        }
      },
      trigger: 'change',
    },
  ],
  quantity: [
    { required: true, message: '请输入数量', trigger: 'blur' },
    {
      validator: (_, value, callback) => {
        if (value == null || value < 1) {
          callback(new Error('数量至少为 1'))
        } else {
          callback()
        }
      },
      trigger: 'change',
    },
  ],
  category: [
    { required: true, message: '请选择分类', trigger: 'change' },
  ],
  endTime: [
    {
      validator: (_, value, callback) => {
        if (form.saleType === 'DIRECT') { callback(); return }
        if (!value) { callback(new Error('请选择截止时间')); return }
        if (new Date(value) <= new Date()) { callback(new Error('截止时间须为将来的时间')); return }
        callback()
      },
      trigger: 'change',
    },
  ],
}

async function submit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const payload = {
      title:       form.title,
      description: form.description,
      startPrice:  form.startPrice,
      quantity:    form.quantity,
      category:    form.category,
      saleType:    form.saleType,
      endTime:     form.saleType === 'DIRECT' ? null : form.endTime,
    }
    const auction = await createAuction(payload)
    ElMessage.success('商品发布成功！')
    router.push(`/auctions/${auction.id}`)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.publish-page {
  max-width: 720px;
  margin: 32px auto;
  padding: 0 16px;
}
.publish-card {
  border-radius: 8px;
}
.card-header {
  padding: 4px 0;
}
.publish-form {
  margin-top: 8px;
}
.submit-row {
  margin-top: 8px;
}
.btn-submit {
  width: 100%;
}
.type-hint {
  margin-top: 8px;
  font-size: 13px;
  color: #909399;
  line-height: 1.5;
}
</style>
