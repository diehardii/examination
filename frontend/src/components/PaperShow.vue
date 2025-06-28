<template>
  <div class="content">
    <div class="card">
      <h1 class="header">{{ examPaper.examPaperName }}</h1>

      <div class="paper-info">
        <p><strong>科目：</strong>{{ examPaper.examPaperSubject }}</p>
        <p><strong>难度：</strong>{{ examPaper.examPaperDifficulty }}</p>
        <p><strong>题目数量：</strong>{{ examPaper.examPaperQuestionNumber }}</p>
        <p><strong>内容要求：</strong>{{ examPaper.examPaperContent }}</p>
      </div>

      <div class="questions" v-for="(question, index) in examPaper.questions" :key="question.id">
        <div class="question">
          <p>{{ index + 1 }}. {{ question.content }}</p>

          <div v-if="question.questionType === 'SINGLE_CHOICE'">
            <p>A. {{ question.optionA }}</p>
            <p>B. {{ question.optionB }}</p>
            <p>C. {{ question.optionC }}</p>
            <p>D. {{ question.optionD }}</p>
            <p><strong>正确答案：</strong>{{ question.correctAnswer }}</p>
          </div>

          <div v-if="question.questionType === 'JUDGE'">
            <p><strong>正确答案：</strong>
              {{ question.correctAnswer ? '正确' : '错误' }}
            </p>
          </div>
        </div>
      </div>
    </div>
    <div class="button-container">
      <input type="button" value="返回" @click="$router.go(-1)">
    </div>
  </div>
</template>

<script>
import { getExamPaper } from '@/service/examPaperService';

export default {
  data() {
    return {
      examPaper: {}
    };
  },
  async created() {
    try {
      console.log('试卷Id'+this.$route.params.id);
      this.examPaper = await getExamPaper(this.$route.params.id);
    } catch (error) {
      console.error('获取试卷详情失败:', error);
    }
  }
};
</script>

<style scoped>
/* 保持原有样式不变 */
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
}

.paper-info {
  margin-bottom: 20px;
}

.questions {
  margin-bottom: 15px;
  padding-bottom: 15px;
  border-bottom: 1px solid #eee;
}

.button-container {
  margin-top: 20px;
  text-align: center;
}

input[type="button"] {
  background-color: #28a745;
  border: none;
  color: white;
  padding: 10px 20px;
  text-align: center;
  text-decoration: none;
  display: inline-block;
  font-size: 16px;
  margin: 4px 2px;
  cursor: pointer;
  border-radius: 4px;
  transition-duration: 0.4s;
}

input[type="button"]:hover {
  background-color: white;
  color: black;
  border: 2px solid #28a745;
}
</style>