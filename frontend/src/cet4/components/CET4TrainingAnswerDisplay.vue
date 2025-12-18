<template>
  <div class="training-answer-container">
    <div class="header-section">
      <h1 class="training-header">训练答案与解析</h1>
      <div class="action-buttons">
        <el-button type="default" @click="$emit('backToWrongQuestion')">
          返回错题
        </el-button>
        <el-button type="primary" @click="$emit('retryTraining')">
          再练一次
        </el-button>
      </div>
    </div>

    <div v-if="questionData" class="answer-content">
      <!-- Writing -->
      <div v-if="questionType === 'Writing'" class="result-card structured">
        <h2>Part I · Writing</h2>
        <div v-if="questionData.topic" class="topic-block">
          <h3>写作主题</h3>
          <p class="topic-text">{{ questionData.topic }}</p>
        </div>
        <div v-if="questionData.passage" class="passage-block">
          <h3>题目要求</h3>
          <pre class="passage-text">{{ questionData.passage }}</pre>
        </div>
        
        <!-- 你的作文 -->
        <div class="answer-display-section">
          <h4>你的作文：</h4>
          <pre class="user-answer-text">{{ userTrainingAnswer }}</pre>
          
          <!-- AI评分 -->
          <div v-if="aiScore !== undefined" class="ai-grading-section">
            <div class="ai-score-header">
              <h4>🤖 AI评分结果</h4>
              <span class="ai-score-value">{{ aiScore }} 分</span>
            </div>
            <div v-if="aiFeedback" class="ai-feedback">
              <h5>📝 评价反馈:</h5>
              <p>{{ aiFeedback }}</p>
            </div>
            <div v-if="aiReasoning" class="ai-reasoning">
              <h5>📊 详细分析:</h5>
              <p>{{ aiReasoning }}</p>
            </div>
          </div>
        </div>

        <!-- 参考答案 -->
        <div v-if="questionData.reference_answer" class="reference-section">
          <h4>📚 参考答案：</h4>
          <pre class="reference-text">{{ questionData.reference_answer }}</pre>
        </div>
      </div>

      <!-- Translation -->
      <div v-else-if="questionType === 'Translation'" class="result-card structured">
        <h2>Part IV · Translation</h2>
        <div v-if="questionData.topic" class="topic-block">
          <h3>翻译主题</h3>
          <p class="topic-text">{{ questionData.topic }}</p>
        </div>
        <div v-if="questionData.passage" class="passage-block">
          <h3>翻译内容</h3>
          <pre class="passage-text">{{ questionData.passage }}</pre>
        </div>
        
        <!-- 你的翻译 -->
        <div class="answer-display-section">
          <h4>你的翻译：</h4>
          <pre class="user-answer-text">{{ userTrainingAnswer }}</pre>
          
          <!-- AI评分 -->
          <div v-if="aiScore !== undefined" class="ai-grading-section">
            <div class="ai-score-header">
              <h4>🤖 AI评分结果</h4>
              <span class="ai-score-value">{{ aiScore }} 分</span>
            </div>
            <div v-if="aiFeedback" class="ai-feedback">
              <h5>📝 评价反馈:</h5>
              <p>{{ aiFeedback }}</p>
            </div>
            <div v-if="aiReasoning" class="ai-reasoning">
              <h5>📊 详细分析:</h5>
              <p>{{ aiReasoning }}</p>
            </div>
          </div>
        </div>

        <!-- 参考答案 -->
        <div v-if="questionData.reference_translation" class="reference-section">
          <h4>📚 参考译文：</h4>
          <pre class="reference-text">{{ questionData.reference_translation }}</pre>
        </div>
      </div>

      <!-- 客观题答案对比 -->
      <div v-else class="result-card structured">
        <h2>{{ getSectionTitle() }}</h2>
        
        <div v-if="questionData.passage" class="passage-block">
          <h3>文章/听力内容</h3>
          <pre class="passage-text">{{ questionData.passage || questionData.listening_content }}</pre>
        </div>

        <div v-if="questionData.word_bank" class="word-bank">
          <h4>词库：</h4>
          <div class="word-list">
            <span v-for="word in questionData.word_bank" :key="word" class="word-item">
              {{ word }}
            </span>
          </div>
        </div>

        <!-- 题目答案对比 -->
        <div class="questions-block">
          <div
            v-for="(q, qIdx) in getQuestions()"
            :key="`q-${qIdx}`"
            class="question-answer-item"
          >
            <div class="question-header">
              <span class="question-no">{{ getQuestionNumber(q, qIdx) }}</span>
              <p class="question-text" v-if="q.question_content">{{ q.question_content }}</p>
              <p class="question-text" v-else-if="q.statement_content">{{ q.statement_content }}</p>
            </div>

            <!-- 选项显示（如果有） -->
            <div v-if="q.options" class="options-list">
              <div
                v-for="opt in q.options"
                :key="opt.option_mark"
                class="option-item"
                :class="getOptionClass(q, opt)"
              >
                <span class="option-label">
                  <strong>{{ opt.option_mark }}.</strong> {{ opt.option_content }}
                </span>
                <span v-if="isCorrectOption(q, opt)" class="correct-mark">✓ 正确答案</span>
                <span v-else-if="isUserSelectedOption(q, opt)" class="wrong-mark">✗ 你的答案</span>
              </div>
            </div>

            <!-- 填空/匹配答案显示 -->
            <div v-else class="answer-comparison">
              <div class="answer-row">
                <span class="label">你的答案：</span>
                <span 
                  class="user-answer"
                  :class="{ correct: isAnswerCorrect(q, qIdx), wrong: !isAnswerCorrect(q, qIdx) }"
                >
                  {{ getUserAnswer(q, qIdx) || '未作答' }}
                  <span v-if="isAnswerCorrect(q, qIdx)" class="correct-mark">✓</span>
                  <span v-else class="wrong-mark">✗</span>
                </span>
              </div>
              <div class="answer-row">
                <span class="label">正确答案：</span>
                <span class="correct-answer">{{ getCorrectAnswer(q) }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 统计信息 -->
        <div class="statistics-section">
          <h4>答题统计</h4>
          <div class="stats-grid">
            <div class="stat-item">
              <span class="stat-label">总题数：</span>
              <span class="stat-value">{{ totalQuestions }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">正确数：</span>
              <span class="stat-value correct">{{ correctCount }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">错误数：</span>
              <span class="stat-value wrong">{{ wrongCount }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">正确率：</span>
              <span class="stat-value">{{ accuracy }}%</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { computed } from 'vue';

export default {
  name: 'TrainingAnswerDisplay',
  props: {
    questionData: {
      type: Object,
      required: true
    },
    userTrainingAnswers: {
      type: Object,
      default: () => ({})
    },
    userTrainingAnswer: {
      type: String,
      default: ''
    },
    aiScore: {
      type: Number,
      default: undefined
    },
    aiFeedback: {
      type: String,
      default: ''
    },
    aiReasoning: {
      type: String,
      default: ''
    }
  },
  emits: ['retryTraining', 'backToWrongQuestion'],
  setup(props) {
    // 获取题型
    const questionType = computed(() => {
      if (!props.questionData) return '';
      
      if (props.questionData.metadata && props.questionData.metadata.question_type) {
        return props.questionData.metadata.question_type;
      }
      
      if (props.questionData.question_type) {
        return props.questionData.question_type;
      }
      
      if (props.questionData.part_id) {
        const partId = props.questionData.part_id;
        if (partId === 'Section A') return 'BlankedCloze';
        if (partId === 'Section B') return 'Matching';
        if (partId === 'Section C') return 'ReadingComprehension';
      }
      
      if (props.questionData.unit_type) {
        const unitType = props.questionData.unit_type.toLowerCase();
        if (unitType.includes('news')) return 'NewsReport';
        if (unitType.includes('conversation')) return 'Conversation';
        if (unitType.includes('passage')) return 'ListeningPassage';
      }
      
      return '';
    });

    // 获取section标题
    const getSectionTitle = () => {
      const type = questionType.value;
      if (type === 'NewsReport' || type === 'Conversation' || type === 'ListeningPassage') {
        return 'Part II · Listening Comprehension';
      } else if (type === 'BlankedCloze') {
        return 'Part III · Section A (选词填空)';
      } else if (type === 'Matching') {
        return 'Part III · Section B (段落匹配)';
      } else if (type === 'ReadingComprehension') {
        return 'Part III · Section C (阅读理解)';
      }
      return '答案解析';
    };

    // 获取题目列表
    const getQuestions = () => {
      if (props.questionData.questions) {
        return props.questionData.questions;
      } else if (props.questionData.statements) {
        return props.questionData.statements;
      }
      return [];
    };

    // 获取题号
    const getQuestionNumber = (q, idx) => {
      return q.question_number || q.statement_number || (idx + 1);
    };

    // 获取用户答案
    const getUserAnswer = (q, idx) => {
      const qNum = getQuestionNumber(q, idx);
      return props.userTrainingAnswers[qNum] || '';
    };

    // 获取正确答案
    const getCorrectAnswer = (q) => {
      return q.correct_answer || q.answer || '';
    };

    // 判断答案是否正确
    const isAnswerCorrect = (q, idx) => {
      const userAns = getUserAnswer(q, idx);
      const correctAns = getCorrectAnswer(q);
      return userAns && correctAns && 
             userAns.toString().toLowerCase().trim() === correctAns.toString().toLowerCase().trim();
    };

    // 判断是否是正确选项
    const isCorrectOption = (q, opt) => {
      const correctAns = getCorrectAnswer(q);
      return opt.option_mark === correctAns;
    };

    // 判断是否是用户选择的选项
    const isUserSelectedOption = (q, opt) => {
      const qNum = q.question_number;
      const userAns = props.userTrainingAnswers[qNum];
      return userAns === opt.option_mark;
    };

    // 获取选项样式类
    const getOptionClass = (q, opt) => {
      if (isCorrectOption(q, opt)) {
        return 'correct-option';
      } else if (isUserSelectedOption(q, opt)) {
        return 'wrong-option';
      }
      return '';
    };

    // 统计信息
    const totalQuestions = computed(() => {
      return getQuestions().length;
    });

    const correctCount = computed(() => {
      const questions = getQuestions();
      return questions.filter((q, idx) => isAnswerCorrect(q, idx)).length;
    });

    const wrongCount = computed(() => {
      return totalQuestions.value - correctCount.value;
    });

    const accuracy = computed(() => {
      if (totalQuestions.value === 0) return 0;
      return Math.round((correctCount.value / totalQuestions.value) * 100);
    });

    return {
      questionType,
      getSectionTitle,
      getQuestions,
      getQuestionNumber,
      getUserAnswer,
      getCorrectAnswer,
      isAnswerCorrect,
      isCorrectOption,
      isUserSelectedOption,
      getOptionClass,
      totalQuestions,
      correctCount,
      wrongCount,
      accuracy
    };
  }
};
</script>

<style scoped>
.training-answer-container {
  background: white;
  border-radius: 12px;
  padding: 30px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.header-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
  padding-bottom: 20px;
  border-bottom: 2px solid #e4e7ed;
}

.training-header {
  margin: 0;
  color: #2c3e50;
  font-size: 28px;
  font-weight: 700;
}

.action-buttons {
  display: flex;
  gap: 12px;
}

.answer-content {
  margin-top: 20px;
}

.result-card {
  background: #f9fafb;
  border-radius: 12px;
  padding: 24px;
}

.result-card h2 {
  color: #2c3e50;
  font-size: 24px;
  font-weight: 700;
  margin: 0 0 20px 0;
  padding-bottom: 12px;
  border-bottom: 3px solid #67c23a;
}

.topic-block,
.passage-block {
  margin-bottom: 20px;
}

.topic-block h3,
.passage-block h3 {
  color: #606266;
  font-size: 18px;
  font-weight: 600;
  margin: 0 0 12px 0;
}

.topic-text {
  color: #303133;
  font-size: 16px;
  line-height: 1.6;
  margin: 0;
}

.passage-text {
  background: white;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  padding: 16px;
  color: #303133;
  font-size: 15px;
  line-height: 1.8;
  white-space: pre-wrap;
  word-wrap: break-word;
  margin: 0;
}

.answer-display-section {
  margin-top: 20px;
  padding: 16px;
  background: white;
  border-radius: 8px;
}

.answer-display-section h4 {
  color: #606266;
  font-size: 16px;
  font-weight: 600;
  margin: 0 0 12px 0;
}

.user-answer-text {
  background: #f5f7fa;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 12px;
  color: #303133;
  font-size: 15px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-wrap: break-word;
  margin: 0 0 16px 0;
}

.ai-grading-section {
  margin-top: 20px;
  padding: 16px;
  background: linear-gradient(135deg, #667eea15 0%, #764ba215 100%);
  border-radius: 8px;
  border: 2px solid #667eea;
}

.ai-score-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.ai-score-header h4 {
  margin: 0;
  color: #667eea;
  font-size: 18px;
  font-weight: 600;
}

.ai-score-value {
  font-size: 24px;
  font-weight: 700;
  color: #67c23a;
}

.ai-feedback,
.ai-reasoning {
  margin-bottom: 12px;
}

.ai-feedback h5,
.ai-reasoning h5 {
  margin: 0 0 8px 0;
  color: #606266;
  font-size: 15px;
  font-weight: 600;
}

.ai-feedback p,
.ai-reasoning p {
  margin: 0;
  color: #303133;
  font-size: 14px;
  line-height: 1.6;
}

.reference-section {
  margin-top: 20px;
  padding: 16px;
  background: #ecf5ff;
  border-radius: 8px;
  border: 1px solid #b3d8ff;
}

.reference-section h4 {
  margin: 0 0 12px 0;
  color: #409eff;
  font-size: 16px;
  font-weight: 600;
}

.reference-text {
  background: white;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  padding: 12px;
  color: #303133;
  font-size: 15px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-wrap: break-word;
  margin: 0;
}

.word-bank {
  margin-bottom: 20px;
  padding: 16px;
  background: white;
  border-radius: 8px;
  border: 1px solid #e4e7ed;
}

.word-bank h4 {
  color: #606266;
  font-size: 16px;
  font-weight: 600;
  margin: 0 0 12px 0;
}

.word-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.word-item {
  padding: 6px 12px;
  background: #ecf5ff;
  color: #409eff;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
}

.questions-block {
  margin-top: 20px;
}

.question-answer-item {
  margin-bottom: 24px;
  padding: 16px;
  background: white;
  border-radius: 8px;
  border: 1px solid #e4e7ed;
}

.question-header {
  margin-bottom: 12px;
}

.question-no {
  display: inline-block;
  padding: 4px 8px;
  background: #409eff;
  color: white;
  border-radius: 4px;
  font-size: 14px;
  font-weight: 600;
  margin-right: 8px;
}

.question-text {
  display: inline;
  color: #303133;
  font-size: 15px;
  line-height: 1.6;
  margin: 0;
}

.options-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.option-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
}

.option-item.correct-option {
  background: #f0f9ff;
  border-color: #67c23a;
  border-width: 2px;
}

.option-item.wrong-option {
  background: #fef0f0;
  border-color: #f56c6c;
  border-width: 2px;
}

.option-label {
  flex: 1;
  color: #303133;
  font-size: 14px;
  line-height: 1.6;
}

.correct-mark {
  color: #67c23a;
  font-weight: 600;
  font-size: 14px;
}

.wrong-mark {
  color: #f56c6c;
  font-weight: 600;
  font-size: 14px;
}

.answer-comparison {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.answer-row {
  display: flex;
  align-items: center;
  padding: 10px;
  background: #f5f7fa;
  border-radius: 6px;
}

.answer-row .label {
  min-width: 100px;
  color: #606266;
  font-size: 14px;
  font-weight: 600;
}

.answer-row .user-answer,
.answer-row .correct-answer {
  flex: 1;
  font-size: 15px;
  font-weight: 500;
}

.user-answer.correct {
  color: #67c23a;
}

.user-answer.wrong {
  color: #f56c6c;
}

.correct-answer {
  color: #67c23a;
}

.statistics-section {
  margin-top: 30px;
  padding: 20px;
  background: linear-gradient(135deg, #667eea15 0%, #764ba215 100%);
  border-radius: 8px;
  border: 2px solid #667eea;
}

.statistics-section h4 {
  margin: 0 0 16px 0;
  color: #667eea;
  font-size: 18px;
  font-weight: 600;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 16px;
}

.stat-item {
  padding: 12px;
  background: white;
  border-radius: 6px;
  text-align: center;
}

.stat-label {
  display: block;
  color: #606266;
  font-size: 14px;
  margin-bottom: 8px;
}

.stat-value {
  display: block;
  font-size: 24px;
  font-weight: 700;
  color: #409eff;
}

.stat-value.correct {
  color: #67c23a;
}

.stat-value.wrong {
  color: #f56c6c;
}
</style>
