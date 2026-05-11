<template>
  <el-dialog
    v-model="visible"
    title="编辑商品"
    width="580px"
    destroy-on-close
    @open="resetForm"
  >
    <el-alert
      v-if="hasBids"
      type="warning"
      :closable="false"
      style="margin-bottom: 20px"
      description="该拍卖已有出价，为保护竞拍者，价格和截止时间已锁定。"
    />

    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-position="top"
      size="default"
    >
      <el-form-item label="商品名称" prop="title">
        <el-input v-model="form.title" clearable />
      </el-form-item>

      <el-form-item label="详细描述" prop="description">
        <el-input v-model="form.description" type="textarea" :rows="3" />
      </el-form-item>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="分类" prop="category">
            <el-select v-model="form.category" style="width: 100%">
              <el-option v-for="c in CATEGORIES" :key="c.value" :label="c.label" :value="c.value" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="数量" prop="quantity">
            <el-input-number v-model="form.quantity" :min="1" :precision="0" style="width: 100%" />
          </el-form-item>
        </el-col>
      </el-row>

      <!-- Price: always shown for DIRECT; shown for AUCTION only when no bids -->
      <el-form-item
        v-if="showPriceField"
        :label="isDirect ? '售价（¥）' : '起拍价（¥）'"
        prop="startPrice"
      >
        <el-input-number
          v-model="form.startPrice"
          :min="0.01"
          :precision="2"
          :step="1"
          style="width: 100%"
          controls-position="right"
        />
      </el-form-item>

      <!-- End time: shown for AUCTION only when no bids -->
      <el-form-item v-if="showEndTimeField" label="拍卖截止时间" prop="endTime">
        <el-date-picker
          v-model="form.endTime"
          type="datetime"
          placeholder="请选择截止日期和时间"
          :disabled-date="isPastDate"
          style="width: 100%"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="save">保存修改</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { updateAuction } from '../api/auction.js'

const CATEGORIES = [
  { label: '电子产品', value: 'Electronics' },
  { label: '图书教材', value: 'Books' },
  { label: '生活用品', value: 'Daily Use' },
  { label: '体育器材', value: 'Sports' },
  { label: '其他类',   value: 'Others' },
]

const props = defineProps({
  modelValue: { type: Boolean, required: true },
  auction:    { type: Object,  default: null  },
})
const emit = defineEmits(['update:modelValue', 'updated'])

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
})

const formRef = ref(null)
const saving  = ref(false)

const form = reactive({
  title:       '',
  description: '',
  category:    '',
  quantity:    1,
  startPrice:  null,
  endTime:     null,
})

const hasBids      = computed(() => (props.auction?.bidCount ?? 0) > 0)
const isDirect     = computed(() => props.auction?.saleType === 'DIRECT')
const showPriceField   = computed(() => isDirect.value || !hasBids.value)
const showEndTimeField = computed(() => !isDirect.value && !hasBids.value)

const isPastDate = (date) => date < new Date(new Date().setHours(0, 0, 0, 0))

const rules = computed(() => ({
  title: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  description: [{ required: true, message: '请输入详细描述', trigger: 'blur' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }],
  quantity: [{
    validator: (_, v, cb) => v >= 1 ? cb() : cb(new Error('数量至少为 1')),
    trigger: 'change',
  }],
  startPrice: showPriceField.value ? [{
    validator: (_, v, cb) => v > 0 ? cb() : cb(new Error('价格须大于 0')),
    trigger: 'change',
  }] : [],
  endTime: showEndTimeField.value ? [{
    validator: (_, v, cb) => {
      if (!v) { cb(new Error('请选择截止时间')); return }
      if (new Date(v) <= new Date()) { cb(new Error('截止时间须为将来的时间')); return }
      cb()
    },
    trigger: 'change',
  }] : [],
}))

function resetForm() {
  const a = props.auction
  if (!a) return
  form.title       = a.title       ?? ''
  form.description = a.description ?? ''
  form.category    = a.category    ?? ''
  form.quantity    = a.quantity    ?? 1
  form.startPrice  = Number(a.startPrice) || null
  form.endTime     = a.endTime ? new Date(a.endTime) : null
  formRef.value?.clearValidate()
}

async function save() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    const payload = {
      title:       form.title,
      description: form.description,
      category:    form.category,
      quantity:    form.quantity,
    }
    if (showPriceField.value)   payload.startPrice = form.startPrice
    if (showEndTimeField.value) payload.endTime    = form.endTime

    await updateAuction(props.auction.id, payload)
    ElMessage.success('商品信息已更新！')
    emit('updated')
    visible.value = false
  } catch {
    // interceptor handles error messaging
  } finally {
    saving.value = false
  }
}
</script>
