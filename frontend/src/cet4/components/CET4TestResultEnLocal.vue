<template>
  <div class="test-result-container">
    <SideBarMenu />
    <div class="main-content">
      <div class="result-wrapper">
        <h1 class="header">考试结果 - {{ examPaperEn?.examPaperEnName || '加载中...' }}</h1>
        
        <!-- 提示：本地模式 -->
        <div class="local-mode-tip">
          <span>📝 错题再训练模式（本地评分，不保存记录）</span>
        </div>

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
            </div>
          </div>

          <!-- Part I · Writing -->
          <div v-if="writingUnits.length" class="result-card structured exam-section" style="margin-top: 16px">
            <div class="section-header">
              <div>
                <h2>Part I · Writing</h2>
                <p class="section-meta" v-if="writingUnits.length > 1">共 {{ writingUnits.length }} 道写作题</p>
              </div>
            </div>

            <div
              v-for="(writing, wIdx) in writingUnits"
              :key="`writing-${wIdx}`"
              class="passage-section"
              :style="wIdx > 0 ? 'margin-top: 20px; border-top: 2px solid #e0e0e0; padding-top: 20px;' : ''"
            >
              <div class="passage-header" v-if="writingUnits.length > 1">
                <h3>写作题 {{ wIdx + 1 }}</h3>
              </div>

              <pre class="passage-text">{{ writing.passage }}</pre>
              <div class="answer-display-section">
                <h4>你的作文：</h4>
                <pre class="user-answer-text">{{ writing.userAnswer || '未作答' }}</pre>
                <div class="ai-note">
                  <p>💡 本地模式不支持AI评分，如需AI评分请使用正常考试模式</p>
                </div>
              </div>
            </div>
          </div>

          <!-- Part II · Listening Comprehension -->
          <div v-if="listeningUnits.length" class="result-card structured exam-section">
            <div class="section-header">
              <div>
                <h2>Part II · Listening Comprehension</h2>
                <p class="section-meta">共 {{ listeningUnits.length }} 个单元</p>
              </div>
            </div>

            <div
              v-for="(unit, uIdx) in listeningUnits"
              :key="`unit-${uIdx}`"
              class="passage-section"
            >
                <div class="passage-header">
                  <h3>{{ unit.unit_type || '听力单元' }}</h3>
                </div>

              <div v-if="unit.listening_content" class="passage-content-block">
                <pre class="passage-text">{{ unit.listening_content }}</pre>
              </div>

              <div class="questions-block" v-if="unit.questions.length">
                <div
                  v-for="(q, qIdx) in unit.questions"
                  :key="`q-${uIdx}-${qIdx}`"
                  class="question-item"
                  :class="getQuestionClassWithSegment(q.question_number, q.segmentId)"
                >
                  <div class="question-header">
                    <span class="question-no">{{ q.question_number || (qIdx + 1) }}</span>
                    <p class="question-text">{{ q.question_content || q['题干'] }}</p>
                    <span class="result-indicator" :class="getResultClassWithSegment(q.question_number, q.segmentId)">
                      {{ getResultTextWithSegment(q.question_number, q.segmentId) }}
                    </span>
                  </div>
                  <div class="options-list">
                    <label
                      v-for="opt in (q.options || optionsOf(q))"
                      :key="opt.option_mark || opt.mark"
                      class="radio-option"
                      :class="getOptionClassWithSegment(q.question_number, q.segmentId, opt.option_mark || opt.mark)"
                    >
                      <input
                        type="radio"
                        :name="`lq-${uIdx}-${qIdx}`"
                        :value="opt.option_mark || opt.mark"
                        :checked="isCorrectAnswerWithSegment(q.question_number, q.segmentId, opt.option_mark || opt.mark)"
                        disabled
                      />
                      <span class="option-label">
                        <strong>{{ opt.option_mark || opt.mark }}.</strong> {{ opt.option_content || opt.text }}
                      </span>
                    </label>
                  </div>
                  <div class="answer-display">
                    <span class="answer-label">你的答案:</span>
                    <span class="answer-value" :class="getUserAnswerClassWithSegment(q.question_number, q.segmentId)">
                      {{ getUserAnswerWithSegment(q.question_number, q.segmentId) || '未作答' }}
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Section A（选词填空） -->
          <div v-if="sectionAUnits.length" class="result-card structured exam-section">
            <div class="section-header">
              <div>
                <h2>Section A（选词填空）</h2>
                <p class="section-meta">共 {{ sectionAUnits.length }} 篇文章</p>
              </div>
            </div>

            <div
              v-for="(unit, uIdx) in sectionAUnits"
              :key="`sectionA-${uIdx}`"
              class="passage-section"
            >
              <div class="passage-header" v-if="sectionAUnits.length > 1">
                <h3>{{ unit.title || ('文章 ' + (uIdx + 1)) }}</h3>
              </div>

              <div class="section-block passage-block">
                <h3>题干</h3>
                <pre class="passage-text">{{ unit.passage }}</pre>
              </div>

              <div class="section-block question-block" v-if="unit.blank_numbers.length">
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
                  <select class="answer-select" disabled>
                    <option value="" selected>{{ getCorrectAnswerWithSegment(number, unit.segmentId) }}</option>
                  </select>
                  <div class="answer-display inline">
                    <span class="answer-label">你的答案:</span>
                    <span class="answer-value" :class="getUserAnswerClassWithSegment(number, unit.segmentId)">
                      {{ getUserAnswerWithSegment(number, unit.segmentId) || '未作答' }}
                    </span>
                  </div>
                </div>
              </div>

              <div class="section-block options-block" v-if="unit.options.length">
                <h3>备选词</h3>
                <div class="option-grid">
                  <div
                    v-for="option in unit.options"
                    :key="`${unit.segmentId}-${option.letter}`"
                    class="option-chip"
                  >
                    <span class="option-mark">{{ option.letter }}</span>
                    <span class="option-text">{{ option.word }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Section B 段落匹配 -->
          <div v-if="sectionBUnits.length" class="result-card structured exam-section">
            <div class="section-header">
              <div>
                <h2>Section B（段落匹配）</h2>
                <p class="section-meta">共 {{ sectionBUnits.length }} 篇文章</p>
              </div>
            </div>

            <div
              v-for="(unit, uIdx) in sectionBUnits"
              :key="`sectionB-${uIdx}`"
              class="passage-section"
            >
              <div class="passage-header" v-if="sectionBUnits.length > 1">
                <h3>{{ unit.title || ('文章 ' + (uIdx + 1)) }}</h3>
              </div>

              <div class="section-block article-block">
                <h3>文章</h3>
                <div class="paragraph-list">
                  <div
                    v-for="paragraph in unit.article"
                    :key="paragraph.paragraph_mark"
                    class="paragraph-item"
                  >
                    <span class="paragraph-mark">{{ paragraph.paragraph_mark }}</span>
                    <p class="paragraph-content">{{ paragraph.paragraph_content }}</p>
                  </div>
                </div>
              </div>

              <div class="section-block statements-block">
                <h3>题目（每个陈述句对应的段落）</h3>
                <div
                  v-for="statement in unit.statements"
                  :key="`${unit.segmentId}_${statement.question_number}`"
                  class="statement-row"
                  :class="getQuestionClassWithSegment(statement.question_number, unit.segmentId)"
                >
                  <div class="statement-header">
                    <span class="question-no">{{ statement.question_number }}</span>
                    <p class="statement-text">{{ statement.statement_content }}</p>
                    <span class="result-indicator" :class="getResultClassWithSegment(statement.question_number, unit.segmentId)">
                      {{ getResultTextWithSegment(statement.question_number, unit.segmentId) }}
                    </span>
                  </div>
                  <select class="paragraph-select" disabled>
                    <option value="" selected>{{ getCorrectAnswerWithSegment(statement.question_number, unit.segmentId) }}</option>
                  </select>
                  <div class="answer-display inline">
                    <span class="answer-label">你的答案:</span>
                    <span class="answer-value" :class="getUserAnswerClassWithSegment(statement.question_number, unit.segmentId)">
                      {{ getUserAnswerWithSegment(statement.question_number, unit.segmentId) || '未作答' }}
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Section C 篇章阅读 -->
          <div v-if="sectionCData && sectionCData.passages.length" class="result-card structured exam-section">
            <div class="section-header">
              <div>
                <h2>Section C（篇章阅读）</h2>
                <p class="section-meta">共 {{ sectionCData.passages.length }} 篇短文</p>
              </div>
            </div>

            <div
              v-for="(passage, pIdx) in sectionCData.passages"
              :key="pIdx"
              class="passage-section"
            >
              <div class="passage-header">
                <h3>{{ passage.passage_mark }}</h3>
              </div>
              <div class="passage-content-block">
                <pre class="passage-text">{{ passage.passage_content }}</pre>
              </div>

              <div class="questions-block">
                <div
                  v-for="question in passage.questions"
                  :key="`${question.segmentId}-${question.question_number}`"
                  class="question-item"
                  :class="getQuestionClassWithSegment(question.question_number, question.segmentId)"
                >
                  <div class="question-header">
                    <span class="question-no">{{ question.question_number }}</span>
                    <p class="question-text">{{ question.question_content }}</p>
                    <span class="result-indicator" :class="getResultClassWithSegment(question.question_number, question.segmentId)">
                      {{ getResultTextWithSegment(question.question_number, question.segmentId) }}
                    </span>
                  </div>
                  <div class="options-list">
                    <label
                      v-for="option in question.options"
                      :key="option.option_mark"
                      class="radio-option"
                      :class="getOptionClassWithSegment(question.question_number, question.segmentId, option.option_mark)"
                    >
                      <input
                        type="radio"
                        :name="`q-${question.segmentId}-${question.question_number}`"
                        :value="option.option_mark"
                        :checked="isCorrectAnswerWithSegment(question.question_number, question.segmentId, option.option_mark)"
                        disabled
                      />
                      <span class="option-label">
                        <strong>{{ option.option_mark }}.</strong> {{ option.option_content }}
                      </span>
                    </label>
                  </div>
                  <div class="answer-display">
                    <span class="answer-label">你的答案:</span>
                    <span class="answer-value" :class="getUserAnswerClassWithSegment(question.question_number, question.segmentId)">
                      {{ getUserAnswerWithSegment(question.question_number, question.segmentId) || '未作答' }}
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Part IV Translation -->
          <div v-if="translationUnits.length" class="result-card structured exam-section" style="margin-top: 24px">
            <div class="section-header">
              <div>
                <h2>Part IV · Translation</h2>
                <p class="section-meta" v-if="translationUnits.length > 1">共 {{ translationUnits.length }} 道翻译题</p>
              </div>
            </div>

            <div
              v-for="(translation, tIdx) in translationUnits"
              :key="`translation-${tIdx}`"
              class="passage-section"
              :style="tIdx > 0 ? 'margin-top: 20px; border-top: 2px solid #e0e0e0; padding-top: 20px;' : ''"
            >
              <div class="passage-header" v-if="translationUnits.length > 1">
                <h3>翻译题 {{ tIdx + 1 }}</h3>
              </div>

              <pre class="passage-text">{{ translation.passage }}</pre>
              <div class="answer-display-section">
                <h4>你的翻译：</h4>
                <pre class="user-answer-text">{{ translation.userAnswer || '未作答' }}</pre>
                <div class="ai-note">
                  <p>💡 本地模式不支持AI评分，如需AI评分请使用正常考试模式</p>
                </div>
              </div>
            </div>
          </div>

          <!-- 返回按钮 -->
          <div class="back-btn-container">
            <button class="back-btn" @click="goBack">返回错题本</button>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import SideBarMenu from '@/common/components/SideBarMenu.vue'

export default {
  name: 'TestResultEnLocal',
  components: {
    SideBarMenu
  },
  setup() {
    const router = useRouter()
    
    const loading = ref(false)
    const errorMessage = ref('')
    const testRecord = ref({})
    const questionDetails = ref([])
    const examPaperEn = ref(null)
    
    // 答案映射：segmentId-questionNumber -> { correctAnswer, userAnswer, isCorrect }
    const answerMap = ref({})

    const parseJsonSafe = (text) => {
      try {
        return typeof text === 'string' ? JSON.parse(text) : text
      } catch {
        return null
      }
    }

    // 格式化分数，保留两位小数
    const formatScore = (score) => {
      if (score === null || score === undefined) {
        return '0.00'
      }
      const numScore = typeof score === 'number' ? score : parseFloat(score)
      return isNaN(numScore) ? '0.00' : numScore.toFixed(2)
    }

    // 从 sessionStorage 加载本地结果数据
    onMounted(async () => {
      loading.value = true
      
      try {
        const tempResultJson = sessionStorage.getItem('tempTestResult')
        if (!tempResultJson) {
          errorMessage.value = '未找到测试结果数据，请重新答题'
          loading.value = false
          return
        }
        
        const tempResult = parseJsonSafe(tempResultJson)
        
        if (!tempResult || !tempResult.units) {
          errorMessage.value = '测试结果数据格式错误'
          loading.value = false
          return
        }
        
        // 构建本地评分结果
        const localQuestionDetails = []
        let correctCount = 0
        
        // 遍历所有单元和题目，计算得分
        tempResult.units.forEach(unit => {
          // 解析 document 字段获取题目数据
          let unitData = unit
          if (typeof unit.document === 'string') {
            try {
              unitData = JSON.parse(unit.document)
            } catch (e) {
              unitData = unit
            }
          } else if (unit.document) {
            // document 已经是对象
            unitData = unit.document
          }
          
          // 从 metadata 或 unitData 中获取信息
          const segmentId = unit.metadata?.segment_id || unitData.segment_id
          const unitType = unit.metadata?.question_type || unitData.unit_type || 'Unknown'
          
          // 处理听力题目 (NewsReport, Conversation, ListeningPassage)
          // 注意: question_and_options 是题目数组
          const questions = unitData.questions || unitData.question_and_options || []
          if (Array.isArray(questions) && questions.length > 0) {
            questions.forEach(q => {
              const questionNumber = q.question_number || q['题号']
              const uniqueKey = `${segmentId}-${questionNumber}`
              
              // 从答案中获取用户的作答
              const userAnswer = tempResult.answers[uniqueKey] || tempResult.answers[questionNumber] || ''
              const correctAnswer = tempResult.correctAnswers[uniqueKey] || tempResult.correctAnswers[questionNumber] || q.answer || q['答案'] || ''
              
              const isCorrect = userAnswer.trim().toUpperCase() === correctAnswer.trim().toUpperCase()
              if (isCorrect) correctCount++
              
              localQuestionDetails.push({
                segmentId,
                questionNumber,
                questionType: unitType,
                correctAnswer,
                userAnswer,
                isCorrect,
                questionDocument: JSON.stringify(unitData)
              })
            })
          }
          
          // 处理 Section A (Banked Cloze) 题目
          if (unitData.unit_type === 'SectionA' || unitType === 'BlankedCloze') {
            const blankNumbers = unitData.blank_numbers || []
            blankNumbers.forEach(blankNum => {
              const uniqueKey = `${segmentId}-${blankNum}`
              
              const userAnswer = tempResult.answers[uniqueKey] || tempResult.answers[blankNum] || ''
              const correctAnswer = tempResult.correctAnswers[uniqueKey] || tempResult.correctAnswers[blankNum] || ''
              
              const isCorrect = userAnswer.trim().toUpperCase() === correctAnswer.trim().toUpperCase()
              if (isCorrect) correctCount++
              
              localQuestionDetails.push({
                segmentId,
                questionNumber: blankNum,
                questionType: 'BlankedCloze',
                correctAnswer,
                userAnswer,
                isCorrect,
                questionDocument: JSON.stringify(unitData)
              })
            })
          }
          
          // 处理 Section B (Matching) 题目
          if (unitData.unit_type === 'SectionB' || unitType === 'Matching') {
            const statements = unitData.statements || []
            statements.forEach(stmt => {
              const questionNumber = stmt['题号'] || stmt.question_number
              const uniqueKey = `${segmentId}-${questionNumber}`
              
              const userAnswer = tempResult.answers[uniqueKey] || tempResult.answers[questionNumber] || ''
              const correctAnswer = tempResult.correctAnswers[uniqueKey] || tempResult.correctAnswers[questionNumber] || stmt['答案'] || stmt.answer || ''
              
              const isCorrect = userAnswer.trim().toUpperCase() === correctAnswer.trim().toUpperCase()
              if (isCorrect) correctCount++
              
              localQuestionDetails.push({
                segmentId,
                questionNumber,
                questionType: 'Matching',
                correctAnswer,
                userAnswer,
                isCorrect,
                questionDocument: JSON.stringify(unitData)
              })
            })
          }
          
          // 处理 Section C (Reading Passages) 题目
          if (unitData.unit_type === 'SectionC' || unitType === 'ReadingPassage') {
            const passages = unitData.passages || []
            passages.forEach(passage => {
              if (passage.questions && Array.isArray(passage.questions)) {
                passage.questions.forEach(q => {
                  const questionNumber = q['题号'] || q.question_number
                  const uniqueKey = `${segmentId}-${questionNumber}`
                  
                  const userAnswer = tempResult.answers[uniqueKey] || tempResult.answers[questionNumber] || ''
                  const correctAnswer = tempResult.correctAnswers[uniqueKey] || tempResult.correctAnswers[questionNumber] || q['答案'] || q.answer || ''
                  
                  const isCorrect = userAnswer.trim().toUpperCase() === correctAnswer.trim().toUpperCase()
                  if (isCorrect) correctCount++
                  
                  localQuestionDetails.push({
                    segmentId,
                    questionNumber,
                    questionType: 'ReadingPassage',
                    correctAnswer,
                    userAnswer,
                    isCorrect,
                    questionDocument: JSON.stringify({ ...passage, segmentId })
                  })
                })
              }
            })
          }
        })
        
        // 设置测试记录（错题再训练模式不计算总分）
        const objectiveCount = localQuestionDetails.length
        
        testRecord.value = {
          correctNumber: correctCount,
          testEnScore: 0,  // 错题再训练模式不显示总分
          testEnId: 'local-temp'
        }
        
        // 设置题目详情
        questionDetails.value = localQuestionDetails
        
        // 设置试卷信息
        examPaperEn.value = {
          examPaperEnName: tempResult.examPaperName || '错题再训练'
        }
        
        // 构建答案映射
        localQuestionDetails.forEach(detail => {
          const uniqueKey = `${detail.segmentId}-${detail.questionNumber}`
          answerMap.value[uniqueKey] = {
            correctAnswer: detail.correctAnswer,
            userAnswer: detail.userAnswer,
            isCorrect: detail.isCorrect,
            questionType: detail.questionType,
            questionDocument: detail.questionDocument
          }
          // 同时保留纯题号的映射（用于向后兼容）
          if (!answerMap.value[detail.questionNumber]) {
            answerMap.value[detail.questionNumber] = answerMap.value[uniqueKey]
          }
        })
      } catch (error) {
        errorMessage.value = '加载测试结果失败：' + error.message
      } finally {
        loading.value = false
      }
    })

    // 计算客观题数量
    const objectiveQuestionsCount = computed(() => {
      return questionDetails.value.filter(d => d.questionType !== 'Writing' && d.questionType !== 'Translation').length
    })

    // Part I Writing 数据（支持多个Writing题型）
    const writingUnits = computed(() => {
      const writingDetails = questionDetails.value.filter(d => d.questionType === 'Writing')
      if (writingDetails.length === 0) return []
      
      const units = []
      writingDetails.forEach(detail => {
        if (!detail.questionDocument) return
        
        const doc = parseJsonSafe(detail.questionDocument)
        if (!doc || !doc.writing) return
        
        units.push({
          segmentId: detail.segmentId,
          passage: `${doc.writing.topic || ''}

${doc.writing.passage || ''}

${doc.writing.prompt_requirement || ''}`,
          userAnswer: detail.userAnswer
        })
      })
      
      return units
    })

    // Part IV Translation 数据（支持多个Translation题型）
    const translationUnits = computed(() => {
      const translationDetails = questionDetails.value.filter(d => d.questionType === 'Translation')
      if (translationDetails.length === 0) return []
      
      const units = []
      translationDetails.forEach(detail => {
        if (!detail.questionDocument) return
        
        const doc = parseJsonSafe(detail.questionDocument)
        if (!doc || !doc.translation) return
        
        units.push({
          segmentId: detail.segmentId,
          passage: `${doc.translation.topic || ''}\n\n${doc.translation.passage || ''}`,
          userAnswer: detail.userAnswer
        })
      })
      
      return units
    })

    // Part II Listening 数据
    const listeningUnits = computed(() => {
      const listeningDetails = questionDetails.value.filter(d => 
        d.questionType === 'NewsReport' || d.questionType === 'Conversation' || d.questionType === 'ListeningPassage'
      )
      if (listeningDetails.length === 0) return []
      
      // 按segment_id分组
      const segmentMap = {}
      listeningDetails.forEach(detail => {
        const segmentId = detail.segmentId
        if (!segmentMap[segmentId]) {
          segmentMap[segmentId] = {
            segmentId,
            document: detail.questionDocument,
            questions: []
          }
        }
      })
      
      // 解析每个unit
      const units = []
      Object.values(segmentMap).forEach(segment => {
        const doc = parseJsonSafe(segment.document)
        if (doc) {
          // 为每个问题添加 segmentId
          const questions = (doc.question_and_options || doc.questions || []).map(q => ({
            ...q,
            segmentId: segment.segmentId
          }))
          
          units.push({
            unit_type: doc.unit_type || '',
            listening_content: doc.listening_content || '',
            questions,
            segmentId: segment.segmentId
          })
        }
      })
      return units
    })

    // Section A 数据（支持多个BlankedCloze题型）
    const sectionAUnits = computed(() => {
      const blankedClozeDetails = questionDetails.value.filter(d => d.questionType === 'BlankedCloze')
      if (blankedClozeDetails.length === 0) return []
      
      // 按segment_id分组
      const segmentMap = {}
      blankedClozeDetails.forEach(detail => {
        const segmentId = detail.segmentId
        if (!segmentMap[segmentId]) {
          segmentMap[segmentId] = {
            segmentId,
            document: detail.questionDocument
          }
        }
      })
      
      // 解析每个unit
      const units = []
      Object.keys(segmentMap).sort().forEach(segmentId => {
        const doc = parseJsonSafe(segmentMap[segmentId].document)
        if (!doc) {
          return
        }
        
        let sectionData = doc
        if (doc.reading_comprehension && doc.reading_comprehension.section_a) {
          sectionData = doc.reading_comprehension.section_a
        } else if (doc.section_a) {
          sectionData = doc.section_a
        }
        
        const passage = sectionData.passage || ''
        let blankNumbers = []
        
        // 从 answers 数组中提取题号
        if (Array.isArray(sectionData.answers) && sectionData.answers.length > 0) {
          blankNumbers = sectionData.answers
            .map(item => item.question_number)
            .filter(num => num != null && num !== '')
            .sort((a, b) => {
              const numA = parseInt(a)
              const numB = parseInt(b)
              if (!isNaN(numA) && !isNaN(numB)) {
                return numA - numB
              }
              return String(a).localeCompare(String(b))
            })
        }
        
        
        let rawOptions = sectionData.word_options || []
        let options = []
        if (Array.isArray(rawOptions)) {
          options = rawOptions.map(opt => {
            if (typeof opt === 'object' && opt !== null) {
              return {
                letter: opt.letter || '',
                word: opt.word || ''
              }
            }
            return { letter: '', word: String(opt) }
          }).filter(opt => opt.word)
        }
        
        // 提取标题（如果有）
        const title = sectionData.title || sectionData.passage_mark || ''
        
        units.push({
          segmentId,
          title,
          passage,
          blank_numbers: blankNumbers,
          options
        })
      })
      return units
    })

    // Section B 数据（支持多个Matching题型）
    const sectionBUnits = computed(() => {
      const matchingDetails = questionDetails.value.filter(d => d.questionType === 'Matching')
      if (matchingDetails.length === 0) return []
      
      // 按segment_id分组
      const segmentMap = {}
      matchingDetails.forEach(detail => {
        const segmentId = detail.segmentId
        if (!segmentMap[segmentId]) {
          segmentMap[segmentId] = {
            segmentId,
            document: detail.questionDocument
          }
        }
      })
      
      // 解析每个unit
      const units = []
      Object.keys(segmentMap).sort().forEach(segmentId => {
        const doc = parseJsonSafe(segmentMap[segmentId].document)
        if (!doc) return
        
        let sectionData = doc
        if (doc.reading_comprehension && doc.reading_comprehension.section_b) {
          sectionData = doc.reading_comprehension.section_b
        } else if (doc.section_b) {
          sectionData = doc.section_b
        }
        
        // 提取标题（如果有）
        const title = sectionData.title || sectionData.passage_mark || ''
        
        units.push({
          segmentId,
          title,
          article: sectionData.article || sectionData.paragraphs || [],
          statements: sectionData.statements || sectionData.questions || []
        })
      })
      
      return units
    })

    // Section C 数据
    const sectionCData = computed(() => {
      const sectionCDetails = questionDetails.value.filter(d => d.questionType === 'ReadingPassage')
      if (sectionCDetails.length === 0) return null
      
      // 按segment_id分组
      const segmentMap = {}
      sectionCDetails.forEach(detail => {
        const segmentId = detail.segmentId
        if (!segmentMap[segmentId]) {
          segmentMap[segmentId] = detail.questionDocument
        }
      })
      
      const passages = []
      Object.keys(segmentMap).sort().forEach(segmentId => {
        const doc = parseJsonSafe(segmentMap[segmentId])
        if (doc && doc.passage_content && doc.questions) {
          const questionsWithSegmentId = doc.questions.map(q => ({
            ...q,
            segmentId
          }))
          
          passages.push({
            segmentId,
            passage_mark: doc.passage_mark || 'Passage',
            passage_content: doc.passage_content,
            questions: questionsWithSegmentId
          })
        }
      })
      
      return { passages }
    })

    // ========== 带 segmentId 的方法（用于解决题号重复问题） ==========
    
    const getAnswerByKey = (questionNumber, segmentId = null) => {
      if (segmentId) {
        const uniqueKey = `${segmentId}-${questionNumber}`
        return answerMap.value[uniqueKey]
      }
      return answerMap.value[questionNumber] || answerMap.value[String(questionNumber)] || answerMap.value[Number(questionNumber)]
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

    const optionsOf = (q) => {
      try {
        const std = q && q['选项']
        const preferredOrder = ['A', 'B', 'C', 'D']
        if (std && typeof std === 'object' && !Array.isArray(std)) {
          const arr = []
          preferredOrder.forEach((k) => {
            if (std[k] && String(std[k]).trim()) {
              arr.push({ mark: k, text: String(std[k]).trim() })
            }
          })
          if (!arr.length) {
            Object.keys(std).forEach((k) => {
              const v = std[k]
              if (v && String(v).trim()) {
                arr.push({ mark: String(k).trim(), text: String(v).trim() })
              }
            })
          }
          if (arr.length) return arr
        }
        const arr1 = q && Array.isArray(q.options) ? q.options : []
        if (arr1.length) {
          return arr1.map((o) => ({
            mark: String(o.option_mark || o.mark || '').trim(),
            text: String(o.option_content || o.text || o.content || '').trim(),
          })).filter((o) => o.mark && o.text)
        }
      } catch (e) {}
      return []
    }

    const goBack = () => {
      // 清除临时数据
      sessionStorage.removeItem('tempTestResult')
      // 返回错题本
      router.push('/wrong-questions')
    }

    return {
      loading,
      errorMessage,
      testRecord,
      questionDetails,
      examPaperEn,
      objectiveQuestionsCount,
      writingUnits,
      translationUnits,
      listeningUnits,
      sectionAUnits,
      sectionBUnits,
      sectionCData,
      getCorrectAnswerWithSegment,
      getUserAnswerWithSegment,
      isCorrectAnswerWithSegment,
      getQuestionClassWithSegment,
      getResultClassWithSegment,
      getResultTextWithSegment,
      getOptionClassWithSegment,
      getUserAnswerClassWithSegment,
      optionsOf,
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
  margin-left: 290px;
  width: calc(100% - 290px);
  box-sizing: border-box;
}

.result-wrapper {
  max-width: 880px;
  margin: 0 auto;
  padding: 32px 20px 60px;
  box-sizing: border-box;
}

.header {
  text-align: center;
  margin-bottom: 20px;
  color: #2c3e50;
  font-size: 24px;
  font-weight: 700;
}

/* 本地模式提示 */
.local-mode-tip {
  background: linear-gradient(135deg, #ff6b6b 0%, #ee5a6f 100%);
  color: white;
  padding: 12px 20px;
  border-radius: 8px;
  text-align: center;
  margin-bottom: 20px;
  font-weight: 600;
  box-shadow: 0 4px 12px rgba(255, 107, 107, 0.3);
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
  color: #2c3e50;
  font-size: 16px;
  min-width: 40px;
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

/* Section A 选词填空 */
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
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 12px;
}

.option-chip {
  display: flex;
  gap: 8px;
  align-items: center;
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
}

/* Section B 段落匹配 */
.article-block {
  border: 1px solid #e0e0e0;
  border-radius: 12px;
  padding: 18px;
  background: #f9fbfd;
  margin-bottom: 20px;
}

.paragraph-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.paragraph-item {
  display: flex;
  gap: 12px;
  padding: 12px;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e0e0e0;
}

.paragraph-mark {
  font-weight: 700;
  font-size: 16px;
  color: #2e7d32;
  min-width: 24px;
  flex-shrink: 0;
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
  gap: 12px;
  align-items: flex-start;
  flex-wrap: wrap;
}

.statement-text {
  margin: 0;
  color: #37474f;
  line-height: 1.6;
  flex: 1;
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
}

/* AI提示区域（本地模式） */
.ai-note {
  padding: 16px;
  background: #fff9e6;
  border-radius: 8px;
  border: 2px dashed #ffa726;
}

.ai-note p {
  margin: 0;
  color: #e65100;
  font-size: 14px;
  line-height: 1.6;
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
  .main-content {
    margin-left: 0;
    width: 100%;
  }
  
  .summary-stats {
    flex-direction: column;
    gap: 20px;
  }
  
  .question-row,
  .statement-row {
    flex-direction: column;
  }
  
  .answer-display.inline {
    display: flex;
    margin-left: 0;
    margin-top: 12px;
  }
}
</style>
