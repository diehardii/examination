<template>
  <div class="analysis-wrapper">
    <h1>试卷解析 - {{ subjectLabel }}</h1>

    <!-- 上传卡片 -->
    <div class="upload-card">
      <div class="upload-icon">📑</div>
      <p class="upload-tip">
        请选择本地 Word 试卷文件（.doc/.docx），系统会提取文本并调用 Coze 智能体进行结构化解析。
      </p>

      <label class="upload-button">
        <input type="file" accept=".doc,.docx,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document" @change="handleFileChange" hidden />
        选择 Word 文件
      </label>

      <div v-if="fileName" class="file-name">已选择：{{ fileName }}</div>

      <div class="concurrent-toggle">
        <button class="parse-button" type="button" @click="handleParseAndAnalyze" :disabled="!selectedFile || structureLoading">
          {{ structureLoading ? '解析中...' : '开始解析' }}
        </button>
      </div>
      
      <!-- 解析进度条 -->
      <el-progress
        v-if="structureLoading"
        :percentage="Math.floor(progress)"
        :stroke-width="15"
        :text-inside="true"
        status="success"
        style="margin-top: 16px; width: 100%"
      />
    </div>

    <!-- 状态提示 -->
    <div v-if="structureLoading" class="status loading">正在解析试卷，请稍候……（Coze 智能体处理中）</div>
    <div v-if="errorMessage" class="status error">{{ errorMessage }}</div>
    <div v-if="successMessage" class="status success">{{ successMessage }}</div>

    <!-- 解析结果概览 -->
    <div v-if="parseResult" class="result-card structured">
      <h2>解析结果概览</h2>
      <div class="result-info">
        <p><strong>试卷ID：</strong>{{ parseResult.examPaperId }}</p>
        <p><strong>输出文件：</strong>{{ parseResult.outputFilePath }}</p>
        <p><strong>解析片段数：</strong>{{ parseResult.totalSegments }}</p>
      </div>
    </div>

    <!-- 原始片段列表展示（Coze返回的解析结果） -->
    <div v-if="segments && segments.length > 0" class="result-card structured">
      <h2>解析结果</h2>
      
      <div 
        v-for="(segment, index) in segments" 
        :key="`raw-segment-${index}`"
        class="segment-block"
      >
        <!-- Part 说明 - 仅在该Part第一个segment且有description时显示 -->
        <div v-if="isFirstSegmentOfPart(segment, index) && getPartDescription(segment.part_number)" class="description-block part-desc">
          <div class="desc-icon">📋</div>
          <div class="desc-content">
            <div class="desc-label">{{ getPartName(segment.part_number) }} 说明</div>
            <div class="desc-text">{{ getPartDescription(segment.part_number) }}</div>
          </div>
        </div>
        
        <!-- Section 说明 - 仅在该Section第一个segment显示 -->
        <div v-if="isFirstSegmentOfSection(segment, index) && getSectionDescription(segment.part_number, segment.section_number)" class="description-block section-desc">
          <div class="desc-icon">📝</div>
          <div class="desc-content">
            <div class="desc-label">{{ getSectionName(segment.part_number, segment.section_number) }} 说明</div>
            <div class="desc-text">{{ getSectionDescription(segment.part_number, segment.section_number) }}</div>
          </div>
        </div>
        
        <!-- Segment 说明 -->
        <div v-if="getSegmentDescription(segment.part_number, segment.section_number, segment.segment_number)" class="description-block segment-desc-block">
          <div class="desc-icon">💡</div>
          <div class="desc-content">
            <div class="desc-label">题目说明</div>
            <div class="desc-text">{{ getSegmentDescription(segment.part_number, segment.section_number, segment.segment_number) }}</div>
          </div>
        </div>
        
        <!-- 片段头部信息 -->
        <div class="segment-header">
          <span class="segment-number">{{ segment.segment_name || `片段 ${segment.segment_number || index + 1}` }}</span>
          <!-- topic主题标签已隐藏 -->
        </div>

        <!-- 阅读材料/文章内容 -->
        <div v-if="segment.content" class="content-block">
          <h4 class="content-title">📖 阅读材料</h4>
          <pre :class="['passage-text', { 'writing-prompt-text': isLongAnswerSegment(segment) }]">{{ segment.content }}</pre>
        </div>

        <!-- ========== 根据题目类型判断展示方式 ========== -->
        
        <!-- 完形填空（part_number=3, section_number=1）：文章+题号下拉框+答案 -->
        <template v-if="isClozeTest(segment)">
          <!-- 题目列表 - 下拉框选择（显示选项内容） -->
          <div class="section-block question-block" v-if="getQuestionItems(segment).length > 0">
            <h4>📋 题目</h4>
            <div
              v-for="(q, qIdx) in getQuestionItems(segment)"
              :key="`q-${index}-${qIdx}`"
              class="question-row"
            >
              <div class="question-info">
                <span class="question-no">{{ q.question_number }}</span>
              </div>
              <select class="answer-select">
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
          
          <!-- 答案展示 -->
          <div v-if="getQuestionItems(segment).length > 0" class="answers-section">
            <h4 class="answer-title">✅ 参考答案</h4>
            <div :class="['answers-grid', { 'writing-answers': isLongAnswerSegment(segment) } ]">
              <div 
                v-for="q in getQuestionItems(segment)" 
                :key="`ans-${index}-${q.question_number}`"
                class="answer-item"
                v-show="q.answer"
              >
                <span class="answer-number" v-if="!isLongAnswerSegment(segment)">{{ q.question_number }}</span>
                <span class="answer-value">{{ q.answer }}</span>
              </div>
            </div>
          </div>
        </template>
        
        <!-- 七选五（part_number=2, section_number=2）：正文+题号下拉框（选A-G）+答案 -->
        <template v-else-if="isSevenChooseFive(segment)">
          <!-- 题目列表 - 下拉框直接显示选项内容 -->
          <div class="section-block statements-block" v-if="getQuestionItems(segment).length > 0">
            <h4>📋 题目（请选择每题对应的选项）</h4>
            <div
              v-for="(q, qIdx) in getQuestionItems(segment)"
              :key="`q-${index}-${qIdx}`"
              class="statement-row"
            >
              <div class="statement-header">
                <span class="question-no">{{ q.question_number }}</span>
                <p class="statement-text" v-if="q.question_content">{{ q.question_content }}</p>
              </div>
              <select class="paragraph-select">
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
          
          <!-- 答案展示 -->
          <div v-if="getQuestionItems(segment).length > 0" class="answers-section">
            <h4 class="answer-title">✅ 参考答案</h4>
            <div :class="['answers-grid', { 'writing-answers': isLongAnswerSegment(segment) } ]">
              <div 
                v-for="q in getQuestionItems(segment)" 
                :key="`ans-${index}-${q.question_number}`"
                class="answer-item"
                v-show="q.answer"
              >
                <span class="answer-number" v-if="!isLongAnswerSegment(segment)">{{ q.question_number }}</span>
                <span class="answer-value">{{ q.answer }}</span>
              </div>
            </div>
          </div>
        </template>
        
        <!-- 语法填空（part_number=3, section_number=2）：正文+题号输入框+答案 -->
        <template v-else-if="isGrammarFill(segment)">
          <!-- 题目列表 - 单行输入框版本（3列网格） -->
          <div class="questions-block grammar-fill-section" v-if="getQuestionItems(segment).length > 0">
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
                />
              </div>
            </div>
          </div>
          
          <!-- 答案展示 - 3列网格，题号+答案 -->
          <div v-if="getQuestionItems(segment).length > 0" class="answers-section grammar-answers">
            <h4 class="answer-title">✅ 参考答案</h4>
            <div :class="['grammar-answers-grid', { 'writing-answers': isLongAnswerSegment(segment) } ]">
              <div 
                v-for="q in getQuestionItems(segment)" 
                :key="`ans-${index}-${q.question_number}`"
                class="grammar-answer-item"
                v-show="q.answer"
              >
                <span class="answer-no" v-if="!isLongAnswerSegment(segment)">{{ q.question_number }}.</span>
                <span class="answer-word">{{ q.answer }}</span>
              </div>
            </div>
          </div>
        </template>
        
        <!-- 默认：带选项的选择题（阅读理解、听力等） -->
        <template v-else>
          <!-- 题目列表展示 -->
          <div class="questions-block" v-if="getQuestionItems(segment).length > 0">
            <div 
              v-for="(q, qIdx) in getQuestionItems(segment)" 
              :key="`q-${index}-${qIdx}`"
              class="question-item"
            >
              <!-- 题号和题干 -->
              <div class="question-header">
                <span class="question-no">{{ q.question_number }}</span>
                <!-- 仅当question_content不为空时展示题干 -->
                <p class="question-text" v-if="q.question_content && q.question_content.trim() !== ''">{{ q.question_content }}</p>
              </div>
              
              <!-- 选项列表 -->
              <div class="options-list" v-if="getOptionsOf(q).length > 0">
                <label 
                  v-for="opt in getOptionsOf(q)" 
                  :key="`opt-${index}-${qIdx}-${opt.mark}`"
                  class="radio-option"
                >
                  <input 
                    type="radio" 
                    :name="`q-${index}-${qIdx}`" 
                    :value="opt.mark"
                    disabled
                  />
                  <span class="option-label">
                    <strong>{{ opt.mark }}.</strong> {{ opt.text }}
                  </span>
                </label>
              </div>
            </div>
          </div>
          
          <!-- 答案展示 -->
          <div v-if="getQuestionItems(segment).length > 0" class="answers-section">
            <h4 class="answer-title">✅ 参考答案</h4>
            <div :class="['answers-grid', { 'writing-answers': isLongAnswerSegment(segment) } ]">
              <div 
                v-for="q in getQuestionItems(segment)" 
                :key="`ans-${index}-${q.question_number}`"
                class="answer-item"
                v-show="q.answer"
              >
                <span class="answer-number" v-if="!isLongAnswerSegment(segment)">{{ q.question_number }}</span>
                <span class="answer-value">{{ q.answer }}</span>
              </div>
            </div>
          </div>
        </template>
      </div>
    </div>

    <!-- 存储到 ChromaDB 控件 - 置于页面底部中间 -->
    <div v-if="parseResult && parseResult.examPaperId && segments.length > 0" class="result-card structured metadata-card" style="margin-top: 28px;">
      <h2>写入 ChromaDB（整卷）</h2>
      
      <div class="form-grid">
        <div class="form-field">
          <label>试卷名称 <span style="color: red;">*</span></label>
          <input type="text" v-model.trim="chromaExamPaperName" placeholder="例如：2024年全国卷I高考英语" />
        </div>
      </div>

      <div class="actions section-actions" style="justify-content: center; margin-top: 24px;">
        <button 
          class="submit-button" 
          :disabled="!canStoreToChroma || savingToChroma" 
          @click="handleStoreToChroma"
        >
          {{ savingToChroma ? '存储中...' : '存储整份试卷' }}
        </button>
      </div>

      <p v-if="chromaMessage" :class="['status-tip', chromaMessageType]">
        {{ chromaMessage }}
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElProgress, ElMessageBox } from 'element-plus'
import { 
  parseAndAnalyzePaper
} from '@/hs3/service/HS3paperAnalysisService'
import axios from 'axios'

const route = useRoute()
const currentSubject = ref('GAOKAO')
const subjectLabel = ref('高考英语')

// 文件上传相关
const selectedFile = ref(null)
const fileName = ref('')

// 加载状态
const structureLoading = ref(false)
const progress = ref(0)
const progressInterval = ref(null)

// 消息提示
const errorMessage = ref('')
const successMessage = ref('')

// 解析结果
const parseResult = ref(null)
const segments = ref([])
const paperStructure = ref(null)
const descriptions = ref({ parts: {}, sections: {}, segments: {} })  // 存储各层级的description

// ChromaDB 存储相关
const chromaExamPaperName = ref('')
const savingToChroma = ref(false)
const chromaMessage = ref('')
const chromaMessageType = ref('')

// 计算属性
const canStoreToChroma = computed(() => {
  return Boolean(
    parseResult.value && 
    parseResult.value.examPaperId && 
    chromaExamPaperName.value.trim() &&
    segments.value.length > 0
  )
})

onMounted(async () => {
  // 从query参数获取subject
  try {
    const subjectFromQuery = route.query.subject
    if (subjectFromQuery) {
      currentSubject.value = subjectFromQuery
    }
  } catch (error) {
    console.error('获取科目信息失败:', error)
  }
})

// 文件选择
const handleFileChange = (e) => {
  const file = e.target.files[0]
  if (!file) return
  
  const validTypes = [
    'application/msword',
    'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
  ]
  const fileExt = file.name.toLowerCase().split('.').pop()
  
  if (!validTypes.includes(file.type) && !['doc', 'docx'].includes(fileExt)) {
    errorMessage.value = '请选择 Word 文件（.doc 或 .docx）'
    return
  }
  
  selectedFile.value = file
  fileName.value = file.name
  errorMessage.value = ''
  successMessage.value = ''
  parseResult.value = null
  segments.value = []
  paperStructure.value = null
}

// 解析并分析试卷
const handleParseAndAnalyze = async () => {
  if (!selectedFile.value) {
    errorMessage.value = '请先选择文件'
    return
  }
  
  structureLoading.value = true
  errorMessage.value = ''
  successMessage.value = ''
  progress.value = 0
  parseResult.value = null
  segments.value = []
  paperStructure.value = null
  
  // 模拟进度
  progressInterval.value = setInterval(() => {
    if (progress.value < 90) {
      progress.value += Math.random() * 5
    }
  }, 1000)
  
  try {
    // 调用解析和分析接口（不传知识点参数）
    const result = await parseAndAnalyzePaper(selectedFile.value, [])
    
    if (result.success) {
      parseResult.value = {
        examPaperId: result.examPaperId,
        outputFilePath: result.outputFilePath,
        totalSegments: result.totalSegments || 0
      }
      
      // 保存原始片段数据
      if (result.segments && Array.isArray(result.segments)) {
        segments.value = result.segments
        console.log('[调试] segments数据:', result.segments.slice(0, 2))  // 打印前2个segment
        if (result.segments.length > 0) {
          console.log('[调试] 第一个segment的字段:', Object.keys(result.segments[0]))
        }
      }
      
      // 保存各层级的description信息
      if (result.descriptions) {
        descriptions.value = result.descriptions
        console.log('[调试] descriptions数据:', result.descriptions)  // 打印descriptions
      } else {
        console.log('[调试] 没有收到descriptions数据')
      }
      
      successMessage.value = `解析成功！共解析 ${result.totalSegments || 0} 个片段`
      progress.value = 100
    } else {
      errorMessage.value = result.message || '解析失败'
    }
  } catch (error) {
    console.error('解析失败:', error)
    errorMessage.value = error.response?.data?.message || error.message || '解析失败，请稍后重试'
  } finally {
    structureLoading.value = false
    if (progressInterval.value) {
      clearInterval(progressInterval.value)
      progressInterval.value = null
    }
  }
}

// 存储到 ChromaDB
const handleStoreToChroma = async () => {
  if (savingToChroma.value) return // 防止重复点击造成多次提交

  if (!chromaExamPaperName.value || chromaExamPaperName.value.trim() === '') {
    chromaMessageType.value = 'error'
    chromaMessage.value = '请填写试卷名称后再存储'
    return
  }
  
  savingToChroma.value = true
  chromaMessage.value = ''
  
  try {
    // 准备存储数据
    const payload = {
      examPaperName: chromaExamPaperName.value.trim(),
      subject: '高考', // 高考英语
      examPaperSource: 'real', // 真题解析页面，来源固定为 real
      segments: segments.value.map(seg => {
        // 为每个segment添加必要的元数据
        return {
          ...seg,
          partName: seg.part_name || '',
          partNumber: seg.part_number || 1,
          sectionName: seg.section_name || '',
          sectionNumber: seg.section_number || 1,
          segmentName: seg.segment_name || '',
          segmentNumber: seg.segment_number || 1,
          topic: seg.topic || 'unknown topic'
        }
      })
    }
    console.log('[ChromaDB] 准备存储数据:', payload)
    
    // 调用后端接口存储到ChromaDB
    const response = await axios.post(
      'http://localhost:8080/api/hs3/paper-analysis/store-to-chroma',
      payload,
      { 
        withCredentials: true
      }
    )
    
    if (response.data.success) {
      chromaMessageType.value = 'success'
      chromaMessage.value = ''
      
      // 使用弹窗提示（不使用await，与CET4保持一致）
      ElMessageBox.alert(
        `整份试卷已成功存入 ChromaDB（共保存 ${response.data.segmentsCount} 个大题）`,
        '存储成功',
        {
          confirmButtonText: '确定',
          type: 'success'
        }
      )
    } else {
      chromaMessageType.value = 'error'
      chromaMessage.value = response.data.message || '存储失败'
    }
  } catch (error) {
    console.error('存储到 ChromaDB 失败:', error)
    chromaMessageType.value = 'error'
    if (error.response?.data?.message) {
      chromaMessage.value = error.response.data.message
    } else if (error.message) {
      chromaMessage.value = error.message
    } else {
      chromaMessage.value = '存储失败，请稍后重试'
    }
  } finally {
    savingToChroma.value = false
  }
}

// ========== 题目展示辅助函数 ==========

/**
 * 从segment中获取题目列表
 * 支持 question_and_options 字段（数组或单个对象）
 */
const getQuestionList = (segment) => {
  if (!segment) return []
  
  // 优先使用 question_and_options 字段
  const qao = segment.question_and_options
  if (qao) {
    if (Array.isArray(qao)) {
      return qao
    }
    // 如果是单个对象，包装成数组
    if (typeof qao === 'object') {
      return [qao]
    }
  }
  
  // 兼容 questions 字段
  if (segment.questions && Array.isArray(segment.questions)) {
    return segment.questions
  }
  
  return []
}

/**
 * 从segment中获取答案列表
 * 支持 answers 字段（数组或单个对象）
 */
const getAnswerList = (segment) => {
  if (!segment) return []
  
  const answers = segment.answers
  if (answers) {
    if (Array.isArray(answers)) {
      return answers
    }
    // 如果是单个对象，包装成数组
    if (typeof answers === 'object') {
      return [answers]
    }
  }
  
  return []
}

/**
 * 从题目对象中提取选项列表
 * 兼容多种数据格式
 */
const getOptionsOf = (q) => {
  if (!q) return []
  
  try {
    // 格式1: options 为数组，元素包含 option_mark/option_content
    if (Array.isArray(q.options)) {
      return q.options
        .map((o) => ({
          mark: String(o.option_mark || o.mark || '').trim(),
          text: String(o.option_content || o.text || o.content || '').trim()
        }))
        .filter((o) => o.mark && o.text)
    }
    
    // 格式2: choices 为数组
    if (Array.isArray(q.choices)) {
      return q.choices
        .map((o) => ({
          mark: String(o.option_mark || o.mark || '').trim(),
          text: String(o.option_content || o.text || o.content || '').trim()
        }))
        .filter((o) => o.mark && o.text)
    }
    
    // 格式3: 选项 为对象 { A: '...', B: '...', ... }
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
  } catch (e) {
    console.error('解析选项失败:', e)
  }
  
  return []
}

/**
 * 获取Part的description
 * @param partNumber Part编号
 */
const getPartDescription = (partNumber) => {
  if (!partNumber || !descriptions.value.parts) return null
  // 确保使用字符串类型的key进行查找
  const key = String(partNumber)
  let result = descriptions.value.parts[key] || null
  // 如果找不到，尝试用数字类型查找（兼容后端返回的可能是数字key）
  if (!result) {
    result = descriptions.value.parts[Number(partNumber)] || null
  }
  if (!result && partNumber) {
    console.log('[调试] getPartDescription 未找到:', key, '可用keys:', Object.keys(descriptions.value.parts))
  }
  return result
}

/**
 * 获取Section的description
 * @param partNumber Part编号
 * @param sectionNumber Section编号
 */
const getSectionDescription = (partNumber, sectionNumber) => {
  if (!partNumber || !sectionNumber || !descriptions.value.sections) return null
  const key = `${partNumber}|${sectionNumber}`
  return descriptions.value.sections[key] || null
}

/**
 * 获取Segment的description
 * @param partNumber Part编号
 * @param sectionNumber Section编号
 * @param segmentNumber Segment编号
 */
const getSegmentDescription = (partNumber, sectionNumber, segmentNumber) => {
  if (!partNumber || !sectionNumber || !segmentNumber || !descriptions.value.segments) return null
  const key = `${partNumber}|${sectionNumber}|${segmentNumber}`
  return descriptions.value.segments[key] || null
}

/**
 * 判断是否是Part的第一节第一段
 * @param segment 当前segment
 * @param index 当前索引
 */
const isFirstSegmentOfPart = (segment, index) => {
  if (index === 0) return true
  const currentPartNumber = segment.part_number
  // 检查前一个segment的part_number是否不同
  if (index > 0 && segments.value[index - 1]) {
    return segments.value[index - 1].part_number !== currentPartNumber
  }
  return false
}

/**
 * 判断是否是Section的第一段
 * @param segment 当前segment
 * @param index 当前索引
 */
const isFirstSegmentOfSection = (segment, index) => {
  if (index === 0) return true
  const currentPartNumber = segment.part_number
  const currentSectionNumber = segment.section_number
  // 检查前一个segment是否属于不同的section
  if (index > 0 && segments.value[index - 1]) {
    const prev = segments.value[index - 1]
    return prev.part_number !== currentPartNumber || prev.section_number !== currentSectionNumber
  }
  return false
}

// ========== 新版Coze格式辅助函数 ==========

/**
 * 从segment中获取题目items数组
 * 新版格式：segment.questions.items
 */
const getQuestionItems = (segment) => {
  if (!segment) return []
  
  // 新版格式：questions.items
  if (segment.questions && segment.questions.items && Array.isArray(segment.questions.items)) {
    return segment.questions.items
  }
  
  // 兼容旧格式：question_and_options
  const qao = segment.question_and_options
  if (qao) {
    if (Array.isArray(qao)) return qao
    if (typeof qao === 'object') return [qao]
  }
  
  // 兼容 questions 为数组的格式
  if (segment.questions && Array.isArray(segment.questions)) {
    return segment.questions
  }
  
  return []
}

/**
 * 判断segment中的题目是否有选项（用于区分选择题和填空题）
 */
const hasOptionsInQuestions = (segment) => {
  const items = getQuestionItems(segment)
  if (items.length === 0) return false
  
  // 检查第一道题是否有选项
  const firstQ = items[0]
  if (firstQ && firstQ.options && Array.isArray(firstQ.options) && firstQ.options.length > 0) {
    return true
  }
  
  return false
}

// 写作/长答案检测：用于切换答案展示为全宽排版
const isLongAnswerSegment = (segment) => {
  if (!segment) return false
  // 写作部分直接判定为长答案
  if (segment.part_number === 4 || (segment.section_name && String(segment.section_name).includes('写作'))) {
    return true
  }
  const items = getQuestionItems(segment)
  return items.some((q) => q && q.answer && String(q.answer).length > 80)
}

/**
 * 从segment中获取所有备选答案（用于选词填空题型）
 * 汇总所有题目的选项作为备选词
 */
const getSegmentOptions = (segment) => {
  const items = getQuestionItems(segment)
  if (items.length === 0) return []
  
  // 如果第一道题有选项，收集所有不重复的选项
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
  
  // 按选项标记排序
  const sortOrder = ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O']
  return Array.from(optionsMap.values()).sort((a, b) => {
    const idxA = sortOrder.indexOf(a.mark)
    const idxB = sortOrder.indexOf(b.mark)
    return (idxA === -1 ? 100 : idxA) - (idxB === -1 ? 100 : idxB)
  })
}

// ========== 题型判断函数 ==========

/**
 * 判断是否是完形填空（part_number=3, section_number=1）
 */
const isClozeTest = (segment) => {
  return String(segment.part_number) === '3' && String(segment.section_number) === '1'
}

/**
 * 判断是否是七选五（part_number=2, section_number=2）
 */
const isSevenChooseFive = (segment) => {
  return String(segment.part_number) === '2' && String(segment.section_number) === '2'
}

/**
 * 判断是否是语法填空（part_number=3, section_number=2）
 */
const isGrammarFill = (segment) => {
  return String(segment.part_number) === '3' && String(segment.section_number) === '2'
}

// ========== Part/Section 名称获取函数 ==========

/**
 * 根据part_number获取Part名称
 */
const getPartName = (partNumber) => {
  if (!partNumber) return '未知部分'
  const partNames = {
    '1': '第一部分 听力',
    '2': '第二部分 阅读理解',
    '3': '第三部分 语言知识运用',
    '4': '第四部分 写作'
  }
  const key = String(partNumber)
  return partNames[key] || `第${key}部分`
}

/**
 * 根据part_number和section_number获取Section名称
 */
const getSectionName = (partNumber, sectionNumber) => {
  const sectionNames = {
    '1|1': '听力第一节',
    '1|2': '听力第二节',
    '2|1': '阅读理解第一节',
    '2|2': '阅读理解第二节（七选五）',
    '3|1': '语言知识运用第一节（完形填空）',
    '3|2': '语言知识运用第二节（语法填空）',
    '4|1': '写作第一节（应用文）',
    '4|2': '写作第二节（读后续写）'
  }
  const key = `${partNumber}|${sectionNumber}`
  return sectionNames[key] || `第${sectionNumber}节`
}
</script>

<style scoped>
.analysis-wrapper {
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

.upload-card {
  background: #ffffff;
  padding: 30px;
  border-radius: 14px;
  box-shadow: 0 12px 32px rgba(46, 125, 50, 0.1);
  text-align: center;
  margin-bottom: 30px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  align-items: center;
}

.upload-icon {
  font-size: 48px;
}

.upload-tip {
  color: #546e7a;
  margin: 0;
}

.upload-button {
  display: inline-block;
  padding: 12px 26px;
  background: linear-gradient(135deg, #66bb6a 0%, #43a047 100%);
  color: #fff;
  border-radius: 999px;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.upload-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 18px rgba(102, 187, 106, 0.35);
}

.file-name {
  color: #2e7d32;
  font-weight: 600;
}

.concurrent-toggle {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 12px;
}

.parse-button {
  padding: 10px 24px;
  background: linear-gradient(135deg, #3498db 0%, #2980b9 100%);
  color: #fff;
  border: none;
  border-radius: 999px;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.parse-button:disabled {
  cursor: not-allowed;
  opacity: 0.6;
  box-shadow: none;
  transform: none;
}

.parse-button:not(:disabled):hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(52, 152, 219, 0.35);
}

.status {
  text-align: center;
  margin: 18px 0;
  font-weight: 500;
}

.loading {
  color: #2e7d32;
}

.error {
  color: #d32f2f;
}

.success {
  color: #2e7d32;
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

.metadata-card {
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

.result-info {
  margin-bottom: 20px;
}

.result-info p {
  margin: 8px 0;
  font-size: 14px;
  color: #546e7a;
}

.result-info strong {
  color: #2c3e50;
}

/* Section 样式 */
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

/* 题目说明块样式 */
.description-block {
  display: flex;
  gap: 12px;
  padding: 14px 16px;
  border-radius: 10px;
  margin-bottom: 16px;
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

.description-block .desc-icon {
  font-size: 20px;
  flex-shrink: 0;
}

.description-block .desc-content {
  flex: 1;
}

.description-block .desc-label {
  font-weight: 600;
  font-size: 14px;
  margin-bottom: 4px;
}

.part-desc .desc-label {
  color: #1b5e20;
}

.section-desc .desc-label {
  color: #0d47a1;
}

.segment-desc-block .desc-label {
  color: #e65100;
}

.description-block .desc-text {
  font-size: 14px;
  line-height: 1.6;
  color: #37474f;
}

/* 段落/文章块样式 */
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

.passage-text {
  white-space: pre-wrap;
  word-break: break-word;
  margin: 0;
  color: #37474f;
  font-size: 15px;
  line-height: 1.8;
}

/* 写作提示文本 - 使用等宽字体确保下划线对齐 */
.writing-prompt-text {
  font-family: 'Courier New', Consolas, 'Liberation Mono', monospace;
  font-size: 14px;
  line-height: 1.6;
  letter-spacing: 0;
}

/* Segment 块样式 */
.segment-block {
  margin-bottom: 20px;
  padding: 16px;
  background: #fafbfc;
  border-radius: 10px;
  border: 1px solid #e0e0e0;
}

.segment-id-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: linear-gradient(135deg, #e3f2fd 0%, #bbdefb 100%);
  border-radius: 20px;
  border: 1px solid #90caf9;
  margin-bottom: 12px;
  font-size: 14px;
  box-shadow: 0 2px 6px rgba(33, 150, 243, 0.15);
}

.segment-id-badge.inline {
  margin-bottom: 0;
  margin-top: 8px;
}

.segment-id-badge .badge-label {
  font-weight: 600;
  color: #1565c0;
  font-size: 13px;
}

.segment-id-badge .badge-value {
  font-family: 'Courier New', monospace;
  font-weight: 700;
  color: #0d47a1;
  background: #fff;
  padding: 4px 10px;
  border-radius: 12px;
  border: 1px solid #64b5f6;
  font-size: 13px;
}

.segment-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.segment-number {
  font-weight: 700;
  color: #2e7d32;
  font-size: 16px;
}

.segment-meta {
  font-size: 12px;
  color: #607d8b;
  background: #eceff1;
  padding: 4px 10px;
  border-radius: 6px;
}

.parent-info {
  font-size: 14px;
  color: #546e7a;
  margin-bottom: 12px;
  padding: 10px 14px;
  background: #eceff1;
  border-radius: 8px;
}

/* 题目样式 */
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

.question-no {
  font-weight: 600;
  margin-right: 8px;
  color: #2c3e50;
}

.question-text {
  margin: 0;
  color: #37474f;
  line-height: 1.6;
  flex: 1;
  font-weight: 500;
}

.question-content {
  flex: 1;
}

/* 选项样式 */
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
  transition: all 0.2s ease;
}

.radio-option:hover {
  background: #f0f8f0;
  border-color: #66bb6a;
}

.radio-option input[type="radio"] {
  margin-top: 3px;
  cursor: pointer;
  accent-color: #2e7d32;
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

/* 答案样式 */
.answers-section {
  margin-top: 24px;
  padding: 20px;
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);
  border-radius: 12px;
  border: 2px solid #7dd3fc;
  box-shadow: 0 4px 12px rgba(56, 189, 248, 0.15);
}

.answer-title {
  margin: 0 0 16px 0;
  color: #0c4a6e;
  font-size: 18px;
  font-weight: 700;
  display: flex;
  align-items: center;
  gap: 8px;
}

.answer-content {
  background: #fff;
  padding: 16px;
  border-radius: 10px;
  border: 1px solid #bae6fd;
}

.answer-text {
  font-size: 15px;
  line-height: 1.7;
  color: #37474f;
  white-space: pre-wrap;
  word-break: break-word;
  margin: 0;
}

.answers-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(80px, 1fr));
  gap: 12px;
}

/* 长答案（写作等）改为单列全宽排版，保证可读性 */
.answers-grid.writing-answers,
.grammar-answers-grid.writing-answers {
  grid-template-columns: 1fr;
}

.answers-grid.writing-answers .answer-item,
.grammar-answers-grid.writing-answers .grammar-answer-item {
  align-items: flex-start;
  text-align: left;
}

.answers-grid.writing-answers .answer-value,
.grammar-answers-grid.writing-answers .answer-word {
  font-family: inherit;
  font-size: 16px;
  font-weight: 500;
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.7;
}

.answer-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 12px;
  background: #fff;
  border-radius: 8px;
  border: 2px solid #bae6fd;
  transition: all 0.3s ease;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.answer-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(56, 189, 248, 0.25);
  border-color: #38bdf8;
}

.answer-number {
  font-size: 12px;
  color: #0369a1;
  font-weight: 600;
  margin-bottom: 6px;
}

.answer-value {
  font-size: 20px;
  font-weight: 700;
  color: #0c4a6e;
  font-family: 'Courier New', monospace;
}

/* 阅读材料样式 */
.content-block {
  margin-bottom: 16px;
}

.content-title {
  font-size: 14px;
  color: #546e7a;
  margin-bottom: 8px;
}

/* 元数据卡片样式 */
.paper-list-section {
  position: relative;
  width: 100%;
  box-sizing: border-box;
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

.form-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px;
  margin-top: 16px;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-field label {
  font-weight: 600;
  color: #2c3e50;
}

.form-field input,
.form-field select {
  border: 1px solid #cfd8dc;
  border-radius: 8px;
  padding: 10px 12px;
  font-size: 15px;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.form-field input:focus,
.form-field select:focus {
  outline: none;
  border-color: #43a047;
  box-shadow: 0 0 0 2px rgba(67, 160, 71, 0.15);
}

.selected-paper-info {
  color: #2e7d32;
  margin: 12px 0;
  padding: 10px;
  background: #e8f5e9;
  border-radius: 4px;
  font-size: 14px;
}

.section-actions {
  margin-top: 18px;
  justify-content: flex-end;
}

.submit-button {
  padding: 10px 28px;
  background: linear-gradient(135deg, #2ecc71 0%, #27ae60 100%);
  color: #fff;
  border: none;
  border-radius: 999px;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.submit-button:disabled {
  cursor: not-allowed;
  opacity: 0.6;
  box-shadow: none;
  transform: none;
}

.submit-button:not(:disabled):hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(46, 204, 113, 0.35);
}

.status-tip {
  margin-top: 12px;
  font-size: 14px;
}

.status-tip.success {
  color: #2e7d32;
}

.status-tip.error {
  color: #c62828;
}

/* 响应式 */
@media (max-width: 600px) {
  .upload-card {
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
  
  .options-list {
    margin-left: 0;
  }
}

/* ========== 选词填空样式（参照CET4） ========== */
.section-block {
  margin-bottom: 20px;
}

.section-block h4 {
  color: #2e7d32;
  font-size: 16px;
  margin: 0 0 12px 0;
  font-weight: 600;
}

.options-block {
  padding: 16px;
  background: linear-gradient(135deg, #f3e5f5 0%, #e1bee7 100%);
  border-radius: 10px;
  border: 1px solid #ce93d8;
}

.option-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 10px;
}

.option-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #ba68c8;
  transition: all 0.2s ease;
}

.option-chip:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(186, 104, 200, 0.25);
}

.option-mark {
  font-weight: 700;
  color: #7b1fa2;
  font-size: 14px;
  min-width: 20px;
}

.option-text {
  color: #4a148c;
  font-size: 14px;
}

/* 题目主题标签 */
.segment-topic {
  font-size: 12px;
  color: #1565c0;
  background: linear-gradient(135deg, #e3f2fd 0%, #bbdefb 100%);
  padding: 4px 12px;
  border-radius: 12px;
  border: 1px solid #90caf9;
}

/* ========== 填空题样式（第二节语法填空） ========== */
.fill-blank-section {
  padding: 16px;
  background: #fafbfc;
  border-radius: 10px;
  border: 1px solid #e0e0e0;
}

.fill-blank-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px dashed #e0e0e0;
}

.fill-blank-item:last-child {
  border-bottom: none;
}

.fill-blank-item .question-no {
  min-width: 40px;
  font-weight: 700;
  color: #1565c0;
  font-size: 16px;
}

.fill-blank-input {
  flex: 1;
  padding: 10px 14px;
  border: 2px solid #e0e0e0;
  border-radius: 8px;
  font-size: 15px;
  transition: border-color 0.2s ease;
  background: #fff;
}

.fill-blank-input:focus {
  outline: none;
  border-color: #1565c0;
}

.fill-blank-input:disabled {
  background: #f5f5f5;
  cursor: not-allowed;
}

/* 填空题答案样式 */
.fill-blank-answers {
  background: linear-gradient(135deg, #e8f5e9 0%, #c8e6c9 100%);
  border: 2px solid #66bb6a;
}

.fill-blank-answers-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.fill-blank-answer-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #81c784;
  font-size: 15px;
}

.fill-blank-answer-item .answer-number {
  font-weight: 700;
  color: #2e7d32;
}

.fill-blank-answer-item .answer-word {
  font-weight: 600;
  color: #1b5e20;
  font-family: 'Courier New', monospace;
}

/* 内容块样式 */
.content-block {
  margin-bottom: 16px;
}

.content-title {
  color: #1565c0;
  font-size: 16px;
  margin: 0 0 10px 0;
  font-weight: 600;
}

/* ========== 完形填空样式（带下拉框） ========== */
.cloze-questions-block {
  margin-top: 20px;
}

.cloze-questions-block h4 {
  color: #2e7d32;
  font-size: 16px;
  margin: 0 0 14px 0;
  font-weight: 600;
}

.question-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e0e0e0;
  margin-bottom: 10px;
  transition: all 0.2s ease;
}

.question-row:hover {
  background: #f0f8f0;
  border-color: #66bb6a;
}

.answer-select {
  min-width: 80px;
  padding: 8px 12px;
  border: 2px solid #90caf9;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 600;
  background: #fff;
  color: #1565c0;
  cursor: pointer;
  transition: all 0.2s ease;
}

.answer-select:focus {
  outline: none;
  border-color: #1565c0;
  box-shadow: 0 0 0 3px rgba(21, 101, 192, 0.15);
}

.answer-select:disabled {
  background: #f5f5f5;
  cursor: not-allowed;
}

/* ========== 七选五样式 ========== */
.seven-choose-five-options {
  margin-bottom: 20px;
  padding: 16px;
  background: linear-gradient(135deg, #fff8e1 0%, #ffecb3 100%);
  border-radius: 12px;
  border: 1px solid #ffca28;
}

.seven-choose-five-options h4 {
  color: #ff8f00;
  font-size: 16px;
  margin: 0 0 12px 0;
  font-weight: 700;
  display: flex;
  align-items: center;
  gap: 8px;
}

.seven-option-item {
  display: flex;
  gap: 10px;
  padding: 12px 14px;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #ffe082;
  margin-bottom: 8px;
  transition: all 0.2s ease;
}

.seven-option-item:last-child {
  margin-bottom: 0;
}

.seven-option-item:hover {
  transform: translateX(4px);
  border-color: #ffb300;
  box-shadow: 0 4px 12px rgba(255, 193, 7, 0.25);
}

.seven-option-item .option-mark {
  font-weight: 700;
  color: #ff6f00;
  font-size: 16px;
  min-width: 24px;
}

.seven-option-item .option-text {
  flex: 1;
  color: #5d4037;
  font-size: 14px;
  line-height: 1.7;
}

/* 七选五陈述句/题目样式 */
.statement-row {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 14px 16px;
  background: #fff;
  border-radius: 10px;
  border: 1px solid #e0e0e0;
  margin-bottom: 12px;
  transition: all 0.2s ease;
}

.statement-row:hover {
  border-color: #ffb300;
  background: #fffde7;
}

.statement-header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.statement-text {
  flex: 1;
  color: #37474f;
  font-size: 15px;
  line-height: 1.7;
}

.paragraph-select {
  min-width: 90px;
  padding: 8px 12px;
  border: 2px solid #ffca28;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 600;
  background: #fff;
  color: #ff8f00;
  cursor: pointer;
  transition: all 0.2s ease;
}

.paragraph-select:focus {
  outline: none;
  border-color: #ff8f00;
  box-shadow: 0 0 0 3px rgba(255, 143, 0, 0.15);
}

/* ========== 语法填空样式（输入框） ========== */
.grammar-fill-questions {
  margin-top: 20px;
}

.grammar-fill-questions h4 {
  color: #1565c0;
  font-size: 16px;
  margin: 0 0 14px 0;
  font-weight: 600;
}

.grammar-fill-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 14px;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e0e0e0;
  margin-bottom: 10px;
  transition: all 0.2s ease;
}

.grammar-fill-item:hover {
  background: #e3f2fd;
  border-color: #64b5f6;
}

.grammar-fill-input {
  width: 150px;
  padding: 10px 14px;
  border: 2px solid #90caf9;
  border-radius: 8px;
  font-size: 15px;
  font-family: 'Courier New', monospace;
  background: #fff;
  transition: all 0.2s ease;
}

.grammar-fill-input:focus {
  outline: none;
  border-color: #1565c0;
  box-shadow: 0 0 0 3px rgba(21, 101, 192, 0.15);
}

.grammar-fill-input:disabled {
  background: #f5f5f5;
  cursor: not-allowed;
}

.grammar-hint {
  font-size: 14px;
  color: #757575;
  font-style: italic;
}

/* 语法填空答案样式 */
.grammar-answers-section {
  margin-top: 24px;
  padding: 20px;
  background: linear-gradient(135deg, #e8f5e9 0%, #c8e6c9 100%);
  border-radius: 12px;
  border: 2px solid #66bb6a;
}

.grammar-answers-section .answer-title {
  color: #1b5e20;
}

.grammar-answers-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.grammar-answer-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #81c784;
  font-size: 15px;
  transition: all 0.2s ease;
}

.grammar-answer-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 187, 106, 0.25);
}

.grammar-answer-item .answer-number {
  font-weight: 700;
  color: #2e7d32;
  font-size: 14px;
}

.grammar-answer-item .answer-word {
  font-weight: 600;
  color: #1b5e20;
  font-family: 'Courier New', monospace;
  font-size: 15px;
}

/* ========== 语法填空新样式（3列网格） ========== */
.grammar-fill-section {
  padding: 16px;
  background: #fafbfc;
  border-radius: 10px;
  border: 1px solid #e0e0e0;
}

.grammar-fill-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.grammar-fill-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e0e0e0;
}

.grammar-fill-row .question-no {
  font-weight: 700;
  color: #1565c0;
  font-size: 15px;
  min-width: 32px;
  flex-shrink: 0;
}

.grammar-fill-row .grammar-fill-input {
  flex: 1;
  padding: 8px 10px;
  border: 1px solid #90caf9;
  border-radius: 6px;
  font-size: 14px;
  background: #fff;
  min-width: 0;
}

.grammar-answers {
  background: linear-gradient(135deg, #e8f5e9 0%, #c8e6c9 100%);
  border: 2px solid #66bb6a;
}

.grammar-answers-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.grammar-answer-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #81c784;
}

.grammar-answer-item .answer-no {
  font-weight: 700;
  color: #2e7d32;
  font-size: 15px;
  min-width: 36px;
  flex-shrink: 0;
}

.grammar-answer-item .answer-word {
  font-weight: 600;
  color: #1b5e20;
  font-family: 'Courier New', monospace;
  font-size: 15px;
  word-break: break-word;
}

/* ========== 完形填空样式（参照CET4） ========== */
.question-block {
  padding: 16px;
  background: #fafbfc;
  border-radius: 10px;
  border: 1px solid #e0e0e0;
}

.question-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e0e0e0;
  margin-bottom: 10px;
  transition: all 0.2s ease;
}

.question-row:hover {
  background: #f0f8f0;
  border-color: #66bb6a;
}

.question-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.answer-select {
  flex: 1;
  padding: 10px 14px;
  border: 2px solid #66bb6a;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  background: #fff;
  color: #2e7d32;
  cursor: pointer;
  transition: all 0.2s ease;
  min-width: 200px;
}

.answer-select:focus {
  outline: none;
  border-color: #2e7d32;
  box-shadow: 0 0 0 3px rgba(46, 125, 50, 0.15);
}

.answer-select:disabled {
  background: #f5f5f5;
  cursor: not-allowed;
}

/* 移除旧的完形填空样式 */
.cloze-block,
.cloze-questions-grid,
.cloze-question-row,
.cloze-question-no,
.cloze-select {
  /* 已废弃，使用question-block等替代 */
}
</style>
