<template>
  <div class="training-question-container">
    <div class="header-section">
      <h1 class="training-header">错题同类型再训练</h1>
      <div class="action-buttons">
        <el-button type="default" @click="$emit('backToWrongQuestion')">
          返回错题
        </el-button>
        <el-button type="primary" @click="submitAnswer" :disabled="!canSubmit">
          提交答案
        </el-button>
      </div>
    </div>

    <div v-if="questionData" class="question-content">
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
        <div class="answer-area">
          <h4>答题区域：</h4>
          <textarea 
            v-model="subjectiveAnswer"
            class="answer-textarea"
            placeholder="请在此处输入你的作文..."
            rows="12"
          ></textarea>
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
        <div class="answer-area">
          <h4>答题区域：</h4>
          <textarea 
            v-model="subjectiveAnswer"
            class="answer-textarea"
            placeholder="请在此处输入你的翻译..."
            rows="8"
          ></textarea>
        </div>
      </div>

      <!-- 听力题型 -->
      <div v-else-if="isListeningType" class="result-card structured">
        <h2>Part II · Listening Comprehension</h2>
        <div v-if="questionData.listening_content" class="passage-block">
          <h3>听力内容</h3>
          <pre class="passage-text">{{ questionData.listening_content }}</pre>
        </div>
        <div v-if="questionData.questions" class="questions-block">
          <div
            v-for="(q, qIdx) in questionData.questions"
            :key="`q-${qIdx}`"
            class="question-item"
          >
            <div class="question-header">
              <span class="question-no">题号 {{ q.question_number || (qIdx + 1) }}</span>
              <p class="question-text" v-if="q.question_content">{{ q.question_content }}</p>
            </div>
            <div class="options-list">
              <label
                v-for="opt in (q.options || [])"
                :key="opt.option_mark"
                class="radio-option"
              >
                <input
                  type="radio"
                  :name="`q-${qIdx}`"
                  :value="opt.option_mark"
                  v-model="objectiveAnswers[q.question_number || (qIdx + 1)]"
                />
                <span class="option-label">
                  <strong>{{ opt.option_mark }}.</strong> {{ opt.option_content }}
                </span>
              </label>
            </div>
          </div>
        </div>
      </div>

      <!-- Section A (选词填空) -->
      <div v-else-if="questionType === 'BlankedCloze'" class="result-card structured">
        <h2>Part III · Section A (选词填空)</h2>
        <div v-if="questionData.word_bank" class="word-bank">
          <h4>词库：</h4>
          <div class="word-list">
            <span v-for="word in questionData.word_bank" :key="word" class="word-item">
              {{ word }}
            </span>
          </div>
        </div>
        <div v-if="questionData.passage" class="passage-block">
          <h3>文章</h3>
          <pre class="passage-text">{{ questionData.passage }}</pre>
        </div>
        <div v-if="questionData.questions" class="questions-block">
          <div
            v-for="(q, qIdx) in questionData.questions"
            :key="`q-${qIdx}`"
            class="question-item"
          >
            <div class="question-header">
              <span class="question-no">{{ q.question_number }}.</span>
              <p class="question-text">请选择合适的词填入空格</p>
            </div>
            <input
              type="text"
              v-model="objectiveAnswers[q.question_number]"
              class="fill-input"
              placeholder="输入词语"
            />
          </div>
        </div>
      </div>

      <!-- Section B (段落匹配) -->
      <div v-else-if="questionType === 'Matching'" class="result-card structured">
        <h2>Part III · Section B (段落匹配)</h2>
        <div v-if="questionData.passage" class="passage-block">
          <h3>文章</h3>
          <pre class="passage-text">{{ questionData.passage }}</pre>
        </div>
        <div v-if="questionData.statements" class="statements-block">
          <h4>请为每个陈述选择对应的段落标号：</h4>
          <div
            v-for="(stmt, sIdx) in questionData.statements"
            :key="`stmt-${sIdx}`"
            class="statement-item"
          >
            <div class="statement-header">
              <span class="statement-no">{{ stmt.statement_number }}.</span>
              <p class="statement-text">{{ stmt.statement_content }}</p>
            </div>
            <input
              type="text"
              v-model="objectiveAnswers[stmt.statement_number]"
              class="paragraph-input"
              placeholder="输入段落标号(如A, B, C...)"
            />
          </div>
        </div>
      </div>

      <!-- Section C (阅读理解) -->
      <div v-else-if="questionType === 'ReadingComprehension'" class="result-card structured">
        <h2>Part III · Section C (阅读理解)</h2>
        <div v-if="questionData.passage" class="passage-block">
          <h3>文章</h3>
          <pre class="passage-text">{{ questionData.passage }}</pre>
        </div>
        <div v-if="questionData.questions" class="questions-block">
          <div
            v-for="(q, qIdx) in questionData.questions"
            :key="`q-${qIdx}`"
            class="question-item"
          >
            <div class="question-header">
              <span class="question-no">题号 {{ q.question_number || (qIdx + 1) }}</span>
              <p class="question-text" v-if="q.question_content">{{ q.question_content }}</p>
            </div>
            <div class="options-list">
              <label
                v-for="opt in (q.options || [])"
                :key="opt.option_mark"
                class="radio-option"
              >
                <input
                  type="radio"
                  :name="`q-${qIdx}`"
                  :value="opt.option_mark"
                  v-model="objectiveAnswers[q.question_number || (qIdx + 1)]"
                />
                <span class="option-label">
                  <strong>{{ opt.option_mark }}.</strong> {{ opt.option_content }}
                </span>
              </label>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, computed } from 'vue';
import { ElMessage } from 'element-plus';

export default {
  name: 'TrainingQuestionDisplay',
  props: {
    questionData: {
      type: Object,
      required: true
    }
  },
  emits: ['submitTrainingAnswer', 'backToWrongQuestion'],
  setup(props, { emit }) {
    const subjectiveAnswer = ref('');
    const objectiveAnswers = ref({});

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

    // 是否是听力题型
    const isListeningType = computed(() => {
      const type = questionType.value;
      return type === 'NewsReport' || type === 'Conversation' || type === 'ListeningPassage';
    });

    // 是否可以提交
    const canSubmit = computed(() => {
      const type = questionType.value;
      
      if (type === 'Writing' || type === 'Translation') {
        return subjectiveAnswer.value.trim().length > 0;
      } else {
        return Object.keys(objectiveAnswers.value).length > 0;
      }
    });

    // 提交答案
    const submitAnswer = () => {
      if (!canSubmit.value) {
        ElMessage.warning('请先完成答题');
        return;
      }

      const type = questionType.value;
      
      if (type === 'Writing' || type === 'Translation') {
        emit('submitTrainingAnswer', {
          subjectiveAnswer: subjectiveAnswer.value
        });
      } else {
        emit('submitTrainingAnswer', {
          objectiveAnswers: objectiveAnswers.value
        });
      }
    };

    return {
      subjectiveAnswer,
      objectiveAnswers,
      questionType,
      isListeningType,
      canSubmit,
      submitAnswer
    };
  }
};
</script>

<style scoped>
.training-question-container {
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

.question-content {
  margin-top: 20px;
}

.result-card {
  background: #f9fafb;
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 20px;
}

.result-card h2 {
  color: #2c3e50;
  font-size: 24px;
  font-weight: 700;
  margin: 0 0 20px 0;
  padding-bottom: 12px;
  border-bottom: 3px solid #667eea;
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

.answer-area {
  margin-top: 20px;
}

.answer-area h4 {
  color: #606266;
  font-size: 16px;
  font-weight: 600;
  margin: 0 0 12px 0;
}

.answer-textarea {
  width: 100%;
  padding: 12px;
  border: 2px solid #dcdfe6;
  border-radius: 8px;
  font-size: 15px;
  line-height: 1.6;
  resize: vertical;
  transition: border-color 0.3s;
}

.answer-textarea:focus {
  outline: none;
  border-color: #667eea;
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

.question-item {
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

.radio-option {
  display: flex;
  align-items: flex-start;
  padding: 12px;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.3s;
}

.radio-option:hover {
  background: #f5f7fa;
  border-color: #409eff;
}

.radio-option input[type="radio"] {
  margin-top: 4px;
  margin-right: 12px;
  cursor: pointer;
}

.option-label {
  flex: 1;
  color: #303133;
  font-size: 14px;
  line-height: 1.6;
}

.fill-input,
.paragraph-input {
  width: 100%;
  padding: 10px;
  border: 2px solid #dcdfe6;
  border-radius: 6px;
  font-size: 15px;
  transition: border-color 0.3s;
}

.fill-input:focus,
.paragraph-input:focus {
  outline: none;
  border-color: #667eea;
}

.statements-block {
  margin-top: 20px;
}

.statements-block h4 {
  color: #606266;
  font-size: 16px;
  font-weight: 600;
  margin: 0 0 16px 0;
}

.statement-item {
  margin-bottom: 16px;
  padding: 16px;
  background: white;
  border-radius: 8px;
  border: 1px solid #e4e7ed;
}

.statement-header {
  margin-bottom: 12px;
}

.statement-no {
  display: inline-block;
  padding: 4px 8px;
  background: #67c23a;
  color: white;
  border-radius: 4px;
  font-size: 14px;
  font-weight: 600;
  margin-right: 8px;
}

.statement-text {
  display: inline;
  color: #303133;
  font-size: 15px;
  line-height: 1.6;
  margin: 0;
}
</style>
