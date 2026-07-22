<script setup lang="ts">import { ref, computed, onMounted, onUnmounted } from 'vue';
import { ElCard, ElTable, ElTableColumn, ElDialog, ElButton, ElTag, ElMessage } from 'element-plus';
import * as echarts from 'echarts';
interface Task {
 id: string;
 title: string;
 fieldName: string;
 dueDate: string;
 status: '待执行' | '执行中' | '已完成';
 description?: string;
}
const mockTasks: Task[] = [
 { id: '1', title: '喷洒三环唑', fieldName: '水稻田A', dueDate: '2026-07-25', status: '待执行', description: '针对稻瘟病进行预防性喷洒，每亩用量200毫升' },
 { id: '2', title: '除草作业', fieldName: '玉米地B', dueDate: '2026-07-26', status: '待执行', description: '清除田间杂草，确保作物正常生长' },
 { id: '3', title: '施肥管理', fieldName: '蔬菜基地C', dueDate: '2026-07-24', status: '已完成', description: '施加有机肥，提高土壤肥力' },
 { id: '4', title: '病虫害巡查', fieldName: '水果园D', dueDate: '2026-07-27', status: '执行中', description: '全面巡查果园，及时发现病虫害迹象' },
 { id: '5', title: '灌溉调度', fieldName: '水稻田A', dueDate: '2026-07-23', status: '已完成', description: '根据天气情况调整灌溉水量' }
];
const tasks = ref<Task[]>([]);
const dialogVisible = ref(false);
const selectedTask = ref<Task | null>(null);
const chartRef = ref<HTMLDivElement | null>(null);
let chartInstance: echarts.ECharts | null = null;
const statusColors: Record<string, 'primary' | 'success' | 'warning' | 'info' | 'danger'> = {
 '待执行': 'warning',
 '执行中': 'info',
 '已完成': 'success'
};
const completedCount = computed(() => tasks.value.filter(t => t.status === '已完成').length);
const pendingCount = computed(() => tasks.value.filter(t => t.status !== '已完成').length);
const loadTasks = () => {
 tasks.value = [...mockTasks];
};
const initChart = () => {
 if (!chartRef.value)
 return;
 chartInstance = echarts.init(chartRef.value);
 updateChart();
};
const updateChart = () => {
 if (!chartInstance)
 return;
 const option: echarts.EChartsOption = {
 title: {
 text: '本周任务完成情况',
 left: 'center',
 textStyle: {
 fontSize: 16,
 fontWeight: 500
 }
 },
 tooltip: {
 trigger: 'axis',
 axisPointer: {
 type: 'shadow'
 }
 },
 grid: {
 left: '3%',
 right: '4%',
 bottom: '3%',
 containLabel: true
 },
 xAxis: {
 type: 'category',
 data: ['已完成', '未完成'],
 axisLabel: {
 fontSize: 14
 }
 },
 yAxis: {
 type: 'value',
 axisLabel: {
 fontSize: 14
 }
 },
 series: [
 {
 name: '任务数',
 type: 'bar',
 barWidth: '50%',
 data: [
 { value: completedCount.value, itemStyle: { color: '#67c23a' } },
 { value: pendingCount.value, itemStyle: { color: '#e6a23c' } }
 ],
 label: {
 show: true,
 position: 'top',
 fontSize: 14,
 fontWeight: 'bold'
 }
 }
 ]
 };
 chartInstance.setOption(option);
};
const handleRowClick = (row: Task) => {
 selectedTask.value = row;
 dialogVisible.value = true;
};
const markAsCompleted = async () => {
 if (!selectedTask.value)
 return;
 const index = tasks.value.findIndex(t => t.id === selectedTask.value!.id);
 if (index !== -1) {
 await new Promise(resolve => setTimeout(resolve, 500));
 tasks.value[index].status = '已完成';
 selectedTask.value!.status = '已完成';
 ElMessage.success('任务已标记为完成');
 updateChart();
 }
};
const handleResize = () => {
 chartInstance?.resize();
};
onMounted(() => {
 loadTasks();
 initChart();
 window.addEventListener('resize', handleResize);
});
onUnmounted(() => {
 window.removeEventListener('resize', handleResize);
 chartInstance?.dispose();
});
</script>

<template>
  <div class="farmer-task-container">
    <ElCard class="chart-card" style="margin-bottom: 20px;">
      <div ref="chartRef" class="chart-container"></div>
    </ElCard>

    <ElCard class="task-card" header="任务列表">
      <ElTable
        :data="tasks"
        row-key="id"
        border
        @row-click="handleRowClick"
        style="width: 100%;"
      >
        <ElTableColumn prop="title" label="任务名称" min-width="200" />
        <ElTableColumn prop="fieldName" label="关联地块" min-width="150" />
        <ElTableColumn prop="dueDate" label="截止日期" min-width="120" />
        <ElTableColumn prop="status" label="状态" min-width="100">
          <template #default="{ row }">
            <ElTag :type="statusColors[row.status]">{{ row.status }}</ElTag>
          </template>
        </ElTableColumn>
      </ElTable>
    </ElCard>

    <ElDialog
      v-model="dialogVisible"
      title="任务详情"
      width="500px"
      :close-on-click-modal="false"
    >
      <div v-if="selectedTask" class="task-detail">
        <div class="detail-item">
          <span class="label">任务名称：</span>
          <span class="value">{{ selectedTask.title }}</span>
        </div>
        <div class="detail-item">
          <span class="label">关联地块：</span>
          <span class="value">{{ selectedTask.fieldName }}</span>
        </div>
        <div class="detail-item">
          <span class="label">截止日期：</span>
          <span class="value">{{ selectedTask.dueDate }}</span>
        </div>
        <div class="detail-item">
          <span class="label">当前状态：</span>
          <ElTag :type="statusColors[selectedTask.status]">{{ selectedTask.status }}</ElTag>
        </div>
        <div v-if="selectedTask.description" class="detail-item">
          <span class="label">任务描述：</span>
          <span class="value">{{ selectedTask.description }}</span>
        </div>
      </div>
      <template #footer>
        <ElButton @click="dialogVisible = false">关闭</ElButton>
        <ElButton
          v-if="selectedTask && selectedTask.status !== '已完成'"
          type="primary"
          @click="markAsCompleted"
        >
          标记为已完成
        </ElButton>
        <ElButton
          v-else
          type="success"
          disabled
        >
          已完成
        </ElButton>
      </template>
    </ElDialog>
  </div>
</template>

<style scoped>
.farmer-task-container {
  padding: 20px;
  background: #fafafa;
  min-height: calc(100vh - 60px);
}

.chart-card {
  max-width: 1200px;
  margin: 0 auto 20px;
}

.chart-container {
  width: 100%;
  min-height: 300px;
}

.task-card {
  max-width: 1200px;
  margin: 0 auto;
}

.task-detail {
  padding: 10px 0;
}

.detail-item {
  display: flex;
  margin-bottom: 16px;
  align-items: flex-start;
}

.detail-item:last-child {
  margin-bottom: 0;
}

.detail-item .label {
  width: 100px;
  color: #606266;
  flex-shrink: 0;
  font-weight: 500;
}

.detail-item .value {
  color: #303133;
  flex: 1;
}
</style>