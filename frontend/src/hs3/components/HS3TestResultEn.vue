<template>
  <div class="test-result-container">
    <div class="main-content">
      <div class="result-wrapper">
        <h1 class="header">考试结果 - {{ examPaperEn?.examPaperEnName || '加载中...' }}</h1>

        <div v-if="loading" class="loading-tip">加载结果中...</div>
        <div v-else-if="errorMessage" class="error-tip">{{ errorMessage }}</div>

        <template v-else>
          <!-- 成绩摘要卡片 -->
          <div class="summary-card">
            <div class="summary-stats">
              <div class="stat-item">
                <span class="stat-label">答对题数</span>
                <span class="stat-value correct">{{ testRecord.correctNumber }} / {{ objectiveQuestionsCount }}</span>
              </div>
              <div class="stat-item">
                <span class="stat-label">总分</span>
                <span class="stat-value score">{{ formatScore(testRecord.testEnScore) }}</span>
              </div>
            </div>
          </div>

          <!-- Part I 听力理解 -->
          <div v-if="listeningUnits.length" class="result-card structured exam-section">
            <div class="section-header">
              <div>
                <h2>第一部分 · 听力理解</h2>
                <p class="section-meta">共 {{ listeningUnits.length }} 个听力单元</p>
              </div>
            </div>

            <div
              v-for="(unit, uIdx) in listeningUnits"
              :key="`unit-${uIdx}`"
              class="passage-section"
            >
              <div class="passage-header">
                <h3>{{ unit.segment_name || '听力单元' }}</h3>
              </div>

              <div v-if="unit.listening_content" class="passage-content-block">
                <pre class="passage-text">{{ unit.listening_content }}</pre>
              </div>

              <div class="questions-block" v-if="unit.questions.length">
                <div
                  v-for="(q, qIdx) in unit.questions"
                  :key="`q-${uIdx}-${qIdx}`"
                  class="question-item"
                  :class="getQuestionClassWithSegment(q.question_number, unit.segmentId)"
                >
                  <div class="question-header">
                    <span class="question-no">{{ q.question_number || (qIdx + 1) }}</span>
                    <p class="question-text">{{ q.question || q.question_content || '' }}</p>
                    <span class="result-indicator" :class="getResultClassWithSegment(q.question_number, unit.segmentId)">
                      {{ getResultTextWithSegment(q.question_number, unit.segmentId) }}
                    </span>
                  </div>
                  <div class="options-list" v-if="q.options && q.options.length">
                    <label
                      v-for="opt in q.options"
                      :key="opt.option_mark || opt.mark"
                      class="radio-option"
                      :class="getOptionClassWithSegment(q.question_number, unit.segmentId, opt.option_mark || opt.mark)"
                    >
                      <input
                        type="radio"
                        :name="`lq-${uIdx}-${qIdx}`"
                        :value="opt.option_mark || opt.mark"
                        :checked="isCorrectAnswerWithSegment(q.question_number, unit.segmentId, opt.option_mark || opt.mark)"
                        disabled
                      />
                      <span class="option-label">
                        <strong>{{ opt.option_mark || opt.mark }}.</strong> {{ opt.option_content || opt.content }}
                      </span>
                    </label>
                  </div>
                  <div class="answer-display">
                    <span class="answer-label">正确答案:</span>
                    <span class="answer-value answer-correct">{{ getCorrectAnswerWithSegment(q.question_number, unit.segmentId) }}</span>
                    <span class="answer-label" style="margin-left: 20px;">你的答案:</span>
                    <span class="answer-value" :class="getUserAnswerClassWithSegment(q.question_number, unit.segmentId)">
                      {{ getUserAnswerWithSegment(q.question_number, unit.segmentId) || '未作答' }}
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Part II 阅读理解 -->
          <div v-if="readingUnits.length" class="result-card structured exam-section">
            <div class="section-header">
              <div>
                <h2>第二部分 · 阅读理解</h2>
                <p class="section-meta">共 {{ readingUnits.length }} 篇阅读</p>
              </div>
            </div>

            <div
              v-for="(unit, uIdx) in readingUnits"
              :key="`reading-${uIdx}`"
              class="passage-section"
            >
              <div class="passage-header">
                <h3>{{ unit.segment_name || ('阅读 ' + (uIdx + 1)) }}</h3>
              </div>

              <div class="passage-content-block" v-if="unit.passage">
                <pre class="passage-text">{{ unit.passage }}</pre>
              </div>

              <div class="questions-block" v-if="unit.questions.length">
                <div
                  v-for="(q, qIdx) in unit.questions"
                  :key="`rq-${uIdx}-${qIdx}`"
                  class="question-item"
                  :class="getQuestionClassWithSegment(q.question_number, unit.segmentId)"
                >
                  <div class="question-header">
                    <span class="question-no">{{ q.question_number || (qIdx + 1) }}</span>
                    <p class="question-text">{{ q.question || q.question_content || '' }}</p>
                    <span class="result-indicator" :class="getResultClassWithSegment(q.question_number, unit.segmentId)">
                      {{ getResultTextWithSegment(q.question_number, unit.segmentId) }}
                    </span>
                  </div>
                  <div class="options-list" v-if="q.options && q.options.length">
                    <label
                      v-for="opt in q.options"
                      :key="opt.option_mark || opt.mark"
                      class="radio-option"
                      :class="getOptionClassWithSegment(q.question_number, unit.segmentId, opt.option_mark || opt.mark)"
                    >
                      <input
                        type="radio"
                        :name="`rq-${uIdx}-${qIdx}`"
                        :value="opt.option_mark || opt.mark"
                        :checked="isCorrectAnswerWithSegment(q.question_number, unit.segmentId, opt.option_mark || opt.mark)"
                        disabled
                      />
                      <span class="option-label">
                        <strong>{{ opt.option_mark || opt.mark }}.</strong> {{ opt.option_content || opt.content }}
                      </span>
                    </label>
                  </div>
                  <div class="answer-display">
                    <span class="answer-label">正确答案:</span>
                    <span class="answer-value answer-correct">{{ getCorrectAnswerWithSegment(q.question_number, unit.segmentId) }}</span>
                    <span class="answer-label" style="margin-left: 20px;">你的答案:</span>
                    <span class="answer-value" :class="getUserAnswerClassWithSegment(q.question_number, unit.segmentId)">
                      {{ getUserAnswerWithSegment(q.question_number, unit.segmentId) || '未作答' }}
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 七选五 -->
          <div v-if="sevenChooseFiveUnits.length" class="result-card structured exam-section">
            <div class="section-header">
              <div>
                <h2>七选五</h2>
                <p class="section-meta">共 {{ sevenChooseFiveUnits.length }} 篇</p>
              </div>
            </div>

            <div
              v-for="(unit, uIdx) in sevenChooseFiveUnits"
              :key="`scf-${uIdx}`"
              class="passage-section"
            >
              <div class="passage-header" v-if="sevenChooseFiveUnits.length > 1">
                <h3>{{ unit.segment_name || ('七选五 ' + (uIdx + 1)) }}</h3>
              </div>

              <div class="section-block passage-block" v-if="unit.passage">
                <h3>文章</h3>
                <pre class="passage-text">{{ unit.passage }}</pre>
              </div>

              <div class="section-block options-block" v-if="unit.word_options && unit.word_options.length">
                <h3>备选项</h3>
                <div class="option-grid">
                  <div
                    v-for="option in unit.word_options"
                    :key="option.letter"
                    class="option-chip"
                  >
                    <span class="option-mark">{{ option.letter }}</span>
                    <span class="option-text">{{ option.content }}</span>
                  </div>
                </div>
              </div>

              <div class="section-block question-block" v-if="unit.blank_numbers && unit.blank_numbers.length">
                <h3>题目</h3>
                <div
                  v-for="number in unit.blank_numbers"
                  :key="`${unit.segmentId}_${number}`"
                  class="question-row"
                  :class="getQuestionClassWithSegment(number, unit.segmentId)"
                >
                  <div class="question-info">
                    <span class="question-no">{{ number }}</span>
                    <span class="result-indicator" :class="getResultClassWithSegment(number, unit.segmentId)">
                      {{ getResultTextWithSegment(number, unit.segmentId) }}
                    </span>
                  </div>
                  <div class="answer-display">
                    <span class="answer-label">正确答案:</span>
                    <span class="answer-value answer-correct">{{ getSevenChooseFiveOptionContent(getCorrectAnswerWithSegment(number, unit.segmentId), unit.word_options) }}</span>
                    <span class="answer-label" style="margin-left: 20px;">你的答案:</span>
                    <span class="answer-value" :class="getUserAnswerClassWithSegment(number, unit.segmentId)">
                      {{ getSevenChooseFiveOptionContent(getUserAnswerWithSegment(number, unit.segmentId), unit.word_options) || '未作答' }}
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 完形填空 -->
          <div v-if="clozeUnits.length" class="result-card structured exam-section">
            <div class="section-header">
              <div>
                <h2>完形填空</h2>
                <p class="section-meta">共 {{ clozeUnits.length }} 篇</p>
              </div>
            </div>

            <div
              v-for="(unit, uIdx) in clozeUnits"
              :key="`cloze-${uIdx}`"
              class="passage-section"
            >
              <div class="passage-header" v-if="clozeUnits.length > 1">
                <h3>{{ unit.segment_name || ('完形填空 ' + (uIdx + 1)) }}</h3>
              </div>

              <div class="passage-content-block" v-if="unit.passage">
                <pre class="passage-text">{{ unit.passage }}</pre>
              </div>

              <div class="questions-block" v-if="unit.questions.length">
                <div
                  v-for="(q, qIdx) in unit.questions"
                  :key="`cq-${uIdx}-${qIdx}`"
                  class="question-item"
                  :class="getQuestionClassWithSegment(q.question_number, unit.segmentId)"
                >
                  <div class="question-header">
                    <span class="question-no">{{ q.question_number || (qIdx + 1) }}</span>
                    <span class="result-indicator" :class="getResultClassWithSegment(q.question_number, unit.segmentId)">
                      {{ getResultTextWithSegment(q.question_number, unit.segmentId) }}
                    </span>
                  </div>
                  <div class="options-list" v-if="q.options && q.options.length">
                    <label
                      v-for="opt in q.options"
                      :key="opt.option_mark || opt.mark"
                      class="radio-option"
                      :class="getOptionClassWithSegment(q.question_number, unit.segmentId, opt.option_mark || opt.mark)"
                    >
                      <input
                        type="radio"
                        :name="`cq-${uIdx}-${qIdx}`"
                        :value="opt.option_mark || opt.mark"
                        :checked="isCorrectAnswerWithSegment(q.question_number, unit.segmentId, opt.option_mark || opt.mark)"
                        disabled
                      />
                      <span class="option-label">
                        <strong>{{ opt.option_mark || opt.mark }}.</strong> {{ opt.option_content || opt.content }}
                      </span>
                    </label>
                  </div>
                  <div class="answer-display">
                    <span class="answer-label">正确答案:</span>
                    <span class="answer-value answer-correct">{{ getCorrectAnswerWithSegment(q.question_number, unit.segmentId) }}</span>
                    <span class="answer-label" style="margin-left: 20px;">你的答案:</span>
                    <span class="answer-value" :class="getUserAnswerClassWithSegment(q.question_number, unit.segmentId)">
                      {{ getUserAnswerWithSegment(q.question_number, unit.segmentId) || '未作答' }}
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 语法填空 -->
          <div v-if="grammarUnits.length" class="result-card structured exam-section">
            <div class="section-header">
              <div>
                <h2>语法填空</h2>
                <p class="section-meta">共 {{ grammarUnits.length }} 篇</p>
              </div>
            </div>

            <div
              v-for="(unit, uIdx) in grammarUnits"
              :key="`grammar-${uIdx}`"
              class="passage-section"
            >
              <div class="passage-header" v-if="grammarUnits.length > 1">
                <h3>{{ unit.segment_name || ('语法填空 ' + (uIdx + 1)) }}</h3>
              </div>

              <div class="passage-content-block" v-if="unit.passage">
                <pre class="passage-text">{{ unit.passage }}</pre>
              </div>

              <div class="section-block question-block" v-if="unit.blank_numbers && unit.blank_numbers.length">
                <h3>填空</h3>
                <div
                  v-for="number in unit.blank_numbers"
                  :key="`${unit.segmentId}_${number}`"
                  class="question-row"
                  :class="getQuestionClassWithSegment(number, unit.segmentId)"
                >
                  <div class="question-info">
                    <span class="question-no">{{ number }}</span>
                    <span class="result-indicator" :class="getResultClassWithSegment(number, unit.segmentId)">
                      {{ getResultTextWithSegment(number, unit.segmentId) }}
                    </span>
                  </div>
                  <div class="answer-display inline">
                    <span class="answer-label">正确答案:</span>
                    <span class="answer-value answer-correct">{{ getCorrectAnswerWithSegment(number, unit.segmentId) }}</span>
                    <span class="answer-label" style="margin-left: 12px;">你的答案:</span>
                    <span class="answer-value" :class="getUserAnswerClassWithSegment(number, unit.segmentId)">
                      {{ getUserAnswerWithSegment(number, unit.segmentId) || '未作答' }}
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 应用文写作 -->
          <div v-if="applicationWritingUnits.length" class="result-card structured exam-section">
            <div class="section-header">
              <div>
                <h2>应用文写作</h2>
                <p class="section-meta" v-if="applicationWritingUnits.length > 1">共 {{ applicationWritingUnits.length }} 道写作题</p>
              </div>
            </div>

            <div
              v-for="(writing, wIdx) in applicationWritingUnits"
              :key="`app-writing-${wIdx}`"
              class="passage-section"
              :style="wIdx > 0 ? 'margin-top: 20px; border-top: 2px solid #e0e0e0; padding-top: 20px;' : ''"
            >
              <div class="passage-header" v-if="applicationWritingUnits.length > 1">
                <h3>应用文写作 {{ wIdx + 1 }}</h3>
              </div>

              <div class="passage-content-block" v-if="writing.prompt">
                <h4>题目要求：</h4>
                <pre class="passage-text writing-prompt-text">{{ writing.prompt }}</pre>
              </div>

              <div class="answer-display-section">
                <h4>你的作文：</h4>
                <pre class="user-answer-text">{{ writing.userAnswer || '未作答' }}</pre>
                <div v-if="writing.aiGrade" class="ai-grading-section">
                  <div class="ai-score-header">
                    <h4>🤖 AI评分结果</h4>
                  </div>
                  <div class="ai-feedback">
                    <pre class="ai-content">{{ writing.aiGrade }}</pre>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 读后续写 -->
          <div v-if="continuationWritingUnits.length" class="result-card structured exam-section">
            <div class="section-header">
              <div>
                <h2>读后续写</h2>
                <p class="section-meta" v-if="continuationWritingUnits.length > 1">共 {{ continuationWritingUnits.length }} 道续写题</p>
              </div>
            </div>

            <div
              v-for="(writing, wIdx) in continuationWritingUnits"
              :key="`cont-writing-${wIdx}`"
              class="passage-section"
              :style="wIdx > 0 ? 'margin-top: 20px; border-top: 2px solid #e0e0e0; padding-top: 20px;' : ''"
            >
              <div class="passage-header" v-if="continuationWritingUnits.length > 1">
                <h3>读后续写 {{ wIdx + 1 }}</h3>
              </div>

              <div class="passage-content-block" v-if="writing.passage">
                <h4>原文：</h4>
                <pre class="passage-text writing-prompt-text">{{ writing.passage }}</pre>
              </div>

              <div class="passage-content-block" v-if="writing.prompt" style="margin-top: 16px;">
                <h4>续写要求：</h4>
                <pre class="passage-text writing-prompt-text">{{ writing.prompt }}</pre>
              </div>

              <div class="answer-display-section">
                <h4>你的续写：</h4>
                <pre class="user-answer-text">{{ writing.userAnswer || '未作答' }}</pre>
                <div v-if="writing.aiGrade" class="ai-grading-section">
                  <div class="ai-score-header">
                    <h4>🤖 AI评分结果</h4>
                  </div>
                  <div class="ai-feedback">
                    <pre class="ai-content">{{ writing.aiGrade }}</pre>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 返回按钮 -->
          <div class="back-btn-container">
            <button class="back-btn" @click="goBack">返回试卷列表</button>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import axios from 'axios'
import { getPaperDisplay } from '../service/HS3paperAnalysisService'

export default {
  name: 'HS3TestResultEn',
  setup() {
    const router = useRouter()
    const route = useRoute()
    
    // 试卷原题数据（从 getPaperDisplay 获取）
    const paperSegments = ref([])
    
    const loading = ref(false)
    const errorMessage = ref('')
    const testRecord = ref({})
    const questionDetails = ref([])
    const examPaperEn = ref(null)
    
    // 答案映射：segmentId-questionNumber -> { correctAnswer, userAnswer, isCorrect, etc. }
    const answerMap = ref({})

    const parseJsonSafe = (text) => {
      try {
        return JSON.parse(text)
      } catch {
        return null
      }
    }

    // 格式化分数
    const formatScore = (score) => {
      if (score === null || score === undefined) {
        return '0.00'
      }
      const numScore = typeof score === 'number' ? score : parseFloat(score)
      return isNaN(numScore) ? '0.00' : numScore.toFixed(2)
    }

    onMounted(async () => {
      const testEnId = route.params.id
      
      if (!testEnId) {
        errorMessage.value = '测试ID缺失'
        return
      }

      loading.value = true
      
      try {
        // 1. 获取答题结果
        const response = await axios.get(`http://localhost:8080/api/hs3/test/result-details/${testEnId}`, {
          withCredentials: true
        })
        
        if (response.data.success) {
          testRecord.value = response.data.testRecord
          questionDetails.value = response.data.questionDetails || []
          examPaperEn.value = response.data.examPaperEn
          
          // 2. 获取试卷原题数据（包含content）
          if (testRecord.value.examPaperEnId) {
            try {
              const paperData = await getPaperDisplay(String(testRecord.value.examPaperEnId))
              if (paperData && paperData.segments) {
                // 解析试卷segments数据
                paperSegments.value = paperData.segments.map(seg => {
                  let parsedData = {}
                  if (seg.questionData) {
                    try {
                      parsedData = typeof seg.questionData === 'string' ? JSON.parse(seg.questionData) : seg.questionData
                    } catch (e) {
                      parsedData = {}
                    }
                  }
                  return {
                    ...parsedData,
                    segment_name: parsedData.segment_name || seg.segmentName || '',
                    content: parsedData.content || parsedData.passage || parsedData.article || seg.content || ''
                  }
                })
                console.log('试卷原题数据:', paperSegments.value)
              }
            } catch (paperError) {
              console.error('获取试卷原题失败:', paperError)
            }
          }
          
          // 构建答案映射
          questionDetails.value.forEach(detail => {
            const uniqueKey = `${detail.segmentId}-${detail.questionNumber}`
            answerMap.value[uniqueKey] = {
              correctAnswer: detail.correctAnswer,
              userAnswer: detail.userAnswer,
              isCorrect: detail.isCorrect,
              aiGrade: detail.aiGrade,
              questionType: detail.questionType,
              questionDocument: detail.questionDocument
            }
            // 同时保留纯题号的映射（用于向后兼容）
            if (!answerMap.value[detail.questionNumber]) {
              answerMap.value[detail.questionNumber] = answerMap.value[uniqueKey]
            }
          })
          
          console.log('答案映射:', answerMap.value)
          console.log('题目详情:', questionDetails.value)
        } else {
          errorMessage.value = response.data.message || '加载测试结果失败'
        }
      } catch (error) {
        console.error('加载测试结果失败:', error)
        errorMessage.value = '加载测试结果失败，请稍后重试'
      } finally {
        loading.value = false
      }
    })
    
    // 根据segment_name获取试卷原题的content
    const getSegmentContent = (segmentName) => {
      if (!segmentName || !paperSegments.value.length) return ''
      const seg = paperSegments.value.find(s => s.segment_name === segmentName)
      return seg ? seg.content : ''
    }

    // 计算客观题数量（排除主观题）
    const objectiveQuestionsCount = computed(() => {
      return questionDetails.value.filter(d => 
        d.questionType !== '应用文写作' && d.questionType !== '读后续写'
      ).length
    })

    // 听力单元数据
    const listeningUnits = computed(() => {
      const listeningDetails = questionDetails.value.filter(d => d.questionType === '听力')
      if (listeningDetails.length === 0) return []
      
      const segmentMap = {}
      listeningDetails.forEach(detail => {
        const segmentId = detail.segmentId
        if (!segmentMap[segmentId]) {
          segmentMap[segmentId] = {
            segmentId,
            document: detail.questionDocument,
            segment_name: segmentId, // segment_name 就是 segmentId
            questions: []
          }
        }
      })
      
      const units = []
      Object.values(segmentMap).forEach(segment => {
        const doc = parseJsonSafe(segment.document)
        const questions = doc ? extractQuestionsFromDoc(doc, segment.segmentId) : []
        
        // 优先从 paperSegments 获取 content
        const contentFromPaper = getSegmentContent(segment.segmentId)
        const contentFromDoc = doc ? (doc.content || doc.listening_content || doc.passage || '') : ''
        
        units.push({
          segmentId: segment.segmentId,
          segment_name: segment.segmentId,
          listening_content: contentFromPaper || contentFromDoc,
          questions
        })
      })
      
      return units
    })

    // 阅读理解单元数据
    const readingUnits = computed(() => {
      const readingDetails = questionDetails.value.filter(d => d.questionType === '阅读理解')
      if (readingDetails.length === 0) return []
      
      const segmentMap = {}
      readingDetails.forEach(detail => {
        const segmentId = detail.segmentId
        if (!segmentMap[segmentId]) {
          segmentMap[segmentId] = {
            segmentId,
            document: detail.questionDocument,
            segment_name: segmentId,
            questions: []
          }
        }
      })
      
      const units = []
      Object.values(segmentMap).forEach(segment => {
        const doc = parseJsonSafe(segment.document)
        const questions = doc ? extractQuestionsFromDoc(doc, segment.segmentId) : []
        
        // 优先从 paperSegments 获取 content
        const contentFromPaper = getSegmentContent(segment.segmentId)
        const contentFromDoc = doc ? (doc.content || doc.passage || doc.reading_passage || '') : ''
        
        units.push({
          segmentId: segment.segmentId,
          segment_name: segment.segmentId,
          passage: contentFromPaper || contentFromDoc,
          questions
        })
      })
      
      return units
    })

    // 七选五单元数据
    const sevenChooseFiveUnits = computed(() => {
      const scfDetails = questionDetails.value.filter(d => d.questionType === '七选五')
      if (scfDetails.length === 0) return []
      
      const segmentMap = {}
      scfDetails.forEach(detail => {
        const segmentId = detail.segmentId
        if (!segmentMap[segmentId]) {
          segmentMap[segmentId] = {
            segmentId,
            document: detail.questionDocument
          }
        }
      })
      
      const units = []
      Object.keys(segmentMap).sort().forEach(segmentId => {
        const doc = parseJsonSafe(segmentMap[segmentId].document)
        
        // 优先从 paperSegments 获取 content
        const contentFromPaper = getSegmentContent(segmentId)
        const contentFromDoc = doc ? (doc.content || doc.passage || '') : ''
        const passage = contentFromPaper || contentFromDoc
        
        let blankNumbers = []
        
        // 提取题号
        if (doc && Array.isArray(doc.answers)) {
          blankNumbers = doc.answers
            .map(item => item.question_number)
            .filter(num => num != null && num !== '')
            .sort((a, b) => parseInt(a) - parseInt(b))
        } else if (doc && doc.questions && doc.questions.items) {
          blankNumbers = doc.questions.items
            .map(item => item.question_number)
            .filter(num => num != null && num !== '')
            .sort((a, b) => parseInt(a) - parseInt(b))
        }
        
        // 提取备选项
        let wordOptions = doc ? (doc.word_options || doc.options || []) : []
        if (!Array.isArray(wordOptions)) wordOptions = []
        
        units.push({
          segmentId,
          segment_name: segmentId,
          passage,
          blank_numbers: blankNumbers,
          word_options: wordOptions
        })
      })
      
      return units
    })

    // 完形填空单元数据
    const clozeUnits = computed(() => {
      const clozeDetails = questionDetails.value.filter(d => d.questionType === '完形填空')
      if (clozeDetails.length === 0) return []
      
      const segmentMap = {}
      clozeDetails.forEach(detail => {
        const segmentId = detail.segmentId
        if (!segmentMap[segmentId]) {
          segmentMap[segmentId] = {
            segmentId,
            document: detail.questionDocument
          }
        }
      })
      
      const units = []
      Object.values(segmentMap).forEach(segment => {
        const doc = parseJsonSafe(segment.document)
        const questions = doc ? extractQuestionsFromDoc(doc, segment.segmentId) : []
        
        // 优先从 paperSegments 获取 content
        const contentFromPaper = getSegmentContent(segment.segmentId)
        const contentFromDoc = doc ? (doc.content || doc.passage || '') : ''
        
        units.push({
          segmentId: segment.segmentId,
          segment_name: segment.segmentId,
          passage: contentFromPaper || contentFromDoc,
          questions
        })
      })
      
      return units
    })

    // 语法填空单元数据
    const grammarUnits = computed(() => {
      const grammarDetails = questionDetails.value.filter(d => d.questionType === '语法填空')
      if (grammarDetails.length === 0) return []
      
      const segmentMap = {}
      grammarDetails.forEach(detail => {
        const segmentId = detail.segmentId
        if (!segmentMap[segmentId]) {
          segmentMap[segmentId] = {
            segmentId,
            document: detail.questionDocument
          }
        }
      })
      
      const units = []
      Object.keys(segmentMap).sort().forEach(segmentId => {
        const doc = parseJsonSafe(segmentMap[segmentId].document)
        
        // 优先从 paperSegments 获取 content
        const contentFromPaper = getSegmentContent(segmentId)
        const contentFromDoc = doc ? (doc.content || doc.passage || '') : ''
        const passage = contentFromPaper || contentFromDoc
        
        let blankNumbers = []
        
        // 提取题号
        if (doc && Array.isArray(doc.answers)) {
          blankNumbers = doc.answers
            .map(item => item.question_number)
            .filter(num => num != null && num !== '')
            .sort((a, b) => parseInt(a) - parseInt(b))
        } else if (doc && doc.questions && doc.questions.items) {
          blankNumbers = doc.questions.items
            .map(item => item.question_number)
            .filter(num => num != null && num !== '')
            .sort((a, b) => parseInt(a) - parseInt(b))
        }
        
        units.push({
          segmentId,
          segment_name: segmentId,
          passage,
          blank_numbers: blankNumbers
        })
      })
      
      return units
    })

    // 应用文写作单元数据
    const applicationWritingUnits = computed(() => {
      const writingDetails = questionDetails.value.filter(d => d.questionType === '应用文写作')
      if (writingDetails.length === 0) return []
      
      const units = []
      writingDetails.forEach(detail => {
        const doc = parseJsonSafe(detail.questionDocument)
        
        // 优先从 paperSegments 获取 content
        const contentFromPaper = getSegmentContent(detail.segmentId)
        let prompt = ''
        if (doc) {
          prompt = doc.content || doc.prompt || doc.topic || doc.requirement || ''
          if (doc.writing) {
            prompt = doc.writing.content || doc.writing.topic || doc.writing.prompt || doc.writing.requirement || prompt
          }
        }
        
        units.push({
          segmentId: detail.segmentId,
          prompt: contentFromPaper || prompt,
          userAnswer: detail.userAnswer || '',
          aiGrade: detail.aiGrade || ''
        })
      })
      
      return units
    })

    // 读后续写单元数据
    const continuationWritingUnits = computed(() => {
      const writingDetails = questionDetails.value.filter(d => d.questionType === '读后续写')
      if (writingDetails.length === 0) return []
      
      const units = []
      writingDetails.forEach(detail => {
        const doc = parseJsonSafe(detail.questionDocument)
        
        // 优先从 paperSegments 获取 content
        const contentFromPaper = getSegmentContent(detail.segmentId)
        let passage = ''
        let prompt = ''
        if (doc) {
          passage = doc.content || doc.passage || doc.reading_passage || ''
          prompt = doc.prompt || doc.continuation_prompt || doc.requirement || ''
          if (doc.continuation_writing) {
            passage = doc.continuation_writing.content || doc.continuation_writing.passage || passage
            prompt = doc.continuation_writing.prompt || prompt
          }
        }
        
        units.push({
          segmentId: detail.segmentId,
          passage: contentFromPaper || passage,
          prompt,
          userAnswer: detail.userAnswer || '',
          aiGrade: detail.aiGrade || ''
        })
      })
      
      return units
    })

    // 从文档中提取问题（兼容多种格式）
    const extractQuestionsFromDoc = (doc, segmentId) => {
      let questions = []
      
      // 尝试不同的路径
      if (doc.questions && doc.questions.items) {
        questions = doc.questions.items
      } else if (doc.questions && Array.isArray(doc.questions)) {
        questions = doc.questions
      } else if (doc.question_and_options) {
        questions = doc.question_and_options
      }
      
      // 为每个问题添加 segmentId
      return questions.map(q => ({
        ...q,
        segmentId
      }))
    }

    // ========== 带 segmentId 的方法 ==========
    
    const getAnswerByKey = (questionNumber, segmentId = null) => {
      if (segmentId) {
        const uniqueKey = `${segmentId}-${questionNumber}`
        return answerMap.value[uniqueKey]
      }
      return answerMap.value[questionNumber] || answerMap.value[String(questionNumber)]
    }

    const getCorrectAnswerWithSegment = (questionNumber, segmentId) => {
      const answer = getAnswerByKey(questionNumber, segmentId)
      return answer ? answer.correctAnswer : ''
    }

    const getUserAnswerWithSegment = (questionNumber, segmentId) => {
      const answer = getAnswerByKey(questionNumber, segmentId)
      return answer ? answer.userAnswer : ''
    }

    const isCorrectAnswerWithSegment = (questionNumber, segmentId, optionMark) => {
      const correctAnswer = getCorrectAnswerWithSegment(questionNumber, segmentId)
      return correctAnswer === optionMark
    }

    const getQuestionClassWithSegment = (questionNumber, segmentId) => {
      const answer = getAnswerByKey(questionNumber, segmentId)
      if (!answer) return ''
      return answer.isCorrect ? 'correct-question' : 'wrong-question'
    }

    const getResultClassWithSegment = (questionNumber, segmentId) => {
      const answer = getAnswerByKey(questionNumber, segmentId)
      if (!answer) return ''
      return answer.isCorrect ? 'result-correct' : 'result-wrong'
    }

    const getResultTextWithSegment = (questionNumber, segmentId) => {
      const answer = getAnswerByKey(questionNumber, segmentId)
      if (!answer) return ''
      return answer.isCorrect ? '✓ 正确' : '✗ 错误'
    }

    const getOptionClassWithSegment = (questionNumber, segmentId, optionMark) => {
      const answer = getAnswerByKey(questionNumber, segmentId)
      if (!answer) return ''
      
      const isCorrect = answer.correctAnswer === optionMark
      const isUserChoice = answer.userAnswer === optionMark
      
      if (isCorrect) {
        return 'option-correct'
      } else if (isUserChoice && !isCorrect) {
        return 'option-wrong'
      }
      return ''
    }

    const getUserAnswerClassWithSegment = (questionNumber, segmentId) => {
      const answer = getAnswerByKey(questionNumber, segmentId)
      if (!answer) return ''
      return answer.isCorrect ? 'answer-correct' : 'answer-wrong'
    }

    // 获取七选五选项的完整内容（选项标号 + 内容）
    const getSevenChooseFiveOptionContent = (optionLetter, wordOptions) => {
      if (!optionLetter || !wordOptions || !Array.isArray(wordOptions)) return optionLetter || ''
      const option = wordOptions.find(opt => opt.letter === optionLetter)
      if (option && option.content) {
        return `${optionLetter}. ${option.content}`
      }
      return optionLetter
    }

    const goBack = () => {
      router.push('/hs3/papers')
    }

    return {
      loading,
      errorMessage,
      testRecord,
      questionDetails,
      examPaperEn,
      objectiveQuestionsCount,
      listeningUnits,
      readingUnits,
      sevenChooseFiveUnits,
      clozeUnits,
      grammarUnits,
      applicationWritingUnits,
      continuationWritingUnits,
      getCorrectAnswerWithSegment,
      getUserAnswerWithSegment,
      isCorrectAnswerWithSegment,
      getQuestionClassWithSegment,
      getResultClassWithSegment,
      getResultTextWithSegment,
      getOptionClassWithSegment,
      getUserAnswerClassWithSegment,
      getSevenChooseFiveOptionContent,
      formatScore,
      goBack
    }
  }
}
</script>

<style scoped>
.test-result-container {
  display: flex;
  min-height: 100vh;
  background: #f5f7fa;
}

.main-content {
  flex: 1;
  padding: 20px;
  width: 100%;
  box-sizing: border-box;
}

.result-wrapper {
  max-width: 900px;
  margin: 0 auto;
  padding: 32px 20px 60px;
}

.header {
  text-align: center;
  margin-bottom: 28px;
  color: #2c3e50;
  font-size: 24px;
  font-weight: 700;
}

.loading-tip,
.error-tip {
  padding: 40px;
  text-align: center;
  color: #546e7a;
  font-size: 16px;
}

.error-tip {
  color: #d32f2f;
}

/* 成绩摘要卡片 */
.summary-card {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 30px;
  border-radius: 16px;
  box-shadow: 0 8px 24px rgba(102, 126, 234, 0.3);
  margin-bottom: 30px;
}

.summary-stats {
  display: flex;
  gap: 60px;
  justify-content: center;
  flex-wrap: wrap;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.stat-label {
  font-size: 16px;
  opacity: 0.9;
  font-weight: 500;
}

.stat-value {
  font-size: 36px;
  font-weight: 700;
}

.stat-value.correct {
  color: #4caf50;
}

.stat-value.score {
  color: #ffd54f;
}

/* 试卷内容样式 */
.result-card {
  background: #fff;
  padding: 26px;
  border-radius: 14px;
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.08);
  margin-top: 24px;
}

.result-card h2 {
  margin-top: 0;
  color: #2e7d32;
}

.passage-text {
  white-space: pre-wrap;
  word-break: break-word;
  background: #f7f9fb;
  padding: 18px;
  border-radius: 10px;
  max-height: 420px;
  overflow-y: auto;
  color: #37474f;
  margin: 0;
  font-family: inherit;
  line-height: 1.8;
}

/* 写作提示文本 - 使用等宽字体确保下划线对齐 */
.writing-prompt-text {
  font-family: 'Courier New', Consolas, 'Liberation Mono', monospace;
  font-size: 14px;
  line-height: 1.6;
  letter-spacing: 0;
  max-height: none; /* 不限制高度，完整显示写作格子 */
}

.exam-section {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.section-meta {
  margin: 4px 0 0;
  color: #607d8b;
  font-size: 14px;
}

.section-block {
  margin-top: 16px;
}

.section-block h3 {
  margin-bottom: 8px;
  color: #2c3e50;
}

.passage-block {
  border: 1px solid #e0e0e0;
  border-radius: 12px;
  padding: 18px;
  background: #f9fbfd;
}

.passage-section {
  margin-bottom: 30px;
  padding-bottom: 30px;
  border-bottom: 2px solid #e0e0e0;
}

.passage-section:last-child {
  border-bottom: none;
}

.passage-header h3 {
  color: #2c3e50;
  margin-bottom: 12px;
}

.passage-content-block {
  margin: 16px 0;
}

.passage-content-block h4 {
  color: #546e7a;
  margin-bottom: 10px;
}

.questions-block {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.question-item {
  padding: 16px;
  border: 2px solid #e0e0e0;
  border-radius: 10px;
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
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.question-no {
  font-weight: 600;
  color: #fff;
  background: #1976d2;
  padding: 4px 12px;
  border-radius: 16px;
  font-size: 14px;
  min-width: 40px;
  text-align: center;
}

.question-text {
  flex: 1;
  margin: 0;
  color: #37474f;
  line-height: 1.6;
}

.result-indicator {
  padding: 4px 12px;
  border-radius: 16px;
  font-size: 13px;
  font-weight: 600;
}

.result-indicator.result-correct {
  background: #4caf50;
  color: white;
}

.result-indicator.result-wrong {
  background: #f44336;
  color: white;
}

.options-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.radio-option {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  background: #fff;
  cursor: not-allowed;
  transition: all 0.2s ease;
}

.radio-option.option-correct {
  border-color: #4caf50;
  background: #e8f5e9;
  font-weight: 600;
}

.radio-option.option-wrong {
  border-color: #f44336;
  background: #ffebee;
}

.radio-option input[type="radio"] {
  cursor: not-allowed;
}

.option-label {
  flex: 1;
  color: #37474f;
}

.answer-display {
  margin-top: 12px;
  padding: 10px 12px;
  background: #f5f5f5;
  border-radius: 8px;
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.answer-display.inline {
  display: inline-flex;
  margin-left: 12px;
  margin-top: 0;
}

.answer-label {
  font-weight: 600;
  color: #546e7a;
  font-size: 14px;
}

.answer-value {
  font-size: 15px;
  padding: 4px 10px;
  border-radius: 6px;
  font-weight: 500;
}

.answer-value.answer-correct {
  background: #e8f5e9;
  color: #2e7d32;
}

.answer-value.answer-wrong {
  background: #ffebee;
  color: #c62828;
}

/* 填空题样式 */
.question-block {
  margin-top: 20px;
}

.question-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border: 2px solid #e0e0e0;
  border-radius: 8px;
  margin-bottom: 10px;
  background: #fafbfc;
  flex-wrap: wrap;
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
  min-width: 120px;
}

.answer-select {
  flex: 1;
  min-width: 120px;
  max-width: 200px;
  border: 2px solid #4caf50;
  border-radius: 8px;
  padding: 10px 12px;
  font-size: 15px;
  background: #e8f5e9;
  color: #2e7d32;
  font-weight: 600;
  cursor: not-allowed;
}

.options-block {
  margin-top: 20px;
}

.options-block h3 {
  margin-bottom: 12px;
  color: #2c3e50;
}

.option-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 12px;
}

.option-chip {
  display: flex;
  gap: 8px;
  align-items: flex-start;
  padding: 10px 12px;
  border: 1px solid #e0e0e0;
  border-radius: 10px;
  background: #fff;
}

.option-mark {
  font-weight: 600;
  color: #2c3e50;
}

.option-text {
  color: #37474f;
  flex: 1;
}

/* 主观题答案展示 */
.answer-display-section {
  margin-top: 20px;
}

.answer-display-section h4 {
  color: #546e7a;
  margin-bottom: 10px;
  font-size: 16px;
}

.user-answer-text {
  white-space: pre-wrap;
  word-break: break-word;
  background: #e3f2fd;
  padding: 18px;
  border-radius: 10px;
  color: #1565c0;
  margin: 0 0 20px 0;
  border: 2px solid #2196f3;
  font-family: inherit;
  line-height: 1.8;
}

/* AI评分区域 */
.ai-grading-section {
  margin-top: 24px;
  padding: 20px;
  background: linear-gradient(135deg, #f5f7fa 0%, #e8eaf6 100%);
  border-radius: 12px;
  border: 2px solid #7c4dff;
}

.ai-score-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.ai-score-header h4 {
  margin: 0;
  color: #5e35b1;
  font-size: 18px;
}

.ai-feedback {
  margin-top: 8px;
}

.ai-content {
  white-space: pre-wrap;
  word-break: break-word;
  margin: 0;
  color: #37474f;
  line-height: 1.6;
  background: white;
  padding: 16px;
  border-radius: 8px;
  font-family: inherit;
}

/* 返回按钮 */
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

@media (max-width: 768px) {
  .summary-stats {
    flex-direction: column;
    gap: 20px;
  }
  
  .question-row {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .answer-display.inline {
    display: flex;
    margin-left: 0;
    margin-top: 12px;
  }
}
</style>
