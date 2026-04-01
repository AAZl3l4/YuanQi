<script setup>
import { ref, onMounted, computed } from 'vue'
import { getRelayConfigList, createRelayConfig, updateRelayConfig, deleteRelayConfig, getRelayConfig } from '@/api/relay'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const currentUserId = computed(() => userStore.user?.id)

const configs = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const formRef = ref()
const onlyMine = ref(true)
const isEdit = ref(false)

const form = ref({
  id: null,
  name: '',
  description: '',
  personaPrompt: '',
  isPublic: 0
})

const rules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  personaPrompt: [{ required: true, message: '请输入人设提示词', trigger: 'blur' }]
}

const loadConfigs = async () => {
  loading.value = true
  try {
    const res = await getRelayConfigList({ page: 1, size: 100, onlyMine: onlyMine.value })
    if (res.code === 200) {
      configs.value = res.data.records || []
    }
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const openCreateDialog = () => {
  isEdit.value = false
  form.value = { id: null, name: '', description: '', personaPrompt: '', isPublic: 0 }
  dialogVisible.value = true
}

const openEditDialog = async (id) => {
  try {
    const res = await getRelayConfig(id)
    if (res.code === 200) {
      isEdit.value = true
      form.value = {
        id: res.data.id,
        name: res.data.name,
        description: res.data.description || '',
        personaPrompt: res.data.personaPrompt,
        isPublic: res.data.isPublic || 0
      }
      dialogVisible.value = true
    }
  } catch (error) {
    ElMessage.error('获取配置信息失败')
  }
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  
  try {
    let res
    if (isEdit.value) {
      res = await updateRelayConfig(form.value)
    } else {
      res = await createRelayConfig(form.value)
    }
    if (res.code === 200) {
      ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
      dialogVisible.value = false
      loadConfigs()
    }
  } catch (error) {
    console.error(error)
  }
}

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定删除该配置吗？', '提示', { type: 'warning' })
    const res = await deleteRelayConfig(id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadConfigs()
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error(error)
    }
  }
}

onMounted(() => {
  loadConfigs()
})
</script>

<template>
  <div class="relay-config-view page-container">
    <div class="page-header">
      <h2 class="page-title">中转配置</h2>
      <div class="header-actions">
        <el-radio-group v-model="onlyMine" @change="loadConfigs" size="small">
          <el-radio-button :value="true">我的</el-radio-button>
          <el-radio-button :value="false">公开</el-radio-button>
        </el-radio-group>
        <el-button type="primary" @click="openCreateDialog">
          <el-icon><Plus /></el-icon>
          新建配置
        </el-button>
      </div>
    </div>
    
    <el-row :gutter="16" v-loading="loading">
      <el-col v-for="config in configs" :key="config.id" :span="8">
        <div class="config-card card">
          <div class="card-header">
            <h3>{{ config.name }}</h3>
            <div class="card-actions">
              <el-button v-if="config.userId === currentUserId" text type="primary" @click="openEditDialog(config.id)">
                <el-icon><Edit /></el-icon>
              </el-button>
              <el-button v-if="config.userId === currentUserId" text type="danger" @click="handleDelete(config.id)">
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>
          </div>
          <p class="card-desc">{{ config.description || '暂无描述' }}</p>
          <div class="card-prompt">
            <span class="label">人设提示词:</span>
            <p>{{ config.personaPrompt }}</p>
          </div>
          <div v-if="config.username" class="card-owner">
            创建者: {{ config.username }}
          </div>
        </div>
      </el-col>
    </el-row>
    
    <el-empty v-if="!loading && configs.length === 0" description="暂无配置" />
    
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑中转配置' : '新建中转配置'" width="600px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入配置名称" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" placeholder="请输入描述" />
        </el-form-item>
        <el-form-item label="人设提示词" prop="personaPrompt">
          <el-input
            v-model="form.personaPrompt"
            type="textarea"
            :rows="5"
            placeholder="请输入人设提示词"
          />
        </el-form-item>
        <el-form-item label="是否公开" prop="isPublic">
          <el-radio-group v-model="form.isPublic">
            <el-radio :value="0">私有</el-radio>
            <el-radio :value="1">公开</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.relay-config-view {
  max-width: 1400px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-lg);
}

.page-title {
  user-select: none;
}

.header-actions {
  display: flex;
  gap: var(--spacing-md);
  align-items: center;
}

.config-card {
  padding: var(--spacing-md);
  margin-bottom: var(--spacing-md);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-sm);
}

.card-header h3 {
  font-size: var(--font-size-lg);
  font-weight: 500;
  user-select: none;
}

.card-actions {
  display: flex;
  gap: var(--spacing-xs);
}

.card-desc {
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
  margin-bottom: var(--spacing-md);
  user-select: none;
}

.card-prompt {
  background: var(--color-bg-secondary);
  padding: var(--spacing-sm);
  border-radius: var(--radius-md);
  font-size: var(--font-size-sm);
}

.card-prompt .label {
  color: var(--color-text-muted);
  user-select: none;
}

.card-prompt p {
  margin-top: var(--spacing-xs);
  color: var(--color-text-secondary);
}

.card-owner {
  margin-top: var(--spacing-sm);
  font-size: var(--font-size-xs);
  color: var(--color-text-muted);
  user-select: none;
}
</style>
