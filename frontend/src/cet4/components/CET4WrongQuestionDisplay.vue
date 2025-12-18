<template>
  <div class="wrong-question-display">
    <div class="header-container">
      <h1 class="header">错题详情</h1>
      <div class="action-buttons">
        <el-button 
          v-if="rawOutput && showRawOutputButton" 
          type="info" 
          @click="$emit('showRawOutput')"
          class="view-output-button"
          icon="View"
        >
          查看原始输出
        </el-button>
        <el-button 
          v-if="questionData && !loading && showTutoringButton" 
          type="success" 
          @click="$emit('startTutoring')"
          class="tutoring-button"
          icon="ChatDotRound"
        >
          💬 错题答疑
        </el-button>
        <el-button 
          v-if="questionData && !loading && showRetryButton" 
          type="primary" 
          @click="$emit('retryQuestion')"
          :loading="generatingExam"
          class="retry-button"
        >
          {{ generatingExam ? '生成中...' : '📝 错题同类型再训练' }}
        </el-button>
      </div>
    </div>

    <div v-if="loading" class="loading-tip">加载中...</div>
    <div v-else-if="errorMessage" class="error-tip">{{ errorMessage }}</div>

    <template v-else-if="questionData">
      <!-- Part I · Writing -->
      <div v-if="questionType === 'Writing'" class="result-card structured" style="margin-top: 16px">
        <h2>Part I · Writing</h2>
        <div v-if="questionData.topic" class="topic-block">
          <h3>写作主题</h3>
          <p class="topic-text">{{ questionData.topic }}</p>
        </div>
        <div v-if="questionData.passage" class="passage-block">
          <h3>题目要求</h3>
          <pre class="passage-text">{{ questionData.passage }}</pre>
        </div>
        <div class="answer-display-section">
          <h4>你的作文：</h4>
          <pre class="user-answer-text">{{ userAnswer || '未作答' }}</pre>
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
      </div>

      <!-- Part IV · Translation -->
      <div v-else-if="questionType === 'Translation'" class="result-card structured" style="margin-top: 16px">
        <h2>Part IV · Translation</h2>
        <div v-if="questionData.topic" class="topic-block">
          <h3>翻译主题</h3>
          <p class="topic-text">{{ questionData.topic }}</p>
        </div>
        <div v-if="questionData.passage" class="passage-block">
          <h3>翻译内容</h3>
          <pre class="passage-text">{{ questionData.passage }}</pre>
        </div>
        <div class="answer-display-section">
          <h4>你的翻译：</h4>
          <pre class="user-answer-text">{{ userAnswer || '未作答' }}</pre>
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
      </div>

      <!-- Listening Comprehension -->
      <div v-if="isListeningType" class="result-card structured exam-section">
        <div class="section-header">
          <h2>Part II · Listening Comprehension</h2>
          <p class="section-meta" v-if="questionData.unit_type">{{ questionData.unit_type }}</p>
        </div>

        <div class="passage-section">
          <div v-if="questionData.listening_content" class="passage-content-block">
            <pre class="passage-text">{{ questionData.listening_content }}</pre>
          </div>

          <div class="questions-block" v-if="listeningQuestions && listeningQuestions.length">
            <div
              v-for="(q, idx) in listeningQuestions"
              :key="`q-${idx}`"
              class="question-item"
              :class="getQuestionClass(q)"
            >
              <div class="question-header">
                <span class="question-no">{{ q.question_number || (idx + 1) }}</span>
                <p class="question-text">{{ q.question_content || q['题干'] }}</p>
                <span class="result-indicator" :class="getResultClass(q)">
                  {{ getResultText(q) }}
                </span>
              </div>
              <div class="options-list">
                <label
                  v-for="opt in (q.options || optionsOf(q))"
                  :key="opt.option_mark || opt.mark"
                  class="radio-option"
                  :class="getOptionClass(q, opt.option_mark || opt.mark)"
                >
                  <input
                    type="radio"
                    :value="opt.option_mark || opt.mark"
                    :checked="isCorrectOption(q, opt.option_mark || opt.mark)"
                    disabled
                  />
                  <span class="option-label">
                    <strong>{{ opt.option_mark || opt.mark }}.</strong> {{ opt.option_content || opt.text }}
                  </span>
                </label>
              </div>
              <div class="answer-display">
                <span class="answer-label">你的答案:</span>
                <span class="answer-value" :class="getUserAnswerClass(q)">
                  {{ getUserAnswer(q.question_number) || '未作答' }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Section A - 选词填空 -->
      <div v-else-if="isSectionA && sectionAData" class="result-card structured exam-section">
        <div class="section-header">
          <h2>Section A（选词填空）</h2>
        </div>

        <div class="section-block passage-block" v-if="sectionAData.passage">
          <h3>题干</h3>
          <pre class="passage-text">{{ sectionAData.passage }}</pre>
        </div>

        <div class="section-block question-block" v-if="sectionAData.blank_numbers">
          <h3>题目</h3>
          <div
            v-for="number in sectionAData.blank_numbers"
            :key="number"
            class="question-row"
            :class="getBlankQuestionClass(number)"
          >
            <div class="question-info">
              <span class="question-no">{{ number }}</span>
              <span class="result-indicator" :class="getBlankResultClass(number)">
                {{ getBlankResultText(number) }}
              </span>
            </div>
            <select class="answer-select" disabled>
              <option value="" selected>{{ getCorrectAnswer(number) }}</option>
            </select>
            <div class="answer-display inline">
              <span class="answer-label">你的答案:</span>
              <span class="answer-value" :class="getBlankAnswerClass(number)">
                {{ getUserAnswer(number) || '未作答' }}
              </span>
            </div>
          </div>
        </div>

        <div class="section-block options-block" v-if="sectionAData.options || sectionAData.word_options">
          <h3>备选词</h3>
          <div class="option-grid">
            <div
              v-for="option in (sectionAData.options || sectionAData.word_options)"
              :key="option.letter"
              class="option-chip"
            >
              <span class="option-mark">{{ option.letter }}</span>
              <span class="option-text">{{ option.word }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Section B - 段落匹配 -->
      <div v-else-if="isSectionB && sectionBData" class="result-card structured exam-section">
        <div class="section-header">
          <h2>Section B（段落匹配）</h2>
          <p class="section-meta" v-if="sectionBData.statements">共 {{ sectionBData.statements.length }} 题</p>
        </div>

        <div class="section-block article-block" v-if="sectionBData.article">
          <h3>文章</h3>
          <div class="paragraph-list">
            <div
              v-for="paragraph in sectionBData.article"
              :key="paragraph.paragraph_mark"
              class="paragraph-item"
            >
              <span class="paragraph-mark">{{ paragraph.paragraph_mark }}</span>
              <p class="paragraph-content">{{ paragraph.paragraph_content }}</p>
            </div>
          </div>
        </div>

        <div class="section-block statements-block" v-if="sectionBData.statements">
          <h3>题目（每个陈述句对应的段落）</h3>
          <div
            v-for="statement in sectionBData.statements"
            :key="statement.question_number"
            class="statement-row"
            :class="getQuestionClass(statement.question_number)"
          >
            <div class="statement-header">
              <span class="question-no">{{ statement.question_number }}</span>
              <p class="statement-text">{{ statement.statement_content || statement.statement }}</p>
              <span class="result-indicator" :class="getResultClass(statement.question_number)">
                {{ getResultText(statement.question_number) }}
              </span>
            </div>
            <select class="paragraph-select" disabled>
              <option value="" selected>{{ getCorrectAnswer(statement.question_number) }}</option>
            </select>
            <div class="answer-display inline">
              <span class="answer-label">你的答案:</span>
              <span class="answer-value" :class="getUserAnswerClass(statement.question_number)">
                {{ getUserAnswer(statement.question_number) || '未作答' }}
              </span>
            </div>
          </div>
        </div>
      </div>

      <!-- Section C - 仔细阅读 -->
      <div v-else-if="questionData.part_id === 'Section C' || questionType === 'ReadingPassage'" class="result-card structured exam-section">
        <div class="section-header">
          <h2>Section C（仔细阅读）</h2>
          <p class="section-meta" v-if="questionData.passage_mark">{{ questionData.passage_mark }}</p>
        </div>

        <div class="section-block passage-block">
          <h3>文章</h3>
          <pre class="passage-text">{{ questionData.passage_content || questionData.passage }}</pre>
        </div>

        <div class="section-block question-block" v-if="questionData.questions">
          <h3>题目</h3>
          <div
            v-for="(q, idx) in questionData.questions"
            :key="`q-${idx}`"
            class="question-item"
            :class="getQuestionClass(q)"
          >
            <div class="question-header">
              <span class="question-no">{{ q.question_number }}</span>
              <p class="question-text">{{ q.question_content }}</p>
              <span class="result-indicator" :class="getResultClass(q)">
                {{ getResultText(q) }}
              </span>
            </div>
            <div class="options-list">
              <label
                v-for="opt in q.options"
                :key="opt.option_mark"
                class="radio-option"
                :class="getOptionClass(q, opt.option_mark)"
              >
                <input
                  type="radio"
                  :checked="opt.option_mark === q.correct_answer"
                  disabled
                />
                <span class="option-label">
                  <strong>{{ opt.option_mark }}.</strong> {{ opt.option_content }}
                </span>
              </label>
            </div>
            <div class="answer-display">
              <span class="answer-label">你的答案:</span>
              <span class="answer-value" :class="getUserAnswerClass(q)">
                {{ getUserAnswer(q.question_number) || '未作答' }}
              </span>
            </div>
          </div>
        </div>
      </div>

      <!-- 返回按钮 -->
      <div class="back-btn-container">
        <button @click="$emit('goBack')" class="back-btn">返回错题列表</button>
      </div>
    </template>
  </div>
</template>

<script>
import { computed } from 'vue';

export default {
  name: 'WrongQuestionDisplay',
  props: {
    loading: Boolean,
    errorMessage: String,
    questionData: Object,
    userAnswers: Object,
    correctAnswers: {
      type: Object,
      default: () => ({})
    },
    userAnswer: String,
    aiScore: Number,
    aiFeedback: String,
    aiReasoning: String,
    rawOutput: String,
    generatingExam: Boolean,
    examPaperEnSource: String,
    segmentIdSelf: String,
    showTutoringButton: {
      type: Boolean,
      default: true
    },
    showRetryButton: {
      type: Boolean,
      default: true
    },
    showRawOutputButton: {
      type: Boolean,
      default: true
    }
  },
  emits: ['showRawOutput', 'retryQuestion', 'startTutoring', 'goBack'],
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

      // Fallback: if has passage_content/passage + questions, treat as 阅读理解
      if ((props.questionData.passage_content || props.questionData.passage) && Array.isArray(props.questionData.questions)) {
        return 'ReadingPassage';
      }
      
      return '';
    });

    const isListeningType = computed(() => {
      const type = questionType.value;
      return type === 'NewsReport' || type === 'Conversation' || type === 'ListeningPassage';
    });

    const isSectionA = computed(() => {
      return questionType.value === 'BlankedCloze';
    });

    const isSectionB = computed(() => {
      return questionType.value === 'Matching';
    });

    const sectionBData = computed(() => {
      if (!isSectionB.value || !props.questionData) return null;
      
      if (props.questionData.reading_comprehension && props.questionData.reading_comprehension.section_b) {
        return props.questionData.reading_comprehension.section_b;
      }
      
      if (props.questionData.article && props.questionData.statements) {
        return props.questionData;
      }
      
      return null;
    });

    const sectionAData = computed(() => {
      if (!isSectionA.value || !props.questionData) return null;
      
      let data = null;
      
      if (props.questionData.reading_comprehension && props.questionData.reading_comprehension.section_a) {
        data = props.questionData.reading_comprehension.section_a;
      } else if (props.questionData.passage || props.questionData.answers) {
        data = props.questionData;
      }
      
      if (!data) return null;
      
      let blankNumbers = [];
      if (data.answers && Array.isArray(data.answers)) {
        blankNumbers = data.answers
          .map(item => item.question_number)
          .filter(num => num != null && num !== '')
          .sort((a, b) => {
            const numA = parseInt(a);
            const numB = parseInt(b);
            if (!isNaN(numA) && !isNaN(numB)) {
              return numA - numB;
            }
            return String(a).localeCompare(String(b));
          });
      }
      
      let options = [];
      const rawOptions = data.word_options || data.options || [];
      if (Array.isArray(rawOptions)) {
        options = rawOptions.map(opt => {
          if (typeof opt === 'object' && opt !== null) {
            return {
              letter: opt.letter || '',
              word: opt.word || ''
            };
          }
          return { letter: '', word: String(opt) };
        }).filter(opt => opt.word);
      }
      
      return {
        ...data,
        blank_numbers: blankNumbers,
        options: options
      };
    });

    const listeningQuestions = computed(() => {
      if (!isListeningType.value || !props.questionData) return [];
      
      if (props.questionData.question_and_options && Array.isArray(props.questionData.question_and_options)) {
        return props.questionData.question_and_options;
      }
      
      if (props.questionData.questions && Array.isArray(props.questionData.questions)) {
        return props.questionData.questions;
      }
      
      return [];
    });

    const getUserAnswer = (questionNumber) => {
      if (questionNumber === undefined || questionNumber === null) {
        return '';
      }
      const key = String(questionNumber);
      return props.userAnswers[key] || '';
    };

    const getCorrectAnswer = (questionNumber) => {
      if (questionNumber === undefined || questionNumber === null || questionNumber === '') {
        return '';
      }
      const key = String(questionNumber);

      if (props.correctAnswers && (props.correctAnswers[key] || props.correctAnswers[questionNumber])) {
        return props.correctAnswers[key] || props.correctAnswers[questionNumber] || '';
      }
      
      if (props.questionData && props.questionData.correct_answers) {
        const answer = props.questionData.correct_answers[key] || props.questionData.correct_answers[questionNumber];
        if (answer) return answer;
      }
      
      if (sectionAData.value && sectionAData.value.answers) {
        const answerObj = sectionAData.value.answers.find(a => 
          String(a.question_number) === key || a.question_number === questionNumber
        );
        if (answerObj) {
          return answerObj.answer || answerObj.correct_answer || '';
        }
      }
      
      if (sectionBData.value && sectionBData.value.statements) {
        const statement = sectionBData.value.statements.find(s => 
          String(s.question_number) === key || s.question_number === questionNumber
        );
        if (statement) {
          return statement.correct_paragraph || statement.correct_answer || '';
        }
      }
      
      return '';
    };

    const answerCandidateKeys = [
      'correct_answer',
      'correctAnswer',
      'answer',
      'standard_answer',
      'standardAnswer',
      'answer_key',
      'answerKey',
      'correct_option',
      'correctOption',
      'correct_option_mark',
      'correctOptionMark',
      'correct_option_letter',
      'correctOptionLetter',
      'correct_paragraph',
      'correctParagraph',
      'reference_answer',
      'referenceAnswer'
    ];

    const pickAnswerFromSource = (source) => {
      if (!source || typeof source !== 'object') return '';
      for (const key of answerCandidateKeys) {
        if (!Object.prototype.hasOwnProperty.call(source, key)) continue;
        const value = source[key];
        if (Array.isArray(value)) {
          const normalized = value
            .map(item => (item === undefined || item === null ? '' : String(item).trim()))
            .filter(Boolean);
          if (normalized.length) {
            return normalized[0];
          }
        } else if (value !== undefined && value !== null) {
          const strVal = String(value).trim();
          if (strVal) {
            return value;
          }
        }
      }
      return '';
    };

    const extractCorrectAnswerFromQuestion = (question) => {
      if (!question || typeof question !== 'object') return '';
      const direct = pickAnswerFromSource(question);
      if (direct) return direct;
      if (question.metadata && typeof question.metadata === 'object') {
        return pickAnswerFromSource(question.metadata);
      }
      return '';
    };

    const normalizeQuestionInput = (question) => {
      if (question && typeof question === 'object') {
        const fallbackNumber =
          question.question_number ||
          question.questionNumber ||
          question.question_no ||
          question.questionNo ||
          question.statement_number ||
          question.statementNumber ||
          question.number ||
          question.no ||
          '';
        if (fallbackNumber && fallbackNumber !== question.question_number) {
          return {
            ...question,
            question_number: fallbackNumber
          };
        }
        return question;
      }
      if (question === undefined || question === null || question === '') {
        return {
          question_number: '',
          correct_answer: ''
        };
      }
      return {
        question_number: question,
        correct_answer: getCorrectAnswer(question)
      };
    };

    const normalizeAnswerValue = (value) => {
      if (value === undefined || value === null) return '';
      return String(value).trim().replace(/\s+/g, ' ').toUpperCase();
    };

    const answersMatch = (left, right) => {
      const normalizedLeft = normalizeAnswerValue(left);
      const normalizedRight = normalizeAnswerValue(right);
      if (!normalizedLeft || !normalizedRight) return false;
      return normalizedLeft === normalizedRight;
    };

    const getQuestionCorrectAnswerValue = (question) => {
      const normalizedQuestion = normalizeQuestionInput(question);
      const extracted = extractCorrectAnswerFromQuestion(normalizedQuestion);
      const fallback = extracted || normalizedQuestion.correct_answer || '';
      if (Array.isArray(fallback)) {
        const [first] = fallback
          .map(item => (item === undefined || item === null ? '' : String(item).trim()))
          .filter(Boolean);
        return first || '';
      }
      const fallbackValue = fallback === undefined || fallback === null ? '' : String(fallback).trim();
      if (fallbackValue) {
        return fallbackValue;
      }
      return getCorrectAnswer(normalizedQuestion.question_number) || '';
    };

    const isCorrect = (question) => {
      const normalizedQuestion = normalizeQuestionInput(question);
      const userAns = getUserAnswer(normalizedQuestion.question_number);
      const correctAns = getQuestionCorrectAnswerValue(normalizedQuestion);
      if (!userAns || !correctAns) {
        return false;
      }
      return answersMatch(userAns, correctAns);
    };

    const isCorrectOption = (question, optionMark) => {
      const correctAns = getQuestionCorrectAnswerValue(question);
      if (!correctAns) return false;
      return answersMatch(optionMark, correctAns);
    };

    const getQuestionClass = (question) => {
      return isCorrect(question) ? 'correct-question' : 'wrong-question';
    };

    const getResultClass = (question) => {
      return isCorrect(question) ? 'result-correct' : 'result-wrong';
    };

    const getResultText = (question) => {
      return isCorrect(question) ? '✓ 正确' : '✗ 错误';
    };

    const getOptionClass = (question, optionMark) => {
      const questionInfo = normalizeQuestionInput(question);
      if (isCorrectOption(questionInfo, optionMark)) {
        return 'option-correct';
      }
      const userAns = getUserAnswer(questionInfo.question_number);
      if (userAns && answersMatch(optionMark, userAns)) {
        return 'option-wrong';
      }
      return '';
    };

    const getUserAnswerClass = (question) => {
      return isCorrect(question) ? 'answer-correct' : 'answer-wrong';
    };

    const isBlankAnswerCorrect = (number) => {
      const userAns = getUserAnswer(number);
      const correctAns = getCorrectAnswer(number);
      if (!userAns || !correctAns) return false;
      return answersMatch(userAns, correctAns);
    };

    const getBlankQuestionClass = (number) => {
      return isBlankAnswerCorrect(number) ? 'correct-question' : 'wrong-question';
    };

    const getBlankResultClass = (number) => {
      return isBlankAnswerCorrect(number) ? 'result-correct' : 'result-wrong';
    };

    const getBlankResultText = (number) => {
      return isBlankAnswerCorrect(number) ? '✓ 正确' : '✗ 错误';
    };

    const getBlankAnswerClass = (number) => {
      return isBlankAnswerCorrect(number) ? 'answer-correct' : 'answer-wrong';
    };

    const optionsOf = (q) => {
      try {
        if (Array.isArray(q.options) && q.options.length > 0) {
          return q.options.map(o => ({
            mark: o.option_mark || o.mark || '',
            text: o.option_content || o.text || o.content || '',
            option_mark: o.option_mark || o.mark || '',
            option_content: o.option_content || o.text || o.content || ''
          })).filter(o => o.mark && o.text);
        }
        
        const std = q && q['选项'];
        const preferredOrder = ['A', 'B', 'C', 'D'];
        if (std && typeof std === 'object' && !Array.isArray(std)) {
          const arr = [];
          preferredOrder.forEach((k) => {
            if (std[k] && String(std[k]).trim()) {
              arr.push({ 
                mark: k, 
                text: String(std[k]).trim(),
                option_mark: k,
                option_content: String(std[k]).trim()
              });
            }
          });
          if (arr.length) return arr;
        }
      } catch (e) {
      }
      return [];
    };

    return {
      questionType,
      isListeningType,
      isSectionA,
      isSectionB,
      sectionAData,
      sectionBData,
      listeningQuestions,
      getUserAnswer,
      getCorrectAnswer,
      isCorrectOption,
      getQuestionClass,
      getResultClass,
      getResultText,
      getOptionClass,
      getUserAnswerClass,
      getBlankQuestionClass,
      getBlankResultClass,
      getBlankResultText,
      getBlankAnswerClass,
      optionsOf
    };
  }
};
</script>

<style scoped>
.wrong-question-display {
  padding: 30px;
  height: 100%;
  overflow-y: auto;
}

/* 复用原有样式 */
.header-container {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-left: 20px; /* 右移避免被sidebar遮挡 */
}

.header {
  margin: 0;
  color: #2c3e50;
  font-size: 28px;
  font-weight: 700;
}

.action-buttons {
  display: flex;
  gap: 12px;
}

.loading-tip,
.error-tip {
  text-align: center;
  padding: 40px;
  font-size: 18px;
  color: #909399;
}

.error-tip {
  color: #f56c6c;
}

.metadata-info {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

/* 其余样式保持与原组件一致 */
.result-card {
  background: white;
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
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
  color: #546e7a;
  font-size: 18px;
  font-weight: 600;
  margin: 0 0 12px 0;
}

.topic-text {
  color: #37474f;
  font-size: 16px;
  line-height: 1.8;
  margin: 0;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px;
}

.passage-text {
  white-space: pre-wrap;
  word-break: break-word;
  background: #f8f9fa;
  padding: 20px;
  border-radius: 10px;
  color: #37474f;
  margin: 0;
  border: 2px solid #e9ecef;
  font-size: 15px;
  line-height: 1.8;
  font-family: inherit;
}

.answer-display-section {
  margin-top: 24px;
}

.answer-display-section h4 {
  color: #546e7a;
  margin: 0 0 12px 0;
  font-size: 16px;
  font-weight: 600;
}

.user-answer-text {
  white-space: pre-wrap;
  word-break: break-word;
  background: #e3f2fd;
  padding: 20px;
  border-radius: 10px;
  color: #1565c0;
  margin: 0 0 20px 0;
  border: 2px solid #2196f3;
  font-size: 15px;
  line-height: 1.8;
  min-height: 100px;
}

.ai-grading-section {
  margin-top: 24px;
  padding: 24px;
  background: linear-gradient(135deg, #f5f7fa 0%, #e8eaf6 100%);
  border-radius: 12px;
  border: 2px solid #7c4dff;
}

.ai-score-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 2px solid #7c4dff;
}

.ai-score-header h4 {
  margin: 0;
  color: #4a148c;
  font-size: 20px;
  font-weight: 700;
}

.ai-score-value {
  font-size: 32px;
  font-weight: 700;
  color: #7c4dff;
}

.ai-feedback,
.ai-reasoning {
  margin-top: 16px;
}

.ai-feedback h5,
.ai-reasoning h5 {
  color: #4a148c;
  font-size: 16px;
  font-weight: 600;
  margin: 0 0 8px 0;
}

.ai-feedback p,
.ai-reasoning p {
  color: #37474f;
  line-height: 1.8;
  margin: 0;
}

.exam-section {
  margin-top: 20px;
}

.section-header {
  margin-bottom: 20px;
}

.section-meta {
  color: #909399;
  font-size: 14px;
  margin: 8px 0 0 0;
}

.passage-section {
  margin-top: 16px;
}

.passage-content-block {
  margin-bottom: 24px;
}

.questions-block,
.section-block {
  margin-top: 20px;
}

.question-item {
  padding: 20px;
  border: 2px solid #e0e0e0;
  border-radius: 12px;
  margin-bottom: 16px;
  background: #fafbfc;
  transition: all 0.3s ease;
}

.question-item.correct-question {
  border-color: #4caf50;
  background: #f1f8f4;
}

.question-item.wrong-question {
  border-color: #f44336;
  background: #fef5f5;
}

.question-header {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 16px;
}

.question-no {
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  background: #667eea;
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 14px;
}

.question-text {
  flex: 1;
  margin: 0;
  color: #37474f;
  font-size: 16px;
  line-height: 1.8;
}

.result-indicator {
  flex-shrink: 0;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 600;
}

.result-correct {
  background: #e8f5e9;
  color: #2e7d32;
}

.result-wrong {
  background: #ffebee;
  color: #c62828;
}

.options-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 16px;
}

.radio-option {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px 16px;
  border: 2px solid #e0e0e0;
  border-radius: 8px;
  cursor: not-allowed;
  transition: all 0.3s ease;
  background: white;
}

.radio-option.option-correct {
  border-color: #4caf50;
  background: #f1f8f4;
}

.radio-option.option-wrong {
  border-color: #f44336;
  background: #fef5f5;
}

.radio-option input[type="radio"] {
  margin-top: 4px;
  cursor: not-allowed;
}

.option-label {
  flex: 1;
  color: #37474f;
  line-height: 1.6;
}

.answer-display {
  display: flex;
  gap: 12px;
  padding: 12px 16px;
  background: #f5f7fa;
  border-radius: 8px;
}

.answer-display.inline {
  display: inline-flex;
  margin-left: 12px;
}

.answer-label {
  font-weight: 600;
  color: #546e7a;
}

.answer-value {
  font-weight: 700;
}

.answer-correct {
  color: #2e7d32;
}

.answer-wrong {
  color: #c62828;
}

.question-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  border: 2px solid #e0e0e0;
  border-radius: 8px;
  margin-bottom: 12px;
  background: #fafbfc;
}

.question-row.correct-question {
  border-color: #4caf50;
  background: #f1f8f4;
}

.question-row.wrong-question {
  border-color: #f44336;
  background: #fef5f5;
}

.question-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.answer-select {
  flex: 1;
  max-width: 200px;
  padding: 10px 12px;
  border: 2px solid #4caf50;
  border-radius: 8px;
  font-size: 15px;
  background: #e8f5e9;
  color: #2e7d32;
  font-weight: 600;
  cursor: not-allowed;
}

.option-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 12px;
}

.option-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: white;
  border: 2px solid #e0e0e0;
  border-radius: 8px;
}

.option-mark {
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  background: #667eea;
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 13px;
}

.option-text {
  flex: 1;
  color: #37474f;
  font-size: 15px;
  font-weight: 500;
}

.article-block {
  margin-top: 20px;
}

.paragraph-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.paragraph-item {
  display: flex;
  gap: 12px;
  padding: 16px;
  background: white;
  border: 2px solid #e0e0e0;
  border-radius: 8px;
}

.paragraph-mark {
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  background: #667eea;
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 14px;
}

.paragraph-content {
  margin: 0;
  color: #37474f;
  line-height: 1.8;
  flex: 1;
}

.statements-block {
  margin-top: 20px;
}

.statement-row {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 16px;
  border: 2px solid #e0e0e0;
  border-radius: 8px;
  margin-bottom: 12px;
  background: #fafbfc;
}

.statement-row.correct-question {
  border-color: #4caf50;
  background: #f1f8f4;
}

.statement-row.wrong-question {
  border-color: #f44336;
  background: #fef5f5;
}

.statement-header {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  flex-wrap: wrap;
}

.statement-text {
  flex: 1;
  margin: 0;
  color: #37474f;
  line-height: 1.6;
}

.paragraph-select {
  max-width: 120px;
  padding: 10px 12px;
  border: 2px solid #4caf50;
  border-radius: 8px;
  font-size: 15px;
  background: #e8f5e9;
  color: #2e7d32;
  font-weight: 600;
  cursor: not-allowed;
}

.back-btn-container {
  text-align: center;
  margin-top: 40px;
}

.back-btn {
  padding: 14px 40px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.back-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.4);
}

.retry-button {
  padding: 10px 20px;
  font-size: 16px;
  font-weight: 500;
  border-radius: 8px;
  transition: all 0.3s ease;
}

.retry-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}
</style>
