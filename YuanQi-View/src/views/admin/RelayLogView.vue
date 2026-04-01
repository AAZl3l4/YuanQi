<script setup>
import { ref, onMounted } from 'vue'
import { getAdminRelayLogs } from '@/api/relay'

const logs = ref([])
const loading = ref(false)

const loadLogs = async () => {
  loading.value = true
  try {
    const res = await getAdminRelayLogs({ page: 1, size: 100 })
    if (res.code === 200) {
      logs.value = res.data.records || []
    }
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadLogs()
})
</script>

<template>
  <div class="relay-log-view page-container">
    <h2 class="page-title">中转调用记录</h2>
    
    <el-table :data="logs" v-loading="loading" class="card">
      <el-table-column prop="username" label="用户" width="120" />
      <el-table-column prop="configName" label="配置" width="150" />
      <el-table-column prop="message" label="消息" min-width="200" show-overflow-tooltip />
      <el-table-column prop="inputTokens" label="输入Token" width="100" />
      <el-table-column prop="outputTokens" label="输出Token" width="100" />
      <el-table-column prop="createTime" label="调用时间" width="180" />
    </el-table>
  </div>
</template>

<style scoped>
.relay-log-view {
  max-width: 1200px;
}
</style>
