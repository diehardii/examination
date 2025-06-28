<template>

    <div class="main-content">
      <div class="content">
        <div class="card">
          <div class="header-container">
            <h1 class="header">试卷用户关联视图</h1>
          </div>

          <div class="analysis-container">
            <div class="analysis-section left-section">
              <h3 class="sticky-title">试卷列表</h3>
              <div class="record-list fixed-height">
                <table id="examPapersTable">
                  <thead>
                  <tr>
                    <th>试卷名称</th>
                    <th>试卷内容</th>
                    <th>科目</th>
                    <th>难度</th>
                  </tr>
                  </thead>
                  <tbody>
                  <tr v-for="(paper, index) in examPapers"
                      :key="paper.id"
                      @click="handleExamPaperSelect(paper, $event)"
                      :class="{ 'highlight': (index === 0 && !selectedExamPaper) ||
                                            (selectedExamPaper && paper.id === selectedExamPaper.id) }">
                    <td>{{ paper.examPaperName }}</td>
                    <td class="question-content">{{ paper.examPaperContent }}</td>
                    <td>{{ paper.examPaperSubject }}</td>
                    <td>{{ paper.examPaperDifficulty }}</td>
                  </tr>
                  </tbody>
                </table>
              </div>
            </div>

            <div class="analysis-section right-section">
              <h3 class="sticky-title">关联用户列表</h3>
              <div class="question-list fixed-height">
                <div class="table-wrapper">
                  <table id="userDetailsTable">
                    <thead>
                    <tr>
                      <th width="100px">用户账户</th>
                      <th width="100px">真实姓名</th>
                      <th width="150px">大学</th>
                      <th width="150px">院系</th>
                      <th width="80px">测试次数</th>
                    </tr>
                    </thead>
                    <tbody>
                    <tr v-for="user in usersForPaper" :key="user.userId">
                      <td>{{ user.userName }}</td>
                      <td>{{ user.realName || '-' }}</td>
                      <td>{{ user.university || '-' }}</td>
                      <td>{{ user.department || '-' }}</td>
                      <td>{{ user.testCount }}</td>
                    </tr>
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          </div>

          <div class="action-btn-container">
            <button
                type="button"
                class="delete-btn"
                :disabled="!selectedExamPaper"
                @click="handleDeleteExamPaper"
            >
              删除选中试卷
            </button>
          </div>
        </div>
      </div>
    </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  fetchAllExamPapers,
  fetchUsersForExamPaper,
  deleteExamPaper
} from '@/service/paperClearService.js';

const examPapers = ref([]);
const usersForPaper = ref([]);
const selectedExamPaper = ref(null);

onMounted(async () => {
  try {
    examPapers.value = await fetchAllExamPapers();
    if (examPapers.value.length > 0) {
      await handleExamPaperSelect(examPapers.value[0]);
    }
  } catch (error) {
    console.error('加载试卷列表失败:', error);
    ElMessage.error('加载试卷列表失败');
  }
});

const handleExamPaperSelect = async (paper, event) => {
  selectedExamPaper.value = paper;

  if (event) {
    const rows = document.querySelectorAll('#examPapersTable tbody tr');
    rows.forEach(r => r.classList.remove('highlight'));
    event.currentTarget.classList.add('highlight');
  } else {
    const firstRow = document.querySelector('#examPapersTable tbody tr');
    if (firstRow) firstRow.classList.add('highlight');
  }

  try {
    usersForPaper.value = await fetchUsersForExamPaper(paper.id);
  } catch (error) {
    console.error('加载用户列表失败:', error);
    ElMessage.error('加载关联用户失败');
  }
};

const handleDeleteExamPaper = async () => {
  if (!selectedExamPaper.value) return;

  try {
    await ElMessageBox.confirm(
        `确定要删除试卷 "${selectedExamPaper.value.examPaperName}" 吗？此操作不可恢复！`,
        '警告',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning',
        }
    );

    await deleteExamPaper(selectedExamPaper.value.id);
    ElMessage.success('试卷删除成功');

    examPapers.value = await fetchAllExamPapers();
    if (examPapers.value.length > 0) {
      await handleExamPaperSelect(examPapers.value[0]);
    } else {
      usersForPaper.value = [];
      selectedExamPaper.value = null;
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除试卷失败:', error);
      ElMessage.error(`删除试卷失败: ${error.response?.data || error.message}`);
    }
  }
};
</script>


<style scoped>
/* 基础布局 - 精确高度控制 */
.main-content {
  display: flex;
  flex-direction: column;
  height: calc(90vh - 40px);
  padding: 20px;
  box-sizing: border-box;
  background-color: #f5f5f5;
  margin-left: 20px;
  overflow: hidden;
}

.content {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}

.card {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  padding: 20px;
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
}

/* 头部区域 */
.header-container {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-bottom: 15px;
  flex-shrink: 0;
}

.header {
  color: #333;
  margin: 0;
  font-size: 22px;
  text-align: center;
}

/* 核心内容区 */
.analysis-container {
  display: flex;
  justify-content: space-between;
  gap: 15px;
  flex: 1;
  min-height: 0;
  margin-bottom: 15px;
  overflow: hidden;
}

.analysis-section {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  position: relative;
}

.left-section {
  width: 55%; /* 左侧稍宽 */
}

.right-section {
  width: 45%; /* 右侧稍窄 */
}

/* 表格标题 */
.sticky-title {
  position: sticky;
  top: 0;
  margin: 0;
  padding: 10px 12px;
  background-color: #f8f9fa;
  border-bottom: 1px solid #dee2e6;
  color: #003366;
  z-index: 20;
  font-size: 15px;
  font-weight: 600;
}

/* 表格容器 */
.record-list, .question-list {
  flex: 1;
  overflow: auto;
  border: 1px solid #ddd;
  background-color: #f0f8ff;
  border-radius: 4px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.1);
  min-height: 160px;
}

.table-wrapper {
  width: 100%;
  height: 100%;
  overflow: auto;
}

/* 表格样式优化 */
table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
  table-layout: auto; /* 改为自动布局 */
}

/* 试卷表格列宽设置 */
#examPapersTable th:nth-child(1),
#examPapersTable td:nth-child(1) {
  width: 25%; /* 试卷名称列宽度 */
  min-width: 150px; /* 最小宽度 */
}

#examPapersTable th:nth-child(2),
#examPapersTable td:nth-child(2) {
  width: 40%; /* 试卷内容列宽度 */
}

#examPapersTable th:nth-child(3),
#examPapersTable td:nth-child(3) {
  width: 20%; /* 科目列宽度 */
}

#examPapersTable th:nth-child(4),
#examPapersTable td:nth-child(4) {
  width: 15%; /* 难度列宽度 */
}

/* 用户表格列宽设置（保持不变） */
#userDetailsTable th:nth-child(1),
#userDetailsTable td:nth-child(1) {
  width: 80px;
}

#userDetailsTable th:nth-child(2),
#userDetailsTable td:nth-child(2) {
  width: 120px;
}

#userDetailsTable th:nth-child(3),
#userDetailsTable td:nth-child(3) {
  width: 120px;
}

#userDetailsTable th:nth-child(4),
#userDetailsTable td:nth-child(4) {
  width: 120px;
}

#userDetailsTable th:nth-child(5),
#userDetailsTable td:nth-child(5) {
  width: 100px;
}

thead tr {
  position: sticky;
  top: 0;
  z-index: 10;
  background-color: #dee2e6;
  background-image: linear-gradient(to bottom, #dee2e6, #dee2e6);
}

thead th {
  padding: 10px 12px;
  text-align: left;
  color: #003366;
  font-weight: 600;
  border-bottom: 2px solid #d8bfd8;
}

tbody tr {
  transition: background-color 0.2s;
}

tbody tr td {
  padding: 10px 12px;
  border-bottom: 1px solid #dee2e6;
  color: #333;
}

/* 试卷名称特殊样式 */
#examPapersTable td:nth-child(1) {
  white-space: normal; /* 允许换行 */
  word-break: break-word; /* 单词内换行 */
}

.question-content {
  white-space: normal;
  word-break: break-word;
}

/* 交互效果 */
tbody tr.highlight {
  background-color: #e6f7ff !important;
  color: #003366 !important;
}

tbody tr:hover:not(.highlight) {
  background-color: #f8f9fa;
}

/* 按钮区域 */
.action-btn-container {
  display: flex;
  justify-content: center;
  padding: 12px 0;
  background: white;
  border-top: 1px solid #eee;
  margin-top: auto;
  flex-shrink: 0;
  position: sticky;
  bottom: 0;
}

.delete-btn {
  background-color: #f56c6c;
  color: white;
  padding: 8px 16px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: background-color 0.3s;
  min-width: 140px;
}

.delete-btn:hover:not(:disabled) {
  background-color: #e05d5d;
}

.delete-btn:disabled {
  background-color: #cccccc;
  cursor: not-allowed;
  opacity: 0.7;
}

/* 滚动条样式 */
::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .analysis-container {
    flex-direction: column;
  }
  .left-section, .right-section {
    width: 100%;
  }
  .right-section {
    margin-top: 15px;
  }

  .record-list, .question-list {
    min-height: 120px;
  }

  /* 移动端调整列宽 */
  #examPapersTable th:nth-child(1),
  #examPapersTable td:nth-child(1) {
    width: 30%;
    min-width: 120px;
  }

  #examPapersTable th:nth-child(2),
  #examPapersTable td:nth-child(2) {
    width: 40%;
  }
}
</style>