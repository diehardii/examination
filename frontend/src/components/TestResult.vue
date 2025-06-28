<template>
  <div class="result-container">
    <h1 class="header">考试结果</h1>

    <div class="summary">
      <h2>{{ examPaper.examPaperName }}</h2>
      <p>总题数: {{ total }} 题   答对: {{ correctNumber }} 题  得分: {{ testScore }} 分</p>

    </div>

    <div class="detail-section">
      <h3>单选题</h3>
      <table>
        <thead>
        <tr>
          <th>题号</th>
          <th>你的答案</th>
          <th>正确答案</th>
          <th>结果</th>
        </tr>
        </thead>
        <tbody>
        <tr v-for="(answer, number) in singleChoiceUserAnswers" :key="'sc'+number">
          <td>{{ number }}</td>
          <td>{{ answer }}</td>
          <td>{{ singleChoiceCorrectAnswers[number] }}</td>
          <td :class="answer === singleChoiceCorrectAnswers[number] ? 'correct' : 'wrong'">
            {{ answer === singleChoiceCorrectAnswers[number] ? '✓' : '✗' }}
          </td>
        </tr>
        </tbody>
      </table>
    </div>

    <div class="detail-section">
      <h3>判断题</h3>
      <table>
        <thead>
        <tr>
          <th>题号</th>
          <th>你的答案</th>
          <th>正确答案</th>
          <th>结果</th>
        </tr>
        </thead>
        <tbody>
        <tr v-for="(answer, number) in judgeUserAnswers" :key="'jq'+number">
          <td>{{ number }}</td>
          <td>{{ answer }}</td>
          <td>{{ judgeCorrectAnswers[number] }}</td>
          <td :class="answer === judgeCorrectAnswers[number] ? 'correct' : 'wrong'">
            {{ answer === judgeCorrectAnswers[number] ? '✓' : '✗' }}
          </td>
        </tr>
        </tbody>
      </table>
    </div>

    <div class="back-btn-container">
      <button class="back-btn" @click="goBack">返回试卷列表</button>
    </div>
  </div>
</template>

<script>
export default {

  name: 'TestResult',

  data() {
    return {
      examPaper: {},
      total: 0,
      correctNumber: 0,
      testScore: 0,
      singleChoiceUserAnswers: {},
      judgeUserAnswers: {},
      singleChoiceCorrectAnswers: {},
      judgeCorrectAnswers: {}
    };
  },
  created() {

      this.loadResultData();

  },
  methods: {
    loadResultData() {
      const resultData = JSON.parse(sessionStorage.getItem('test_result'));
      if (resultData) {
        this.examPaper = resultData.examPaper || {};
        this.total = resultData.total || 0;
        this.correctNumber = resultData.correctNumber || 0;
        this.testScore = resultData.testScore || 0;
        this.singleChoiceUserAnswers = resultData.singleChoiceUserAnswers || {};
        this.judgeUserAnswers = resultData.judgeUserAnswers || {};
        this.singleChoiceCorrectAnswers = resultData.singleChoiceCorrectAnswers || {};
        this.judgeCorrectAnswers = resultData.judgeCorrectAnswers || {};
      } else {
        this.$router.push({ name: 'PaperTest' });
      }
    },
    goBack() {
      this.$router.push({ name: 'PaperTest' });
    }
  }
};
</script>

<style scoped>
.result-container {
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

.summary {
  background-color: #f5f5f5;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 30px;
  text-align: center;
}

.summary h2 {
  color: #333;
  margin-bottom: 15px;
}

.summary p {
  font-size: 18px;
  margin: 10px 0;
  color: #555;
}

.detail-section {
  margin-bottom: 30px;
}

.detail-section h3 {
  color: #333;
  margin-bottom: 15px;
  padding-bottom: 5px;
  border-bottom: 1px solid #ddd;
}

table {
  width: 100%;
  border-collapse: collapse;
  margin-bottom: 20px;
}

table, th, td {
  border: 1px solid #ddd;
}

th, td {
  padding: 12px;
  text-align: left;
}

th {
  background-color: #f2f2f2;
}

tr:nth-child(even) {
  background-color: #f9f9f9;
}

.correct {
  color: green;
  font-weight: bold;
}

.wrong {
  color: red;
  font-weight: bold;
}

.back-btn-container {
  text-align: center;
  margin-top: 30px;
}

.back-btn {
  padding: 12px 30px;
  background-color: #45a049;
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 16px;
  cursor: pointer;
  transition: background-color 0.3s;
}

.back-btn:hover {
  background-color: #45a049;
}
</style>