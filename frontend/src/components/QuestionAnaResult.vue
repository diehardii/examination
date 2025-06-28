<!-- views/AnalysisResult.vue -->
<template>
  <div class="analysis-result-container">
    <el-page-header @back="goBack" title="返回">
      <template #content>
        <span class="header-title">试题分析结果</span>
      </template>
    </el-page-header>

    <div class="result-content">
      <el-card class="question-info-card">
        <template #header>
          <div class="card-header">
            <span>题目信息</span>
          </div>
        </template>

        <div class="question-info">
          <p><strong>题号：</strong>{{ questionData.number }}</p>
          <p><strong>题目：</strong>{{ questionData.content }}</p>

          <div v-if="questionData.type === '单选题' ">
            <p><strong>选项：</strong></p>
            <ul>
              <li v-for="(value, key) in questionData.options" :key="key">
                <strong>{{ key }}:</strong> {{ value }}
              </li>
            </ul>
          </div>

          <p><strong>你的答案：</strong>{{ questionData.userAnswer }}</p>
          <p><strong>正确答案：</strong>{{ questionData.correctAnswer }}</p>
        </div>
      </el-card>

      <el-card class="analysis-result-card">
        <template #header>
          <div class="card-header">
            <span>分析结果</span>
          </div>
        </template>

        <div class="analysis-content" v-html="analysisData"></div>
      </el-card>

      <div class="back-button">
        <el-button
            type="primary"
            @click="goBack"
            class="green-button">
          返回分析页面
        </el-button>
      </div>
    </div>
  </div>
</template>

<script>
import { useRouter } from 'vue-router'

export default {
  props: {
    analysisData: {
      type: String,
      default: ''
    },
    questionData: {
      type: Object,
      default: () => ({
        number: '',
        content: '',
        type: '',
        options: {},
        userAnswer: '',
        correctAnswer: ''
      })
    }
  },

  setup() {
    const router = useRouter()

    const goBack = () => {
      router.push({ name: 'QuestionAna' })
    }

    return {
      goBack
    }
  }
}
</script>

<style scoped>
.analysis-result-container {
  padding: 20px;
}

.header-title {
  font-size: 18px;
  font-weight: bold;
}

.result-content {
  margin-top: 20px;
}

.question-info-card, .analysis-result-card {
  margin-bottom: 20px;
}

.question-info p, .question-info li {
  margin: 8px 0;
  line-height: 1.6;
}

.analysis-content {
  line-height: 1.8;
}

.back-button {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

/* 新增绿色按钮样式 */
.green-button {
  background-color: #4CAF50;
  color: white;
  border: none;
}

.green-button:hover {
  background-color: #45a049;
}

.green-button:active {
  background-color: #3e8e41;
}
</style>