<template>

  <div class="content">
    <div class="card">
      <h1 class="header">基于错误题目生成新试卷</h1>

      <div class="form-list-container">
        <!-- 左边：表单 -->
        <div class="form-section">
          <form @submit.prevent="submitForm">
            试卷名称: <input type="text" v-model="form.name" required><br><br>
            <span class="hint">例如：C++期末卷，试卷名称不能重复</span><br><br>
            单选题数量: <input type="number" v-model="form.singleChoiceNumber" required><br><br>
            判断题数量: <input type="number" v-model="form.judgeNumber" required><br><br>

            <!-- 修改后的提交按钮 -->
            <button
                type="submit"
                :disabled="isSubmitting"
                class="submit-btn"
            >
              {{ isSubmitting ? '生成中...' : '生成试卷' }}
            </button>

            <!-- 添加进度条 -->
            <el-progress
                v-if="isSubmitting"
                :percentage="progress"
                :stroke-width="15"
                :text-inside="true"
                status="success"
                style="margin-top: 20px; width: 100%"
            />

            <div v-if="error" class="error">{{ error }}</div>
            <div v-if="success" class="success">{{ success }}</div>
          </form>
        </div>

        <!-- 右边：试卷列表 -->
        <div class="list-section">

          <div class="paper-list">
            <table>
              <caption class="sticky-title">我的错误试卷</caption>
              <thead>
              <tr>
                <th>试卷ID</th>
                <th>试卷名称</th>
                <th>科目</th>
              </tr>
              </thead>
              <tbody>
              <tr v-for="paper in wrongPapers"
                  :key="paper.id"
                  :class="{highlight: selectedPaperId === paper.id}"
                  @click="selectPaper(paper.id)">
                <td>{{ paper.id }}</td>
                <td>{{ paper.examPaperName }}</td>
                <td>{{ paper.examPaperSubject }}</td>
              </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  </div>

</template>

<script>
import { generateExamPaperFromWrong } from '@/service/paperGenFromWrongService.js';
import { getUserExamPapers } from '@/service/userExamPaperService.js';
import { ElProgress } from 'element-plus';

export default {
  name: 'PaperGenFromWrong',
  components: {
    ElProgress
  },
  data() {
    return {
      form: {
        name: '',
        content: '',
        singleChoiceNumber: 10,
        judgeNumber: 5,
        examPaperId: null
      },
      wrongPapers: [],
      selectedPaperId: null,
      error: '',
      success: '',
      // 新增的加载状态相关数据
      isSubmitting: false,
      progress: 0,
      progressInterval: null
    };
  },
  async created() {
    await this.loadWrongPapers();
  },
  methods: {
    async loadWrongPapers() {
      try {
        const currentUser = JSON.parse(localStorage.getItem('user') || '{}')
        const username = currentUser.username
        if (username) {
          const papers = await getUserExamPapers(username);
          this.wrongPapers = papers.filter(p => p.examPaperDifficulty === '自定义');
          if (this.wrongPapers.length > 0) {
            this.selectedPaperId = this.wrongPapers[0].id;
          }
        }
      } catch (error) {
        this.error = '加载错误试卷失败: ' + error.message;
      }
    },
    selectPaper(paperId) {
      this.selectedPaperId = paperId;
    },
    // 进度条控制方法
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
    async submitForm() {
      this.error = '';
      this.success = '';
      this.isSubmitting = true;
      this.startProgress();

      if (!this.selectedPaperId) {
        this.error = '请先选择一份错误试卷';
        this.isSubmitting = false;
        this.stopProgress();
        return;
      }
      this.form.content = '根据错题生成的试卷'
      try {
        const requestData = {
          ...this.form,
          examPaperId: this.selectedPaperId,
          difficulty: '自定义',
          subject: this.wrongPapers.find(p => p.id === this.selectedPaperId).examPaperSubject
        };

        const paperData = await generateExamPaperFromWrong(requestData);

        if (paperData?.id) {
          this.$router.push({
            name: 'PaperShow',
            params: { id: paperData.id }
          });
        } else {
          throw new Error('生成的试卷数据不完整，缺少ID');
        }
      } catch (error) {
        this.error = '生成试卷时出错: ' + (error.response?.data?.error || error.message);
      } finally {
        this.stopProgress();
        this.isSubmitting = false;
      }
    }
  },
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
  background-color: #f5f5f5;
  margin-left: 260px;
  width: calc(100% - 260px);
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

.header {
  color: #333;
  margin-bottom: 20px;
  font-size: 24px;
}

.form-list-container {
  display: flex;
  justify-content: space-between;
  gap: 20px;
}

.form-section {
  width: 45%;
}

.list-section {
  width: 50%;
}

.paper-list {
  width: 100%;
  height: 400px;
  overflow: auto;
  border: 1px solid #ddd;
  background-color: #f0f8ff;
  border-radius: 4px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.1);
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
  width: 100%;
}

table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

thead tr {
  position: sticky;
  top: 0;
  z-index: 10;
  background-color: #f3e5ff !important;
  background-image: linear-gradient(to bottom, #f3e5ff, #e6d1f9);
}

thead th {
  padding: 12px 15px;
  text-align: left;
  background-color: #f8f9fa;
  color: #003366;
  font-weight: 600;
  border-bottom: 2px solid #d8bfd8;
}

tbody tr {
  cursor: pointer;
  transition: background-color 0.2s;
}

tbody tr td {
  padding: 12px 15px;
  border-bottom: 1px solid #dee2e6;
  color: #333;
}

tbody tr.highlight {
  background-color: #e6f7ff !important;
  color: #003366 !important;
}

tbody tr:hover:not(.highlight) {
  background-color: #f8f9fa;
}

input[type="text"], input[type="number"] {
  width: 100%;
  padding: 8px;
  margin: 5px 0;
  border: 1px solid #ddd;
  border-radius: 4px;
}

/* 修改后的提交按钮样式 */
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

.hint {
  color: #666;
  font-size: 0.9em;
}

.error {
  color: red;
  margin-top: 10px;
}

.success {
  color: green;
  margin-top: 10px;
}

.paper-list::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

.paper-list::-webkit-scrollbar-track {
  background: #f1f1f1;
}

.paper-list::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 4px;
}

.paper-list::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}


caption.sticky-title {
  position: sticky;
  top: 0;
  left: 0;
  padding: 15px;
  background-color: #f8f9fa;
  border-bottom: 1px solid #dee2e6;
  color: #003366;
  z-index: 20;
  width: 100%;
  box-sizing: border-box;
  text-align: center;
  font-size: 1.17em; /* h3的默认大小 */
  font-weight: bold; /* h3的默认粗细 */
  margin: 0;

}
</style>