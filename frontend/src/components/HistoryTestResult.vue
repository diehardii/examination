<template>
  <div class="result-container">
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
import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { getTestRecordDetails } from '@/service/userTestRecordDetailService'

export default {
  name: 'TestResult',
  props: {
    testId: {
      type: String,
      required: true
    },
    onClose: {
      type: Function,
      required: true
    }
  },
  setup(props) {
    const router = useRouter()

    // 使用 ref 定义所有响应式数据
    const loading = ref(false)
    const error = ref(null)
    const examPaper = ref({})
    const total = ref(0)
    const correctNumber = ref(0)
    const testScore = ref(0)
    const singleChoiceUserAnswers = ref({})
    const judgeUserAnswers = ref({})
    const singleChoiceCorrectAnswers = ref({})
    const judgeCorrectAnswers = ref({})

    const loadTestResult = async (testId) => {
      console.log("begin loading test : " + testId)
      try {
        loading.value = true
        error.value = null
        const response = await getTestRecordDetails(testId)

        // 假设 response.testResult 已经是对象，不需要再 parse
        const resultData = response.testResult || {}

        if (Object.keys(resultData).length > 0) {
          examPaper.value = resultData.examPaper || {}
          total.value = resultData.total || 0
          correctNumber.value = resultData.correctNumber || 0
          testScore.value = resultData.testScore || 0
          singleChoiceUserAnswers.value = resultData.singleChoiceUserAnswers || {}
          judgeUserAnswers.value = resultData.judgeUserAnswers || {}
          singleChoiceCorrectAnswers.value = resultData.singleChoiceCorrectAnswers || {}
          judgeCorrectAnswers.value = resultData.judgeCorrectAnswers || {}
        } else {
          props.onClose
        }

        console.log("开始加载考试细节", JSON.stringify(resultData, null, 2))
      } catch (err) {
        error.value = err.message || '加载考试结果失败'
        console.error('加载考试结果失败:', err)
      } finally {
        loading.value = false
      }
    }

    const goBack = () => {
      props.onClose(); // 这里必须调用函数
    };


    watch(() => props.testId, (newTestId) => {
      if (newTestId) {
        loadTestResult(newTestId)
      }
    }, { immediate: true })

    // 返回所有模板需要使用的变量和方法
    return {
      loading,
      error,
      examPaper,
      total,
      correctNumber,
      testScore,
      singleChoiceUserAnswers,
      judgeUserAnswers,
      singleChoiceCorrectAnswers,
      judgeCorrectAnswers,
      goBack
    }
  }
}
</script>



<style scoped>
.result-container {
  width: 90%;
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.header {
  text-align: center;
  color: #003366;
  margin-bottom: 30px;
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
  background-color: #4CAF50;
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