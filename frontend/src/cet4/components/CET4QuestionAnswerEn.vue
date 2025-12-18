<template>
  <div class="question-container">
    <SideBarMenu />
    <div class="main-content">
      <div class="question-wrapper">
        <h1 class="header">在线答题 - {{ examPaperName || '加载中...' }}</h1>

        <div v-if="loading" class="loading-tip">加载试卷中...</div>
        <div v-else-if="errorMessage" class="error-tip">{{ errorMessage }}</div>

        <!-- Part I · Writing - 单个题目模式 -->
        <div v-if="writingPassage && !writingPassages.length" class="result-card structured" style="margin-top: 16px">
          <h2>Part I · Writing</h2>
          <pre class="passage-text">{{ writingPassage }}</pre>
          <div class="answer-area">
            <h4>答题区域：</h4>
            <textarea 
              v-model="writingAnswer"
              class="answer-textarea"
              placeholder="请在此处输入你的作文..."
              rows="8"
            ></textarea>
          </div>
        </div>

        <!-- Part I · Writing - 多个题目模式（强化训练） -->
        <div v-if="writingPassages.length > 0" class="result-card structured exam-section" style="margin-top: 16px">
          <div class="section-header">
            <div>
              <h2>Part I · Writing</h2>
              <p class="section-meta">共 {{ writingPassages.length }} 道写作题</p>
            </div>
          </div>

          <div
            v-for="(writing, wIdx) in writingPassages"
            :key="`writing-${wIdx}`"
            class="writing-section"
            style="margin-top: 20px; border-top: 2px solid #e0e0e0; padding-top: 20px;"
          >
            <h3>写作题 {{ writing.index }}</h3>
            <pre class="passage-text">{{ writing.passage }}</pre>
            <div class="answer-area">
              <h4>答题区域：</h4>
              <textarea 
                v-model="writingAnswers[writing.segment_id]"
                class="answer-textarea"
                :placeholder="'请在此处输入第 ' + writing.index + ' 道作文...'"
                rows="8"
              ></textarea>
            </div>
          </div>
        </div>

        <!-- Part II · Listening Comprehension -->
        <div v-if="listeningUnitsAB.length" class="result-card structured exam-section">
          <div class="section-header">
            <div>
              <h2>Part II · Listening Comprehension</h2>
              <p class="section-meta">共 {{ listeningUnitsAB.length }} 个单元</p>
            </div>
            <div class="audio-controls">
              <div v-if="audioServiceStatus === 'checking'" class="service-status checking">
                🔍 检查音频服务中...
              </div>
              <div v-if="audioServiceStatus === 'unavailable'" class="service-status unavailable">
                ⚠️ 音频服务不可用
              </div>
              
              <button 
                v-if="!audioGenerating && !hasAudio && audioServiceStatus === 'available'"
                class="generate-audio-btn"
                @click="generateListeningAudio"
                :disabled="audioGenerating"
              >
                <span>🔊 生成听力音频</span>
              </button>
              <div v-else-if="audioGenerating" class="audio-placeholder">正在生成...</div>
              <div v-if="hasAudio && !audioGenerating" class="audio-player">
                <button
                  type="button"
                  class="play-btn"
                  :disabled="audioGenerating || !hasAudio"
                  @click="toggleAudio"
                >
                  {{ isPlaying ? '⏸️ 暂停' : '▶️ 播放' }}
                </button>
                <div class="audio-progress">
                  <span>{{ formatTime(currentTime) }}</span>
                  <input 
                    type="range" 
                    min="0" 
                    :max="audioDuration" 
                    v-model="currentTime"
                    @input="seekAudio"
                    class="progress-slider"
                    :disabled="audioGenerating || !hasAudio"
                  >
                  <span>{{ formatTime(audioDuration) }}</span>
                </div>
                <button
                  type="button"
                  class="stop-btn"
                  :disabled="audioGenerating || !hasAudio"
                  @click="stopAudio"
                >
                  ⏹️ 停止
                </button>
              </div>
            </div>
          </div>

          <div
            v-for="(unit, uIdx) in listeningUnitsAB"
            :key="`unit-${uIdx}`"
            class="passage-section"
          >
            <div class="passage-header">
              <h3>{{ unit.unit_type || '听力单元' }}</h3>
            </div>

            <!-- 听力原文已隐藏，学生需通过音频作答 -->
            <!-- <div v-if="unit.listening_content" class="passage-content-block">
              <h4>听力内容</h4>
              <pre class="passage-text">{{ unit.listening_content }}</pre>
            </div> -->

            <!-- 显示题目和选项 -->
            <div class="questions-block" v-if="unit.questions.length">
              <div
                v-for="(q, qIdx) in unit.questions"
                :key="`q-${uIdx}-${qIdx}`"
                class="question-item"
              >
                <!-- 题干已隐藏，只显示题号 -->
                <div class="question-header">
                  <span class="question-no">题号 {{ q.question_number || (qIdx + 1) }}</span>
                  <!-- <p class="question-text" v-if="q.question_content">{{ q.question_content }}</p> -->
                </div>
                <div class="options-list">
                  <label
                    v-for="opt in (q.options || optionsOf(q))"
                    :key="opt.option_mark || opt.mark"
                    class="radio-option"
                  >
                    <input
                      type="radio"
                      :name="`lq-${uIdx}-${qIdx}`"
                      :value="opt.option_mark || opt.mark"
                      v-model="listeningAnswers[`${unit.segment_id}-${q.question_number || (qIdx + 1)}`]"
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

        <!-- Section A（选词填空） - 支持单个或多个题目 -->
        <div v-if="sectionA" class="result-card structured exam-section">
          <div class="section-header">
            <div>
              <h2>Section A（选词填空）</h2>
              <p class="section-meta" v-if="!sectionA.isMultiple">共 {{ sectionABlankNumbers.length }} 题</p>
              <p class="section-meta" v-else>共 {{ sectionA.passages.length }} 个段落</p>
            </div>
          </div>

          <!-- 单个题目模式 -->
          <template v-if="!sectionA.isMultiple">
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
                  v-model="sectionAAnswers[`${sectionA.segment_id}-${number}`]"
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
          </template>

          <!-- 多个题目模式 -->
          <template v-else>
            <div
              v-for="(passage, pIdx) in sectionA.passages"
              :key="`passage-${pIdx}`"
              class="passage-section"
              style="margin-top: 20px; border-top: 2px solid #e0e0e0; padding-top: 20px;"
            >
              <div class="section-block passage-block">
                <h3>题干</h3>
                <pre class="passage-text">{{ passage.passage }}</pre>
              </div>

              <div class="section-block question-block" v-if="passage.blank_numbers.length">
                <h3>题目</h3>
                <div
                  v-for="number in passage.blank_numbers"
                  :key="`${pIdx}-${number}`"
                  class="question-row"
                >
                  <div class="question-info">
                    <span class="question-no">{{ number }}</span>
                  </div>
                  <select
                    class="answer-select"
                    v-model="sectionAAnswers[`${passage.segment_id}-${number}`]"
                  >
                    <option value="">请选择答案</option>
                    <option
                      v-for="option in passage.options"
                      :key="`${pIdx}-${number}-${optionKey(option)}`"
                      :value="displayOptionMark(option)"
                    >
                      {{ displayOptionMark(option) }}：{{ displayOptionText(option) }}
                    </option>
                  </select>
                </div>
              </div>

              <div class="section-block options-block" v-if="passage.options.length">
                <h3>备选词</h3>
                <div class="option-grid">
                  <div
                    v-for="option in passage.options"
                    :key="`${pIdx}-${optionKey(option)}`"
                    class="option-chip"
                  >
                    <span class="option-mark">{{ displayOptionMark(option) }}</span>
                    <span class="option-text">{{ displayOptionText(option) }}</span>
                  </div>
                </div>
              </div>
            </div>
          </template>
        </div>

        <!-- Section B 段落匹配 - 支持单个或多个题目 -->
        <div v-if="sectionB" class="result-card structured exam-section">
          <div class="section-header">
            <div>
              <h2>Section B（段落匹配）</h2>
              <p class="section-meta" v-if="!sectionB.isMultiple">共 {{ sectionBStatements.length }} 题</p>
              <p class="section-meta" v-else>共 {{ sectionB.matchings.length }} 组匹配题</p>
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
              <h3>题目（请选择每个陈述句对应的段落）</h3>
              <div
                v-for="statement in sectionBStatements"
                :key="`${statement.segment_id}-${statement.question_number}`"
                class="statement-row"
              >
                <div class="statement-header">
                  <span class="question-no">{{ statement.question_number }}</span>
                  <p class="statement-text">{{ statement.statement_content }}</p>
                </div>
                <select
                  class="paragraph-select"
                  v-model="sectionBAnswers[`${statement.segment_id}-${statement.question_number}`]"
                >
                  <option value="">请选择段落</option>
                  <option
                    v-for="mark in sectionBParagraphMarks"
                    :key="mark"
                    :value="mark"
                  >
                    {{ mark }}
                  </option>
                </select>
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
                <h3>题目（请选择每个陈述句对应的段落）</h3>
                <div
                  v-for="statement in matching.statements"
                  :key="`${statement.segment_id}-${statement.question_number}`"
                  class="statement-row"
                >
                  <div class="statement-header">
                    <span class="question-no">{{ statement.question_number }}</span>
                    <p class="statement-text">{{ statement.statement_content }}</p>
                  </div>
                  <select
                    class="paragraph-select"
                    v-model="sectionBAnswers[`${statement.segment_id}-${statement.question_number}`]"
                  >
                    <option value="">请选择段落</option>
                    <option
                      v-for="paragraph in matching.article"
                      :key="`${mIdx}-opt-${paragraph.paragraph_mark}`"
                      :value="paragraph.paragraph_mark"
                    >
                      {{ paragraph.paragraph_mark }}
                    </option>
                  </select>
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
                      :name="`q-${question.segment_id}-${question.question_number}`"
                      :value="option.option_mark"
                      v-model="sectionCAnswers[`${question.segment_id}-${question.question_number}`]"
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

        <!-- Part IV Translation - 单个题目模式 -->
        <div v-if="translationPassage && !translationPassages.length" class="result-card structured" style="margin-top: 24px">
          <h2>Part IV · Translation</h2>
          <pre class="passage-text">{{ translationPassage }}</pre>
          <div class="answer-area">
            <h4>答题区域：</h4>
            <textarea 
              v-model="translationAnswer"
              class="answer-textarea"
              placeholder="请在此处输入你的翻译..."
              rows="6"
            ></textarea>
          </div>
        </div>

        <!-- Part IV Translation - 多个题目模式（强化训练） -->
        <div v-if="translationPassages.length > 0" class="result-card structured exam-section" style="margin-top: 24px">
          <div class="section-header">
            <div>
              <h2>Part IV · Translation</h2>
              <p class="section-meta">共 {{ translationPassages.length }} 道翻译题</p>
            </div>
          </div>

          <div
            v-for="(translation, tIdx) in translationPassages"
            :key="`translation-${tIdx}`"
            class="translation-section"
            style="margin-top: 20px; border-top: 2px solid #e0e0e0; padding-top: 20px;"
          >
            <h3>翻译题 {{ translation.index }}</h3>
            <pre class="passage-text">{{ translation.passage }}</pre>
            <div class="answer-area">
              <h4>答题区域：</h4>
              <textarea 
                v-model="translationAnswers[translation.segment_id]"
                class="answer-textarea"
                :placeholder="'请在此处输入第 ' + translation.index + ' 道翻译...'"
                rows="6"
              ></textarea>
            </div>
          </div>
        </div>

        <!-- 提交按钮 -->
        <div class="submit-btn-container" v-if="!loading && !errorMessage">
          <!-- 提交进度条 -->
          <el-progress
            v-if="isSubmitting"
            :percentage="Math.floor(submitProgress)"
            :stroke-width="15"
            :text-inside="true"
            status="success"
            style="margin-bottom: 20px; width: 100%"
          />
          <button 
            class="submit-btn" 
            @click="submitAnswers"
            :disabled="isSubmitting"
          >
            {{ isSubmitting ? '正在批改...' : '提交试卷' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted, computed, watch, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox, ElProgress } from 'element-plus'
import SideBarMenu from '@/common/components/SideBarMenu.vue'
import { queryExamPaperUnitsCET4 } from '@/cet4/service/CET4paperAnalysisServiceCET4'
import axios from 'axios'

export default {
  name: 'QuestionAnswerEn',
  components: {
    SideBarMenu,
    ElProgress
  },
  props: {
    examPaperEnId: {
      type: [String, Number],
      default: ''
    }
  },
  setup(props) {
    const router = useRouter()
    const route = useRoute()
    // 不再使用 require，改为使用 useRoute()，并从 query 读取来源
    const examPaperEnSource = ref(route.query.exam_paper_en_source)
    const examPaperId = ref('')
    const examPaperName = ref('')
    const loading = ref(false)
    const errorMessage = ref('')
    const units = ref([])

    // 答案数据
    const writingAnswer = ref('')  // 单个写作题答案
    const writingAnswers = ref({})  // 多个写作题答案（segment_id: answer）
    const translationAnswer = ref('')  // 单个翻译题答案
    const translationAnswers = ref({})  // 多个翻译题答案（segment_id: answer）
    const listeningAnswers = ref({})
    const sectionAAnswers = ref({})
    const sectionBAnswers = ref({})
    const sectionCAnswers = ref({})

    // 新增：用于存储标准答案
    const correctAnswers = ref({})

    // 提交进度条相关状态
    const isSubmitting = ref(false)
    const submitProgress = ref(0)
    const submitProgressInterval = ref(null)

    // 音频相关状态
    const audioGenerating = ref(false)
    const hasAudio = ref(false)
    const audioUrl = ref('')
    const audio = ref(null)
    const isPlaying = ref(false)
    const currentTime = ref(0)
    const audioDuration = ref(0)
    const audioServiceStatus = ref('checking') // checking, available, unavailable
    // Java后端音频服务接口（会自动调用Coze和Python服务）
    const JAVA_AUDIO_API_BASE = '/api/audio'

    const disposeAudio = () => {
      if (!audio.value) return
      try {
        audio.value.pause()
        audio.value.currentTime = 0
        // 强制释放资源，避免出现“暂停后又自动开始播放”的竞态
        audio.value.src = ''
        if (typeof audio.value.load === 'function') {
          audio.value.load()
        }
      } catch (e) {
      } finally {
        audio.value = null
        isPlaying.value = false
        currentTime.value = 0
        audioDuration.value = 0
      }
    }

    const parseJsonSafe = (text) => {
      try {
        return JSON.parse(text)
      } catch {
        return null
      }
    }

    // 新增：标准化篇章阅读问题结构（兼容 questions / question_and_options / 选项 等多种格式）
    const normalizeReadingQuestions = (raw) => {
      if (!raw) return []
      // 如果是 question_and_options（如听力的结构），转换为阅读统一结构
      if (Array.isArray(raw)) {
        return raw.map((q) => {
          const question_number = q.question_number || q.no || q.id || ''
          const question_content = q.question_content || q.question || q.text || ''
          let options = []

          // 多种选项来源：q.options 数组、q.选项 对象、或 question_and_options 内部结构
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
            // 若未覆盖全部，则补齐其它键
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

    onMounted(async () => {
      // 检查音频服务状态
      await checkAudioServiceStatus()
      
      if (!props.examPaperEnId) {
        errorMessage.value = '试卷ID缺失'
        return
      }

      loading.value = true
      errorMessage.value = ''
      
      try {
        // 先检查是否是强化训练临时模式（未保存的试卷）
        const intensiveTrainData = sessionStorage.getItem('intensiveTrainQuestions')
        const isTempMode = props.examPaperEnId === 'temp' || route.query.temp_mode === 'true'
        
        if (isTempMode && intensiveTrainData) {
          // 使用 sessionStorage 中的临时数据
          
          const trainData = JSON.parse(intensiveTrainData)
          
          // 将后端返回的格式转换为 ChromaDB 的格式
          // 后端格式: { questionType, segmentIdSelf, documentJson, examPaperEnSource }
          // ChromaDB格式: { document, metadata: { question_type, segment_id, part_id, exam_paper_en_source } }
          const convertedUnits = (trainData.questions || []).map((q, index) => {
            // 解析 documentJson
            let parsedDoc = null
            try {
              parsedDoc = typeof q.documentJson === 'string' ? JSON.parse(q.documentJson) : q.documentJson
            } catch (e) {
              parsedDoc = {}
            }
            
            // 确定 part_id
            let partId = ''
            const qType = q.questionType
            if (qType === 'Writing') {
              partId = '1'
            } else if (qType === 'Translation') {
              partId = '4'
            } else if (qType === 'NewsReport' || qType === 'Conversation' || qType === 'ListeningPassage') {
              partId = '2'
            } else if (qType === 'BlankedCloze' || qType === 'Matching' || qType === 'ReadingComprehension') {
              partId = '3'
            }
            
            return {
              document: q.documentJson,
              metadata: {
                question_type: q.questionType,
                segment_id: q.segmentIdSelf || `${index + 1}${q.questionType}`,
                part_id: partId,
                exam_paper_en_source: q.examPaperEnSource || trainData.examPaperEnSource,
                exam_paper_en_id: 'temp',
                exam_paper_en_name: '强化训练试卷（未保存）'
              }
            }
          })
          
          // 将转换后的数据赋值给 units
          units.value = convertedUnits
          
          // 设置试卷基本信息
          examPaperEnSource.value = trainData.examPaperEnSource || 'AIfromself'
          examPaperId.value = 'temp'
          examPaperName.value = '强化训练试卷（未保存）'
          // 清除临时数据（可选，如果想保留以便刷新页面后仍可用，可以不清除）
          // sessionStorage.removeItem('intensiveTrainQuestions')
        } else {
          // 正常模式：使用examPaperId查询ChromaDB
          const result = await queryExamPaperUnitsCET4(props.examPaperEnId, '')
          
          if (result?.success && result?.units) {
            units.value = result.units
            
            // 设置试卷基本信息
            const firstUnit = result.units[0]
            if (firstUnit && firstUnit.metadata) {
              examPaperId.value = firstUnit.metadata.exam_paper_en_id
              examPaperName.value = firstUnit.metadata.exam_paper_en_name || '试卷'
              // 从 metadata 中获取 exam_paper_en_source，如果 URL 参数中没有的话
              if (!examPaperEnSource.value && firstUnit.metadata.exam_paper_en_source) {
                examPaperEnSource.value = firstUnit.metadata.exam_paper_en_source
              }
            }
          } else {
            throw new Error(result?.message || '查询失败')
          }
        }
      } catch (error) {
        errorMessage.value = error.message || '查询试卷单元失败，请稍后重试'
      } finally {
        loading.value = false
      }
    })

    // 解析并提取各个部分的数据（复用PaperDisplayView的逻辑）
    const writingPassage = ref('')  // 保留，用于单个写作题
    const writingPassages = ref([])  // 新增：支持多个写作题
    const translationPassage = ref('')
    const translationPassages = ref([])  // 新增：支持多个翻译题
    const listeningUnitsAB = ref([])
    const listeningUnitsSectionA = ref([])  // 新增：只存储Section A的听力内容
    const sectionA = ref(null)
    const sectionB = ref(null)
    const sectionC = ref(null)

    // 监听units变化，解析试卷内容
    const parseUnits = () => {
      if (!units.value.length) return

      // 判断是否为强化训练场景（需要在最开始定义，后续多处使用）
      const isIntensiveTrain = examPaperEnSource.value === 'AIfromself' || examPaperEnSource.value === 'AIfromWrongBank'

      // Part I Writing - 支持多个写作题
      const writingUnits = units.value.filter(u => {
        const meta = u.metadata || {}
        return meta.question_type === 'Writing' && String(meta.part_id) === '1'
      })
      
      // 判断是否为强化训练场景
      const hasMultipleWriting = isIntensiveTrain && writingUnits.length > 1
      
      if (writingUnits.length > 0) {
        if (hasMultipleWriting) {
          // 多个写作题模式
          const passages = []
          writingUnits.forEach((unit, index) => {
            const doc = parseJsonSafe(unit.document)
            const segment_id = unit.metadata?.segment_id || `writing-${index}`
            let passage = ''
            
            if (doc?.writing?.passage) {
              passage = doc.writing.passage
            } else {
              passage = unit.document || ''
            }
            
            passages.push({
              segment_id,
              passage,
              index: index + 1
            })
          })
          writingPassages.value = passages
          writingPassage.value = ''  // 清空单题模式的数据
        } else {
          // 单个写作题模式（兼容旧逻辑）
          const writingUnit = writingUnits[0]
          const doc = parseJsonSafe(writingUnit.document)
          if (doc?.writing?.passage) {
            writingPassage.value = doc.writing.passage
          } else {
            writingPassage.value = writingUnit.document || ''
          }
          writingPassages.value = []  // 清空多题模式的数据
        }
      }

      // Part IV Translation - 支持多个翻译题
      const translationUnits = units.value.filter(u => {
        const meta = u.metadata || {}
        return meta.question_type === 'Translation' && String(meta.part_id) === '4'
      })
      
      const hasMultipleTranslation = isIntensiveTrain && translationUnits.length > 1
      
      if (translationUnits.length > 0) {
        if (hasMultipleTranslation) {
          // 多个翻译题模式
          const passages = []
          translationUnits.forEach((unit, index) => {
            const doc = parseJsonSafe(unit.document)
            const segment_id = unit.metadata?.segment_id || `translation-${index}`
            let passage = ''
            
            if (doc?.translation?.passage) {
              passage = doc.translation.passage
            } else {
              passage = unit.document || ''
            }
            
            passages.push({
              segment_id,
              passage,
              index: index + 1
            })
          })
          translationPassages.value = passages
          translationPassage.value = ''  // 清空单题模式的数据
        } else {
          // 单个翻译题模式（兼容旧逻辑）
          const translationUnit = translationUnits[0]
          const doc = parseJsonSafe(translationUnit.document)
          if (doc?.translation?.passage) {
            translationPassage.value = doc.translation.passage
          } else {
            translationPassage.value = translationUnit.document || ''
          }
          translationPassages.value = []  // 清空多题模式的数据
        }
      }

      // Part II Listening
      const sectionABUnits = units.value.filter(u => {
        const meta = u.metadata || {}
        const qt = meta.question_type || ''
        return (qt === 'NewsReport' || qt === 'Conversation' || qt === 'ListeningPassage') && String(meta.part_id) === '2'
      })
      
      if (sectionABUnits.length > 0) {
        // 按照CET4听力标准顺序排序：News Report (Section A) → Conversation (Section B) → Passage (Section C)
        sectionABUnits.sort((a, b) => {
          const segmentA = a.metadata?.segment_id || ''
          const segmentB = b.metadata?.segment_id || ''
          
          // 提取section类型（根据unit_type或segment_id判断）
          const getSectionOrder = (segmentId) => {
            const lowerSegment = segmentId.toLowerCase()
            // News Report 优先级最高
            if (lowerSegment.includes('news')) return 1
            // Conversation 第二
            if (lowerSegment.includes('conversation')) return 2
            // Passage 第三
            if (lowerSegment.includes('passage')) return 3
            // 未知类型放最后
            return 4
          }
          
          const orderA = getSectionOrder(segmentA)
          const orderB = getSectionOrder(segmentB)
          
          // 先按section类型排序
          if (orderA !== orderB) {
            return orderA - orderB
          }
          
          // 同section内按segment_id排序
          return segmentA.localeCompare(segmentB)
        })
        
        const allUnits = []
        const sectionAUnitsOnly = []  // 新增：只保存Section A的单元
        
        sectionABUnits.forEach(unit => {
          const obj = parseJsonSafe(unit.document)
          if (obj) {
            const unit_type = (obj?.unit_type || '').trim()
            const listening_content = (obj?.listening_content || '').trim()
            const questions = Array.isArray(obj?.question_and_options) ? obj.question_and_options : []
            const segment_id = unit.metadata?.segment_id || ''  // 获取segment_id
            const unitData = { unit_type, listening_content, questions, segment_id }
            
            allUnits.push(unitData)

            // 提取听力题答案
            if (Array.isArray(questions)) {
              questions.forEach(q => {
                if (q.question_number && q.answer) {
                  correctAnswers.value[q.question_number] = q.answer
                }
              })
            }
            
            // 判断是否为Section A (News Report)
            const segmentId = (unit.metadata?.segment_id || '').toLowerCase()
            if (segmentId.includes('news')) {
              sectionAUnitsOnly.push(unitData)
            }
          }
        })
        
        listeningUnitsAB.value = allUnits
        listeningUnitsSectionA.value = sectionAUnitsOnly  // 新增：设置Section A数据
      }

      // Section A - 选词填空（支持多个BlankedCloze题目）
      const sectionAUnits = units.value.filter(u => {
        const meta = u.metadata || {}
        return meta.question_type === 'BlankedCloze' && String(meta.part_id) === '3'
      })
      
      // 修改：只在强化训练场景下才处理多个BlankedCloze题目
      if (sectionAUnits.length > 0) {
        try {
          // 如果是强化训练且有多个单元，则合并处理
          if (isIntensiveTrain && sectionAUnits.length > 1) {
            const allPassages = []
            
            sectionAUnits.forEach((sectionAUnit, index) => {
              const segment_id = sectionAUnit.metadata?.segment_id || `sectionA-${index}`
              const doc = parseJsonSafe(sectionAUnit.document)
              if (doc) {
                let sectionData = doc
                if (doc.reading_comprehension && doc.reading_comprehension.section_a) {
                  sectionData = doc.reading_comprehension.section_a
                }

                // 提取选词填空答案（使用 segment_id-question_number 作为键）
                if (Array.isArray(sectionData.answers)) {
                  sectionData.answers.forEach(ans => {
                    if (ans.question_number && ans.answer) {
                      const answerKey = `${segment_id}-${ans.question_number}`
                      correctAnswers.value[answerKey] = ans.answer
                    }
                  })
                }
                
                const passage = sectionData.passage || ''
                let finalBlankNumbers = []
                
                if (Array.isArray(sectionData.answers) && sectionData.answers.length > 0) {
                  finalBlankNumbers = sectionData.answers
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
                
                allPassages.push({
                  segment_id,  // 添加 segment_id
                  passage,
                  blank_numbers: finalBlankNumbers,
                  options: options,
                  passageIndex: index + 1  // 标记是第几个passage
                })
              }
            })
            
            sectionA.value = {
              passages: allPassages,
              isMultiple: true
            }
          } else {
            // 非强化训练或只有一个单元，使用原有逻辑（只取第一个）
            const sectionAUnit = sectionAUnits[0]
            const segment_id = sectionAUnit.metadata?.segment_id || 'sectionA-0'
            const doc = parseJsonSafe(sectionAUnit.document)
            if (doc) {
              let sectionData = doc
              if (doc.reading_comprehension && doc.reading_comprehension.section_a) {
                sectionData = doc.reading_comprehension.section_a
              }

              // 提取选词填空答案（使用 segment_id-question_number 作为键）
              if (Array.isArray(sectionData.answers)) {
                sectionData.answers.forEach(ans => {
                  if (ans.question_number && ans.answer) {
                    const answerKey = `${segment_id}-${ans.question_number}`
                    correctAnswers.value[answerKey] = ans.answer
                  }
                })
              }
              
              const passage = sectionData.passage || ''
              let finalBlankNumbers = []
              
              if (Array.isArray(sectionData.answers) && sectionData.answers.length > 0) {
                finalBlankNumbers = sectionData.answers
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
              
              sectionA.value = {
                segment_id,  // 添加 segment_id
                passage,
                blank_numbers: finalBlankNumbers,
                options: options
              }
            }
          }
        } catch (e) {
        }
      }

      // Section B - 段落匹配（支持多个Matching题目）
      const sectionBUnits = units.value.filter(u => {
        const meta = u.metadata || {}
        return meta.question_type === 'Matching' && String(meta.part_id) === '3'
      })
      
      // 判断是否为强化训练场景（只有强化训练才可能有多个Matching）
      const hasMultipleMatching = isIntensiveTrain && sectionBUnits.length > 1
      
      // 修改：只在强化训练场景下合并多个Matching题目
      if (sectionBUnits.length > 0) {
        try {
          if (hasMultipleMatching) {
            // 强化训练多题目模式：每个Matching作为独立的配对展示
            const matchings = []
            
            sectionBUnits.forEach((sectionBUnit, index) => {
              const segment_id = sectionBUnit.metadata?.segment_id || `sectionB-${index}`
              const doc = parseJsonSafe(sectionBUnit.document)
              if (doc) {
                let sectionData = doc
                if (doc.reading_comprehension && doc.reading_comprehension.section_b) {
                  sectionData = doc.reading_comprehension.section_b
                }

                // 提取段落匹配答案（使用 segment_id-question_number 作为键）
                if (Array.isArray(sectionData.statements)) {
                  sectionData.statements.forEach(stmt => {
                    if (stmt.question_number && stmt.answer) {
                      const answerKey = `${segment_id}-${stmt.question_number}`
                      correctAnswers.value[answerKey] = stmt.answer
                    }
                  })
                }
                
                // 添加 segment_id 到每个 statement
                const statementsWithSegmentId = (sectionData.statements || []).map(stmt => ({
                  ...stmt,
                  segment_id
                }))
                
                // 每个Matching作为独立单元，包含文章和题目
                matchings.push({
                  segment_id,
                  matchingIndex: index + 1,  // 第几个Matching
                  article: sectionData.article || [],
                  statements: statementsWithSegmentId
                })
              }
            })
            
            sectionB.value = {
              isMultiple: true,
              matchings  // 配对数组
            }
          } else {
            // 非强化训练或只有一个单元，使用原有逻辑（只取第一个）
            const sectionBUnit = sectionBUnits[0]
            const segment_id = sectionBUnit.metadata?.segment_id || 'sectionB-0'
            const doc = parseJsonSafe(sectionBUnit.document)
            if (doc) {
              let sectionData = doc
              if (doc.reading_comprehension && doc.reading_comprehension.section_b) {
                sectionData = doc.reading_comprehension.section_b
              }

              // 提取段落匹配答案（使用 segment_id-question_number 作为键）
              if (Array.isArray(sectionData.statements)) {
                sectionData.statements.forEach(stmt => {
                  if (stmt.question_number && stmt.answer) {
                    const answerKey = `${segment_id}-${stmt.question_number}`
                    correctAnswers.value[answerKey] = stmt.answer
                  }
                })
              }
              
              // 添加 segment_id 到每个 statement
              const statementsWithSegmentId = (sectionData.statements || []).map(stmt => ({
                ...stmt,
                segment_id
              }))
              
              sectionB.value = {
                isMultiple: false,
                article: sectionData.article || [],
                statements: statementsWithSegmentId
              }
            }
          }
        } catch (e) {
        }
      }

      // 检查所有Part 3的数据
      const part3Units = units.value.filter(u => {
        const meta = u.metadata || {}
        return String(meta.part_id) === '3'
      })

      // Section C - 放宽过滤
      const sectionCUnits = part3Units.filter(u => {
        const meta = u.metadata || {}
        const segmentId = (meta.segment_id || '').toString()
        const qt = (meta.question_type || '').toString()
        const isReadingType = qt === 'ReadingPassage' || qt === '篇章阅读'
        const isC1C2 = segmentId.includes('C1') || segmentId.includes('C2')
        const isReadingPassageSeg = segmentId.toLowerCase().includes('readingpassage')
        return isReadingType && (isC1C2 || isReadingPassageSeg)
      })
      
      if (sectionCUnits.length > 0) {
        // 按segment_id排序（C1在C2前）
        sectionCUnits.sort((a, b) => {
          const segmentA = a.metadata?.segment_id || ''
          const segmentB = b.metadata?.segment_id || ''
          return String(segmentA).localeCompare(String(segmentB))
        })
        
        const passages = []
        
        sectionCUnits.forEach(unit => {
          try {
            const segment_id = unit.metadata?.segment_id || ''
            const doc = parseJsonSafe(unit.document)
            if (!doc) return

            // 兼容多种字段：passage_content / passage / content / text
            const passage_content = (doc.passage_content || doc.passage || doc.content || doc.text || '').toString().trim()
            // passage 标记：passage_mark / title / topic / 默认值
            const passage_mark = (doc.passage_mark || doc.title || doc.topic || 'Passage').toString()

            // 兼容 questions / question_and_options
            const rawQuestions = Array.isArray(doc.questions) ? doc.questions : (Array.isArray(doc.question_and_options) ? doc.question_and_options : [])
            const questions = normalizeReadingQuestions(rawQuestions)

            if (passage_content && questions.length) {
              // 为每个问题添加 segment_id
              const questionsWithSegmentId = questions.map(q => ({
                ...q,
                segment_id
              }))
              
              passages.push({
                segment_id,  // 添加 segment_id
                passage_mark,
                passage_content,
                question_count: questions.length,
                questions: questionsWithSegmentId
              })

              // 提取篇章阅读答案（使用 segment_id-question_number 作为键）
              rawQuestions.forEach((q) => {
                const qNum = q.question_number || q.no || q.id
                const ans = q.answer || q.correct || q.key
                if (qNum && ans && segment_id) {
                  const answerKey = `${segment_id}-${qNum}`
                  correctAnswers.value[answerKey] = ans
                }
              })
            }
          } catch (e) {
          }
        })

        if (passages.length) {
          sectionC.value = { passages }
        }
      }
    }

    // 监听units变化
    watch(units, () => {
      if (units.value.length > 0) {
        parseUnits()
      }
    }, { immediate: true })

    // 计算属性（复用PaperDisplayView的逻辑）
    const sectionAPassage = computed(() => sectionA.value?.passage || '')
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

    // 检查音频服务状态（直接返回可用，不再调用健康检查端点）
    const checkAudioServiceStatus = async () => {
      audioServiceStatus.value = 'available'
      return true
    }

    // 生成听力音频（调用Java后端API，由Java后端调用Coze生成标准朗读内容，再调用Python服务合成音频）
    const generateListeningAudio = async () => {
      // 如果正在生成音频，禁止重复触发
      if (audioGenerating.value) return
      
      // 使用所有听力内容（包括News Report、Conversation、Passage）
      if (listeningUnitsAB.value.length === 0) {
        ElMessage.warning('没有听力内容')
        return
      }

      // 先进入“生成中”状态并清理旧音频，避免检查服务期间乱点触发播放旧音频
      audioGenerating.value = true

      disposeAudio()
      audioUrl.value = ''
      hasAudio.value = false
      isPlaying.value = false
      currentTime.value = 0

      const isServiceAvailable = await checkAudioServiceStatus()
      
      if (!isServiceAvailable) {
        ElMessage.error('音频服务不可用，请确保音频合成服务正在运行')
        audioGenerating.value = false
        return
      }
      
      try {
        // 调用Java后端接口：/api/audio/generate-listening-full
        // Java后端会：1) 调用Coze生成标准朗读脚本，2) 调用Python服务合成音频，3) 返回音频URL
        const url = `${JAVA_AUDIO_API_BASE}/generate-listening-full`

        // 构造完整的听力单元数据（包含所有字段，让Java后端可以调用Coze）
        const payloadSegments = listeningUnitsAB.value.map(u => {
          return {
            unit_type: u.unit_type || '',
            listening_content: u.listening_content || u.listeningContent || u.content || '',
            questions: u.questions || [],
            segment_id: u.segment_id || u.id
          }
        }).filter(s => s.listening_content && s.listening_content.trim().length > 0)

        if (!payloadSegments.length) {
          ElMessage.error('未能从试卷中提取到可合成的听力文本')
          audioGenerating.value = false
          return
        }

        const response = await axios.post(url, {
          segments: payloadSegments,
          exam_paper_en_source: examPaperEnSource.value
        }, { 
          withCredentials: true, 
          timeout: 30 * 60 * 1000  // 30分钟超时，覆盖长时音频生成
        })
        
        if (response.data.success) {
          // Java后端返回的audio_url已经是完整URL
          audioUrl.value = response.data.audio_url
          hasAudio.value = true
          ElMessage.success('听力音频生成成功！')
         } else {
           throw new Error(response.data.error || '音频生成失败')
         }
       } catch (error) {
        const serverMsg = error.response?.data?.message || error.response?.data?.error
        ElMessage.error('音频生成失败：' + (serverMsg || error.message))
       } finally {
         audioGenerating.value = false
       }
    }

    // 提交进度条控制函数
    const startSubmitProgress = () => {
      submitProgress.value = 0
      const totalDurationSeconds = 30 // 30秒
      const step = 100 / totalDurationSeconds
      submitProgressInterval.value = setInterval(() => {
        submitProgress.value = Math.min(submitProgress.value + step, 99) // 预留1%由stopProgress收尾
      }, 1000)
    }

    const stopSubmitProgress = () => {
      clearInterval(submitProgressInterval.value)
      submitProgress.value = 100
      setTimeout(() => {
        submitProgress.value = 0
        isSubmitting.value = false
      }, 500)
    }

    const submitAnswers = async () => {
      // 构建完整的答案对象
      const allAnswers = {
        writing: {},
        translation: {},
        listening: listeningAnswers.value,
        sectionA: sectionAAnswers.value,
        sectionB: sectionBAnswers.value,
        sectionC: sectionCAnswers.value
      }

      // 处理写作答案：支持单题和多题模式
      if (writingPassages.value.length > 0) {
        // 多题模式：使用 segment_id 作为键
        allAnswers.writing = writingAnswers.value
      } else {
        // 单题模式：使用固定的 answer 键
        allAnswers.writing = { answer: writingAnswer.value }
      }

      // 处理翻译答案：支持单题和多题模式
      if (translationPassages.value.length > 0) {
        // 多题模式：使用 segment_id 作为键
        allAnswers.translation = translationAnswers.value
      } else {
        // 单题模式：使用固定的 answer 键
        allAnswers.translation = { answer: translationAnswer.value }
      }

      // 计算客观题总数和已答题数，但不强制要求全部作答
      const totalObjectiveQuestions = 
        Object.keys(listeningAnswers.value).length +
        Object.keys(sectionAAnswers.value).length +
        Object.keys(sectionBAnswers.value).length +
        Object.keys(sectionCAnswers.value).length

      const answeredCount = 
        Object.values(listeningAnswers.value).filter(v => v && v.trim() !== '').length +
        Object.values(sectionAAnswers.value).filter(v => v && v.trim() !== '').length +
        Object.values(sectionBAnswers.value).filter(v => v && v.trim() !== '').length +
        Object.values(sectionCAnswers.value).filter(v => v && v.trim() !== '').length

      // 提醒用户还有题目未作答，但允许提交
      if (answeredCount < totalObjectiveQuestions) {
        const unansweredCount = totalObjectiveQuestions - answeredCount
        const confirmMessage = `还有 ${unansweredCount} 道客观题未作答，未答题将视为错误。确定要提交吗？`
        
        try {
          await ElMessageBox.confirm(confirmMessage, '确认提交', {
            confirmButtonText: '确定提交',
            cancelButtonText: '继续答题',
            type: 'warning'
          })
        } catch {
          return // 用户选择继续答题，不提交
        }
      }

      // 开始提交，显示进度条
      isSubmitting.value = true
      startSubmitProgress()

      try {
        // 调用后端API提交答案
        const response = await axios.post('http://localhost:8080/api/exam-paper/submit-answer-en', {
          examPaperEnId: props.examPaperEnId,
          answers: allAnswers,
          correctAnswers: correctAnswers.value,
          exam_paper_en_source: examPaperEnSource.value
        }, {
          withCredentials: true,
          timeout: 30 * 60 * 1000 // 30分钟，提交和判分可能耗时较长
        })
        
        // 停止进度条
        stopSubmitProgress()

        if (response.data.success) {
          ElMessage.success('答案提交成功！')
          
          // 清除强化训练的临时数据（如果存在）
          sessionStorage.removeItem('intensiveTrainQuestions')
          
          // 跳转到结果页面
          const testEnId = response.data.testEnId
          router.push(`/test-result-en/${testEnId}`)
        } else {
          ElMessage.error('提交失败：' + response.data.message)
        }
        
      } catch (error) {
        // 停止进度条
        stopSubmitProgress()
        ElMessage.error('提交答案失败，请稍后重试')
      }
    }

    // 组件卸载时清理定时器和音频
    onUnmounted(() => {
      if (submitProgressInterval.value) {
        clearInterval(submitProgressInterval.value)
      }
      // 停止并清理音频，避免页面关闭后声音仍在播放
      disposeAudio()
    })

    // 补充缺失的音频控制函数
    const initAudio = () => {
      // 如果正在生成音频，禁止初始化
      if (audioGenerating.value) return
      if (audioUrl.value && !audio.value) {
        audio.value = new Audio(audioUrl.value)
        audio.value.addEventListener('timeupdate', () => {
          if (!audioGenerating.value) {
            currentTime.value = audio.value.currentTime
          }
        })
        audio.value.addEventListener('loadedmetadata', () => {
          audioDuration.value = audio.value.duration
        })
        audio.value.addEventListener('ended', () => {
          isPlaying.value = false
          currentTime.value = 0
          // 播放结束后重置音频时间，便于下次从头播放
          if (audio.value) {
            audio.value.currentTime = 0
          }
        })
      }
    }
    const playAudio = () => {
      // 生成中禁止播放
      if (audioGenerating.value) return
      // 没有音频URL时禁止播放
      if (!audioUrl.value || !hasAudio.value) return
      if (!audio.value) initAudio()
      if (audio.value) {
        // 如果音频已播放完毕（currentTime接近duration），从头开始
        if (audio.value.currentTime >= audio.value.duration - 0.1) {
          audio.value.currentTime = 0
          currentTime.value = 0
        }
        audio.value.play()
        isPlaying.value = true
      }
    }
    const pauseAudio = () => {
      if (audioGenerating.value) return
      if (audio.value) {
        audio.value.pause()
        isPlaying.value = false
      }
    }
    const stopAudio = () => {
      if (audioGenerating.value) return
      if (audio.value) {
        audio.value.pause()
        audio.value.currentTime = 0
        isPlaying.value = false
        currentTime.value = 0
      }
    }
    const toggleAudio = () => {
      // 生成中禁止操作
      if (audioGenerating.value) return
      // 没有音频时禁止操作
      if (!audioUrl.value || !hasAudio.value) return
      isPlaying.value ? pauseAudio() : playAudio()
    }
    const seekAudio = (event) => {
      // 生成中禁止拖动
      if (audioGenerating.value) return
      // 没有音频时禁止操作
      if (!audio.value) return
      const t = parseFloat(event.target.value)
      audio.value.currentTime = t
      currentTime.value = t
    }
    const formatTime = (s) => { if(!s||isNaN(s)) return '00:00'; const m=Math.floor(s/60), sec=Math.floor(s%60); return `${m.toString().padStart(2,'0')}:${sec.toString().padStart(2,'0')}` }

    return {
      examPaperName,
      loading,
      errorMessage,
      writingPassage,
      writingPassages,  // 新增：多个写作题
      writingAnswers,   // 新增：多个写作题答案
      translationPassage,
      translationPassages,  // 新增：多个翻译题
      translationAnswers,   // 新增：多个翻译题答案
      listeningUnitsAB,
      listeningUnitsSectionA,  // 新增：导出Section A数据
      sectionA,
      sectionB,
      sectionC,
      sectionAPassage,
      sectionABlankNumbers,
      sectionAOptions,
      sectionBArticle,
      sectionBStatements,
      sectionBParagraphMarks,
      sectionCPassages,
      writingAnswer,
      translationAnswer,
      listeningAnswers,
      sectionAAnswers,
      sectionBAnswers,
      sectionCAnswers,
      optionsOf,
      optionKey,
      displayOptionMark,
      displayOptionText,
      submitAnswers,
      // 提交进度条相关
      isSubmitting,
      submitProgress,
      // 音频相关
      audioServiceStatus,
      audioGenerating,
      hasAudio,
      audioUrl,
      isPlaying,
      currentTime,
      audioDuration,
      checkAudioServiceStatus,
      generateListeningAudio,
      toggleAudio,
      stopAudio,
      seekAudio,
      formatTime,
      examPaperEnSource,
    }
  }
}
</script>

<style scoped>
.question-container {
  display: flex;
  min-height: 100vh;
}

.main-content {
  flex: 1;
  padding: 20px;
  margin-left: 290px;
  width: calc(100% - 290px);
  box-sizing: border-box;
}

.question-wrapper {
  max-width: 880px;
  margin: 0 auto;
  padding: 32px 20px 60px;
  box-sizing: border-box;
}

.header {
  text-align: center;
  margin-bottom: 28px;
  color: #2c3e50;
  font-size: 24px;
}

.loading-tip,
.error-tip {
  padding: 20px;
  text-align: center;
  color: #546e7a;
  font-size: 14px;
}

.error-tip {
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

.answer-area {
  margin-top: 20px;
  padding: 20px;
  border: 1px solid #e0e0e0;
  border-radius: 10px;
  background: #fafbfc;
}

.answer-area h4 {
  margin: 0 0 10px 0;
  color: #2c3e50;
}

.answer-textarea {
  width: 100%;
  padding: 12px;
  border: 1px solid #cfd8dc;
  border-radius: 8px;
  font-size: 15px;
  font-family: inherit;
  resize: vertical;
  min-height: 120px;
}

.answer-textarea:focus {
  outline: none;
  border-color: #43a047;
  box-shadow: 0 0 0 2px rgba(67, 160, 71, 0.15);
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

.audio-controls {
  display: flex;
  align-items: center;
  gap: 15px;
  margin-bottom: 20px;
  padding: 15px;
  background: #f0f8ff;
  border-radius: 10px;
  border: 1px solid #e1f5fe;
}

.service-status {
  padding: 8px 12px;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
}

.service-status.checking {
  background: #fff3cd;
  color: #856404;
  border: 1px solid #ffeaa7;
}

.service-status.unavailable {
  background: #f8d7da;
  color: #721c24;
  border: 1px solid #f5c6cb;
}

.generate-audio-btn {
  padding: 10px 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 600;
  transition: all 0.3s ease;
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.3);
}

.generate-audio-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.4);
}

.generate-audio-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.audio-player {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 12px 18px;
  background: #fff;
  border-radius: 10px;
  border: 1px solid #e0e0e0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.audio-placeholder {
  min-width: 160px;
  padding: 12px 14px;
  border-radius: 10px;
  background: #f0f2f5;
  color: #555;
  font-size: 14px;
  text-align: center;
  border: 1px solid #d0d7de;
}

.play-btn {
  padding: 8px 16px;
  background: #4CAF50;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-weight: 600;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 100px;
  white-space: nowrap;
  justify-content: center;
}

.play-btn:hover {
  background: #45a049;
  transform: scale(1.05);
}

.play-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}

.stop-btn {
  padding: 8px 16px;
  background: #f44336;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-weight: 600;
  transition: all 0.2s ease;
  min-width: 100px;
  white-space: nowrap;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stop-btn:hover {
  background: #d32f2f;
  transform: scale(1.05);
}

.stop-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}

.audio-progress {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
  min-width: 200px;
}

.audio-progress span {
  font-size: 12px;
  color: #666;
  font-weight: 600;
  min-width: 35px;
}

.progress-slider {
  appearance: none;
  flex: 1;
  height: 6px;
  border-radius: 3px;
  background: #e0e0e0;
  outline: none;
  cursor: pointer;
  -webkit-appearance: none;
}

.progress-slider::-webkit-slider-thumb {
  -webkit-appearance: none;
  appearance: none;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: #4CAF50;
  cursor: pointer;
  border: 2px solid white;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
  transition: all 0.2s ease;
}

.progress-slider::-webkit-slider-thumb:hover {
  transform: scale(1.2);
  background: #45a049;
}

.progress-slider::-moz-range-thumb {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: #4CAF50;
  cursor: pointer;
  border: 2px solid white;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
  transition: all 0.2s ease;
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
  appearance: none;
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
  cursor: pointer;
  transition: background-color 0.2s;
}

.radio-option:hover {
  background-color: #f5f5f5;
}

.radio-option input[type="radio"] {
  cursor: pointer;
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

.submit-btn-container {
  text-align: center;
  margin-top: 40px;
}

.submit-btn {
  padding: 12px 30px;
  background-color: #4CAF50;
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 0.3s, transform 0.2s;
}

.submit-btn:hover {
  background-color: #45a049;
  transform: translateY(-1px);
}

.submit-btn:active {
  transform: translateY(0);
}

@media (max-width: 600px) {
  .question-wrapper {
    padding: 24px 20px 60px;
  }

  .main-content {
    margin-left: 0;
    width: 100%;
    padding: 15px;
  }

  pre {
    max-height: 320px;
  }

  .option-grid {
    grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  }
}
</style>