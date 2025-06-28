<template>
  <div class="analysis-result">
    <h2>考试科目分析结果 - {{ subject }}</h2>

    <div class="result-card">
      <div class="result-content" v-html="formattedResult"></div>
    </div>

    <button class="back-btn" @click="goBack">返回</button>
  </div>
</template>

<script>
export default {
  name: 'AnalysisResult',
  data() {
    return {
      subject: this.$route.params.subject,
      result: decodeURIComponent(this.$route.query.result)
    };
  },
  computed: {
    formattedResult() {
      return this.result.replace(/\n/g, '<br>');
    }
  },
  methods: {
    goBack() {
      this.$router.go(-1);
    }
  }
};
</script>

<style scoped>
.analysis-result {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
}

h2 {
  text-align: center;
  color: #2c3e50;
  margin-bottom: 30px;
}

.result-card {
  background-color: #f9f9f9;
  border-radius: 8px;
  padding: 25px;
  margin-bottom: 30px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.result-content {
  line-height: 1.8;
  font-size: 16px;
}

.back-btn {
  display: block;
  margin: 0 auto;
  padding: 10px 20px;
  background-color: #28a745;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 16px;
}

.back-btn:hover {
  background-color: #218838;
}
</style>