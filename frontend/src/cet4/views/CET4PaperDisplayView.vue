<template>
  <div class="display-wrapper">
    <h1>试卷展示</h1>

    <!-- 试卷列表选择区域 -->
    <div class="paper-list-section">
      <h3 class="sticky-title">请选择要展示的试卷</h3>
      <div v-if="loadingPaperList" class="loading-tip">加载试卷列表中...</div>
      <div v-else-if="paperListError" class="error-tip">{{ paperListError }}</div>
      <div v-else class="paper-list">
        <table v-if="examPaperList.length > 0">
          <thead>
            <tr>
              <th>试卷ID</th>
              <th>试卷名称</th>
              <th>试卷描述</th>
              <th>科目</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="paper in examPaperList"
              :key="paper.id"
              :class="{ highlight: selectedExamPaperId === paper.id }"
              @click="selectExamPaper(paper)"
            >
              <td>{{ paper.id }}</td>
              <td>{{ paper.examPaperEnName }}</td>
              <td>{{ paper.examPaperEnDesc || '—' }}</td>
              <td>{{ paper.examPaperEnSubject || '—' }}</td>
            </tr>
          </tbody>
        </table>
        <div v-else class="empty-tip">暂无试卷记录</div>
      </div>
      <div class="refresh-btn-container" v-if="examPaperList.length > 0">
        <button class="refresh-button" @click="loadExamPaperList">
          刷新列表
        </button>
      </div>
    </div>

    <!-- 查询提示 -->
    <div v-if="selectedExamPaperId && loading" class="query-info">
      <p>已选择试卷ID：<strong>{{ selectedExamPaperId }}</strong>，正在加载试卷内容...</p>
    </div>

    <div v-if="errorMessage" class="status error">{{ errorMessage }}</div>

    <!-- Part I · Writing -->
    <div v-if="writingPassage" class="result-card structured" style="margin-top: 16px">
      <h2>Part I · Writing</h2>
      <pre class="passage-text">{{ writingPassage }}</pre>
    </div>

    <!-- Part II · Listening Comprehension -->
    <div v-if="listeningUnitsAB.length" class="result-card structured exam-section">
      <div class="section-header">
        <div>
          <h2>Part II · Listening Comprehension</h2>
          <p class="section-meta">共 {{ listeningUnitsAB.length }} 个单元</p>
        </div>
      </div>

      <div
        v-for="(unit, uIdx) in listeningUnitsAB"
        :key="`unit-ab-${uIdx}`"
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
            :key="`q-ab-${uIdx}-${qIdx}`"
            class="question-item"
          >
            <div class="question-header">
              <span class="question-no">{{ q.question_number || (qIdx + 1) }}</span>
              <p class="question-text">{{ q.question_content || q['题干'] }}</p>
            </div>
            <div class="options-list">
              <label
                v-for="opt in (q.options || optionsOf(q))"
                :key="opt.option_mark || opt.mark"
                class="radio-option"
              >
                <input
                  type="radio"
                  :name="`lq-ab-${uIdx}-${qIdx}`"
                  :value="opt.option_mark || opt.mark"
                  :checked="false"
                  disabled
                />
                <span class="option-label">
                  <strong>{{ opt.option_mark || opt.mark }}.</strong> {{ opt.option_content || opt.text }}
                </span>
              </label>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Section A（选词填空） -->
    <div v-if="sectionA" class="result-card structured exam-section">
      <div class="section-header">
        <div>
          <h2>Section A（选词填空）</h2>
          <p class="section-meta">共 {{ sectionABlankNumbers.length }} 题</p>
        </div>
      </div>

      <div class="section-block passage-block">
        <h3>题干</h3>
        <pre class="passage-text">{{ sectionAPassage }}</pre>
      </div>

      <div
        class="section-block question-block"
        v-if="sectionABlankNumbers.length"
      >
        <h3>题目</h3>
        <div
          v-for="number in sectionABlankNumbers"
          :key="number"
          class="question-row"
        >
          <div class="question-info">
            <span class="question-no">{{ number }}</span>
          </div>
          <select
            class="answer-select"
            v-model="selectedAnswers[number]"
          >
            <option value="">请选择答案</option>
            <option
              v-for="option in sectionAOptions"
              :key="`${number}-${optionKey(option)}`"
              :value="displayOptionMark(option)"
            >
              {{ displayOptionMark(option) }}：{{ displayOptionText(option) }}
            </option>
          </select>
        </div>
      </div>

      <div class="section-block options-block" v-if="sectionAOptions.length">
        <h3>备选词</h3>
        <div class="option-grid">
          <div
            v-for="option in sectionAOptions"
            :key="optionKey(option)"
            class="option-chip"
          >
            <span class="option-mark">{{ displayOptionMark(option) }}</span>
            <span class="option-text">{{ displayOptionText(option) }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Section B 段落匹配 - 支持单个或多个题目 -->
    <div v-if="sectionB" class="result-card structured exam-section">
      <div class="section-header">
        <div>
          <h2>Section B（段落匹配）</h2>
          <p class="section-meta">共 {{ sectionB.statement_count }} 题</p>
        </div>
      </div>

      <!-- 单个题目模式 -->
      <template v-if="!sectionB.isMultiple">
        <div class="section-block article-block">
          <h3>文章</h3>
          <div class="paragraph-list">
            <div
              v-for="paragraph in sectionBArticle"
              :key="paragraph.paragraph_mark"
              class="paragraph-item"
            >
              <span class="paragraph-mark">{{ paragraph.paragraph_mark }}</span>
              <p class="paragraph-content">{{ paragraph.paragraph_content }}</p>
            </div>
          </div>
        </div>

        <div class="section-block statements-block">
          <h3>题目（正确答案对应的段落）</h3>
          <div
            v-for="statement in sectionBStatements"
            :key="statement.question_number"
            class="statement-row"
          >
            <div class="statement-header">
              <span class="question-no">{{ statement.question_number }}</span>
              <p class="statement-text">{{ statement.statement_content }}</p>
            </div>
            <div class="answer-display">
              <span class="answer-label">答案：</span>
              <span class="answer-value">{{ statement.answer }}</span>
            </div>
          </div>
        </div>
      </template>

      <!-- 多个题目模式 - 配对展示 -->
      <template v-else>
        <div
          v-for="(matching, mIdx) in sectionB.matchings"
          :key="`matching-${mIdx}`"
          class="matching-section"
          style="margin-top: 20px; border-top: 2px solid #e0e0e0; padding-top: 20px;"
        >
          <div class="section-block article-block">
            <h3>文章</h3>
            <div class="paragraph-list">
              <div
                v-for="paragraph in matching.article"
                :key="`${mIdx}-${paragraph.paragraph_mark}`"
                class="paragraph-item"
              >
                <span class="paragraph-mark">{{ paragraph.paragraph_mark }}</span>
                <p class="paragraph-content">{{ paragraph.paragraph_content }}</p>
              </div>
            </div>
          </div>

          <div class="section-block statements-block">
            <h3>题目（正确答案对应的段落）</h3>
            <div
              v-for="statement in matching.statements"
              :key="`${mIdx}-${statement.question_number}`"
              class="statement-row"
            >
              <div class="statement-header">
                <span class="question-no">{{ statement.question_number }}</span>
                <p class="statement-text">{{ statement.statement_content }}</p>
              </div>
              <div class="answer-display">
                <span class="answer-label">答案：</span>
                <span class="answer-value">{{ statement.answer }}</span>
              </div>
            </div>
          </div>
        </div>
      </template>
    </div>

    <!-- Section C 篇章阅读 -->
    <div v-if="sectionC" class="result-card structured exam-section">
      <div class="section-header">
        <div>
          <h2>Section C（篇章阅读）</h2>
          <p class="section-meta">共 {{ sectionCPassages.length }} 篇短文</p>
        </div>
      </div>

      <div
        v-for="(passage, pIdx) in sectionCPassages"
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
            :key="question.question_number"
            class="question-item"
          >
            <div class="question-header">
              <span class="question-no">{{ question.question_number }}</span>
              <p class="question-text">{{ question.question_content }}</p>
            </div>
            <div class="options-list">
              <label
                v-for="option in question.options"
                :key="option.option_mark"
                class="radio-option"
              >
                <input
                  type="radio"
                  :name="`q-${question.question_number}`"
                  :value="option.option_mark"
                  :checked="false"
                  disabled
                />
                <span class="option-label">
                  <strong>{{ option.option_mark }}.</strong> {{ option.option_content }}
                </span>
              </label>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Part IV Translation -->
    <div v-if="translationPassage" class="result-card structured" style="margin-top: 24px">
      <h2>Part IV · Translation</h2>
      <pre class="passage-text">{{ translationPassage }}</pre>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { queryExamPaperUnitsCET4 } from '@/cet4/service/CET4paperAnalysisServiceCET4'
import axios from 'axios'

const examPaperId = ref('')
const examPaperName = ref('')
const loading = ref(false)
const errorMessage = ref('')
const units = ref([])
const examPaperList = ref([])
const selectedExamPaperId = ref(null)
const loadingPaperList = ref(false)
const paperListError = ref('')
const selectedAnswers = ref({})
const sectionBAnswers = ref({})

const canQuery = computed(() => {
  return examPaperId.value.trim() || examPaperName.value.trim()
})

// 加载试卷列表
const loadExamPaperList = async () => {
  loadingPaperList.value = true
  paperListError.value = ''
  try {
    const response = await axios.get('http://localhost:8080/api/cet4/exam-paper-en/', {
      withCredentials: true
    })
    if (response.data.success) {
      examPaperList.value = response.data.data || []
    } else {
      paperListError.value = response.data.message || '加载试卷列表失败'
    }
  } catch (error) {
    paperListError.value = error.response?.data?.message || '加载试卷列表失败，请稍后重试'
  } finally {
    loadingPaperList.value = false
  }
}

// 选择试卷
const selectExamPaper = async (paper) => {
  if (selectedExamPaperId.value === paper.id) {
    // 如果已选中，再次点击则取消选择
    selectedExamPaperId.value = null
    examPaperId.value = ''
    examPaperName.value = ''
    units.value = []
    errorMessage.value = ''
  } else {
    // 选中新的试卷
    selectedExamPaperId.value = paper.id
    examPaperId.value = String(paper.id)
    examPaperName.value = paper.examPaperEnName || ''
    
    // 自动查询试卷内容
    await handleQuery()
  }
}

onMounted(() => {
  loadExamPaperList()
})

const parseJsonSafe = (text) => {
  try {
    return JSON.parse(text)
  } catch {
    return null
  }
}

const handleQuery = async () => {
  if (!examPaperId.value.trim()) {
    errorMessage.value = '请先选择试卷'
    return
  }
  
  loading.value = true
  errorMessage.value = ''
  units.value = []
  
  try {
    // 使用examPaperId查询ChromaDB
    const result = await queryExamPaperUnitsCET4(examPaperId.value, examPaperName.value)
    if (result?.success && result?.units) {
      units.value = result.units
      
      errorMessage.value = ''
    } else {
      throw new Error(result?.message || '查询失败')
    }
  } catch (error) {
    errorMessage.value = error.message || '查询试卷单元失败，请稍后重试'
    units.value = []
  } finally {
    loading.value = false
  }
}

// 解析并提取各个部分的数据
const writingPassage = computed(() => {
  const writingUnit = units.value.find(u => {
    const meta = u.metadata || {}
    return meta.question_type === 'Writing' && String(meta.part_id) === '1'
  })
  if (!writingUnit) return ''
  const doc = parseJsonSafe(writingUnit.document)
  // 新格式：直接包含writing对象
  if (doc?.writing?.passage) {
    return doc.writing.passage
  }
  // 兼容旧格式
  return writingUnit.document || ''
})

const translationPassage = computed(() => {
  const translationUnit = units.value.find(u => {
    const meta = u.metadata || {}
    return meta.question_type === 'Translation' && String(meta.part_id) === '4'
  })
  if (!translationUnit) return ''
  const doc = parseJsonSafe(translationUnit.document)
  // 新格式：直接包含translation对象
  if (doc?.translation?.passage) {
    return doc.translation.passage
  }
  // 兼容旧格式
  return translationUnit.document || ''
})

const listeningUnitsAB = computed(() => {
  // Part 2 Section A/B: 查找所有听力单元（新格式中每个unit独立存储）
  const sectionABUnits = units.value.filter(u => {
    const meta = u.metadata || {}
    const qt = meta.question_type || ''
    return (qt === 'NewsReport' || qt === 'Conversation' || qt === 'ListeningPassage') && String(meta.part_id) === '2'
  })
  
  if (sectionABUnits.length === 0) return []
  
  // 按segment_id中的unit索引排序
  sectionABUnits.sort((a, b) => {
    const segmentA = a.metadata?.segment_id || ''
    const segmentB = b.metadata?.segment_id || ''
    return segmentA.localeCompare(segmentB)
  })
  
  // 解析每个unit的document（新格式中每个document就是一个完整的unit对象）
  const allUnits = []
  sectionABUnits.forEach(unit => {
    const obj = parseJsonSafe(unit.document)
    if (obj) {
      // 新格式：document直接是unit对象，包含unit_type、listening_content、question_and_options
      const unit_type = (obj?.unit_type || '').trim()
      const listening_content = (obj?.listening_content || '').trim()
      const questions = Array.isArray(obj?.question_and_options) ? obj.question_and_options : []
      allUnits.push({ unit_type, listening_content, questions })
    }
  })
  
  return allUnits
})

// 移除listeningUnitsC，因为新格式中听力不再区分Section C
const listeningUnitsC = computed(() => {
  return [] // 新格式中所有听力单元都在listeningUnitsAB中
})

const sectionA = computed(() => {
  const blankedClozeUnits = units.value.filter(u => {
    const meta = u.metadata || {}
    return meta.question_type === 'BlankedCloze' && String(meta.part_id) === '3'
  })
  
  if (blankedClozeUnits.length === 0) {
    return null
  }
  
  // 判断是否为强化训练（从第一个unit的metadata中获取exam_paper_en_source）
  const examPaperEnSource = blankedClozeUnits[0]?.metadata?.exam_paper_en_source || ''
  const isIntensiveTrain = examPaperEnSource === 'AIfromself' || examPaperEnSource === 'AIfromWrongBank'
  
  // 只有强化训练且有多个单元时才合并，否则只取第一个
  const unit = blankedClozeUnits[0]
  
  try {
    const doc = parseJsonSafe(unit.document)
    if (!doc) {
      return null
    }
    
    // 新格式：直接包含reading_comprehension.section_a
    let sectionData = doc
    if (doc.reading_comprehension && doc.reading_comprehension.section_a) {
      sectionData = doc.reading_comprehension.section_a
    }
    
    const passage = sectionData.passage || ''
    const blankCount = Number(sectionData.blank_count || 0)
    const startNumber = Number(sectionData.start_number || 26)
    const blankNumbers = sectionData.blank_numbers || []
    
    // 处理options
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
    
    // 优先从answers数组中提取题号（客观题）
    let finalBlankNumbers = []
    if (Array.isArray(sectionData.answers) && sectionData.answers.length > 0) {
      // 从answers数组中提取question_number，并按question_number排序
      finalBlankNumbers = sectionData.answers
        .map(item => item.question_number)
        .filter(num => num != null && num !== '')
        .sort((a, b) => {
          // 数字排序
          const numA = parseInt(a)
          const numB = parseInt(b)
          if (!isNaN(numA) && !isNaN(numB)) {
            return numA - numB
          }
          return String(a).localeCompare(String(b))
        })
    }
    
    // 如果没有从answers中获取到题号，则使用blank_numbers或生成
    if (!finalBlankNumbers.length) {
      finalBlankNumbers = blankNumbers
      if (!finalBlankNumbers.length && blankCount > 0) {
        finalBlankNumbers = Array.from({ length: blankCount }, (_, idx) => String(startNumber + idx))
      }
    }
    
    return {
      passage,
      blank_numbers: finalBlankNumbers,
      blank_count: blankCount,
      start_number: startNumber,
      options: options
    }
  } catch (e) {
    return null
  }
})

const sectionB = computed(() => {
  // 查找所有Matching题目
  const matchingUnits = units.value.filter(u => {
    const meta = u.metadata || {}
    return meta.question_type === 'Matching' && String(meta.part_id) === '3'
  })
  
  if (matchingUnits.length === 0) {
    return null
  }
  
  // 判断是否为强化训练（从第一个unit的metadata中获取exam_paper_en_source）
  const examPaperEnSource = matchingUnits[0]?.metadata?.exam_paper_en_source || ''
  const isIntensiveTrain = examPaperEnSource === 'AIfromself' || examPaperEnSource === 'AIfromWrongBank'
  const hasMultipleMatching = isIntensiveTrain && matchingUnits.length > 1
  
  try {
    if (hasMultipleMatching) {
      // 强化训练多题目模式：每个Matching作为独立的配对展示
      const matchings = []
      
      matchingUnits.forEach((unit, index) => {
        const doc = parseJsonSafe(unit.document)
        if (!doc) {
          return
        }
        
        // 新格式：直接包含reading_comprehension.section_b
        let sectionData = doc
        if (doc.reading_comprehension && doc.reading_comprehension.section_b) {
          sectionData = doc.reading_comprehension.section_b
        }
        
        // 每个Matching作为独立单元，包含文章和题目
        matchings.push({
          matchingIndex: index + 1,  // 第几个Matching
          article: sectionData.article || [],
          statements: sectionData.statements || []
        })
      })
      
      return {
        isMultiple: true,
        matchings,
        statement_count: matchings.reduce((sum, m) => sum + m.statements.length, 0)
      }
    } else {
      // 非强化训练或只有一个单元，使用原有逻辑（只取第一个）
      const unit = matchingUnits[0]
      const doc = parseJsonSafe(unit.document)
      if (!doc) {
        return null
      }
      
      // 新格式：直接包含reading_comprehension.section_b
      let sectionData = doc
      if (doc.reading_comprehension && doc.reading_comprehension.section_b) {
        sectionData = doc.reading_comprehension.section_b
      }
      
      return {
        isMultiple: false,
        article: sectionData.article || [],
        statements: sectionData.statements || [],
        statement_count: (sectionData.statements || []).length
      }
    }
  } catch (e) {
    return null
  }
})

// 新增：标准化篇章阅读问题结构，兼容多种字段
const normalizeReadingQuestions = (raw) => {
  if (!raw) return []
  if (Array.isArray(raw)) {
    return raw.map((q) => {
      const question_number = q.question_number || q.no || q.id || ''
      const question_content = q.question_content || q.question || q.text || ''
      let options = []

      if (Array.isArray(q.options)) {
        options = q.options.map((o) => ({
          option_mark: o.option_mark || o.mark || o.letter || '',
          option_content: o.option_content || o.text || o.content || ''
        })).filter(o => o.option_mark && o.option_content)
      } else if (q['选项'] && typeof q['选项'] === 'object') {
        const preferredOrder = ['A', 'B', 'C', 'D', 'E', 'F']
        preferredOrder.forEach((k) => {
          const v = q['选项'][k]
          if (v && String(v).trim()) {
            options.push({ option_mark: k, option_content: String(v).trim() })
          }
        })
        Object.keys(q['选项']).forEach((k) => {
          if (!options.find(o => o.option_mark === k)) {
            const v = q['选项'][k]
            if (v && String(v).trim()) {
              options.push({ option_mark: String(k).trim(), option_content: String(v).trim() })
            }
          }
        })
      } else if (Array.isArray(q.question_and_options)) {
        options = q.question_and_options.map((o) => ({
          option_mark: o.option_mark || o.mark || o.letter || '',
          option_content: o.option_content || o.text || o.content || ''
        })).filter(o => o.option_mark && o.option_content)
      }

      return { question_number, question_content, options }
    }).filter(q => q.question_number && q.options && q.options.length)
  }
  return []
}

// Section C - 篇章阅读（支持C1和C2）
const sectionC = computed(() => {
  // 查找所有篇章阅读的units（segment_id为C1/C2，question_type=ReadingPassage）
  const sectionCUnits = units.value.filter(u => {
    const meta = u.metadata || {}
    const segmentId = (meta.segment_id || '').toString()
    return meta.question_type === 'ReadingPassage' && 
           String(meta.part_id) === '3' && 
           (segmentId.includes('C1') || segmentId.includes('C2'))
  })
  
  if (sectionCUnits.length === 0) {
    return null
  }
  
  // 按segment_id排序（C1在C2前）
  sectionCUnits.sort((a, b) => {
    const segmentA = a.metadata?.segment_id || ''
    const segmentB = b.metadata?.segment_id || ''
    return String(segmentA).localeCompare(String(segmentB))
  })
  
  const passages = []
  
  sectionCUnits.forEach(unit => {
    try {
      const doc = parseJsonSafe(unit.document)
      if (!doc) return

      // 放宽字段：passage_content / passage / content / text
      const passage_content = (doc.passage_content || doc.passage || doc.content || doc.text || '').toString().trim()
      const passage_mark = (doc.passage_mark || doc.title || doc.topic || 'Passage').toString()

      // 兼容 questions / question_and_options
      const rawQuestions = Array.isArray(doc.questions) ? doc.questions : (Array.isArray(doc.question_and_options) ? doc.question_and_options : [])
      const questions = normalizeReadingQuestions(rawQuestions)

      if (passage_content && questions.length) {
        passages.push({
          passage_mark,
          passage_content,
          question_count: questions.length,
          questions
        })
      }
    } catch (e) {
    }
  })
  
  return passages.length > 0 ? { passages } : null
})

const sectionAPassage = computed(() => sectionA.value?.passage || '[文档中未提及此内容]')
const sectionABlankNumbers = computed(() => sectionA.value?.blank_numbers || [])
const sectionAOptions = computed(() => sectionA.value?.options || [])
const sectionBArticle = computed(() => sectionB.value?.article || [])
const sectionBStatements = computed(() => sectionB.value?.statements || [])
const sectionBParagraphMarks = computed(() => {
  return sectionBArticle.value.map(p => p.paragraph_mark).filter(m => m)
})
const sectionCPassages = computed(() => sectionC.value?.passages || [])

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
const optionKey = (option) => option?.letter || Math.random().toString(36).slice(2)
const displayOptionMark = (option) => option?.letter || ''
const displayOptionText = (option) => option?.word || '[文档中未提及此内容]'
</script>

<style scoped>
.display-wrapper {
  max-width: 880px;
  margin: 0 auto;
  padding: 32px 20px 60px;
  box-sizing: border-box;
}

h1 {
  text-align: center;
  margin-bottom: 28px;
  color: #2c3e50;
}

.query-card {
  background: #ffffff;
  padding: 30px;
  border-radius: 14px;
  box-shadow: 0 12px 32px rgba(46, 125, 50, 0.1);
  text-align: center;
  margin-bottom: 30px;
  display: flex;
  flex-direction: column;
  gap: 20px;
  align-items: center;
}

.query-icon {
  font-size: 48px;
}

.query-tip {
  color: #546e7a;
  margin: 0;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 16px;
  width: 100%;
  max-width: 600px;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 8px;
  text-align: left;
}

.form-field label {
  font-weight: 600;
  color: #2c3e50;
}

.form-field input {
  border: 1px solid #cfd8dc;
  border-radius: 8px;
  padding: 10px 12px;
  font-size: 15px;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.form-field input:focus {
  outline: none;
  border-color: #43a047;
  box-shadow: 0 0 0 2px rgba(67, 160, 71, 0.15);
}

.query-button {
  padding: 12px 32px;
  background: linear-gradient(135deg, #3498db 0%, #2980b9 100%);
  color: #fff;
  border: none;
  border-radius: 999px;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.query-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.query-button:not(:disabled):hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(52, 152, 219, 0.35);
}

.status {
  text-align: center;
  margin: 18px 0;
  font-weight: 500;
}

.error {
  color: #d32f2f;
}

.result-card {
  background: #fff;
  padding: 26px;
  border-radius: 14px;
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.08);
}

.result-card.structured {
  margin-top: 24px;
}

.result-card h2 {
  margin-top: 0;
  color: #2e7d32;
}

pre {
  white-space: pre-wrap;
  word-break: break-word;
  background: #f7f9fb;
  padding: 18px;
  border-radius: 10px;
  max-height: 420px;
  overflow-y: auto;
  color: #37474f;
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

.passage-text {
  white-space: pre-wrap;
  word-break: break-word;
  margin: 0;
  color: #37474f;
  font-size: 15px;
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
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  margin-bottom: 12px;
  background: #fafbfc;
}

.statement-header {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.paragraph-select {
  width: 100%;
  max-width: 200px;
  padding: 10px 12px;
  border: 1px solid #cfd8dc;
  border-radius: 8px;
  font-size: 15px;
  background: #fff;
  color: #37474f;
  cursor: pointer;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
  margin-top: 8px;
}

.paragraph-select:focus {
  outline: none;
  border-color: #43a047;
  box-shadow: 0 0 0 2px rgba(67, 160, 71, 0.15);
}

.question-no {
  font-weight: 600;
  margin-right: 8px;
  color: #2c3e50;
}

.question-block {
  margin-top: 20px;
}

.question-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  margin-bottom: 10px;
  background: #fafbfc;
}

.question-info {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 60px;
}

.answer-select {
  flex: 1;
  border: 1px solid #cfd8dc;
  border-radius: 8px;
  padding: 10px 12px;
  font-size: 15px;
  background: #fff;
  color: #37474f;
  cursor: pointer;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.answer-select:focus {
  outline: none;
  border-color: #43a047;
  box-shadow: 0 0 0 2px rgba(67, 160, 71, 0.15);
}


.statement-text {
  margin: 0;
  color: #37474f;
  line-height: 1.6;
  flex: 1;
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
  color: #2e7d32;
  font-size: 20px;
  margin: 0 0 16px 0;
  font-weight: 700;
}

.passage-content-block {
  border: 1px solid #e0e0e0;
  border-radius: 12px;
  padding: 18px;
  background: #f9fbfd;
  margin-bottom: 24px;
}

.questions-block {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.question-item {
  border: 1px solid #e0e0e0;
  border-radius: 10px;
  padding: 16px;
  background: #fafbfc;
}

.question-header {
  display: flex;
  gap: 12px;
  margin-bottom: 14px;
  align-items: flex-start;
}

.question-text {
  margin: 0;
  color: #37474f;
  line-height: 1.6;
  flex: 1;
  font-weight: 500;
}

.options-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-left: 32px;
}

.radio-option {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px 12px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  background: #fff;
  cursor: default;
}

.radio-option input[type="radio"]:disabled {
  cursor: default;
}

.option-label {
  flex: 1;
  color: #37474f;
  line-height: 1.6;
}

.option-label strong {
  color: #2e7d32;
  margin-right: 6px;
}

/* 试卷列表样式 */
.paper-list-section {
  position: relative;
  width: 100%;
  box-sizing: border-box;
  margin-bottom: 30px;
}

.sticky-title {
  position: sticky;
  top: 0;
  left: 0;
  margin: 0;
  padding: 15px;
  background-color: #f8f9fa;
  border-bottom: 1px solid #dee2e6;
  color: #003366;
  z-index: 20;
  width: 100%;
  box-sizing: border-box;
  font-size: 16px;
  font-weight: 600;
}

.paper-list {
  width: 100%;
  max-height: 400px;
  overflow: auto;
  border: 1px solid #ddd;
  background-color: #f0f8ff;
  border-radius: 4px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.1);
}

.paper-list table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

.paper-list thead tr {
  position: sticky;
  top: 0;
  z-index: 10;
  background-color: #f8f9fa !important;
  background-image: linear-gradient(to bottom, #f8f9fa, #f8f9fa);
}

.paper-list thead th {
  padding: 12px 15px;
  text-align: left;
  color: #003366;
  font-weight: 600;
  border-bottom: 2px solid #dee2e6;
}

.paper-list tbody tr {
  cursor: pointer;
  transition: background-color 0.2s;
}

.paper-list tbody tr td {
  padding: 12px 15px;
  border-bottom: 1px solid #dee2e6;
  color: #333;
}

.paper-list tbody tr.highlight {
  background-color: #e6f7ff !important;
  color: #003366 !important;
}

.paper-list tbody tr:hover:not(.highlight) {
  background-color: #f8f9fa;
}

.paper-list::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

.paper-list::-webkit-scrollbar-track {
  background: #f1f1f1;
}

.paper-list::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 4px;
}

.loading-tip,
.empty-tip,
.error-tip {
  padding: 20px;
  text-align: center;
  color: #546e7a;
  font-size: 14px;
}

.error-tip {
  color: #d32f2f;
}

.refresh-btn-container {
  display: flex;
  justify-content: center;
  width: 100%;
  margin-top: 15px;
}

.refresh-button {
  padding: 8px 20px;
  background-color: #4CAF50;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: background-color 0.2s;
}

.refresh-button:hover {
  background-color: #45a049;
}

.query-info {
  margin: 20px 0;
  padding: 15px;
  background: #e8f5e9;
  border-radius: 8px;
  text-align: center;
  color: #2e7d32;
  font-size: 14px;
}

.query-info strong {
  font-weight: 600;
}

@media (max-width: 600px) {
  .query-card {
    padding: 24px;
  }

  pre {
    max-height: 320px;
  }
  
  .paper-list {
    max-height: 300px;
  }
  
  .paper-list table {
    font-size: 12px;
  }
  
  .paper-list thead th,
  .paper-list tbody tr td {
    padding: 8px 10px;
  }
  
  .sticky-title {
    padding: 12px;
    font-size: 14px;
  }
}
</style>

