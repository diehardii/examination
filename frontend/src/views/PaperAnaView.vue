<template>
  <div class="app-container">
    <SideBarMenu />
    <div class="main-content">
      <div class="paper-analysis">
        <h2>请选择需要分析的科目</h2>

        <div class="analysis-wrapper">
          <div class="analysis-container">
            <div class="subject-list">
              <h3>科目列表</h3>
              <table>
                <thead>
                <tr>
                  <th>科目名称</th>
                </tr>
                </thead>
                <tbody>
                <tr
                    v-for="subject in subjects"
                    :key="subject"
                    @click="selectSubject(subject)"
                    :class="{ 'highlight': selectedSubject === subject }"
                >
                  <td>{{ subject }}</td>
                </tr>
                </tbody>
              </table>
            </div>

            <div class="exam-records">
              <h3>考试记录</h3>
              <div class="records-scrollable">
                <table>
                  <thead>
                  <tr>
                    <th>试卷名称</th>
                    <th>考试时间</th>
                    <th>得分</th>
                    <th>正确题数</th>
                  </tr>
                  </thead>
                  <tbody>
                  <tr v-if="records.length === 0">
                    <td colspan="4" class="no-data">请选择科目查看考试记录</td>
                  </tr>
                  <tr v-for="record in records" :key="record.testId">
                    <td>{{ record.examPaper.examPaperName }}</td>
                    <td>{{ formatDate(record.testTime) }}</td>
                    <td>{{ record.testScore }}</td>
                    <td>{{ record.correctNumber }}</td>
                  </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>

          <div class="analyze-btn-container">
            <!-- 修改点1：修改分析按钮，添加加载状态 -->
            <button
                class="analyze-btn"
                @click="analyze"
                :disabled="!selectedSubject || isAnalyzing"
            >
              {{ isAnalyzing ? '分析中...' : '分析' }}
            </button>

            <!-- 修改点2：添加进度条 -->
            <el-progress
                v-if="isAnalyzing"
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
// 修改点3：导入Element Plus组件
import { ElProgress } from 'element-plus';
import {getUserSubjects} from '@/service/subjectService.js';
import {getExamRecords,analyzeSubject} from '@/service/userTestRecordService';
import SideBarMenu from "@/components/SideBarMenu.vue";

export default {
  name: 'PaperAnalysis',
  components: {
    SideBarMenu,
    ElProgress // 修改点4：注册进度条组件
  },
  data() {
    return {
      subjects: [],
      selectedSubject: null,
      records: [],
      // 修改点5：添加分析状态相关数据
      isAnalyzing: false,
      progress: 0,
      progressInterval: null
    };
  },
  async created() {
    this.subjects = await getUserSubjects();
    if (this.subjects.length > 0) {
      this.selectSubject(this.subjects[0]);
    }
  },
  methods: {
    // 修改点6：添加进度条控制方法
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

    async selectSubject(subject) {
      this.selectedSubject = subject;
      this.records = await getExamRecords(subject);
      console.log(this.records);
    },
    formatDate(dateStr) {
      return new Date(dateStr).toLocaleString();
    },
    async analyze() {
      if (this.selectedSubject) {
        // 修改点7：设置分析状态
        this.isAnalyzing = true;
        this.startProgress();

        try {
          const result = await analyzeSubject(this.selectedSubject);
          this.$router.push({
            name: 'AnalysisResult',
            params: { subject: this.selectedSubject },
            query: { result: encodeURIComponent(result) }
          });
        } catch (error) {
          console.error('分析失败:', error);
        } finally {
          // 修改点8：确保清理分析状态
          this.stopProgress();
          this.isAnalyzing = false;
        }
      }
    }
  },
  // 修改点9：组件销毁时清理定时器
  beforeUnmount() {
    if (this.progressInterval) {
      clearInterval(this.progressInterval);
    }
  }
};
</script>

<style scoped>
.app-container {
  display: flex;
  min-height: 100vh;
}

.main-content {
  flex: 1;
  padding: 20px;
  margin-left: 290px;
  width: calc(100% - 290px);
  box-sizing: border-box;
}

.paper-analysis {
  max-width: 1200px;
  margin: 0 auto;
}

h2 {
  text-align: center;
  color: #2c3e50;
  margin-bottom: 30px;
}

.analysis-wrapper {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 180px);
}

.analysis-container {
  display: flex;
  gap: 20px;
  flex: 1;
  min-height: 0;
}

.subject-list, .exam-records {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.subject-list {
  width: 40%;
}

.exam-records {
  width: 60%;
}

h3 {
  background-color: #f5f5f5;
  padding: 10px;
  margin: 0;
  border-bottom: 1px solid #ddd;
}

.records-scrollable {
  flex: 1;
  overflow-y: auto;
  border: 1px solid #ddd;
  border-radius: 4px;
  max-height: 500px;
}

table {
  width: 100%;
  border-collapse: collapse;
}

th, td {
  padding: 12px 15px;
  text-align: left;
  border-bottom: 1px solid #ddd;
}

th {
  background-color: #f8f9fa;
  font-weight: 600;
  position: sticky;
  top: 0;
}

tbody tr {
  cursor: pointer;
  transition: background-color 0.2s;
}

tbody tr:hover {
  background-color: #f5f5f5;
}

.highlight {
  background-color: #e6f7ff;
}

.no-data {
  text-align: center;
  color: #999;
}

.analyze-btn-container {
  text-align: center;
  margin-top: 20px;
  padding: 10px 0;
}

/* 修改点10：调整分析按钮样式，与示例保持一致 */
.analyze-btn {
  background-color: #4CAF50;
  color: white;
  padding: 10px 20px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 16px;
  min-width: 120px;
  width: 100%;
}

.analyze-btn:hover:not(:disabled) {
  background-color: #45a049;
}

.analyze-btn:disabled {
  background-color: #cccccc;
  cursor: not-allowed;
}

/* 滚动条样式 */
.records-scrollable::-webkit-scrollbar {
  width: 8px;
}

.records-scrollable::-webkit-scrollbar-track {
  background: #f1f1f1;
}

.records-scrollable::-webkit-scrollbar-thumb {
  background: #888;
  border-radius: 4px;
}

.records-scrollable::-webkit-scrollbar-thumb:hover {
  background: #555;
}
</style>