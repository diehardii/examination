<template>
  <div class="question-container">
    <h1 class="header">{{ examPaper.examPaperName }}</h1>
    <div class="question-list">
      <form @submit.prevent="submitAnswers">
        <div v-for="question in questions" :key="question.number" class="question-item">
          <div class="question-content">
            <h3>第{{ question.number }}题: {{ question.content }}</h3>

            <div v-if="question.questionType === 'SINGLE_CHOICE'" class="options">
              <label>
                <input type="radio" :name="'SC_' + question.number" value="A" v-model="answers['SC_' + question.number]">
                A. {{ question.optionA }}
              </label><br>
              <label>
                <input type="radio" :name="'SC_' + question.number" value="B" v-model="answers['SC_' + question.number]">
                B. {{ question.optionB }}
              </label><br>
              <label>
                <input type="radio" :name="'SC_' + question.number" value="C" v-model="answers['SC_' + question.number]">
                C. {{ question.optionC }}
              </label><br>
              <label>
                <input type="radio" :name="'SC_' + question.number" value="D" v-model="answers['SC_' + question.number]">
                D. {{ question.optionD }}
              </label>
            </div>

            <div v-else class="options">
              <label>
                <input type="radio" :name="'JQ_' + question.number" value="T" v-model="answers['JQ_' + question.number]">
                正确
              </label>
              <label>
                <input type="radio" :name="'JQ_' + question.number" value="F" v-model="answers['JQ_' + question.number]">
                错误
              </label>
            </div>
          </div>
        </div>

        <div class="submit-btn-container">
          <button type="submit" class="submit-btn">提交答案</button>
        </div>
      </form>
    </div>
  </div>
</template>

<script>
import { getPaperQuestions, submitAnswers } from '@/service/questionService';

export default {
  name: 'QuestionAnswer',
  data() {
    return {
      examPaper: {},
      questions: [],
      answers: {}
    };
  },
  async created() {
    const paperId = this.$route.params.paperId;
    const response = await getPaperQuestions(paperId);
    this.examPaper = response.examPaper;
    this.questions = response.questions;

    // 初始化答案对象
    this.questions.forEach(q => {
      if (q.questionType === 'SINGLE_CHOICE') {
        this.answers[`SC_${q.number}`] = '';
      } else {
        this.answers[`JQ_${q.number}`] = '';
      }
    });
  },
  methods: {
    async submitAnswers() {
      const allAnswered = this.questions.every(q => {
        const key = q.questionType === 'SINGLE_CHOICE' ? `SC_${q.number}` : `JQ_${q.number}`;
        return this.answers[key] !== '';
      });

      if (!allAnswered) {
        alert('请回答所有题目');
        return;
      }

      const response = await submitAnswers({
        examPaperId: this.examPaper.id,
        answers: this.answers
      });
      console.log(response);
      sessionStorage.setItem('test_result', JSON.stringify(response));
      this.$router.push({ name: 'TestResult' });
    }
  }
};
</script>

<style scoped>
.question-container {
  width: 100%;
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.header {
  text-align: center;
  color: #003366;
  margin-bottom: 30px;
}

.question-list {
  background-color: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.question-item {
  margin-bottom: 30px;
  padding-bottom: 20px;
  border-bottom: 1px solid #eee;
}

.question-content h3 {
  color: #333;
  margin-bottom: 15px;
}

.options {
  margin-left: 20px;
}

.options label {
  display: block;
  margin-bottom: 10px;
  cursor: pointer;
}

.options input[type="radio"] {
  margin-right: 10px;
}

.submit-btn-container {
  text-align: center;
  margin-top: 30px;
}

.submit-btn {
  padding: 12px 30px;
  background-color: #4CAF50;
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 16px;
  cursor: pointer;
  transition: background-color 0.3s;
}

.submit-btn:hover {
  background-color: #45a049;
}
</style>