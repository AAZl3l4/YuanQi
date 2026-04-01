<script setup>
import { ref, onMounted } from 'vue'
import { getMyUsage } from '@/api/usage'

const usage = ref(null)
const loading = ref(false)
const dateRange = ref([])

const loadUsage = async () => {
  loading.value = true
  try {
    const params = {}
    if (dateRange.value?.length === 2) {
      params.startDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    }
    const res = await getMyUsage(params)
    if (res.code === 200) {
      usage.value = res.data
    }
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadUsage()
})
</script>

<template>
  <div class="usage-view page-container">
    <div class="page-header">
      <h2 class="page-title">用量统计</h2>
      <el-date-picker
        v-model="dateRange"
        type="daterange"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        value-format="YYYY-MM-DD"
        @change="loadUsage"
      />
    </div>
    
    <div class="stats-section" v-loading="loading">
      <div class="section-title">对话统计</div>
      <div class="stats-row">
        <div class="stat-item">
          <span class="stat-label">对话次数</span>
          <span class="stat-value">{{ usage?.chatCount || 0 }}</span>
        </div>
        <div class="stat-item">
          <span class="stat-label">输入Token</span>
          <span class="stat-value">{{ usage?.inputTokens || 0 }}</span>
        </div>
        <div class="stat-item">
          <span class="stat-label">输出Token</span>
          <span class="stat-value">{{ usage?.outputTokens || 0 }}</span>
        </div>
      </div>
      
      <div class="section-title">生成统计</div>
      <div class="stats-row">
        <div class="stat-item">
          <span class="stat-label">图片生成</span>
          <span class="stat-value">{{ usage?.imageCount || 0 }}</span>
        </div>
        <div class="stat-item">
          <span class="stat-label">视频生成</span>
          <span class="stat-value">{{ usage?.videoCount || 0 }}</span>
        </div>
      </div>
      
      <div class="section-title">中转统计</div>
      <div class="stats-row">
        <div class="stat-item">
          <span class="stat-label">中转调用</span>
          <span class="stat-value">{{ usage?.relayCount || 0 }}</span>
        </div>
        <div class="stat-item">
          <span class="stat-label">中转输入Token</span>
          <span class="stat-value">{{ usage?.relayInputTokens || 0 }}</span>
        </div>
        <div class="stat-item">
          <span class="stat-label">中转输出Token</span>
          <span class="stat-value">{{ usage?.relayOutputTokens || 0 }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.usage-view {
  max-width: 900px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-xl);
}

.page-title {
  user-select: none;
}

.stats-section {
  background: var(--color-white);
  border-radius: var(--radius-lg);
  padding: var(--spacing-lg);
  border: 1px solid var(--color-border-light);
}

.section-title {
  font-size: var(--font-size-base);
  font-weight: 500;
  color: var(--color-text);
  margin-bottom: var(--spacing-md);
  padding-bottom: var(--spacing-sm);
  border-bottom: 1px solid var(--color-border-light);
  user-select: none;
}

.section-title:not(:first-child) {
  margin-top: var(--spacing-xl);
}

.stats-row {
  display: flex;
  gap: var(--spacing-xl);
}

.stat-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: var(--spacing-md);
  background: var(--color-bg-secondary);
  border-radius: var(--radius-md);
}

.stat-label {
  font-size: var(--font-size-sm);
  color: var(--color-text-muted);
  margin-bottom: var(--spacing-sm);
  user-select: none;
}

.stat-value {
  font-size: var(--font-size-2xl);
  font-weight: 600;
  color: var(--color-primary);
  user-select: none;
}
</style>
