<template>
  <div class="app-container">
    <SideBarMenu />
    <div class="main-content">
      <div class="content">
        <div class="card">
          <h1 class="header">CET4强化训练</h1>
          <div class="workspace">
            <div class="generation-panel">
              <div v-if="loading" class="loading-state">
                <p>正在加载题型...</p>
              </div>
              <form v-else class="generation-form" @submit.prevent="submitTask">
                <section class="section question-types-section">
                  <div class="section-header">
                    <h3 class="section-title">题型与数量</h3>
                    <span class="section-subtitle">为需要训练的题型输入数量</span>
                  </div>
                  <div class="question-type-list">
                    <div v-for="(type, idx) in questionTypes" :key="type" class="question-type-row">
                      <span class="type-label">{{ type }}</span>
                      <input
                        type="number"
                        min="0"
                        v-model.number="counts[idx]"
                        placeholder="0"
                        class="count-input"
                      />
                      <span class="unit-label">道题</span>
                    </div>
                  </div>
                </section>

                <section class="section options-section">
                  <h3 class="section-title">训练选项</h3>
                  <label class="checkbox-label">
                    <input type="checkbox" v-model="fromWrongBank" class="checkbox-input" />
                    <span class="checkbox-text">优先从我的错题库抽题</span>
                  </label>
                  <div class="hint-text">勾选后题目将尽量来自错题库，未命中时自动回退至真实题源</div>
                  <div class="async-row">
                    <label class="checkbox-label inline-checkbox">
                      <input type="checkbox" v-model="useAsyncMode" class="checkbox-input" />
                      <span class="checkbox-text">后台生成</span>
                    </label>
                    <span class="hint-text">后台模式下任务会异步执行，可在右侧列表查看进度</span>
                  </div>
                </section>

                <div v-if="error" class="error">{{ error }}</div>
                <div v-if="success" class="success">{{ success }}</div>

                <div class="submit-section">
                  <el-progress
                    v-if="isGenerating"
                    :percentage="Math.floor(progress)"
                    :stroke-width="15"
                    :text-inside="true"
                    status="success"
                    class="submit-progress"
                  />
                  <button type="submit" :disabled="isGenerating || !hasValidCount" class="submit-btn">
                    {{ isGenerating ? '生成中...' : submitLabel }}
                  </button>
                  <div v-if="!hasValidCount" class="hint-text center-hint">请至少为一种题型设置练习数量</div>
                </div>
              </form>
            </div>

            <div class="task-panel">
              <div class="task-list-section">
                <div class="task-list-header">
                  <h3>生成任务</h3>
                  <button type="button" class="link-btn" @click="fetchTasks()" :disabled="taskLoading">
                    {{ taskLoading ? '刷新中...' : '刷新列表' }}
                  </button>
                </div>
                <p class="task-hint">后台任务会在此处显示，点击成功状态即可继续练习</p>
                <div v-if="taskLoading" class="task-empty">任务数据加载中...</div>
                <div v-else-if="!taskList.length" class="task-empty">暂无任务，提交后台生成后会显示在这里</div>
                <div v-else class="task-list">
                  <div
                    v-for="task in taskList"
                    :key="task.id"
                    class="task-item"
                    :class="task.status ? task.status.toLowerCase() : ''"
                  >
                    <div class="task-info">
                      <div class="task-title-row">
                        <span class="task-name">任务 #{{ task.id }}</span>
                        <span class="task-status-pill" :class="task.status">{{ formatStatus(task.status) }}</span>
                      </div>
                      <div class="task-meta">
                        {{ task.fromWrongBank ? '错题库' : '自定义' }} · 共 {{ task.requestedTotal || 0 }} 题 ·
                        {{ formatTimestamp(task.createdAt) }}
                      </div>
                      <div class="task-types" v-if="task.types && task.types.length">
                        {{ formatTypeSummary(task) }}
                      </div>
                      <el-progress
                        :percentage="Math.floor(task.progress || 0)"
                        :stroke-width="10"
                        :text-inside="true"
                        status="success"
                      />
                      <div v-if="task.status === 'FAILED' && task.message" class="task-error">{{ task.message }}</div>
                    </div>
                    <div class="task-actions">
                      <button
                        v-if="task.status === 'SUCCEEDED'"
                        type="button"
                        class="task-view-btn"
                        @click="viewTask(task)"
                      >
                        查看
                      </button>
                      <button v-else type="button" class="task-view-btn disabled" disabled>
                        {{ actionLabel(task.status) }}
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue';
import { ElMessage } from 'element-plus';
import axios from 'axios';
import { useRouter } from 'vue-router';
import SideBarMenu from '@/common/components/SideBarMenu.vue';

const router = useRouter();
const loading = ref(true);
const questionTypes = ref([]);
const counts = ref([]);
const fromWrongBank = ref(false);
const useAsyncMode = ref(false);
const error = ref('');
const success = ref('');
const isGenerating = ref(false);
const progress = ref(0);
const taskList = ref([]);
const taskLoading = ref(false);

let progressInterval = null;
let taskPollingTimer = null;

const hasValidCount = computed(() => counts.value.some(count => Number(count) > 0));
const submitLabel = computed(() => (useAsyncMode.value ? '提交后台任务' : '立即生成'));

onMounted(async () => {
  await loadQuestionTypes();
  fetchTasks();
  taskPollingTimer = setInterval(() => fetchTasks(false), 15000);
});

async function loadQuestionTypes() {
  try {
    const { data } = await axios.get('/api/intensive/question-types');
    questionTypes.value = data || [];
    counts.value = questionTypes.value.map(() => 0);
  } catch (err) {
    error.value = '加载题型失败: ' + (err.message || '未知错误');
  } finally {
    loading.value = false;
  }
}

async function fetchTasks(showLoading = true) {
  if (showLoading) {
    taskLoading.value = true;
  }
  try {
    const { data } = await axios.get('/api/intensive/tasks?limit=20');
    taskList.value = Array.isArray(data) ? data : [];
  } catch (err) {
  } finally {
    if (showLoading) {
      taskLoading.value = false;
    }
  }
}

function startProgress() {
  progress.value = 0;
  const totalDurationSeconds = 4 * 60;
  const step = 100 / totalDurationSeconds;
  progressInterval = setInterval(() => {
    progress.value = Math.min(progress.value + step, 99);
  }, 1000);
}

function stopProgress() {
  clearInterval(progressInterval);
  progress.value = 100;
  setTimeout(() => (progress.value = 0), 1000);
}

async function submitTask() {
  error.value = '';
  success.value = '';

  if (!hasValidCount.value) {
    error.value = '请至少为一种题型设置练习数量';
    return;
  }

  isGenerating.value = !useAsyncMode.value;
  if (!useAsyncMode.value) {
    startProgress();
  } else {
    progress.value = 0;
  }

  try {
    const examPaperEnSource = fromWrongBank.value ? 'AIfromWrongBank' : 'AIfromself';
    const payload = {
      types: questionTypes.value,
      counts: counts.value,
      fromWrongBank: fromWrongBank.value,
      exam_paper_en_source: examPaperEnSource,
      asyncMode: useAsyncMode.value
    };

    const { data } = await axios.post('/api/intensive/tasks', payload);
    if (!data || !data.success) {
      throw new Error(data?.message || '提交任务失败');
    }

    if (useAsyncMode.value) {
      success.value = '任务已提交，稍后可在右侧任务列表查看进度';
      fetchTasks(false);
    } else {
      persistAndNavigate(data);
    }
  } catch (err) {
    error.value = '生成训练试卷时出错: ' + (err.message || '未知错误');
    ElMessage.error({
      message: '生成训练试卷失败: ' + (err.message || '未知错误'),
      duration: 3000,
      showClose: true
    });
  } finally {
    if (!useAsyncMode.value) {
      stopProgress();
    }
    isGenerating.value = false;
  }
}

async function viewTask(task) {
  if (!task || task.status !== 'SUCCEEDED') {
    return;
  }
  try {
    const { data } = await axios.get(`/api/intensive/tasks/${task.id}/result`);
    if (!data || !data.success) {
      throw new Error(data?.message || '获取任务结果失败');
    }
    persistAndNavigate(data, task.id);
    removeTaskFromList(task.id);
    await deleteTaskOnServer(task.id);
  } catch (err) {
    error.value = '打开任务结果失败: ' + (err.message || '未知错误');
    ElMessage.error({
      message: '获取任务结果失败: ' + (err.message || '未知错误'),
      duration: 3000,
      showClose: true
    });
  }
}

function persistAndNavigate(result, taskId) {
  const trainData = {
    questions: result.questions || [],
    examPaperEnSource: result.examPaperEnSource,
    totalCount: result.totalCount,
    failedCount: result.failedCount,
    failedTypes: result.failedTypes || [],
    paperName: result.examPaperEnName || '强化训练试卷（未保存）',
    timestamp: Date.now()
  };
  sessionStorage.setItem('intensiveTrainQuestions', JSON.stringify(trainData));
  router.push({
    name: 'CET4IntensiveTrainPreview',
    query: {
      exam_paper_en_source: result.examPaperEnSource,
      from_intensive: 'true',
      task_id: taskId || 'sync'
    }
  });
}

function removeTaskFromList(taskId) {
  if (!taskId) {
    return;
  }
  taskList.value = taskList.value.filter(task => task.id !== taskId);
}

async function deleteTaskOnServer(taskId) {
  if (!taskId) {
    return;
  }
  try {
    await axios.delete(`/api/intensive/tasks/${taskId}`);
  } catch (err) {
  }
}

function formatStatus(status) {
  switch ((status || '').toUpperCase()) {
    case 'SUCCEEDED':
      return '已完成';
    case 'FAILED':
      return '失败';
    case 'RUNNING':
      return '生成中';
    case 'PENDING':
      return '排队中';
    default:
      return status || '未知状态';
  }
}

function actionLabel(status) {
  if ((status || '').toUpperCase() === 'FAILED') {
    return '已失败';
  }
  return '处理中';
}

function formatTimestamp(ts) {
  if (!ts) {
    return '--';
  }
  const date = new Date(ts);
  return Number.isNaN(date.getTime()) ? ts : date.toLocaleString();
}

function formatTypeSummary(task) {
  const types = task.types || [];
  const countsArr = task.counts || [];
  const pairs = types
    .map((type, idx) => {
      const count = countsArr[idx] ?? 0;
      return count > 0 ? `${type}×${count}` : null;
    })
    .filter(Boolean);
  return pairs.length ? pairs.join('，') : '暂无题型信息';
}

onBeforeUnmount(() => {
  if (progressInterval) {
    clearInterval(progressInterval);
  }
  if (taskPollingTimer) {
    clearInterval(taskPollingTimer);
  }
});
</script>

<style scoped>
.app-container {
  display: flex;
  min-height: 100vh;
}

.main-content {
  flex: 1;
  padding: 20px;
  background-color: #f5f5f5;
  margin-left: 260px;
  width: calc(100% - 260px);
}

.content {
  padding: 10px 20px;
}

.card {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.08);
  padding: 30px;
  max-width: 1200px;
  margin: 0 auto;
}

.header {
  margin-bottom: 24px;
  font-size: 24px;
  text-align: center;
  color: #1f1f1f;
}

.workspace {
  display: flex;
  gap: 24px;
  align-items: flex-start;
}

.generation-panel {
  flex: 1;
  min-width: 0;
}

.task-panel {
  width: 360px;
  background: #fafafa;
  border-radius: 12px;
  padding: 20px;
  border: 1px solid #eee;
  position: sticky;
  top: 40px;
}

.generation-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.loading-state {
  text-align: center;
  padding: 40px 0;
  color: #666;
}

.section {
  padding: 20px;
  border: 1px solid #e5e5e5;
  border-radius: 10px;
  background: #fff;
}

.section-header {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 14px;
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.section-subtitle {
  font-size: 13px;
  color: #888;
}

.question-type-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.question-type-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border: 1px solid #e3e3e3;
  border-radius: 8px;
  background: #fdfdfd;
}

.type-label {
  flex: 1;
  font-weight: 500;
}

.count-input {
  width: 90px;
  padding: 6px 10px;
  border: 1px solid #d7d7d7;
  border-radius: 6px;
  text-align: center;
}

.unit-label {
  color: #777;
  font-size: 14px;
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  border: 1px solid #e3e3e3;
  border-radius: 8px;
  background: #fdfdfd;
  cursor: pointer;
}

.inline-checkbox {
  margin-top: 12px;
}

.checkbox-input {
  width: 16px;
  height: 16px;
}

.checkbox-text {
  color: #333;
  font-size: 14px;
}

.hint-text {
  color: #777;
  font-size: 13px;
  margin-top: 8px;
}

.async-row {
  margin-top: 12px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.submit-section {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 12px;
  margin-top: 5px;
}

.submit-progress {
  width: 100%;
}

.submit-btn {
  background: linear-gradient(135deg, #3ac28d, #2f8f7f);
  border: none;
  color: #fff;
  padding: 12px 20px;
  border-radius: 8px;
  font-size: 16px;
  cursor: pointer;
  transition: opacity 0.2s ease;
}

.submit-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.center-hint {
  text-align: center;
}

.task-list-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.task-list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.task-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.task-item {
  background: #fff;
  border: 1px solid #e5e5e5;
  border-radius: 10px;
  padding: 14px;
  display: flex;
  gap: 16px;
}

.task-info {
  flex: 1;
}

.task-title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.task-name {
  font-weight: 600;
  color: #333;
}

.task-meta {
  color: #777;
  font-size: 13px;
  margin-bottom: 6px;
}

.task-types {
  font-size: 13px;
  color: #555;
  margin-bottom: 8px;
}

.task-status-pill {
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 13px;
  color: #fff;
  background: #999;
}

.task-status-pill.SUCCEEDED {
  background: #3ac28d;
}

.task-status-pill.FAILED {
  background: #e25757;
}

.task-status-pill.RUNNING,
.task-status-pill.PENDING {
  background: #f0a202;
}

.task-actions {
  display: flex;
  align-items: center;
}

.task-view-btn {
  border: none;
  border-radius: 6px;
  padding: 8px 16px;
  background: #3478f6;
  color: #fff;
  cursor: pointer;
}

.task-view-btn.disabled {
  background: #c8c8c8;
  cursor: not-allowed;
}

.task-error {
  color: #d84343;
  margin-top: 6px;
  font-size: 13px;
}

.task-hint {
  font-size: 13px;
  color: #666;
  margin: 0;
}

.task-empty {
  font-size: 13px;
  color: #777;
  padding: 16px;
  text-align: center;
  border: 1px dashed #ccc;
  border-radius: 8px;
}

.link-btn {
  border: none;
  background: none;
  color: #3478f6;
  cursor: pointer;
  font-size: 14px;
}

.error {
  color: #d84343;
  background: #ffecec;
  padding: 10px 12px;
  border-radius: 6px;
}

.success {
  color: #2e7d32;
  background: #e8f5e9;
  padding: 10px 12px;
  border-radius: 6px;
}

@media (max-width: 1200px) {
  .workspace {
    flex-direction: column;
  }

  .task-panel {
    width: 100%;
    position: static;
  }
}
</style>
