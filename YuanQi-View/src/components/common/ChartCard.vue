<script setup>
import { ref, onMounted, nextTick, onBeforeUnmount, watch } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  option: { type: Object, required: true }
})

const chartRef = ref(null)
let chartInstance = null
const viewMode = ref('chart')
const chartType = ref('bar')

// 检测原始图表类型
const detectChartType = () => {
  const series = props.option.series?.[0]
  return series?.type || 'bar'
}

// 提取表格数据
const getTableData = () => {
  const series = props.option.series?.[0]
  if (!series) return { columns: [], rows: [] }

  // 饼图格式
  if (series.type === 'pie' || (Array.isArray(series.data) && series.data[0]?.name !== undefined)) {
    return {
      columns: ['名称', '数值'],
      rows: (series.data || []).map(d => ({ name: d.name, value: d.value }))
    }
  }

  // 柱状图/折线图格式
  const categories = props.option.xAxis?.data || []
  const values = Array.isArray(series.data) ? series.data : []
  return {
    columns: ['类别', '数值'],
    rows: categories.map((c, i) => ({ category: c, value: values[i] }))
  }
}

const tableInfo = ref(getTableData())

// 根据图表类型构建option
const buildOption = (type) => {
  const series = props.option.series?.[0]
  if (!series) return props.option

  const newOption = JSON.parse(JSON.stringify(props.option))
  const newSeries = newOption.series[0]

  if (type === 'pie') {
    // 转换为饼图
    const categories = props.option.xAxis?.data || []
    const values = series.data
    if (Array.isArray(values) && values[0]?.name !== undefined) {
      newSeries.type = 'pie'
      newSeries.data = values
    } else {
      newSeries.type = 'pie'
      newSeries.data = categories.map((c, i) => ({ name: c, value: values[i] }))
    }
    delete newOption.xAxis
    delete newOption.yAxis
    newOption.tooltip = { trigger: 'item' }
  } else {
    // 柱状图/折线图
    if (series.type === 'pie' || (Array.isArray(series.data) && series.data[0]?.name !== undefined)) {
      const pieData = series.data
      newSeries.type = type
      newSeries.data = pieData.map(d => d.value)
      newOption.xAxis = { type: 'category', data: pieData.map(d => d.name) }
      newOption.yAxis = { type: 'value' }
      newOption.tooltip = { trigger: 'axis' }
    } else {
      newSeries.type = type
    }
  }

  return newOption
}

const initChart = () => {
  if (!chartRef.value) return
  if (chartInstance) chartInstance.dispose()
  chartInstance = echarts.init(chartRef.value)
  chartInstance.setOption(buildOption(chartType.value))
}

const handleResize = () => {
  chartInstance?.resize()
}

watch(viewMode, (val) => {
  if (val === 'chart') nextTick(() => initChart())
})

watch(chartType, () => {
  if (viewMode.value === 'chart') nextTick(() => initChart())
})

onMounted(() => {
  chartType.value = detectChartType()
  nextTick(() => initChart())
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  chartInstance?.dispose()
})
</script>

<template>
  <div class="chart-card">
    <div class="chart-toolbar">
      <el-radio-group v-model="viewMode" size="small">
        <el-radio-button value="chart">图表</el-radio-button>
        <el-radio-button value="table">表格</el-radio-button>
      </el-radio-group>
      <el-radio-group v-if="viewMode === 'chart'" v-model="chartType" size="small" class="chart-type-group">
        <el-radio-button value="bar">柱状图</el-radio-button>
        <el-radio-button value="line">折线图</el-radio-button>
        <el-radio-button value="pie">饼图</el-radio-button>
      </el-radio-group>
    </div>
    <div v-if="viewMode === 'chart'" ref="chartRef" class="chart-container" />
    <div v-else class="table-container">
      <el-table :data="tableInfo.rows" size="small" stripe border>
        <el-table-column
          v-for="(col, idx) in tableInfo.columns"
          :key="idx"
          :label="col"
          :prop="col === '名称' || col === '类别' ? 'name' || 'category' : 'value'"
        >
          <template #default="{ row }">
            {{ idx === 0 ? (row.name || row.category) : row.value }}
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<style scoped>
.chart-card {
  margin: 8px 0;
  border: 1px solid var(--color-border-light, #e4e7ed);
  border-radius: 8px;
  overflow: hidden;
  background: #fff;
}

.chart-toolbar {
  padding: 8px 12px;
  border-bottom: 1px solid var(--color-border-light, #e4e7ed);
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}

.chart-type-group {
  flex-shrink: 0;
}

.chart-container {
  width: 100%;
  height: 300px;
  padding: 8px;
}

.table-container {
  padding: 8px;
}
</style>
