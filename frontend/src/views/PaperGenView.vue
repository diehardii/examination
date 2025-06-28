<template>
  <div class="app-container">
    <!-- 引入侧边栏组件 -->
    <SideBarMenu />

    <!-- 右侧内容区 -->
    <div class="main-content">
      <div class="content">
        <div class="card">
          <h1 class="header">请填写试卷的要求</h1>

          <!-- 表单和图片容器 -->
          <div class="form-image-container">
            <!-- 左边：表单 -->
            <div class="form-section">
              <form @submit.prevent="submitForm">
                试卷名称: <input type="text" v-model="form.name" required><br><br>
                <span class="hint">例如：C++期末卷，试卷名称不能重复</span><br><br>
                试卷内容: <input type="text" v-model="form.content" required><br><br>
                <span class="hint">请提出详细要求，例如：C++基础语法知识</span><br><br>

                <!-- 改动1：将难度和科目放在同一行 -->
                <div class="form-row">
                  <div class="form-group">
                    难度:
                    <select v-model="form.difficulty" required>
                      <option value="">-- 请选择难度 --</option>
                      <option value="简单">简单</option>
                      <option value="中等">中等</option>
                      <option value="困难">困难</option>
                    </select>
                  </div>
                  <div class="form-group">
                    科目：
                    <select id="subject" v-model="form.subject" class="select-control" required>
                      <option value="">-- 请选择科目 --</option>
                      <option v-for="subject in subjects"
                              :key="subject.subjectId"
                              :value="subject.subjectName">
                        {{ subject.subjectName }}
                      </option>
                    </select>
                  </div>
                </div>
                <br>

                <!-- 改动2：将单选题数量和判断题数量放在同一行 -->
                <div class="form-row">
                  <div class="form-group">
                    试卷单选题数量: <input type="text" v-model="form.singleChoiceNumber" required>
                  </div>
                  <div class="form-group">
                    判断题数量: <input type="text" v-model="form.judgeNumber" required>
                  </div>
                </div>
                <br><br>

                <!-- 修改点1：修改提交按钮，添加加载状态 -->
                <button
                    type="submit"
                    :disabled="isSubmitting"
                    class="submit-btn"
                >
                  {{ isSubmitting ? '生成中...' : '生成试卷' }}
                </button>

                <!-- 修改点2：添加进度条 -->
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

            <!-- 右边：图片 -->
            <div class="image-section">
              <img src="../assets/images/paper-gen.jpg" alt="试卷示意图">
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
// 修改点3：导入Element Plus组件
import { ElProgress } from 'element-plus';
import { generateExamPaper } from '@/service/examPaperService';
import { getSubjects } from '@/service/subjectService';
import SideBarMenu from '@/components/SideBarMenu.vue';


export default {
  name: 'PaperGen',
  components: {
    SideBarMenu,
    ElProgress // 修改点4：注册进度条组件
  },
  data() {
    return {
      form: {
        name: '',
        content: '',
        difficulty: '',
        subject: '',
        singleChoiceNumber: 10,
        judgeNumber: 5,
      },
      subjects: [],
      error: '',
      success: '',
      // 修改点5：添加加载状态相关数据
      isSubmitting: false,
      progress: 0,
      progressInterval: null
    };
  },
  async created() {
    try {
      this.subjects = await getSubjects();
    } catch (error) {
      this.error = '获取科目列表失败: ' + error.message;
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

    async submitForm() {
      this.error = '';
      this.success = '';
      // 修改点7：设置加载状态
      this.isSubmitting = true;
      this.startProgress();

      try {
        const paperData = await generateExamPaper(this.form);
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
        // 修改点8：确保清理加载状态
        this.stopProgress();
        this.isSubmitting = false;
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
/* 原有样式保持不变 */
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

.form-image-container {
  display: flex;
  justify-content: space-between;
}

.form-section {
  width: 50%;
}

.image-section {
  width: 45%;
  height: 100%;
  overflow: hidden;
}

.image-section img {
  width: 100%;
  height: 100%;
  object-fit: fill;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

input[type="text"], select {
  width: 100%;
  padding: 8px;
  margin: 5px 0;
  border: 1px solid #ddd;
  border-radius: 4px;
}

/* 修改点10：调整提交按钮样式 */
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

.select-control {
  width: 100%;
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
}

.form-row {
  display: flex;
  justify-content: space-between;
  gap: 20px;
}

.form-group {
  flex: 1;
}
</style>