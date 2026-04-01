<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getSessionList, deleteSession, updateSessionTitle } from '@/api/session'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const sessions = ref([])
const loading = ref(false)
const total = ref(0)
const page = ref(1)

const loadSessions = async () => {
  loading.value = true
  try {
    const res = await getSessionList({ page: page.value, size: 20 })
    if (res.code === 200) {
      sessions.value = res.data.records || []
      total.value = res.data.total
    }
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handleOpenSession = (session) => {
  router.push(`/chat/${session.sessionId}`)
}

const handleDelete = async (session) => {
  try {
    await ElMessageBox.confirm('确定删除该会话吗？', '提示', { type: 'warning' })
    const res = await deleteSession(session.sessionId)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadSessions()
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error(error)
    }
  }
}

const handleRename = async (session) => {
  try {
    const { value } = await ElMessageBox.prompt('请输入新标题', '重命名', {
      inputValue: session.title,
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })
    if (value) {
      const res = await updateSessionTitle(session.sessionId, { sessionId: session.sessionId, title: value })
      if (res.code === 200) {
        ElMessage.success('修改成功')
        loadSessions()
      }
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error(error)
    }
  }
}

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

onMounted(() => {
  loadSessions()
})
</script>

<template>
  <div class="history-view page-container">
    <h2 class="page-title">历史会话</h2>
    
    <el-table 
      :data="sessions" 
      v-loading="loading" 
      class="card"
      @row-click="handleOpenSession"
      :row-class-name="'clickable-row'"
    >
      <el-table-column prop="title" label="标题" min-width="200">
        <template #default="{ row }">
          <div class="title-cell">
            <el-icon v-if="row.agentId" class="agent-icon"><UserFilled /></el-icon>
            <span>{{ row.title || '新会话' }}</span>
            <el-tag v-if="row.agentId" size="small" type="primary" class="agent-tag">智能体</el-tag>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="updateTime" label="更新时间" width="180">
        <template #default="{ row }">
          {{ formatDate(row.updateTime || row.createTime) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button text type="primary" @click.stop="handleRename(row)">
            <el-icon><Edit /></el-icon>
          </el-button>
          <el-button text type="danger" @click.stop="handleDelete(row)">
            <el-icon><Delete /></el-icon>
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <div v-if="total > 20" class="pagination">
      <el-pagination
        v-model:current-page="page"
        :total="total"
        :page-size="20"
        layout="prev, pager, next"
        @current-change="loadSessions"
      />
    </div>
  </div>
</template>

<style scoped>
.history-view {
  max-width: 1200px;
}

.clickable-row {
  cursor: pointer;
}

.clickable-row:hover {
  background-color: var(--color-bg-secondary);
}

.title-cell {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
}

.agent-icon {
  color: var(--color-primary);
}

.agent-tag {
  margin-left: var(--spacing-xs);
}

.pagination {
  margin-top: var(--spacing-lg);
  display: flex;
  justify-content: center;
}
</style>
