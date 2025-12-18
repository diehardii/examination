<template>
  <div class="app-container">
    <SideBarMenu />
    <div class="main-content">
      <div class="paper-container">
        <h1 class="header">在线答题 - {{ paperTitle }}</h1>
        <p class="user-hint" v-if="currentUserName">当前账号：{{ currentUserName }}</p>

        <div v-if="loading" class="placeholder">正在加载试卷...</div>
        <div v-else-if="errorMessage" class="placeholder error">{{ errorMessage }}</div>

        <div v-else>
          <div v-if="segments.length" class="segments-wrapper">
            <div
              v-for="(segment, index) in segments"
              :key="`segment-${index}-${segment.segment_number}`"
              class="segment-block"
            >
              <div
                v-if="isFirstSegmentOfPart(segment, index) && getPartDescription(segment.part_number)"
                class="description-block part-desc"
              >
                <div class="desc-icon">📋</div>
                <div class="desc-content">
                  <div class="desc-label">{{ getPartName(segment.part_number) }} 说明</div>
                  <div class="desc-text">{{ getPartDescription(segment.part_number) }}</div>
                </div>
              </div>

              <div
                v-if="isFirstSegmentOfSection(segment, index) && getSectionDescription(segment.part_number, segment.section_number)"
                class="description-block section-desc"
              >
                <div class="desc-icon">📝</div>
                <div class="desc-content">
                  <div class="desc-label">{{ getSectionName(segment.part_number, segment.section_number) }} 说明</div>
                  <div class="desc-text">{{ getSectionDescription(segment.part_number, segment.section_number) }}</div>
                </div>
              </div>

              <div
                v-if="getSegmentDescription(segment.part_number, segment.section_number, segment.segment_number)"
                class="description-block segment-desc-block"
              >
                <div class="desc-icon">💡</div>
                <div class="desc-content">
                  <div class="desc-label">题目说明</div>
                  <div class="desc-text">{{ getSegmentDescription(segment.part_number, segment.section_number, segment.segment_number) }}</div>
                </div>
              </div>

              <!-- 听力第一题旁添加音频生成控件 -->
              <div 
                v-if="isListening(segment) && isFirstListeningSegment(segment, index)" 
                class="audio-controls-block"
              >
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

              <div v-if="segment.content && !isListening(segment)" class="content-block">
                <h4 class="content-title">📖 阅读材料</h4>
                <pre class="passage-text">{{ segment.content }}</pre>
              </div>

              <template v-if="isClozeTest(segment)">
                <div class="section-block question-block" v-if="getQuestionItems(segment).length">
                  <h4>📋 题目</h4>
                  <div
                    v-for="(q, qIdx) in getQuestionItems(segment)"
                    :key="`q-${index}-${qIdx}`"
                    class="question-row"
                  >
                    <div class="question-info">
                      <span class="question-no">{{ q.question_number }}</span>
                    </div>
                    <select class="answer-select" v-model="answers[answerKey(segment, q)]">
                      <option value="">请选择答案</option>
                      <option
                        v-for="opt in getOptionsOf(q)"
                        :key="`${index}-${qIdx}-${opt.mark}`"
                        :value="opt.mark"
                      >
                        {{ opt.mark }}：{{ opt.text }}
                      </option>
                    </select>
                  </div>
                </div>
              </template>

              <template v-else-if="isSevenChooseFive(segment)">
                <div class="section-block statements-block" v-if="getQuestionItems(segment).length">
                  <h4>📋 题目（请选择每题对应的选项）</h4>
                  <div
                    v-for="(q, qIdx) in getQuestionItems(segment)"
                    :key="`q-${index}-${qIdx}`"
                    class="statement-row"
                  >
                    <div class="statement-header">
                      <span class="question-no">{{ q.question_number }}</span>
                      <p class="statement-text" v-if="!isListening(segment) && getQuestionText(q)">{{ getQuestionText(q) }}</p>
                    </div>
                    <select class="paragraph-select" v-model="answers[answerKey(segment, q)]">
                      <option value="">请选择</option>
                      <option
                        v-for="option in getSegmentOptions(segment)"
                        :key="`opt-${index}-${qIdx}-${option.mark}`"
                        :value="option.mark"
                      >
                        {{ option.mark }}. {{ option.text }}
                      </option>
                    </select>
                  </div>
                </div>
              </template>

              <template v-else-if="isGrammarFill(segment)">
                <div class="questions-block grammar-fill-section" v-if="getQuestionItems(segment).length">
                  <h4>📋 题目</h4>
                  <div class="grammar-fill-grid">
                    <div
                      v-for="(q, qIdx) in getQuestionItems(segment)"
                      :key="`q-${index}-${qIdx}`"
                      class="grammar-fill-row"
                    >
                      <span class="question-no">{{ q.question_number }}</span>
                      <input
                        type="text"
                        class="grammar-fill-input"
                        placeholder="填写"
                        v-model="answers[answerKey(segment, q)]"
                      />
                    </div>
                  </div>
                </div>
              </template>

              <template v-else-if="isWritingSegment(segment)">
                <div class="questions-block" v-if="getQuestionItems(segment).length">
                  <div
                    v-for="(q, qIdx) in getQuestionItems(segment)"
                    :key="`w-${segment.segment_id}-${q.question_number || qIdx}`"
                    class="question-item"
                  >
                    <div class="question-header">
                      <span class="question-no">{{ q.question_number || segment.segment_number || qIdx + 1 }}</span>
                      <p class="question-text" v-if="q.question_content && q.question_content.trim() !== ''">{{ q.question_content }}</p>
                    </div>
                    <div class="writing-area">
                      <textarea
                        class="writing-textarea"
                        rows="10"
                        placeholder="在此作答（可多行输入）"
                        v-model="answers[getWritingAnswerKey(segment, q)]"
                      ></textarea>
                    </div>
                  </div>
                </div>
                <div class="questions-block" v-else>
                  <div class="question-item">
                    <div class="question-header">
                      <span class="question-no">{{ segment.segment_number || index + 1 }}</span>
                      <p class="question-text" v-if="segment.segment_name">{{ segment.segment_name }}</p>
                    </div>
                    <div class="writing-area">
                      <textarea
                        class="writing-textarea"
                        rows="10"
                        placeholder="在此作答（可多行输入）"
                        v-model="answers[getWritingSegmentKey(segment)]"
                      ></textarea>
                    </div>
                  </div>
                </div>
              </template>

              <template v-else>
                <div class="questions-block" v-if="getDisplayQuestionItems(segment).length">
                  <div
                    v-for="(q, qIdx) in getDisplayQuestionItems(segment)"
                    :key="`q-${index}-${qIdx}`"
                    class="question-item"
                  >
                    <div class="question-header">
                      <span class="question-no">{{ q.question_number }}</span>
                      <p
                        class="question-text"
                        v-if="!isListening(segment) && getQuestionText(q)"
                      >
                        {{ getQuestionText(q) }}
                      </p>
                    </div>

                    <div class="options-list" v-if="getOptionsOf(q).length">
                      <label
                        v-for="opt in getOptionsOf(q)"
                        :key="`opt-${index}-${qIdx}-${opt.mark}`"
                        class="radio-option"
                      >
                        <input
                          type="radio"
                          :name="`q-${index}-${qIdx}`"
                          :value="opt.mark"
                          v-model="answers[answerKey(segment, q)]"
                        />
                        <span class="option-label">
                          <strong>{{ opt.mark }}.</strong> {{ opt.text }}
                        </span>
                      </label>
                    </div>

                    <div v-else-if="isWritingSegment(segment)" class="writing-area">
                      <textarea
                        class="writing-textarea"
                        rows="8"
                        placeholder="在此作答（可多行输入）"
                        v-model="answers[answerKey(segment, q)]"
                      ></textarea>
                    </div>
                  </div>
                </div>
              </template>
            </div>
          </div>
          <el-empty v-else :image-size="200" description="暂无题目" />
        </div>

        <div class="submit-btn-container" v-if="!loading && !errorMessage">
          <button class="submit-btn" @click="submitPaper">提交试卷</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import SideBarMenu from '@/common/components/SideBarMenu.vue'
import { useAuthStore } from '@/common/stores/auth'
import { getPaperDisplay } from '@/hs3/service/HS3paperAnalysisService'
import axios from 'axios'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

// 音频相关配置
const JAVA_AUDIO_API_BASE = 'http://localhost:8080/api/hs3/audio'

const currentUserName = computed(() => {
  try {
    const cached = localStorage.getItem('user')
    const parsed = cached ? JSON.parse(cached) : null
    return parsed?.username || authStore.user?.username || ''
  } catch (error) {
    return ''
  }
})

const examPaperId = computed(() => route.params.examPaperId)
const paperTitle = computed(() => paperMeta.value?.examPaperName || '试卷')

const loading = ref(false)
const errorMessage = ref('')
const segments = ref([])
const descriptions = ref({ parts: {}, sections: {}, segments: {} })
const paperMeta = ref(null)
const answers = ref({})

// 音频相关状态
const audioServiceStatus = ref('checking') // checking, available, unavailable
const audioGenerating = ref(false)
const hasAudio = ref(false)
const audioUrl = ref('')
const isPlaying = ref(false)
const currentTime = ref(0)
const audioDuration = ref(0)
const audioElement = ref(null)

onMounted(async () => {
  if (!examPaperId.value) {
    errorMessage.value = '缺少试卷ID'
    return
  }
  await loadPaper()
  await checkAudioServiceStatus()
})

onUnmounted(() => {
  disposeAudio()
})

const loadPaper = async () => {
  loading.value = true
  errorMessage.value = ''
  try {
    const data = await getPaperDisplay(String(examPaperId.value))
    if (!data || data.success === false) {
      throw new Error(data?.message || '获取试卷失败')
    }
    paperMeta.value = data
    normalizeSegments(data.segments || [])
    const keySegments = (segments.value || []).filter(
      (s) => (String(s.part_number) === '1' && String(s.section_number) === '2') ||
        (String(s.part_number) === '2' && String(s.section_number) === '2')
    )
    keySegments.forEach((s) => {
      console.log('[HS3] 关键片段描述', {
        part: s.part_number,
        section: s.section_number,
        segment: s.segment_number,
        description: s.segment_description || s.segmentDescription,
      })
    })
  } catch (error) {
    errorMessage.value = error.message || '获取试卷失败'
  } finally {
    loading.value = false
  }
}

// 检查音频服务状态（直接返回可用，不再调用健康检查端点）
const checkAudioServiceStatus = async () => {
  audioServiceStatus.value = 'available'
  return true
}

// 生成听力音频
const generateListeningAudio = async () => {
  if (audioGenerating.value) return
  
  // 获取所有听力segments（part_number = 1）
  const listeningSegments = segments.value.filter(s => isListening(s))
  
  if (listeningSegments.length === 0) {
    ElMessage.warning('没有听力内容')
    return
  }

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
    const url = `${JAVA_AUDIO_API_BASE}/generate-listening-full`

    // 构造完整的听力单元数据
    const payloadSegments = listeningSegments.map(s => {
      const questions = getQuestionItems(s)
      return {
        content: s.content || '',
        section_name: s.section_name || '',
        section_number: s.section_number || '',
        segment_name: s.segment_name || '',
        segment_number: s.segment_number || '',
        questions: {
          count: questions.length,
          items: questions.map(q => ({
            answer: q.answer || '',
            options: getOptionsOf(q).map(opt => ({
              option_content: opt.text,
              option_mark: opt.mark
            })),
            question_content: getQuestionText(q),
            question_number: q.question_number || '',
            question_score: q.question_score || 1.5
          }))
        }
      }
    }).filter(s => s.content && s.content.trim().length > 0)

    if (!payloadSegments.length) {
      ElMessage.error('未能从试卷中提取到可合成的听力文本')
      audioGenerating.value = false
      return
    }

    const response = await axios.post(url, {
      segments: payloadSegments
    }, { 
      withCredentials: true, 
      timeout: 30 * 60 * 1000  // 30分钟超时
    })
    
    if (response.data.success) {
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

// 音频播放控制
const toggleAudio = () => {
  if (!audioUrl.value) return
  
  if (!audioElement.value) {
    audioElement.value = new Audio(audioUrl.value)
    audioElement.value.addEventListener('loadedmetadata', () => {
      audioDuration.value = audioElement.value.duration
    })
    audioElement.value.addEventListener('timeupdate', () => {
      currentTime.value = audioElement.value.currentTime
    })
    audioElement.value.addEventListener('ended', () => {
      isPlaying.value = false
      currentTime.value = 0
    })
  }
  
  if (isPlaying.value) {
    audioElement.value.pause()
    isPlaying.value = false
  } else {
    audioElement.value.play()
    isPlaying.value = true
  }
}

const stopAudio = () => {
  if (audioElement.value) {
    audioElement.value.pause()
    audioElement.value.currentTime = 0
    currentTime.value = 0
    isPlaying.value = false
  }
}

const seekAudio = (event) => {
  if (audioElement.value) {
    const newTime = parseFloat(event.target.value)
    audioElement.value.currentTime = newTime
    currentTime.value = newTime
  }
}

const formatTime = (seconds) => {
  if (!seconds || isNaN(seconds)) return '0:00'
  const mins = Math.floor(seconds / 60)
  const secs = Math.floor(seconds % 60)
  return `${mins}:${secs.toString().padStart(2, '0')}`
}

const disposeAudio = () => {
  if (audioElement.value) {
    audioElement.value.pause()
    audioElement.value.src = ''
    audioElement.value = null
  }
  isPlaying.value = false
  currentTime.value = 0
  audioDuration.value = 0
}

// 判断是否为听力第一题
const isFirstListeningSegment = (segment, index) => {
  if (!isListening(segment)) return false
  if (index === 0) return true
  // 检查前面是否有听力题
  for (let i = 0; i < index; i++) {
    if (isListening(segments.value[i])) {
      return false
    }
  }
  return true
}

const normalizeSegments = (rawSegments = []) => {
  const partDescMap = {}
  const sectionDescMap = {}
  const segmentDescMap = {}

  const normalized = rawSegments.map((seg, idx) => {
    let parsedData = {}
    if (seg.questionData) {
      try {
        parsedData = typeof seg.questionData === 'string' ? JSON.parse(seg.questionData) : seg.questionData
      } catch (error) {
        parsedData = {}
      }
    }

    const partNumber = parsedData.part_number ?? parsedData.partNumber ?? seg.partNumber ?? seg.part_number ?? ''
    const sectionNumber = parsedData.section_number ?? parsedData.sectionNumber ?? seg.sectionNumber ?? seg.section_number ?? ''
    const segmentNumber = parsedData.segment_number ?? parsedData.segmentNumber ?? seg.segmentNumber ?? seg.segment_number ?? idx + 1
    const segmentName = parsedData.segment_name ?? parsedData.segmentName ?? seg.segmentName ?? seg.segment_name ?? `片段 ${segmentNumber}`

    if (seg.partDescription) {
      partDescMap[String(partNumber)] = seg.partDescription
    }
    if (seg.sectionDescription) {
      sectionDescMap[`${partNumber}|${sectionNumber}`] = seg.sectionDescription
    }
    const segmentDescription =
      parsedData.segment_description ?? parsedData.segmentDescription ?? seg.segmentDescription ?? seg.description
    const segDescKey = `${partNumber}|${sectionNumber}|${segmentNumber}`
    if (segmentDescription) {
      segmentDescMap[segDescKey] = segmentDescription
    }

    return {
      ...parsedData,
      segment_id: parsedData.segment_id ?? parsedData.segmentId ?? seg.segmentId ?? segmentName,
      part_number: partNumber,
      section_number: sectionNumber,
      segment_number: segmentNumber,
      segment_name: segmentName,
      part_name: parsedData.part_name ?? seg.partName,
      section_name: parsedData.section_name ?? seg.sectionName,
      question_type: parsedData.question_type ?? parsedData.questionType ?? seg.questionType,
      content: parsedData.content ?? parsedData.passage ?? parsedData.article ?? seg.content ?? '',
      topic: parsedData.topic ?? seg.topic ?? '',
      segment_description: segmentDescription || segmentDescMap[segDescKey] || '',
    }
  })

  descriptions.value = { parts: partDescMap, sections: sectionDescMap, segments: segmentDescMap }
  segments.value = normalized
  
  // 打印调试信息
  console.log('[HS3] 加载完成, segments数量:', normalized.length)
  normalized.forEach((seg, idx) => {
    if (String(seg.part_number) === '4') { // 写作题
      console.log(`[HS3] 写作题 segment[${idx}]:`, {
        segment_id: seg.segment_id,
        segment_name: seg.segment_name,
        question_type: seg.question_type,
        part_number: seg.part_number,
        section_number: seg.section_number,
        questions_count: getQuestionItems(seg).length
      })
    }
  })
}

const getQuestionItems = (segment) => {
  if (!segment) return []
  if (segment.questions && segment.questions.items && Array.isArray(segment.questions.items)) {
    return segment.questions.items
  }
  const qao = segment.question_and_options
  if (qao) {
    if (Array.isArray(qao)) return qao
    if (typeof qao === 'object') return [qao]
  }
  if (segment.questions && Array.isArray(segment.questions)) {
    return segment.questions
  }
  return []
}

const isReadingSectionOne = (segment) => String(segment.part_number) === '2' && String(segment.section_number) === '1'

const getDisplayQuestionItems = (segment) => {
  const items = getQuestionItems(segment)
  if (!items.length) return []
  if (isReadingSectionOne(segment)) {
    return items.filter((q) => {
      const n = parseInt(q.question_number, 10)
      return Number.isNaN(n) ? true : n <= 35
    })
  }
  return items
}

const getOptionsOf = (q) => {
  if (!q) return []
  try {
    if (Array.isArray(q.options)) {
      return q.options
        .map((o) => ({
          mark: String(o.option_mark || o.mark || '').trim(),
          text: String(o.option_content || o.text || o.content || '').trim(),
        }))
        .filter((o) => o.mark && o.text)
    }
    if (Array.isArray(q.choices)) {
      return q.choices
        .map((o) => ({
          mark: String(o.option_mark || o.mark || '').trim(),
          text: String(o.option_content || o.text || o.content || '').trim(),
        }))
        .filter((o) => o.mark && o.text)
    }
    const std = q['选项']
    if (std && typeof std === 'object' && !Array.isArray(std)) {
      const preferredOrder = ['A', 'B', 'C', 'D', 'E', 'F', 'G']
      const arr = []
      preferredOrder.forEach((k) => {
        if (std[k] && String(std[k]).trim()) {
          arr.push({ mark: k, text: String(std[k]).trim() })
        }
      })
      if (arr.length) return arr
    }
  } catch (error) {
    return []
  }
  return []
}

const getQuestionText = (q) => {
  if (!q) return ''
  const candidates = [
    q.question_content,
    q.question,
    q.title,
    q.text,
    q.stem,
  ]
  for (const c of candidates) {
    if (c && String(c).trim() !== '') {
      return String(c).trim()
    }
  }
  return ''
}

const getSegmentOptions = (segment) => {
  const items = getQuestionItems(segment)
  if (!items.length) return []
  const optionsMap = new Map()
  for (const q of items) {
    if (q.options && Array.isArray(q.options)) {
      for (const opt of q.options) {
        const mark = String(opt.option_mark || opt.mark || '').trim()
        const text = String(opt.option_content || opt.text || '').trim()
        if (mark && text && !optionsMap.has(mark)) {
          optionsMap.set(mark, { mark, text })
        }
      }
    }
  }
  const sortOrder = ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I']
  return Array.from(optionsMap.values()).sort((a, b) => {
    const idxA = sortOrder.indexOf(a.mark)
    const idxB = sortOrder.indexOf(b.mark)
    return (idxA === -1 ? 100 : idxA) - (idxB === -1 ? 100 : idxB)
  })
}

const isListening = (segment) => String(segment.part_number) === '1'
const isWritingSegment = (segment) => String(segment.part_number) === '4'

const isClozeTest = (segment) => String(segment.part_number) === '3' && String(segment.section_number) === '1'
const isSevenChooseFive = (segment) => String(segment.part_number) === '2' && String(segment.section_number) === '2'
const isGrammarFill = (segment) => String(segment.part_number) === '3' && String(segment.section_number) === '2'

const getPartDescription = (partNumber) => descriptions.value.parts[String(partNumber)] || null
const getSectionDescription = (partNumber, sectionNumber) => descriptions.value.sections[`${partNumber}|${sectionNumber}`] || null
const getSegmentDescription = (partNumber, sectionNumber, segmentNumber) => descriptions.value.segments[`${partNumber}|${sectionNumber}|${segmentNumber}`] || null

const isFirstSegmentOfPart = (segment, index) => {
  if (index === 0) return true
  const currentPartNumber = segment.part_number
  if (index > 0 && segments.value[index - 1]) {
    return segments.value[index - 1].part_number !== currentPartNumber
  }
  return false
}

const isFirstSegmentOfSection = (segment, index) => {
  if (index === 0) return true
  const currentPartNumber = segment.part_number
  const currentSectionNumber = segment.section_number
  if (index > 0 && segments.value[index - 1]) {
    const prev = segments.value[index - 1]
    return prev.part_number !== currentPartNumber || prev.section_number !== currentSectionNumber
  }
  return false
}

const getPartName = (partNumber) => {
  if (!partNumber) return '未知部分'
  const partNames = {
    '1': '第一部分 听力',
    '2': '第二部分 阅读理解',
    '3': '第三部分 语言知识运用',
    '4': '第四部分 写作',
  }
  const key = String(partNumber)
  return partNames[key] || `第${key}部分`
}

const getSectionName = (partNumber, sectionNumber) => {
  const sectionNames = {
    '1|1': '听力第一节',
    '1|2': '听力第二节',
    '2|1': '阅读理解第一节',
    '2|2': '阅读理解第二节（七选五）',
    '3|1': '语言知识运用第一节（完形填空）',
    '3|2': '语言知识运用第二节（语法填空）',
    '4|1': '写作第一节（应用文）',
    '4|2': '读后续写',
  }
  const key = `${partNumber}|${sectionNumber}`
  return sectionNames[key] || `第${sectionNumber}节`
}

const answerKey = (segment, q) => {
  const segmentId = segment.segment_id || segment.segment_number || 'seg'
  const questionNumber = q.question_number || 'q'
  return `${segmentId}-${questionNumber}`
}

// 写作题答案key（使用segment唯一标识确保不同写作题独立）
const getWritingAnswerKey = (segment, q) => {
  // 组合多个字段确保唯一性：part_number + section_number + segment_number + question_number
  const partNum = segment.part_number || ''
  const sectionNum = segment.section_number || ''
  const segmentNum = segment.segment_number || ''
  const questionNum = q.question_number || ''
  return `writing-${partNum}-${sectionNum}-${segmentNum}-${questionNum}`
}

// 写作题segment级别的key（当没有questions时）
const getWritingSegmentKey = (segment) => {
  // 组合多个字段确保唯一性
  const partNum = segment.part_number || ''
  const sectionNum = segment.section_number || ''
  const segmentNum = segment.segment_number || ''
  return `writing-${partNum}-${sectionNum}-${segmentNum}`
}

const submitPaper = async () => {
  if (!segments.value.length) {
    ElMessage.warning('题目尚未加载')
    return
  }
  
  // 确认提交
  const filledCount = Object.keys(answers.value).filter((k) => answers.value[k]).length
  const confirmed = confirm(`您已作答 ${filledCount} 道题，确定要提交吗？`)
  if (!confirmed) {
    return
  }

  // 构建提交数据
  const submitData = {
    examPaperEnId: examPaperId.value,
    answers: buildAnswersPayload()
  }

  try {
    loading.value = true
    const response = await axios.post(
      'http://localhost:8080/api/hs3/test/submit',
      submitData,
      { withCredentials: true }
    )

    if (response.data.success) {
      ElMessage.success('提交成功！正在跳转到结果页面...')
      const testEnId = response.data.testEnId
      
      // 使用 Vue Router 跳转到结果页面
      setTimeout(() => {
        router.push(`/hs3/test-result/${testEnId}`)
      }, 1000)
    } else {
      ElMessage.error('提交失败: ' + (response.data.message || '未知错误'))
    }
  } catch (error) {
    console.error('提交试卷失败:', error)
    const errorMsg = error.response?.data?.message || error.message || '提交失败'
    ElMessage.error('提交失败: ' + errorMsg)
  } finally {
    loading.value = false
  }
}

// 构建答案提交payload
const buildAnswersPayload = () => {
  const payload = {
    listening: {},
    reading: {},
    cloze: {},
    grammar: {},
    writing: {},
    application: {}
  }

  // 打印调试信息
  console.log('========== 构建答案提交数据 ==========')
  console.log('当前answers对象:', JSON.stringify(answers.value, null, 2))
  console.log('segments数量:', segments.value.length)
  
  // 遍历所有segments，按题型分类答案（使用part_number和section_number判断，与模板保持一致）
  segments.value.forEach(segment => {
    const segmentId = segment.segment_id
    const partNumber = String(segment.part_number)
    const sectionNumber = String(segment.section_number)
    
    console.log(`\n处理segment: id=${segmentId}, part=${partNumber}, section=${sectionNumber}`)

    // Part 1: 听力
    if (partNumber === '1') {
      const questions = getQuestionItems(segment)
      questions.forEach(q => {
        const questionNumber = q.question_number
        const key = answerKey(segment, q)
        if (answers.value[key]) {
          payload.listening[key] = answers.value[key]
          console.log(`  听力题 ${key} = ${answers.value[key]}`)
        }
      })
    }
    // Part 2: 阅读理解
    else if (partNumber === '2') {
      const questions = getQuestionItems(segment)
      questions.forEach(q => {
        const questionNumber = q.question_number
        const key = answerKey(segment, q)
        if (answers.value[key]) {
          payload.reading[key] = answers.value[key]
          console.log(`  阅读题 ${key} = ${answers.value[key]}`)
        }
      })
    }
    // Part 3 Section 1: 完形填空
    else if (partNumber === '3' && sectionNumber === '1') {
      const questions = getQuestionItems(segment)
      questions.forEach(q => {
        const questionNumber = q.question_number
        const key = answerKey(segment, q)
        if (answers.value[key]) {
          payload.cloze[key] = answers.value[key]
          console.log(`  完形填空 ${key} = ${answers.value[key]}`)
        }
      })
    }
    // Part 3 Section 2: 语法填空
    else if (partNumber === '3' && sectionNumber === '2') {
      const questions = getQuestionItems(segment)
      questions.forEach(q => {
        const questionNumber = q.question_number
        const key = answerKey(segment, q)
        if (answers.value[key]) {
          payload.grammar[key] = answers.value[key]
          console.log(`  语法填空 ${key} = ${answers.value[key]}`)
        }
      })
    }
    // Part 4 Section 1: 应用文写作
    else if (partNumber === '4' && sectionNumber === '1') {
      const questions = getQuestionItems(segment)
      console.log(`  应用文写作: questions数量=${questions.length}, segmentId=${segmentId}`)
      
      // 收集所有写作答案，合并为一个完整答案
      let allAnswers = []
      if (questions.length > 0) {
        questions.forEach((q, qIdx) => {
          const key = getWritingAnswerKey(segment, q)
          console.log(`    检查key: ${key}`)
          console.log(`    答案内容: ${answers.value[key]}`)
          if (answers.value[key]) {
            allAnswers.push(answers.value[key])
          }
        })
      } else {
        const key = getWritingSegmentKey(segment)
        console.log(`    使用segment key: ${key}`)
        console.log(`    答案内容: ${answers.value[key]}`)
        if (answers.value[key]) {
          allAnswers.push(answers.value[key])
        }
      }
      
      // 后端期望的key是segmentId（如"写作应用文写作1"），不带question_number
      if (allAnswers.length > 0) {
        payload.application[segmentId] = allAnswers.join('\n\n')
        console.log(`  ✓ 应用文写作 ${segmentId} 已添加`)
      } else {
        console.log(`  ✗ 应用文写作 ${segmentId} 未找到答案`)
      }
    }
    // Part 4 Section 2: 读后续写
    else if (partNumber === '4' && sectionNumber === '2') {
      const questions = getQuestionItems(segment)
      console.log(`  读后续写: questions数量=${questions.length}, segmentId=${segmentId}`)
      
      // 收集所有写作答案，合并为一个完整答案
      let allAnswers = []
      if (questions.length > 0) {
        questions.forEach((q, qIdx) => {
          const key = getWritingAnswerKey(segment, q)
          console.log(`    检查key: ${key}`)
          console.log(`    答案内容: ${answers.value[key]}`)
          if (answers.value[key]) {
            allAnswers.push(answers.value[key])
          }
        })
      } else {
        const key = getWritingSegmentKey(segment)
        console.log(`    使用segment key: ${key}`)
        console.log(`    答案内容: ${answers.value[key]}`)
        if (answers.value[key]) {
          allAnswers.push(answers.value[key])
        }
      }
      
      // 后端期望的key是segmentId（如"写作读后续写1"），不带question_number
      if (allAnswers.length > 0) {
        payload.writing[segmentId] = allAnswers.join('\n\n')
        console.log(`  ✓ 读后续写 ${segmentId} 已添加`)
      } else {
        console.log(`  ✗ 读后续写 ${segmentId} 未找到答案`)
      }
    }
    // Part 4 其他（没有明确section的情况，作为通用写作处理）
    else if (partNumber === '4') {
      const questions = getQuestionItems(segment)
      console.log(`  写作（Part4其他）: questions数量=${questions.length}, segmentId=${segmentId}`)
      
      let allAnswers = []
      if (questions.length > 0) {
        questions.forEach((q, qIdx) => {
          const key = getWritingAnswerKey(segment, q)
          console.log(`    检查key: ${key}`)
          console.log(`    答案内容: ${answers.value[key]}`)
          if (answers.value[key]) {
            allAnswers.push(answers.value[key])
          }
        })
      } else {
        const key = getWritingSegmentKey(segment)
        console.log(`    使用segment key: ${key}`)
        console.log(`    答案内容: ${answers.value[key]}`)
        if (answers.value[key]) {
          allAnswers.push(answers.value[key])
        }
      }
      
      if (allAnswers.length > 0) {
        payload.writing[segmentId] = allAnswers.join('\n\n')
        console.log(`  ✓ 写作 ${segmentId} 已添加`)
      }
    }
  })

  console.log('\n最终payload:', JSON.stringify(payload, null, 2))
  console.log('========================================\n')
  return payload
}
</script>

<style scoped>
.app-container {
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

.paper-container {
  width: 100%;
  max-width: 1200px;
  margin-top: 30px;
}

.header {
  color: #333;
  margin-bottom: 12px;
  font-size: 24px;
}

.user-hint {
  margin: 4px 0 16px;
  color: #666;
}

.placeholder {
  padding: 24px;
  text-align: center;
  color: #607d8b;
}

.placeholder.error {
  color: #d32f2f;
}

.segments-wrapper {
  margin-top: 12px;
}

.segment-block {
  border: 1px solid #e6e6e6;
  border-radius: 10px;
  padding: 16px;
  margin-bottom: 16px;
  background: #fff;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.04);
}

.content-block {
  background: #f7f9fb;
  border: 1px solid #e0e0e0;
  border-radius: 10px;
  padding: 14px;
  margin-bottom: 12px;
}

.content-title {
  margin: 0 0 8px 0;
  color: #2c3e50;
}

.passage-text {
  white-space: pre-wrap;
  word-break: break-word;
  margin: 0;
  color: #37474f;
  font-size: 15px;
  line-height: 1.8;
}

.description-block {
  display: flex;
  gap: 12px;
  padding: 12px;
  border-radius: 10px;
  margin-bottom: 12px;
  border-left: 4px solid;
}

.description-block.part-desc {
  background: linear-gradient(135deg, #e8f5e9 0%, #c8e6c9 100%);
  border-left-color: #2e7d32;
}

.description-block.section-desc {
  background: linear-gradient(135deg, #e3f2fd 0%, #bbdefb 100%);
  border-left-color: #1565c0;
}

.description-block.segment-desc-block {
  background: linear-gradient(135deg, #fff3e0 0%, #ffe0b2 100%);
  border-left-color: #ef6c00;
}

.desc-icon {
  font-size: 18px;
}

.desc-label {
  font-weight: 600;
  margin-bottom: 4px;
  font-size: 15px;
}

.desc-text {
  font-size: 15px;
  line-height: 1.8;
  color: #37474f;
  white-space: pre-wrap;
  word-break: break-word;
}

.question-block, .questions-block, .statements-block {
  margin-bottom: 12px;
}

.question-row, .grammar-fill-row, .statement-row {
  display: flex;
  align-items: flex-start;
  flex-wrap: wrap;
  gap: 10px;
  padding: 8px 0;
  border-bottom: 1px dashed #e0e0e0;
}

.question-no {
  display: inline-block;
  width: 32px;
  font-weight: 600;
  color: #2e7d32;
}

.answer-select, .paragraph-select {
  flex: 1;
  min-width: 260px;
  padding: 8px;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  background: #fff;
}

.grammar-fill-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.grammar-fill-input {
  flex: 1;
  width: 100%;
  padding: 8px 10px;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
}

.questions-block {
  margin-top: 6px;
}

.question-item {
  padding: 8px 0;
  border-bottom: 1px dashed #e0e0e0;
}

.question-header {
  display: flex;
  gap: 8px;
  align-items: baseline;
}

.question-text {
  margin: 0;
  color: #37474f;
  font-size: 15px;
  line-height: 1.8;
}

.options-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 6px;
}

.radio-option {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  background: #f9fbfd;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  padding: 10px;
}

.writing-area {
  margin-top: 6px;
}

.writing-textarea {
  width: 100%;
  padding: 10px;
  border: 1px solid #d0d7de;
  border-radius: 8px;
  resize: vertical;
  min-height: 240px;
  font-family: inherit;
  line-height: 1.4;
  background: #fdfefe;
}

@media (max-width: 1024px) {
  .grammar-fill-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .grammar-fill-grid {
    grid-template-columns: repeat(1, minmax(0, 1fr));
  }
  .answer-select, .paragraph-select {
    min-width: 100%;
  }
}

.submit-btn-container {
  margin-top: 16px;
  display: flex;
  justify-content: center;
}

.submit-btn {
  padding: 10px 18px;
  background: #4caf50;
  color: #fff;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 15px;
  transition: background-color 0.2s ease;
}

.submit-btn:hover {
  background: #43a047;
}

/* 音频控件样式 */
.audio-controls-block {
  background: linear-gradient(135deg, #e1f5fe 0%, #b3e5fc 100%);
  border: 2px solid #0288d1;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.service-status {
  padding: 8px 12px;
  border-radius: 6px;
  font-size: 14px;
  text-align: center;
}

.service-status.checking {
  background: #fff3e0;
  color: #e65100;
}

.service-status.unavailable {
  background: #ffebee;
  color: #c62828;
}

.generate-audio-btn {
  padding: 12px 24px;
  background: linear-gradient(135deg, #42a5f5 0%, #1e88e5 100%);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px rgba(33, 150, 243, 0.3);
}

.generate-audio-btn:hover:not(:disabled) {
  background: linear-gradient(135deg, #1e88e5 0%, #1565c0 100%);
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(33, 150, 243, 0.4);
}

.generate-audio-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.audio-placeholder {
  padding: 12px;
  text-align: center;
  color: #0277bd;
  font-weight: 600;
  animation: pulse 1.5s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.6; }
}

.audio-player {
  display: flex;
  flex-direction: column;
  gap: 12px;
  background: white;
  padding: 12px;
  border-radius: 8px;
}

.audio-player button {
  padding: 10px 20px;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.play-btn {
  background: linear-gradient(135deg, #66bb6a 0%, #43a047 100%);
  color: white;
  box-shadow: 0 2px 8px rgba(76, 175, 80, 0.3);
}

.play-btn:hover:not(:disabled) {
  background: linear-gradient(135deg, #43a047 0%, #2e7d32 100%);
  transform: translateY(-1px);
}

.stop-btn {
  background: linear-gradient(135deg, #ef5350 0%, #e53935 100%);
  color: white;
  box-shadow: 0 2px 8px rgba(244, 67, 54, 0.3);
}

.stop-btn:hover:not(:disabled) {
  background: linear-gradient(135deg, #e53935 0%, #c62828 100%);
  transform: translateY(-1px);
}

.play-btn:disabled, .stop-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.audio-progress {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #37474f;
  font-size: 14px;
}

.progress-slider {
  flex: 1;
  height: 6px;
  border-radius: 3px;
  outline: none;
  -webkit-appearance: none;
  background: linear-gradient(to right, #42a5f5 0%, #42a5f5 var(--progress, 0%), #e0e0e0 var(--progress, 0%), #e0e0e0 100%);
}

.progress-slider::-webkit-slider-thumb {
  -webkit-appearance: none;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: #1e88e5;
  cursor: pointer;
  box-shadow: 0 2px 4px rgba(0,0,0,0.2);
}

.progress-slider::-moz-range-thumb {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: #1e88e5;
  cursor: pointer;
  border: none;
  box-shadow: 0 2px 4px rgba(0,0,0,0.2);
}

.progress-slider:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
