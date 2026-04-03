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
const expandedPrompts = ref(new Set())
const searchText = ref('')

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

const filteredConfigs = computed(() => {
  if (!searchText.value) return configs.value
  const keyword = searchText.value.toLowerCase()
  return configs.value.filter(c => 
    c.name?.toLowerCase().includes(keyword) ||
    c.description?.toLowerCase().includes(keyword)
  )
})

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

const openEditDialog = async (config) => {
  try {
    const res = await getRelayConfig(config.id)
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

const handleDelete = async (config) => {
  try {
    await ElMessageBox.confirm(
      `确定删除「${config.name}」吗？此操作不可恢复。`, 
      '删除确认', 
      { type: 'warning', confirmButtonText: '确定删除', cancelButtonText: '取消' }
    )
    const res = await deleteRelayConfig(config.id)
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

const getAvatarColor = (name) => {
  const colors = ['#409eff', '#67c23a', '#e6a23c', '#f56c6c', '#909399', '#00d4aa', '#9b59b6', '#3498db']
  let hash = 0
  for (let i = 0; i < name.length; i++) {
    hash = name.charCodeAt(i) + ((hash << 5) - hash)
  }
  return colors[Math.abs(hash) % colors.length]
}

const togglePrompt = (id) => {
  if (expandedPrompts.value.has(id)) {
    expandedPrompts.value.delete(id)
  } else {
    expandedPrompts.value.add(id)
  }
}

const isPromptExpanded = (id) => expandedPrompts.value.has(id)

onMounted(() => {
  loadConfigs()
})
</script>

<template>
  <div class="relay-config-view page-container">
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">
          <el-icon class="title-icon"><Connection /></el-icon>
          中转配置
        </h2>
        <div class="header-stats">
          <div class="stat-item">
            <span class="stat-value">{{ configs.length }}</span>
            <span class="stat-label">{{ onlyMine ? '我的配置' : '公开配置' }}</span>
          </div>
        </div>
      </div>
      <div class="header-right">
        <el-input
          v-model="searchText"
          placeholder="搜索配置..."
          prefix-icon="Search"
          clearable
          class="search-input"
        />
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
    
    <div class="config-grid" v-loading="loading">
      <div v-for="config in filteredConfigs" :key="config.id" class="config-card card">
        <div class="card-header">
          <div class="config-avatar" :style="{ background: getAvatarColor(config.name || '默认') }">
            {{ (config.name || '配')[0] }}
          </div>
          <div class="config-info">
            <div class="config-name">{{ config.name }}</div>
            <div class="config-meta">
              <span class="config-id" v-if="onlyMine">
                <el-icon><Key /></el-icon>
                ID: {{ config.id }}
              </span>
              <span class="creator" v-if="!onlyMine && config.username">
                <el-icon><User /></el-icon>
                {{ config.username }}
              </span>
              <el-tag 
                :type="config.isPublic === 1 ? 'success' : 'info'" 
                size="small"
                effect="light"
              >
                {{ config.isPublic === 1 ? '公开' : '私有' }}
              </el-tag>
            </div>
          </div>
          <div class="card-actions" v-if="config.userId === currentUserId">
            <el-button text type="primary" @click="openEditDialog(config)">
              <el-icon><Edit /></el-icon>
            </el-button>
            <el-button text type="danger" @click="handleDelete(config)">
              <el-icon><Delete /></el-icon>
            </el-button>
          </div>
        </div>
        
        <div class="card-body">
          <div class="description">
            <el-icon class="desc-icon"><Document /></el-icon>
            <span>{{ config.description || '暂无描述' }}</span>
          </div>
          <div class="prompt-preview" v-if="config.personaPrompt">
            <div class="prompt-header" @click="togglePrompt(config.id)">
              <span class="prompt-label">提示词预览</span>
              <el-icon class="expand-icon" :class="{ expanded: isPromptExpanded(config.id) }">
                <ArrowDown />
              </el-icon>
            </div>
            <div class="prompt-content" :class="{ expanded: isPromptExpanded(config.id) }">
              {{ config.personaPrompt }}
            </div>
          </div>
        </div>
        
        <div class="card-footer">
          <div class="footer-time">
            <el-icon><Clock /></el-icon>
            {{ config.createTime }}
          </div>
        </div>
      </div>
    </div>
    
    <el-empty v-if="!loading && filteredConfigs.length === 0" description="暂无配置数据">
      <template #image>
        <el-icon :size="60" color="#c0c4cc"><FolderOpened /></el-icon>
      </template>
    </el-empty>
    
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
        <el-button @click="dialogVisible.value = false">取消</el-button>
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
  align-items: flex-start;
  margin-bottom: var(--spacing-lg);
  flex-wrap: wrap;
  gap: var(--spacing-md);
}

.header-left {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
}

.page-title {
  margin: 0;
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  font-size: 22px;
}

.title-icon {
  color: var(--color-primary);
  font-size: 24px;
}

.header-stats {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  padding-left: 32px;
}

.stat-item {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.stat-value {
  font-size: 20px;
  font-weight: 600;
  color: var(--color-text);
}

.stat-label {
  font-size: 12px;
  color: var(--color-text-muted);
}

.header-right {
  display: flex;
  gap: var(--spacing-md);
  align-items: center;
}

.search-input {
  width: 200px;
}

.config-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
  gap: var(--spacing-md);
}

.config-card {
  display: flex;
  flex-direction: column;
  padding: var(--spacing-md);
  transition: all 0.3s ease;
  border-radius: 12px;
}

.config-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.1);
}

.card-header {
  display: flex;
  gap: var(--spacing-md);
  margin-bottom: var(--spacing-md);
  padding-bottom: var(--spacing-md);
  border-bottom: 1px solid var(--color-border-light);
}

.config-avatar {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 20px;
  font-weight: 600;
  flex-shrink: 0;
}

.config-info {
  flex: 1;
  min-width: 0;
}

.config-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.config-meta {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  flex-wrap: wrap;
}

.config-id {
  display: flex;
  align-items: center;
  gap: 2px;
  font-size: 12px;
  color: var(--color-text-muted);
}

.config-id .el-icon {
  font-size: 12px;
}

.creator {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: var(--color-text-muted);
}

.card-actions {
  display: flex;
  flex-direction: row;
  gap: 4px;
}

.card-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
}

.description {
  display: flex;
  align-items: flex-start;
  gap: var(--spacing-xs);
  font-size: 14px;
  color: var(--color-text-secondary);
  line-height: 1.5;
}

.desc-icon {
  margin-top: 2px;
  color: var(--color-text-muted);
  flex-shrink: 0;
}

.prompt-preview {
  background: var(--color-bg-secondary);
  border-radius: 8px;
  padding: var(--spacing-sm);
  margin-top: var(--spacing-xs);
}

.prompt-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  cursor: pointer;
  user-select: none;
}

.prompt-header:hover .prompt-label {
  color: var(--color-primary);
}

.prompt-label {
  font-size: 12px;
  color: var(--color-text-muted);
  transition: color 0.2s;
}

.expand-icon {
  font-size: 12px;
  color: var(--color-text-muted);
  transition: transform 0.3s ease;
}

.expand-icon.expanded {
  transform: rotate(180deg);
}

.prompt-content {
  font-size: 13px;
  color: var(--color-text-secondary);
  line-height: 1.6;
  margin-top: var(--spacing-xs);
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  transition: all 0.3s ease;
}

.prompt-content.expanded {
  -webkit-line-clamp: unset;
  display: block;
}

.card-footer {
  margin-top: var(--spacing-md);
  padding-top: var(--spacing-sm);
  border-top: 1px solid var(--color-border-light);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.footer-time {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--color-text-muted);
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
  }
  
  .header-right {
    width: 100%;
    flex-wrap: wrap;
  }
  
  .search-input {
    width: 100%;
  }
  
  .config-grid {
    grid-template-columns: 1fr;
  }
}
</style>
