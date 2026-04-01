<script setup>
import { ref, onMounted } from 'vue'
import { getToolList, updateToolStatus, createTool } from '@/api/mcp'
import { ElMessage, ElMessageBox } from 'element-plus'

const tools = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const formRef = ref()
const isEdit = ref(false)

const form = ref({
  id: null,
  name: '',
  description: '',
  enabled: 1,
  sortOrder: 0
})

const rules = {
  name: [{ required: true, message: '请输入工具名称', trigger: 'blur' }],
  description: [{ required: true, message: '请输入工具描述', trigger: 'blur' }]
}

const loadTools = async () => {
  loading.value = true
  try {
    const res = await getToolList()
    if (res.code === 200) {
      tools.value = res.data || []
    }
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handleToggle = async (row) => {
  try {
    const newStatus = row.enabled ? 1 : 0
    const res = await updateToolStatus(row.id, newStatus)
    if (res.code === 200) {
      ElMessage.success(newStatus ? '已启用' : '已禁用')
    } else {
      row.enabled = !row.enabled
    }
  } catch (error) {
    row.enabled = !row.enabled
    console.error(error)
  }
}

const openCreateDialog = () => {
  isEdit.value = false
  form.value = {
    id: null,
    name: '',
    description: '',
    enabled: 1,
    sortOrder: 0
  }
  dialogVisible.value = true
}

const openEditDialog = (row) => {
  isEdit.value = true
  form.value = {
    id: row.id,
    name: row.name,
    description: row.description,
    enabled: row.enabled,
    sortOrder: row.sortOrder || 0
  }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  
  try {
    const res = await createTool(form.value)
    if (res.code === 200) {
      ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
      dialogVisible.value = false
      loadTools()
    }
  } catch (error) {
    console.error(error)
  }
}

onMounted(() => {
  loadTools()
})
</script>

<template>
  <div class="mcp-manage-view page-container">
    <div class="page-header">
      <h2 class="page-title">MCP工具管理</h2>
      <el-button type="primary" @click="openCreateDialog">
        <el-icon><Plus /></el-icon>
        新建工具
      </el-button>
    </div>
    
    <el-table :data="tools" v-loading="loading" class="card">
      <el-table-column prop="name" label="工具名称" min-width="150" />
      <el-table-column prop="description" label="描述" min-width="200" />
      <el-table-column label="状态" width="120">
        <template #default="{ row }">
          <el-switch
            v-model="row.enabled"
            :active-value="1"
            :inactive-value="0"
            @change="handleToggle(row)"
          />
        </template>
      </el-table-column>
      <el-table-column prop="sortOrder" label="排序" width="80" />
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-button text type="primary" @click="openEditDialog(row)">
            <el-icon><Edit /></el-icon>
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑工具' : '新建工具'" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入工具名称（方法名）" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" placeholder="请输入工具描述" />
        </el-form-item>
        <el-form-item label="状态" prop="enabled">
          <el-radio-group v-model="form.enabled">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number v-model="form.sortOrder" :min="0" />
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
.mcp-manage-view {
  max-width: 1000px;
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
</style>
