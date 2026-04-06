<script setup>
import { ref, onMounted } from 'vue'
import { getMyKnowledgeList, createKnowledge, updateKnowledge, deleteKnowledge, getKnowledge } from '@/api/knowledge'
import { uploadFile } from '@/api/file'
import { ElMessage, ElMessageBox } from 'element-plus'

const knowledgeBases = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const formRef = ref()
const fileLoading = ref(false)
const isEdit = ref(false)

// 文档类型限制：TXT、MD、HTML、XML、JSON、CSV、PDF、DOC、DOCX、XLS、XLSX、PPT、PPTX、RTF、ODT、ODS、ODP
const ALLOWED_DOC_TYPES = [
  'text/plain',
  'text/markdown',
  'text/html',
  'text/xml',
  'application/xml',
  'application/json',
  'text/csv',
  'application/pdf',
  'application/msword',
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
  'application/vnd.ms-excel',
  'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  'application/vnd.ms-powerpoint',
  'application/vnd.openxmlformats-officedocument.presentationml.presentation',
  'application/rtf',
  'application/vnd.oasis.opendocument.text',
  'application/vnd.oasis.opendocument.spreadsheet',
  'application/vnd.oasis.opendocument.presentation'
]

const form = ref({
  id: null,
  name: '',
  description: '',
  documentUrl: ''
})

const rules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  documentUrl: [{ required: true, message: '请上传文档', trigger: 'change' }]
}

const loadKnowledge = async () => {
  loading.value = true
  try {
    const res = await getMyKnowledgeList({ page: 1, size: 100 })
    if (res.code === 200) {
      knowledgeBases.value = res.data.records || []
    }
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handleUpload = async (file) => {
  if (!ALLOWED_DOC_TYPES.includes(file.type)) {
    ElMessage.warning('文档格式不支持，仅支持 TXT、MD、HTML、XML、JSON、CSV、PDF、DOC、DOCX、XLS、XLSX、PPT、PPTX 等格式')
    return false
  }
  
  fileLoading.value = true
  try {
    const res = await uploadFile(file)
    if (typeof res === 'string') {
      form.value.documentUrl = res
      ElMessage.success('上传成功')
    }
  } catch (error) {
    ElMessage.error('上传失败')
  } finally {
    fileLoading.value = false
  }
  return false
}

const openCreateDialog = () => {
  isEdit.value = false
  form.value = { id: null, name: '', description: '', documentUrl: '' }
  dialogVisible.value = true
}

const openEditDialog = async (id) => {
  try {
    const res = await getKnowledge(id)
    if (res.code === 200) {
      isEdit.value = true
      form.value = {
        id: res.data.id,
        name: res.data.name,
        description: res.data.description || '',
        documentUrl: res.data.documentUrl
      }
      dialogVisible.value = true
    }
  } catch (error) {
    ElMessage.error('获取知识库信息失败')
  }
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  
  try {
    let res
    if (isEdit.value) {
      res = await updateKnowledge(form.value)
    } else {
      res = await createKnowledge(form.value)
    }
    if (res.code === 200) {
      ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
      dialogVisible.value = false
      loadKnowledge()
    }
  } catch (error) {
    console.error(error)
  }
}

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定删除该知识库吗？', '提示', { type: 'warning' })
    const res = await deleteKnowledge(id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadKnowledge()
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error(error)
    }
  }
}

onMounted(() => {
  loadKnowledge()
})
</script>

<template>
  <div class="knowledge-view page-container">
    <div class="page-header">
      <h2 class="page-title">知识库管理</h2>
      <el-button type="primary" @click="openCreateDialog">
        <el-icon><Plus /></el-icon>
        新建知识库
      </el-button>
    </div>
    
    <el-row :gutter="16" v-loading="loading">
      <el-col v-for="kb in knowledgeBases" :key="kb.id" :span="8">
        <div class="knowledge-card card">
          <div class="card-header">
            <h3>{{ kb.name }}</h3>
            <div class="card-actions">
              <el-button text type="primary" @click="openEditDialog(kb.id)">
                <el-icon><Edit /></el-icon>
              </el-button>
              <el-button text type="danger" @click="handleDelete(kb.id)">
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>
          </div>
          <p class="card-desc">{{ kb.description || '暂无描述' }}</p>
          <div class="card-meta">
            <span>分块: {{ kb.chunkCount || 0 }}</span>
            <el-tag :type="kb.status === 1 ? 'success' : 'info'" size="small">
              {{ kb.status === 1 ? '可用' : '处理中' }}
            </el-tag>
          </div>
        </div>
      </el-col>
    </el-row>
    
    <el-empty v-if="!loading && knowledgeBases.length === 0" description="暂无知识库" />
    
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑知识库' : '新建知识库'" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入知识库名称" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" placeholder="请输入描述" />
        </el-form-item>
        <el-form-item label="文档" prop="documentUrl">
          <el-upload
            :show-file-list="false"
            :before-upload="handleUpload"
            accept=".txt,.md,.html,.xml,.json,.csv,.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.rtf,.odt,.ods,.odp"
          >
            <el-button :loading="fileLoading">
              <el-icon><Upload /></el-icon>
              上传文档
            </el-button>
            <span v-if="form.documentUrl" class="file-name">
              {{ form.documentUrl.split('/').pop() }}
            </span>
          </el-upload>
          <div class="upload-tip">支持 TXT、MD、HTML、XML、JSON、CSV、PDF、DOC、DOCX、XLS、XLSX、PPT、PPTX 等格式</div>
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
.knowledge-view {
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

.knowledge-card {
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

.card-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
  user-select: none;
}

.file-name {
  margin-left: var(--spacing-sm);
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}

.upload-tip {
  font-size: var(--font-size-xs);
  color: var(--color-text-muted);
  margin-top: var(--spacing-xs);
}
</style>
