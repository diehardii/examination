<template>
  <div class="app-container">
    <SideBarMenu />
    <div class="main-content">
      <div class="content">
        <div class="card">
          <div class="header-container">
            <h1 class="header">请选择你要分析的题目</h1>
          </div>

          <div class="analysis-container">
            <div class="analysis-section left-section">
              <h3 class="sticky-title">考试记录</h3>
              <div class="record-list fixed-height">
                <table id="examRecordsTable">
                  <thead>
                  <tr>
                    <th>试卷名称</th>
                    <th>考试时间</th>
                    <th>得分</th>
                  </tr>
                  </thead>
                  <tbody>
                  <tr v-for="(record, index) in userTestRecords"
                      :key="record.testId"
                      @click="handleExamRecordSelect(record, $event)"
                      :class="{ 'highlight': (index === 0 && !selectedTestRecord) ||
                                          (selectedTestRecord && record.testId === selectedTestRecord.testId) }">
                    <td>{{ record.examPaper.examPaperName }}</td>
                    <td>{{ formatDateTime(record.testTime) }}</td>
                    <td>{{ record.testScore }}</td>
                  </tr>
                  </tbody>
                </table>
              </div>
            </div>

            <div class="analysis-section right-section">
              <h3 class="sticky-title">题目详情</h3>
              <div class="question-list fixed-height">
                <div class="table-wrapper">
                  <table id="questionDetailsTable">
                    <thead>
                    <tr>
                      <th width="60px">题号</th>
                      <th>题目</th>
                      <th width="80px">用户答案</th>
                      <th width="80px">正确答案</th>
                    </tr>
                    </thead>
                    <tbody>
                    <tr v-for="(question, index) in questionsWithAnswers"
                        :key="question.number"
                        @click="handleQuestionSelect(question, $event)"
                        :class="{ 'highlight': (index === 0 && !selectedQuestion && questionsWithAnswers.length > 0) ||
                                            (selectedQuestion && question.number === selectedQuestion.number) }">
                      <td>{{ question.number }}</td>
                      <td class="question-content">{{ question.content }}</td>
                      <td>{{ question.userAnswer }}</td>
                      <td>{{ question.correctAnswer }}</td>
                    </tr>
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          </div>

          <div class="action-btn-container">
            <button
                type="submit"
                :disabled="isSubmitting || !selectedQuestion"
                class="submit-btn"
                @click="handleAnalyzeQuestion"
            >
              {{ isSubmitting ? '分析中...' : '分析选中题目' }}
            </button>
            <el-progress
                v-if="isSubmitting"
                :percentage="progress"
                :stroke-width="15"
                :text-inside="true"
                status="success"
                style="margin-top: 20px; width: 100%"
            />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import SideBarMenu from '@/components/SideBarMenu.vue';
import { ElProgress } from 'element-plus';
import { getUserTestRecords} from '@/service/userTestRecordService'
import {  getQuestionsWithAnswers,  analyzeQuestion,  formatDateTime }
  from '@/service/questionService'

export default {
  name: 'QuestionAnalysis',
  components: {
    SideBarMenu,
    ElProgress
  },
  data() {
    return {
      userId: JSON.parse(localStorage.getItem('user') || '{}')?.id || '1',
      userTestRecords: [],
      questionsWithAnswers: [],
      selectedTestRecord: null,
      selectedQuestion: null,
      isSubmitting: false,
      progress: 0,
      progressInterval: null
    };
  },
  async created() {
    await this.loadUserTestRecords();
  },
  methods: {
    async loadUserTestRecords() {
      try {
        this.userTestRecords = await getUserTestRecords(this.userId);
        if (this.userTestRecords.length > 0) {
          await this.handleExamRecordSelect(this.userTestRecords[0]);
        }
      } catch (error) {
        console.error('加载考试记录失败:', error);
      }
    },

    async handleExamRecordSelect(record, event) {
      if (!record) return;

      this.selectedTestRecord = record;

      if (event) {
        const rows = document.querySelectorAll('#examRecordsTable tbody tr');
        rows.forEach(r => r.classList.remove('highlight'));
        event.currentTarget.classList.add('highlight');
      } else {
        this.$nextTick(() => {
          const firstRow = document.querySelector('#examRecordsTable tbody tr');
          if (firstRow) {
            firstRow.classList.add('highlight');
          }
        });
      }

      try {
        this.questionsWithAnswers = await getQuestionsWithAnswers(record.testId, this.userId);
        this.selectedQuestion = null;

        this.$nextTick(() => {
          const questionRows = document.querySelectorAll('#questionDetailsTable tbody tr');
          questionRows.forEach(r => r.classList.remove('highlight'));

          if (this.questionsWithAnswers.length > 0) {
            const firstQuestionRow = document.querySelector('#questionDetailsTable tbody tr');
            if (firstQuestionRow) {
              firstQuestionRow.classList.add('highlight');
              this.selectedQuestion = this.questionsWithAnswers[0];
            }
          }
        });
      } catch (error) {
        console.error('加载题目详情失败:', error);
      }
    },

    handleQuestionSelect(question, event) {
      this.selectedQuestion = question;
      const rows = document.querySelectorAll('#questionDetailsTable tbody tr');
      rows.forEach(r => r.classList.remove('highlight'));
      event.currentTarget.classList.add('highlight');
    },

    startProgress() {
      this.progress = 0;
      this.progressInterval = setInterval(() => {
        this.progress = Math.min(this.progress + 5, 90); // 最多到90%
      }, 1000);
    },

    stopProgress() {
      clearInterval(this.progressInterval);
      this.progress = 100;
      setTimeout(() => this.progress = 0, 1000);
    },

    async handleAnalyzeQuestion() {
      if (!this.selectedQuestion || !this.selectedTestRecord) return;

      this.isSubmitting = true;
      this.startProgress();

      try {
        const result = await analyzeQuestion(
            this.selectedTestRecord.testId,
            this.selectedQuestion.number,
            this.userId
        );

        this.$router.push({
          name: 'QuestionAnaResult',
          query: {
            analysisData: result.analysisResult,
            questionData: JSON.stringify({
              number: this.selectedQuestion.number,
              content: this.selectedQuestion.content,
              userAnswer: this.selectedQuestion.userAnswer,
              correctAnswer: this.selectedQuestion.correctAnswer,
              type: this.selectedQuestion.type,
              options: result.options,
            })
          }
        });
      } catch (error) {
        console.error('分析题目失败:', error);
        alert('分析题目失败，请稍后重试');
      } finally {
        this.stopProgress();
        this.isSubmitting = false;
      }
    },

    goBack() {
      this.$router.go(-1);
    },

    formatDateTime
  },
  beforeUnmount() {
    if (this.progressInterval) {
      clearInterval(this.progressInterval);
    }
  }
};
</script>

<style scoped>
/* Base layout styles */
.app-container {
  display: flex;
  min-height: 100vh;
}

.main-content {
  flex: 1;
  padding: 20px;
  background-color: #f5f5f5;
  margin-left: 250px;
  width: calc(100% - 250px);
}

.content {
  padding: 20px;
}

.card {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  padding: 20px;
}

.header-container {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-bottom: 20px;
  width: 100%;
}

.header {
  color: #333;
  margin-bottom: 20px;
  font-size: 24px;
  text-align: center;
}

/* Analysis container styles */
.analysis-container {
  display: flex;
  justify-content: space-between;
  margin-top: 20px;
  gap: 20px;
}

.analysis-section {
  position: relative;
  box-sizing: border-box;
}

.left-section {
  width: 35%; /* 左侧宽度减少40% */
}

.right-section {
  width: 65%; /* 右侧宽度增加40% */
}

.sticky-title {
  position: sticky;
  top: 0;
  left: 0;
  margin: 0;
  padding: 15px;
  background-color: #f8f9fa;
  border-bottom: 1px solid #dee2e6;
  color: #003366;
  z-index: 20;
  white-space: nowrap;
  box-sizing: border-box;
  width: 100%;
}

.record-list, .question-list {
  width: 100%;
  height: 400px;
  overflow: auto;
  border: 1px solid #ddd;
  background-color: #f0f8ff;
  border-radius: 4px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.1);
  box-sizing: border-box;
}

.table-wrapper {
  overflow-x: auto;
  width: 100%;
}

/* Table styles */
table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
  table-layout: fixed;
}

thead tr {
  position: sticky;
  top: 0;
  z-index: 10;
  background-color: #f3e5ff !important;
  background-image: linear-gradient(to bottom, #f3e5ff, #e6d1f9);
  user-select: none;
}

thead th {
  padding: 12px 15px;
  text-align: left;
  background-color: #f8f9fa;
  color: #003366;
  font-weight: 600;
  border-bottom: 2px solid #d8bfd8;
  white-space: nowrap;
}

tbody tr {
  cursor: pointer;
  transition: background-color 0.2s;
}

tbody tr td {
  padding: 12px 15px;
  border-bottom: 1px solid #dee2e6;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
}

.question-content {
  white-space: normal; /* 允许换行 */
  word-break: break-word; /* 长单词换行 */
}

tbody tr.highlight {
  background-color: #e6f7ff !important;
  color: #003366 !important;
}

tbody tr:hover:not(.highlight) {
  background-color: #f8f9fa;
}

/* Button styles */
.action-btn-container {
  display: flex;
  flex-direction: column;
  justify-content: center;
  width: 100%;
  margin-top: 20px;
}

.submit-btn {
  background-color: #4CAF50;
  color: white;
  padding: 10px 20px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 16px;
  width: 100%;
}

.submit-btn:hover:not(:disabled) {
  background-color: #45a049;
}

.submit-btn:disabled {
  background-color: #cccccc;
  cursor: not-allowed;
}

/* Scrollbar styles */
.record-list::-webkit-scrollbar,
.question-list::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

.record-list::-webkit-scrollbar-track,
.question-list::-webkit-scrollbar-track {
  background: #f1f1f1;
}

.record-list::-webkit-scrollbar-thumb,
.question-list::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 4px;
}

.record-list::-webkit-scrollbar-thumb:hover,
.question-list::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}
</style>