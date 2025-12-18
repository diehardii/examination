<template>
  <div class="analysis-wrapper">
    <h1>试卷解析 - {{ subjectLabel }}</h1>

    <div class="upload-card">
      <div class="upload-icon">📑</div>
      <p class="upload-tip">
        请选择本地 PDF 试卷文件，系统会先提取文本，点击"结构化解析"后展示题目结构。
      </p>

      <label class="upload-button">
        <input type="file" accept=".pdf,.txt,.doc,.docx,application/pdf,text/plain,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document" @change="handleFileChange" hidden />
        选择 PDF 或 文本文件
      </label>

      <div v-if="fileName" class="file-name">已选择：{{ fileName }}</div>

      <div class="concurrent-toggle">
        <label style="margin-left: 12px">
          <input type="checkbox" v-model="useAsync" /> 后台解析（异步）
        </label>
        <button class="parse-button" type="button" @click="handleStructureParse" :disabled="!selectedFile || structureLoading">
          {{ structureLoading ? '解析中...' : '开始解析' }}
        </button>
        <span v-if="analysisMode" class="mode-tag">mode: {{ analysisMode }}</span>
      </div>
      
      <!-- 同步解析进度条 -->
      <el-progress
        v-if="structureLoading && !useAsync"
        :percentage="Math.floor(progress)"
        :stroke-width="15"
        :text-inside="true"
        status="success"
        style="margin-top: 16px; width: 100%"
      />
    </div>

    <!-- Part I ~ Part II 之间（Writing 段），紧随上传控件下方 -->
    <div v-if="writingPassage" class="result-card structured" style="margin-top: 16px">
      <h2>Part I · Writing（Coze 结构化）</h2>
      <div v-if="writingSegmentId" class="segment-id-badge">
        <span class="badge-label">Segment ID:</span>
        <span class="badge-value">{{ writingSegmentId }}</span>
      </div>
      <pre class="passage-text">{{ writingPassage }}</pre>
      
      <!-- 写作参考答案 -->
      <div v-if="writingReferenceAnswer" class="reference-answer-section">
        <h3 class="answer-title">📝 参考范文</h3>
        <div class="reference-answer-content">
          {{ writingReferenceAnswer }}
        </div>
      </div>
    </div>

    <!-- Part II · Listening（Section A/B） -->
    <div v-if="listeningUnitsAB.length" class="result-card structured exam-section">
      <div class="section-header">
        <div>
          <h2>Part II（Listening · Section A/B）</h2>
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
          <div v-if="unit.segment_id" class="segment-id-badge inline">
            <span class="badge-label">Segment ID:</span>
            <span class="badge-value">{{ unit.segment_id }}</span>
          </div>
        </div>

        <!-- 原文：仅当非空时展示 -->
        <div v-if="unit.listening_content" class="passage-content-block">
          <pre class="passage-text">{{ unit.listening_content }}</pre>
        </div>

        <!-- 题目与选项 -->
        <div class="questions-block" v-if="unit.questions.length">
          <div
            v-for="(q, qIdx) in unit.questions"
            :key="`q-ab-${uIdx}-${qIdx}`"
            class="question-item"
          >
            <div class="question-header">
              <!-- 修改题号显示为 question_number -->
              <span class="question-no">{{ q.question_number || q['question_number'] }}</span>
              <p class="question-text">{{ q['题干'] || q['question_content'] || q['question'] }}</p>
            </div>
            <div class="options-list">
              <label
                v-for="opt in optionsOf(q)"
                :key="`opt-ab-${uIdx}-${qIdx}-${opt.mark}`"
                class="radio-option"
              >
                <input
                  type="radio"
                  :name="`lq-ab-${uIdx}-${qIdx}`"
                  :value="opt.mark"
                  v-model="listeningAnswersAB[unitKeyAB(uIdx, qIdx)]"
                />
                <span class="option-label">
                  <strong>{{ opt.mark }}.</strong> {{ opt.text }}
                </span>
              </label>
            </div>
            <div class="answer-choice" v-if="listeningAnswersAB[unitKeyAB(uIdx, qIdx)]">
              已选：
              <strong>{{ listeningAnswersAB[unitKeyAB(uIdx, qIdx)] }}</strong>
              <template v-if="q['选项'] && q['选项'][listeningAnswersAB[unitKeyAB(uIdx, qIdx)]]">
                （{{ q['选项'][listeningAnswersAB[unitKeyAB(uIdx, qIdx)]] }}）
              </template>
            </div>
          </div>
        </div>
        
        <!-- Unit 答案 -->
        <div v-if="unit.answers && unit.answers.length" class="answers-section">
          <h3 class="answer-title">✅ 参考答案</h3>
          <div class="answers-grid">
            <div
              v-for="answer in unit.answers"
              :key="answer.question_number"
              class="answer-item"
            >
              <span class="answer-number">{{ answer.question_number }}</span>
              <span class="answer-value">{{ answer.answer }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Part II · Listening（Section C） -->
    <div v-if="listeningUnitsC.length" class="result-card structured exam-section">
      <div class="section-header">
        <div>
          <h2>Part II（Listening · Section C）</h2>
          <p class="section-meta">共 {{ listeningUnitsC.length }} 个单元</p>
        </div>
      </div>

      <div
        v-for="(unit, uIdx) in listeningUnitsC"
        :key="`unit-c-${uIdx}`"
        class="passage-section"
      >
        <div class="passage-header">
          <h3>{{ unit.unit_type || '听力单元' }}</h3>
          <div v-if="unit.segment_id" class="segment-id-badge inline">
            <span class="badge-label">Segment ID:</span>
            <span class="badge-value">{{ unit.segment_id }}</span>
          </div>
        </div>

        <!-- 原文：仅当非空时展示 -->
        <div v-if="unit.listening_content" class="passage-content-block">
          <pre class="passage-text">{{ unit.listening_content }}</pre>
        </div>

        <!-- 题目与选项 -->
        <div class="questions-block" v-if="unit.questions.length">
          <div
            v-for="(q, qIdx) in unit.questions"
            :key="`q-c-${uIdx}-${qIdx}`"
            class="question-item"
          >
            <div class="question-header">
              <!-- 修改题号显示为 question_number -->
              <span class="question-no">{{ q.question_number || q['question_number'] }}</span>
              <p class="question-text">{{ q['题干'] || q['question_content'] || q['question'] }}</p>
            </div>
            <div class="options-list">
              <label
                v-for="opt in optionsOf(q)"
                :key="`opt-c-${uIdx}-${qIdx}-${opt.mark}`"
                class="radio-option"
              >
                <input
                  type="radio"
                  :name="`lq-c-${uIdx}-${qIdx}`"
                  :value="opt.mark"
                  v-model="listeningAnswersC[unitKeyC(uIdx, qIdx)]"
                />
                <span class="option-label">
                  <strong>{{ opt.mark }}.</strong> {{ opt.text }}
                </span>
              </label>
            </div>
            <div class="answer-choice" v-if="listeningAnswersC[unitKeyC(uIdx, qIdx)]">
              已选：
              <strong>{{ listeningAnswersC[unitKeyC(uIdx, qIdx)] }}</strong>
              <template v-if="q['选项'] && q['选项'][listeningAnswersC[unitKeyC(uIdx, qIdx)]]">
                （{{ q['选项'][listeningAnswersC[unitKeyC(uIdx, qIdx)]] }}）
              </template>
            </div>
          </div>
        </div>
        
        <!-- Unit 答案 -->
        <div v-if="unit.answers && unit.answers.length" class="answers-section">
          <h3 class="answer-title">✅ 参考答案</h3>
          <div class="answers-grid">
            <div
              v-for="answer in unit.answers"
              :key="answer.question_number"
              class="answer-item"
            >
              <span class="answer-number">{{ answer.question_number }}</span>
              <span class="answer-value">{{ answer.answer }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-if="textLoading || structureLoading" class="status loading">正在解析试卷，请稍候……</div>
    <div v-if="errorMessage" class="status error">{{ errorMessage }}</div>

    <div class="task-list-section">
      <div class="task-list-header">
        <h3>解析任务</h3>
        <button type="button" class="link-btn" @click="fetchTaskList()" :disabled="taskLoading">
          {{ taskLoading ? '刷新中...' : '刷新列表' }}
        </button>
      </div>
      <div v-if="taskLoading" class="task-empty">任务数据加载中...</div>
      <div v-else-if="!taskList.length" class="task-empty">暂无解析任务，提交后可在此查看</div>
      <div v-else class="task-list">
        <div
          v-for="task in taskList"
          :key="task.id"
          class="task-item"
          :class="(task.status || '').toLowerCase()"
        >
          <div class="task-info">
            <div class="task-title-row">
              <span class="task-name">{{ displayTaskName(task) }}</span>
              <span class="task-status-pill" :class="task.status">{{ formatTaskStatus(task.status) }}</span>
            </div>
            <div class="task-meta">
              提交于 {{ formatTaskTime(task.createdAt) }}
            </div>
            <el-progress
              :percentage="Math.floor(task.progress || 0)"
              :stroke-width="10"
              :text-inside="true"
              status="success"
            />
            <div v-if="task.status === 'FAILED' && task.message" class="task-error">{{ task.message }}</div>
          </div>
          <div class="task-actions">
            <button
              v-if="task.status === 'SUCCEEDED'"
              type="button"
              class="task-view-btn"
              @click="viewTaskResult(task)"
            >查看</button>
            <button
              v-else
              type="button"
              class="task-view-btn disabled"
              disabled
            >{{ actionLabel(task.status) }}</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 查询控件已移除 -->

    <div v-if="sectionA" class="result-card structured exam-section">
      <div class="section-header">
        <div>
          <h2>Section A（选词填空）</h2>
          <p class="section-meta">共 {{ sectionABlankNumbers.length }} 题</p>
          <div v-if="sectionASegmentId" class="segment-id-badge inline">
            <span class="badge-label">Segment ID:</span>
            <span class="badge-value">{{ sectionASegmentId }}</span>
          </div>
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
        <h3>备选词（不可重复使用）</h3>
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

      <!-- Section A 答案 -->
      <div v-if="sectionAAnswers.length" class="answers-section">
        <h3 class="answer-title">✅ 参考答案</h3>
        <div class="answers-grid">
          <div
            v-for="answer in sectionAAnswers"
            :key="answer.question_number"
            class="answer-item"
          >
            <span class="answer-number">{{ answer.question_number }}</span>
            <span class="answer-value">{{ answer.answer }}</span>
          </div>
        </div>
      </div>

      <div class="actions section-actions" v-if="false"></div>
    </div>

    <!-- Section A 入库控件移除，统一到页面底部 -->

    <div v-if="submittedSummary.length" class="result-card structured">
      <h2>作答结果</h2>
      <ul class="answer-summary">
        <li v-for="item in submittedSummary" :key="item.number">
          <span class="question-no">{{ item.number }}</span>
          <span class="answer-choice">
            {{ item.answer || '未选择' }}
            <template v-if="item.word">
              （{{ item.word }}）
            </template>
          </span>
        </li>
      </ul>
    </div>

    <!-- 移除 Section A 缺省提示 -->

    <!-- Section B 段落匹配 -->
    <div v-if="sectionB" class="result-card structured exam-section">
      <div class="section-header">
        <div>
          <h2>Section B（段落匹配）</h2>
          <p class="section-meta">共 {{ sectionBStatements.length }} 题</p>
          <div v-if="sectionBSegmentId" class="segment-id-badge inline">
            <span class="badge-label">Segment ID:</span>
            <span class="badge-value">{{ sectionBSegmentId }}</span>
          </div>
        </div>
      </div>

      <!-- 文章段落 -->
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

      <!-- 陈述句题目 -->
      <div class="section-block statements-block">
        <h3>题目（请选择每个陈述句对应的段落）</h3>
        <div
          v-for="statement in sectionBStatements"
          :key="statement.question_number"
          class="statement-row"
        >
          <div class="statement-header">
            <span class="question-no">{{ statement.question_number }}</span>
            <p class="statement-text">{{ statement.statement_content }}</p>
          </div>
          <select
            class="paragraph-select"
            v-model="sectionBAnswers[statement.question_number]"
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

      <!-- Section B 答案 -->
      <div v-if="sectionBAnswersData.length" class="answers-section">
        <h3 class="answer-title">✅ 参考答案</h3>
        <div class="answers-grid">
          <div
            v-for="answer in sectionBAnswersData"
            :key="answer.question_number"
            class="answer-item"
          >
            <span class="answer-number">{{ answer.question_number }}</span>
            <span class="answer-value">{{ answer.answer }}</span>
          </div>
        </div>
      </div>

      <!-- Section B 入库控件移除，统一到页面底部 -->
    </div>

    <!-- Section B 元数据与存储控件移除，统一到页面底部 -->

    <!-- Section C1 篇章阅读1 -->
    <div v-if="sectionC1" class="result-card structured exam-section">
      <div class="section-header">
        <div>
          <h2>Section C1（篇章阅读1）</h2>
          <p class="section-meta">{{ sectionC1.passage_mark || 'Passage One' }}</p>
          <div v-if="sectionC1SegmentId" class="segment-id-badge inline">
            <span class="badge-label">Segment ID:</span>
            <span class="badge-value">{{ sectionC1SegmentId }}</span>
          </div>
        </div>
      </div>

      <!-- 短文标题和内容 -->
      <div class="passage-section">
        <div class="passage-header">
          <h3>{{ sectionC1.passage_mark || 'Passage One' }}</h3>
        </div>
        <div class="passage-content-block">
          <pre class="passage-text">{{ sectionC1.passage_content }}</pre>
        </div>

        <!-- 短文对应的问题 -->
        <div class="questions-block">
          <div
            v-for="question in sectionC1.questions"
            :key="question.question_number"
            class="question-item"
          >
            <div class="question-header">
              <span class="question-no">{{ question.question_number }}</span>
              <p class="question-text">{{ question.question_content }}</p>
            </div>
            <!-- 选项：使用 radio -->
            <div class="options-list">
              <label
                v-for="option in question.options"
                :key="option.option_mark"
                class="radio-option"
              >
                <input
                  type="radio"
                  :name="`q-c1-${question.question_number}`"
                  :value="option.option_mark"
                  v-model="sectionC1Answers[question.question_number]"
                />
                <span class="option-label">
                  <strong>{{ option.option_mark }}.</strong> {{ option.option_content }}
                </span>
              </label>
            </div>
          </div>
        </div>
      </div>

      <!-- Section C1 答案 -->
      <div v-if="sectionC1AnswersData.length" class="answers-section">
        <h3 class="answer-title">✅ 参考答案</h3>
        <div class="answers-grid">
          <div
            v-for="answer in sectionC1AnswersData"
            :key="answer.question_number"
            class="answer-item"
          >
            <span class="answer-number">{{ answer.question_number }}</span>
            <span class="answer-value">{{ answer.answer }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Section C2 篇章阅读2 -->
    <div v-if="sectionC2" class="result-card structured exam-section">
      <div class="section-header">
        <div>
          <h2>Section C2（篇章阅读2）</h2>
          <p class="section-meta">{{ sectionC2.passage_mark || 'Passage Two' }}</p>
          <div v-if="sectionC2SegmentId" class="segment-id-badge inline">
            <span class="badge-label">Segment ID:</span>
            <span class="badge-value">{{ sectionC2SegmentId }}</span>
          </div>
        </div>
      </div>

      <!-- 短文标题和内容 -->
      <div class="passage-section">
        <div class="passage-header">
          <h3>{{ sectionC2.passage_mark || 'Passage Two' }}</h3>
        </div>
        <div class="passage-content-block">
          <pre class="passage-text">{{ sectionC2.passage_content }}</pre>
        </div>

        <!-- 短文对应的问题 -->
        <div class="questions-block">
          <div
            v-for="question in sectionC2.questions"
            :key="question.question_number"
            class="question-item"
          >
            <div class="question-header">
              <span class="question-no">{{ question.question_number }}</span>
              <p class="question-text">{{ question.question_content }}</p>
            </div>
            <!-- 选项：使用 radio -->
            <div class="options-list">
              <label
                v-for="option in question.options"
                :key="option.option_mark"
                class="radio-option"
              >
                <input
                  type="radio"
                  :name="`q-c2-${question.question_number}`"
                  :value="option.option_mark"
                  v-model="sectionC2Answers[question.question_number]"
                />
                <span class="option-label">
                  <strong>{{ option.option_mark }}.</strong> {{ option.option_content }}
                </span>
              </label>
            </div>
          </div>
        </div>
      </div>

      <!-- Section C2 答案 -->
      <div v-if="sectionC2AnswersData.length" class="answers-section">
        <h3 class="answer-title">✅ 参考答案</h3>
        <div class="answers-grid">
          <div
            v-for="answer in sectionC2AnswersData"
            :key="answer.question_number"
            class="answer-item"
          >
            <span class="answer-number">{{ answer.question_number }}</span>
            <span class="answer-value">{{ answer.answer }}</span>
          </div>
        </div>
      </div>
    </div>


    <!-- Part IV Translation 到文末，置于页面最后 -->
    <div v-if="translationPassage" class="result-card structured" style="margin-top: 24px">
      <h2>Part IV · Translation（Coze 结构化）</h2>
      <div v-if="translationSegmentId" class="segment-id-badge">
        <span class="badge-label">Segment ID:</span>
        <span class="badge-value">{{ translationSegmentId }}</span>
      </div>
      <pre class="passage-text">{{ translationPassage }}</pre>
      
      <!-- 翻译参考答案 -->
      <div v-if="translationReferenceAnswer" class="reference-answer-section">
        <h3 class="answer-title">📝 参考译文</h3>
        <div class="reference-answer-content">
          {{ translationReferenceAnswer }}
        </div>
      </div>
      <!-- Part IV 入库控件移除，统一到页面底部 -->
    </div>

    <!-- 统一的整卷入库控件（置于全页底部） -->
    <div class="result-card structured metadata-card" style="margin-top: 28px">
      <h2>写入 ChromaDB（整卷）</h2>
      
      <!-- 试卷列表选择区域 -->
      <div class="paper-list-section" style="margin-bottom: 20px;">
        <h3 class="sticky-title">选择已有试卷（可选）</h3>
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

      <div class="form-grid">
        <div class="form-field">
          <label>试卷名称 <span style="color: red;">*</span></label>
          <input type="text" v-model.trim="examPaperName" placeholder="例如：2024年6月大学英语四级" />
        </div>
        <div class="form-field">
          <label>试卷描述</label>
          <input type="text" v-model.trim="examPaperDesc" placeholder="可选：试卷描述信息" />
        </div>
      </div>
      <div v-if="selectedExamPaperId" class="selected-paper-info" style="margin: 12px 0; padding: 10px; background: #e8f5e9; border-radius: 4px; font-size: 14px;">
        已选择试卷ID：<strong>{{ selectedExamPaperId }}</strong>，存储时将使用此试卷ID
      </div>
      <div class="actions section-actions">
        <button class="submit-button" :disabled="!canStoreFullExam || savingToChroma" @click="handleStoreFullExam">
          {{ savingToChroma ? '保存中...' : '保存整份试卷' }}
        </button>
      </div>
      <p v-if="chromaMessage" :class="['status-tip', chromaMessageType]">
        {{ chromaMessage }}
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, nextTick, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import axios from 'axios'
import { extractPaperTextCET4, extractPaperStructureCET4, storeSectionAInChromaCET4, storeSectionBInChromaCET4, storeSectionCInChromaCET4, storeWritingInChromaCET4, storeTranslationInChromaCET4, storePart2ABInChromaCET4, storePart2CInChromaCET4, submitPaperAnalysisTask, listPaperAnalysisTasks, fetchPaperAnalysisTaskResult, deletePaperAnalysisTask } from '@/cet4/service/CET4paperAnalysisServiceCET4'
import { ElProgress, ElMessageBox } from 'element-plus'

const route = useRoute()
const currentSubject = ref('')
const subjectLabel = ref('')

const textLoading = ref(false)
const structureLoading = ref(false)
const useAsync = ref(false)
const analysisMode = ref('')
const selectedFile = ref(null)
const fileName = ref('')
const extractedText = ref('')
const structuredJson = ref('')
const structuredJsonB = ref('')
const structuredListening = ref('')
const structuredListeningAB = ref('')
const structuredListeningC = ref('')
const part1 = ref('')
const part2 = ref('')
const part4 = ref('')
const structuredWriting = ref('')
const structuredTranslation = ref('')

const sectionA = ref(null)
const sectionB = ref(null)
const sectionC1 = ref(null)
const sectionC2 = ref(null)
const errorMessage = ref('')
const selectedAnswers = ref({})
const submittedSummary = ref([])
const listeningAnswers = ref({})
const listeningAnswersAB = ref({})
const listeningAnswersC = ref({})
const examPaperId = ref('')
const examPaperName = ref('')
const examPaperDesc = ref('')
const examPaperSubject = ref('')
const savingToChroma = ref(false)
const chromaMessage = ref('')
const chromaMessageType = ref('success')
const examPaperList = ref([])
const selectedExamPaperId = ref(null)
const loadingPaperList = ref(false)
const paperListError = ref('')
const taskList = ref([])
const taskLoading = ref(false)
const taskPollingTimer = ref(null)

// 同步解析进度条相关
const progress = ref(0)
const progressInterval = ref(null)

onMounted(async () => {
  // 从query参数或session获取subject
  try {
    const subjectFromQuery = route.query.subject
    if (subjectFromQuery) {
      currentSubject.value = subjectFromQuery
    } else {
      const response = await fetch('http://localhost:8080/api/cet4/knowledge-manager/get-subject', {
        credentials: 'include'
      })
      const data = await response.json()
      currentSubject.value = data.subject || ''
    }
    examPaperSubject.value = currentSubject.value
    
    // 设置标签
    const subjectMap = {
      'CET6': '大学英语6级',
      'CET4': '大学英语4级',
      'GAOKAO': '高考英语',
      'HIGH2': '高中二年级英语',
      'HIGH1': '高中一年级英语',
      'ZHONGKAO': '中考英语',
      'MIDDLE2': '初中二年级英语',
      'MIDDLE1': '初中一年级英语'
    }
    subjectLabel.value = subjectMap[currentSubject.value] || currentSubject.value
  } catch (error) {
    // 获取失败时使用空值
  }
  
  // 加载试卷列表
  loadExamPaperList()
  fetchTaskList()
  taskPollingTimer.value = setInterval(() => fetchTaskList(false), 15000)
})

onUnmounted(() => {
  if (taskPollingTimer.value) {
    clearInterval(taskPollingTimer.value)
    taskPollingTimer.value = null
  }
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
const selectExamPaper = (paper) => {
  if (selectedExamPaperId.value === paper.id) {
    // 如果已选中，再次点击则取消选择
    selectedExamPaperId.value = null
    examPaperId.value = ''
  } else {
    // 选中新的试卷
    selectedExamPaperId.value = paper.id
    examPaperId.value = String(paper.id)
    examPaperName.value = paper.examPaperEnName || ''
    examPaperDesc.value = paper.examPaperEnDesc || ''
    examPaperSubject.value = paper.examPaperEnSubject || examPaperSubject.value
  }
}

const formatTaskStatus = (status) => {
  switch ((status || '').toUpperCase()) {
    case 'SUCCEEDED':
      return '已完成'
    case 'FAILED':
      return '失败'
    case 'RUNNING':
      return '处理中'
    case 'PENDING':
      return '排队中'
    default:
      return status || '未知'
  }
}

const formatTaskTime = (ts) => {
  if (!ts) return '--'
  const date = new Date(ts)
  if (Number.isNaN(date.getTime())) {
    return ts
  }
  return date.toLocaleString()
}

const displayTaskName = (task) => {
  if (!task) return '试卷解析任务'
  return task.fileName || '试卷解析任务'
}

const actionLabel = (status) => {
  if ((status || '').toUpperCase() === 'FAILED') {
    return '已失败'
  }
  return '处理中'
}

const fetchTaskList = async (showLoading = true) => {
  if (showLoading) {
    taskLoading.value = true
  }
  try {
    const tasks = await listPaperAnalysisTasks(20)
    taskList.value = Array.isArray(tasks) ? tasks : []
  } catch (error) {
    taskList.value = []
  } finally {
    if (showLoading) {
      taskLoading.value = false
    }
  }
}

const removeTaskFromList = (taskId) => {
  if (!taskId) return
  taskList.value = taskList.value.filter(t => t.id !== taskId)
}

const viewTaskResult = async (task) => {
  if (!task || task.status !== 'SUCCEEDED') return
  try {
    const result = await fetchPaperAnalysisTaskResult(task.id)
    await applyStructuredResult(result)
    removeTaskFromList(task.id)
    await deletePaperAnalysisTask(task.id)
  } catch (error) {
    errorMessage.value = '打开解析结果失败：' + (error.message || '未知错误')
  }
}

const deleteTask = async (task) => {
  if (!task) return
  removeTaskFromList(task.id)
  await deletePaperAnalysisTask(task.id)
}

const handleFileChange = async (event) => {
  const target = event.target
  const file = target.files?.[0]
  if (!file) return
  // 放宽类型判断：部分环境下 file.type 可能为空或为 octet-stream
  const nameOk = (file.name || '').toLowerCase().endsWith('.pdf')
  const typeOk = (file.type || '').toLowerCase() === 'application/pdf'
  selectedFile.value = file
  fileName.value = file.name
  useAsync.value = false
  extractedText.value = ''
  structuredJson.value = ''
  structuredJsonB.value = ''
  structuredListening.value = ''
  structuredListeningAB.value = ''
  structuredListeningC.value = ''
  structuredWriting.value = ''
  structuredTranslation.value = ''
  analysisMode.value = ''
  part1.value = ''
  part2.value = ''
  part4.value = ''
  sectionA.value = null
  sectionB.value = null
  sectionC1.value = null
  sectionC2.value = null
  selectedExamPaperId.value = null
  
  errorMessage.value = ''
  selectedAnswers.value = {}
  submittedSummary.value = []
  listeningAnswers.value = {}
  listeningAnswersAB.value = {}
  listeningAnswersC.value = {}
  examPaperId.value = ''
  examPaperName.value = ''
  examPaperDesc.value = ''
  examPaperSubject.value = currentSubject.value
  chromaMessage.value = ''
  // 直接进行结构化解析

  const lowerName = (file.name || '').toLowerCase()
  const isPDF = nameOk || typeOk
  const isTXT = lowerName.endsWith('.txt')
  const isDOC = lowerName.endsWith('.doc') || lowerName.endsWith('.docx')

  try {
    // 保留到按钮触发解析，避免无意间耗时操作
  } catch (err) {
    errorMessage.value = '文件处理失败，请重试或更换文件'
  }
}



const formatStructuredJson = (data) => {
  if (!data) return ''
  if (typeof data === 'string') {
    try {
      const parsed = JSON.parse(data)
      return JSON.stringify(parsed, null, 2)
    } catch (e) {
      return data
    }
  }
  try {
    return JSON.stringify(data, null, 2)
  } catch (e) {
    return ''
  }
}

const applyStructuredResult = async (result) => {
  structuredJson.value = formatStructuredJson(result.structured || {})
  structuredJsonB.value = formatStructuredJson(result.structuredB || {})
  structuredListening.value = formatStructuredJson(result.structuredListening || {})
  structuredListeningAB.value = formatStructuredJson(result.structuredListeningAB || {})
  structuredListeningC.value = formatStructuredJson(result.structuredListeningC || {})
  part1.value = result.part1 || ''
  part2.value = result.part2 || ''
  part4.value = result.part4 || ''
  structuredWriting.value = formatStructuredJson(result.structuredWriting || {})
  structuredTranslation.value = formatStructuredJson(result.structuredTranslation || {})
  analysisMode.value = result.mode || analysisMode.value

  sectionA.value = result.sectionA || null
  sectionB.value = result.sectionB || null
  sectionC1.value = result.sectionC1 || null
  sectionC2.value = result.sectionC2 || null
  await nextTick()
  if (sectionA.value) {
    initializeSelections()
  } else {
    selectedAnswers.value = {}
    submittedSummary.value = []
  }
  if (sectionB.value) {
    initializeSectionBSelections()
  }
  if (sectionC1.value) {
    initializeSectionC1Selections()
  }
  if (sectionC2.value) {
    initializeSectionC2Selections()
  }
  initializeListeningSelections()
}

// 进度条控制方法
const startProgress = () => {
  progress.value = 0
  const totalDurationSeconds = 4 * 60 // 4分钟
  const step = 100 / totalDurationSeconds
  progressInterval.value = setInterval(() => {
    progress.value = Math.min(progress.value + step, 99) // 预留1%由stopProgress收尾
  }, 1000)
}

const stopProgress = () => {
  clearInterval(progressInterval.value)
  progress.value = 100
  setTimeout(() => progress.value = 0, 1000)
}

const handleStructureParse = async () => {
  if (!selectedFile.value) {
    errorMessage.value = '请先上传试卷 PDF 文件'
    return
  }
  structureLoading.value = true
  errorMessage.value = ''
  
  // 同步模式启动进度条
  if (!useAsync.value) {
    startProgress()
  }
  
  try {
    if (useAsync.value) {
      await submitPaperAnalysisTask(selectedFile.value, true)
      await fetchTaskList(false)
      errorMessage.value = ''
      return
    }
    const result = await extractPaperStructureCET4(selectedFile.value)
    analysisMode.value = result?.mode || 'concurrent'
    
    if (result?.success) {
      await applyStructuredResult(result)
    } else {
      throw new Error(result?.message || '解析失败，请稍后重试')
    }
  } catch (error) {
    errorMessage.value = error.message || '解析失败，请稍后重试'
    sectionA.value = null
    sectionB.value = null
    sectionC1.value = null
    sectionC2.value = null
  } finally {
    // 同步模式停止进度条
    if (!useAsync.value) {
      stopProgress()
    }
    structureLoading.value = false
  }
}

const parseJsonSafe = (text) => {
  try {
    return JSON.parse(text)
  } catch {
    return null
  }
}

const hasStructuredContent = (text, rootKey) => {
  const obj = parseJsonSafe(text)
  const root = obj?.[rootKey]
  if (!root || typeof root !== 'object') return false
  return Object.values(root).some((val) => {
    if (val == null) return false
    if (Array.isArray(val)) return val.length > 0
    if (typeof val === 'object') return Object.keys(val).length > 0
    return String(val).trim() !== ''
  })
}

const writingPassage = computed(() => {
  const obj = parseJsonSafe(structuredWriting.value)
  return obj?.writing?.passage || part1.value || ''
})

const writingSegmentId = computed(() => {
  const obj = parseJsonSafe(structuredWriting.value)
  return obj?.writing?.segment_id || ''
})

const writingReferenceAnswer = computed(() => {
  const obj = parseJsonSafe(structuredWriting.value)
  return obj?.writing?.reference_answer || ''
})

const translationPassage = computed(() => {
  const obj = parseJsonSafe(structuredTranslation.value)
  return obj?.translation?.passage || part4.value || ''
})

const translationSegmentId = computed(() => {
  const obj = parseJsonSafe(structuredTranslation.value)
  return obj?.translation?.segment_id || ''
})

const translationReferenceAnswer = computed(() => {
  const obj = parseJsonSafe(structuredTranslation.value)
  return obj?.translation?.reference_answer || ''
})

const listeningUnits = computed(() => {
  const obj = parseJsonSafe(structuredListening.value)
  const arr = Array.isArray(obj?.units) ? obj.units : []
  return arr.map((u) => {
    const unit_type = (u?.unit_type || '').trim()
    const listening_content = (u?.listening_content || '').trim()
    const questions = Array.isArray(u?.question_and_options) ? u.question_and_options : []
    return { unit_type, listening_content, questions }
  })
})

const listeningUnitsAB = computed(() => {
  const obj = parseJsonSafe(structuredListeningAB.value)
  
  const arr = Array.isArray(obj?.units) ? obj.units : []
  const result = arr.map((u) => {
    const unit_type = (u?.unit_type || '').trim()
    const segment_id = (u?.segment_id || '').trim()
    const listening_content = (u?.listening_content || '').trim()
    const questions = Array.isArray(u?.question_and_options) ? u.question_and_options : []
    const answers = Array.isArray(u?.answers) ? u.answers : []
    return { unit_type, segment_id, listening_content, questions, answers }
  })
  
  return result
})

const listeningUnitsC = computed(() => {
  const obj = parseJsonSafe(structuredListeningC.value)
  const arr = Array.isArray(obj?.units) ? obj.units : []
  return arr.map((u) => {
    const unit_type = (u?.unit_type || '').trim()
    const segment_id = (u?.segment_id || '').trim()
    const listening_content = (u?.listening_content || '').trim()
    const questions = Array.isArray(u?.question_and_options) ? u.question_and_options : []
    const answers = Array.isArray(u?.answers) ? u.answers : []
    return { unit_type, segment_id, listening_content, questions, answers }
  })
})

const initializeListeningSelections = () => {
  listeningAnswers.value = {}
  listeningAnswersAB.value = {}
  listeningAnswersC.value = {}
  listeningUnits.value.forEach((unit, uIdx) => {
    ;(unit.questions || []).forEach((_, qIdx) => {
      listeningAnswers.value[`${uIdx}-${qIdx}`] = ''
    })
  })
  listeningUnitsAB.value.forEach((unit, uIdx) => {
    ;(unit.questions || []).forEach((_, qIdx) => {
      listeningAnswersAB.value[`ab-${uIdx}-${qIdx}`] = ''
    })
  })
  listeningUnitsC.value.forEach((unit, uIdx) => {
    ;(unit.questions || []).forEach((_, qIdx) => {
      listeningAnswersC.value[`c-${uIdx}-${qIdx}`] = ''
    })
  })
}

const displayUnitQuestionNo = (uIdx, qIdx) => {
  // 按单元顺序编号或仅显示本单元内序号，这里采用“单元内序号”
  return String(qIdx + 1)
}

const unitKeyAB = (uIdx, qIdx) => `ab-${uIdx}-${qIdx}`
const unitKeyC = (uIdx, qIdx) => `c-${uIdx}-${qIdx}`

// 从题目对象提取选项，兼容多种返回格式
const optionsOf = (q) => {
  try {
    // 标准：q['选项'] 为对象，A/B/C/D 为 key
    const std = q && q['选项']
    const preferredOrder = ['A', 'B', 'C', 'D']
    if (std && typeof std === 'object' && !Array.isArray(std)) {
      const arr = []
      // 先按 A-D 顺序推入存在的
      preferredOrder.forEach((k) => {
        if (std[k] && String(std[k]).trim()) {
          arr.push({ mark: k, text: String(std[k]).trim() })
        }
      })
      // 若没有任何 A-D 命中，则兜底把所有键值推入（保持可见）
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

    // 兼容：q.options 为数组，元素包含 option_mark/option_content
    const arr1 = q && Array.isArray(q.options) ? q.options : []
    if (arr1.length) {
      return arr1
        .map((o) => ({
          mark: String(o.option_mark || o.mark || '').trim(),
          text: String(o.option_content || o.text || o.content || '').trim(),
        }))
        .filter((o) => o.mark && o.text)
    }

    // 兼容：q.choices 为数组
    const arr2 = q && Array.isArray(q.choices) ? q.choices : []
    if (arr2.length) {
      return arr2
        .map((o) => ({
          mark: String(o.option_mark || o.mark || '').trim(),
          text: String(o.option_content || o.text || o.content || '').trim(),
        }))
        .filter((o) => o.mark && o.text)
    }
  } catch (e) {
    // ignore
  }
  return []
}

// 兼容模板中遗留引用 structuredResult 的告警
const structuredResult = computed(() => structuredJson.value)

// Part II（Listening）题号生成：AB 从 1 开始，C 接续 AB
const listeningQuestionNumbersAB = computed(() => {
  let count = 0
  listeningUnitsAB.value.forEach((unit) => {
    const qs = Array.isArray(unit.questions) ? unit.questions : []
    count += qs.length
  })
  return count ? Array.from({ length: count }, (_, i) => String(1 + i)) : []
})

const listeningQuestionNumbersC = computed(() => {
  let countAB = listeningQuestionNumbersAB.value.length
  let countC = 0
  listeningUnitsC.value.forEach((unit) => {
    const qs = Array.isArray(unit.questions) ? unit.questions : []
    countC += qs.length
  })
  return countC ? Array.from({ length: countC }, (_, i) => String(countAB + 1 + i)) : []
})

const canStoreWriting = computed(() => {
  return Boolean(
    writingPassage.value &&
    examPaperId.value.trim() &&
    examPaperName.value.trim() &&
    examPaperSubject.value.trim() &&
    structuredWriting.value
  )
})

const canStoreTranslation = computed(() => {
  return Boolean(
    translationPassage.value &&
    examPaperId.value.trim() &&
    examPaperName.value.trim() &&
    examPaperSubject.value.trim() &&
    structuredTranslation.value
  )
})

const handleStoreWriting = async () => {
  if (!canStoreWriting.value) {
    chromaMessageType.value = 'error'
    chromaMessage.value = '请完整填写试卷信息后再保存写作部分'
    return
  }
  savingToChroma.value = true
  chromaMessage.value = ''
  try {
    const payload = {
      examPaperId: examPaperId.value.trim(),
      examPaperName: examPaperName.value.trim(),
      subject: examPaperSubject.value.trim(),
      document: structuredWriting.value,
      questionType: '写作',
      partId: 1,
      examPaperEnSource: 'AIfromreal'
    }
    const res = await storeWritingInChromaCET4(payload)
    chromaMessageType.value = 'success'
    chromaMessage.value = res?.message || 'Part I（写作）已写入 ChromaDB'
  } catch (error) {
    chromaMessageType.value = 'error'
    chromaMessage.value = error.message || '写入 ChromaDB 失败，请稍后重试'
  } finally {
    savingToChroma.value = false
  }
}

const handleStoreTranslation = async () => {
  if (!canStoreTranslation.value) {
    chromaMessageType.value = 'error'
    chromaMessage.value = '请完整填写试卷信息后再保存翻译部分'
    return
  }
  savingToChroma.value = true
  chromaMessage.value = ''
  try {
    const payload = {
      examPaperId: examPaperId.value.trim(),
      examPaperName: examPaperName.value.trim(),
      subject: examPaperSubject.value.trim(),
      document: structuredTranslation.value,
      questionType: '翻译',
      partId: 4,
      examPaperEnSource: 'AIfromreal'
    }
    const res = await storeTranslationInChromaCET4(payload)
    chromaMessageType.value = 'success'
    chromaMessage.value = res?.message || 'Part IV（翻译）已写入 ChromaDB'
  } catch (error) {
    chromaMessageType.value = 'error'
    chromaMessage.value = error.message || '写入 ChromaDB 失败，请稍后重试'
  } finally {
    savingToChroma.value = false
  }
}

// 统一整卷入库
const canStoreFullExam = computed(() => {
  return Boolean(
    examPaperName.value.trim() &&
    examPaperSubject.value.trim()
  )
})

const handleStoreFullExam = async () => {
  if (!canStoreFullExam.value) {
    chromaMessageType.value = 'error'
    chromaMessage.value = '请填写试卷名称后再保存整卷'
    return
  }
  
  // 验证试卷名称不能为空
  if (!examPaperName.value.trim()) {
    chromaMessageType.value = 'error'
    chromaMessage.value = '试卷名称不能为空'
    return
  }
  
  savingToChroma.value = true
  chromaMessage.value = ''
  
  try {
    let newExamPaperId = ''
    
    // 如果已选择试卷，直接使用选中的 exam_paper_en_id
    if (selectedExamPaperId.value) {
      newExamPaperId = String(selectedExamPaperId.value)
      examPaperId.value = newExamPaperId
    } else {
      // 如果没有选择试卷,则创建新的试卷记录
      const examPaperEnData = {
        examPaperEnName: examPaperName.value.trim(),
        examPaperEnDesc: examPaperDesc.value.trim() || null,
        examPaperEnSubject: examPaperSubject.value.trim(),
        examPaperEnSource: 'real' // 解析真题
      }
      
      const examPaperResponse = await axios.post(
        'http://localhost:8080/api/cet4/exam-paper-en/',
        examPaperEnData,
        { withCredentials: true }
      )
      
      if (!examPaperResponse.data.success) {
        throw new Error(examPaperResponse.data.message || '创建试卷记录失败')
      }
      
      // 获取插入后的 id 作为 exam_paper_id
      newExamPaperId = String(examPaperResponse.data.id)
      examPaperId.value = newExamPaperId
      
      // 刷新试卷列表，以便新创建的试卷出现在列表中
      await loadExamPaperList()
    }
    
    // 3. 按照原来的逻辑，页面展示了几个 Part 就保存几个 Part 到 chromaDB
    const partsToSave = []
    
    // Part I（写作）
    if (hasStructuredContent(structuredWriting.value, 'writing')) {
      partsToSave.push({
        type: 'writing',
        payload: {
          examPaperId: newExamPaperId,
          examPaperName: examPaperName.value.trim(),
          subject: examPaperSubject.value.trim(),
          document: structuredWriting.value,
          questionType: '写作',
          partId: 1
        },
        save: storeWritingInChromaCET4
      })
    }

    // Section A（选词填空）
    if (sectionA.value && sectionABlankNumbers.value.length) {
      partsToSave.push({
        type: 'sectionA',
        payload: {
          examPaperId: newExamPaperId,
          examPaperName: examPaperName.value.trim(),
          subject: examPaperSubject.value.trim(),
          document: structuredJson.value,
          blankNumbers: sectionABlankNumbers.value,
          questionType: '选词填空',
          partId: 3,
          sectionId: 'A',
        },
        save: storeSectionAInChromaCET4
      })
    }

    // Section B（段落匹配）
    if (sectionB.value && sectionBQuestionNumbers.value.length) {
      partsToSave.push({
        type: 'sectionB',
        payload: {
          examPaperId: newExamPaperId,
          examPaperName: examPaperName.value.trim(),
          subject: examPaperSubject.value.trim(),
          document: structuredJsonB.value,
          blankNumbers: sectionBQuestionNumbers.value,
          questionType: '段落匹配',
          partId: 3,
          sectionId: 'B',
        },
        save: storeSectionBInChromaCET4
      })
    }

    // Section C1和C2（篇章阅读）- 合并存储，后端会分别处理
    if ((sectionC1.value || sectionC2.value) && sectionCAllQuestionNumbers.value.length) {
      // 构建包含section_c1和section_c2的完整document
      const cDocument = {
        reading_comprehension: {}
      }
      if (sectionC1.value) {
        cDocument.reading_comprehension.section_c1 = sectionC1.value
      }
      if (sectionC2.value) {
        cDocument.reading_comprehension.section_c2 = sectionC2.value
      }
      
      partsToSave.push({
        type: 'sectionC',
        payload: {
          examPaperId: newExamPaperId,
          examPaperName: examPaperName.value.trim(),
          subject: examPaperSubject.value.trim(),
          document: JSON.stringify(cDocument),
          blankNumbers: sectionCAllQuestionNumbers.value,
          questionType: '篇章阅读',
          partId: 3,
          sectionId: 'C',
        },
        save: storeSectionCInChromaCET4
      })
    }


    // Part II（Listening · Section A/B）
    if (structuredListeningAB.value && listeningQuestionNumbersAB.value.length) {
      partsToSave.push({
        type: 'listeningAB',
        payload: {
          examPaperId: newExamPaperId,
          examPaperName: examPaperName.value.trim(),
          subject: examPaperSubject.value.trim(),
          document: structuredListeningAB.value,
          blankNumbers: listeningQuestionNumbersAB.value,
          questionType: '听力',
          partId: 2,
          sectionId: 'AB',
        },
        save: storePart2ABInChromaCET4
      })
    }

    // Part II（Listening · Section C）
    if (structuredListeningC.value && listeningQuestionNumbersC.value.length) {
      partsToSave.push({
        type: 'listeningC',
        payload: {
          examPaperId: newExamPaperId,
          examPaperName: examPaperName.value.trim(),
          subject: examPaperSubject.value.trim(),
          document: structuredListeningC.value,
          blankNumbers: listeningQuestionNumbersC.value,
          questionType: '听力',
          partId: 2,
          sectionId: 'C',
        },
        save: storePart2CInChromaCET4
      })
    }

    // Part IV（翻译）
    if (hasStructuredContent(structuredTranslation.value, 'translation')) {
      partsToSave.push({
        type: 'translation',
        payload: {
          examPaperId: newExamPaperId,
          examPaperName: examPaperName.value.trim(),
          subject: examPaperSubject.value.trim(),
          document: structuredTranslation.value,
          questionType: '翻译',
          partId: 4
        },
        save: storeTranslationInChromaCET4
      })
    }

    // 4. 依次保存所有 Part 到 chromaDB，并计算实际保存的题目数量
    if (partsToSave.length === 0) {
      throw new Error('没有可保存的试卷内容')
    }

    console.info('[SaveExam] partsToSave types', partsToSave.map((p) => p.type))

    let totalSavedCount = 0
    for (const part of partsToSave) {
      await part.save(part.payload)
      // 计算每个部分实际保存的题目数量
      switch (part.type) {
        case 'writing':
        case 'translation':
        case 'sectionA':
        case 'sectionB':
          totalSavedCount += 1
          break
        case 'sectionC':
          // Section C 包含 C1 和 C2，各保存一条
          let sectionCCount = 0
          if (sectionC1.value) sectionCCount++
          if (sectionC2.value) sectionCCount++
          totalSavedCount += sectionCCount
          break
        case 'listeningAB':
          // 听力AB：每个unit保存一条记录到ChromaDB
          // 从payload的document中解析units数量，确保与保存的数据一致
          try {
            const abDoc = JSON.parse(part.payload.document)
            const abUnitsCount = Array.isArray(abDoc?.units) ? abDoc.units.length : 0
            totalSavedCount += abUnitsCount || 1
            console.info('[SaveExam] listeningAB units', {
              fromPayloadUnits: abUnitsCount,
              fromStateUnits: listeningUnitsAB.value.length,
              totalSavedCount
            })
          } catch {
            totalSavedCount += listeningUnitsAB.value.length || 1
            console.info('[SaveExam] listeningAB units (fallback state)', {
              fromStateUnits: listeningUnitsAB.value.length,
              totalSavedCount
            })
          }
          break
        case 'listeningC':
          // 听力C：每个unit保存一条记录到ChromaDB
          // 从payload的document中解析units数量，确保与保存的数据一致
          try {
            const cDoc = JSON.parse(part.payload.document)
            const cUnitsCount = Array.isArray(cDoc?.units) ? cDoc.units.length : 0
            totalSavedCount += cUnitsCount || 1
            console.info('[SaveExam] listeningC units', {
              fromPayloadUnits: cUnitsCount,
              fromStateUnits: listeningUnitsC.value.length,
              totalSavedCount
            })
          } catch {
            totalSavedCount += listeningUnitsC.value.length || 1
            console.info('[SaveExam] listeningC units (fallback state)', {
              fromStateUnits: listeningUnitsC.value.length,
              totalSavedCount
            })
          }
          break
        default:
          totalSavedCount += 1
      }
    }

    console.info('[SaveExam] totalSavedCount (all parts)', totalSavedCount)

    chromaMessageType.value = 'success'
    chromaMessage.value = ''
    
    // 使用弹窗提示
    ElMessageBox.alert(
      `整份试卷已按分段顺序写入 ChromaDB（共保存 ${totalSavedCount} 道题目）`,
      '保存成功',
      {
        confirmButtonText: '确定',
        type: 'success'
      }
    )
  } catch (error) {
    chromaMessageType.value = 'error'
    if (error.response?.data?.message) {
      chromaMessage.value = error.response.data.message
    } else if (error.message) {
      chromaMessage.value = error.message
    } else {
      chromaMessage.value = '写入整卷失败，请稍后重试'
    }
  } finally {
    savingToChroma.value = false
  }
}
const sectionAPassage = computed(() => sectionA.value?.passage || '[文档中未提及此内容]')
const sectionASegmentId = computed(() => sectionA.value?.segment_id || '')
const sectionAAnswers = computed(() => sectionA.value?.answers || [])
const sectionABlankNumbers = computed(() => {
  const provided = sectionA.value?.blank_numbers
  if (Array.isArray(provided) && provided.length) {
    return [...provided]
      .map((num) => {
        if (typeof num === 'number') return String(num)
        if (typeof num === 'string') return num.trim()
        return ''
      })
      .filter((num) => num)
      .sort((a, b) => Number(a) - Number(b))
  }
  const blankCount = Number(sectionA.value?.blank_count || 0)
  const startNumber = Number(sectionA.value?.start_number || 1)
  return blankCount ? Array.from({ length: blankCount }, (_, idx) => String(startNumber + idx)) : []
})
const sectionAOptions = computed(() => sectionA.value?.options || [])
const optionKey = (option) => option?.letter || Math.random().toString(36).slice(2)
const displayOptionMark = (option) => option?.letter || ''
const displayOptionText = (option) => option?.word || '[文档中未提及此内容]'
const optionTextMap = computed(() => {
  const map = {}
  sectionAOptions.value.forEach((option) => {
    const mark = displayOptionMark(option)
    if (mark) map[mark] = displayOptionText(option)
  })
  return map
})
const initializeSelections = () => {
  selectedAnswers.value = {}
  submittedSummary.value = []
  sectionABlankNumbers.value.forEach((number) => {
    selectedAnswers.value[number] = ''
  })
}
const canSubmit = computed(() => sectionABlankNumbers.value.length > 0 && sectionABlankNumbers.value.every((number) => selectedAnswers.value[number]))
const submitAnswers = () => {
  const map = optionTextMap.value
  submittedSummary.value = sectionABlankNumbers.value.map((number) => {
    const answer = selectedAnswers.value[number] || '未选择'
    return {
      number,
      answer,
      word: answer !== '未选择' ? map[answer] || '' : '',
    }
  })
}

// Section B 相关
const sectionBAnswers = ref({})
const sectionBSegmentId = computed(() => sectionB.value?.segment_id || '')
const sectionBAnswersData = computed(() => sectionB.value?.answers || [])
const sectionBStatements = computed(() => sectionB.value?.statements || [])
const sectionBArticle = computed(() => sectionB.value?.article || [])
const sectionBParagraphMarks = computed(() => {
  return sectionBArticle.value.map(p => p.paragraph_mark).filter(m => m)
})

const initializeSectionBSelections = () => {
  sectionBAnswers.value = {}
  sectionBStatements.value.forEach((statement) => {
    const qNum = statement.question_number
    if (qNum) {
      sectionBAnswers.value[qNum] = ''
    }
  })
}

// Section C1 相关
const sectionC1Answers = ref({})
const sectionC1SegmentId = computed(() => sectionC1.value?.segment_id || '')
const sectionC1AnswersData = computed(() => sectionC1.value?.answers || [])

const initializeSectionC1Selections = () => {
  sectionC1Answers.value = {}
  if (sectionC1.value?.questions) {
    sectionC1.value.questions.forEach((question) => {
      const qNum = question.question_number
      if (qNum) {
        sectionC1Answers.value[qNum] = ''
      }
    })
  }
}

const sectionC1QuestionNumbers = computed(() => {
  const nums = []
  if (sectionC1.value?.questions) {
    sectionC1.value.questions.forEach((q) => {
      const no = q?.question_number ? String(q.question_number).trim() : ''
      if (no) nums.push(no)
    })
  }
  nums.sort((a, b) => Number(a) - Number(b))
  return nums
})

// Section C2 相关
const sectionC2Answers = ref({})
const sectionC2SegmentId = computed(() => sectionC2.value?.segment_id || '')
const sectionC2AnswersData = computed(() => sectionC2.value?.answers || [])

const initializeSectionC2Selections = () => {
  sectionC2Answers.value = {}
  if (sectionC2.value?.questions) {
    sectionC2.value.questions.forEach((question) => {
      const qNum = question.question_number
      if (qNum) {
        sectionC2Answers.value[qNum] = ''
      }
    })
  }
}

const sectionC2QuestionNumbers = computed(() => {
  const nums = []
  if (sectionC2.value?.questions) {
    sectionC2.value.questions.forEach((q) => {
      const no = q?.question_number ? String(q.question_number).trim() : ''
      if (no) nums.push(no)
    })
  }
  nums.sort((a, b) => Number(a) - Number(b))
  return nums
})

// 合并Section C1和C2的题号（用于存储）
const sectionCAllQuestionNumbers = computed(() => {
  const nums = [...sectionC1QuestionNumbers.value, ...sectionC2QuestionNumbers.value]
  nums.sort((a, b) => Number(a) - Number(b))
  return nums
})

// 查询功能已移除

const canStoreSectionA = computed(() => {
  return Boolean(
    sectionA.value &&
      sectionABlankNumbers.value.length &&
      examPaperId.value.trim() &&
      examPaperName.value.trim() &&
      examPaperSubject.value.trim() &&
      structuredJson.value
  )
})

const handleStoreSectionA = async () => {
  if (!canStoreSectionA.value) {
    chromaMessageType.value = 'error'
    chromaMessage.value = '请完整填写试卷信息并完成题号解析'
    return
  }
  savingToChroma.value = true
  chromaMessage.value = ''
  try {
    const payload = {
      examPaperId: examPaperId.value.trim(),
      examPaperName: examPaperName.value.trim(),
      subject: examPaperSubject.value.trim(),
      document: structuredJson.value,
      blankNumbers: sectionABlankNumbers.value,
      questionType: '选词填空',
      partId: 3,
      sectionId: 'A',
    }
    const response = await storeSectionAInChromaCET4(payload)
    chromaMessageType.value = 'success'
    const successMsg = response?.message || 'Section A 已写入 ChromaDB'
    chromaMessage.value = successMsg
    window.alert(successMsg || '保存成功')
  } catch (error) {
    chromaMessageType.value = 'error'
    chromaMessage.value = error.message || '写入 ChromaDB 失败，请稍后重试'
  } finally {
    savingToChroma.value = false
  }
}

const sectionBQuestionNumbers = computed(() => {
  const nums = (sectionBStatements.value || [])
    .map(s => (s?.question_number ? String(s.question_number).trim() : ''))
    .filter(Boolean)
    .sort((a, b) => Number(a) - Number(b))
  return nums
})

const canStoreSectionB = computed(() => {
  return Boolean(
    sectionB.value &&
    sectionBQuestionNumbers.value.length &&
    examPaperId.value.trim() &&
    examPaperName.value.trim() &&
    examPaperSubject.value.trim() &&
    structuredJsonB.value
  )
})

const handleStoreSectionB = async () => {
  if (!canStoreSectionB.value) {
    chromaMessageType.value = 'error'
    chromaMessage.value = '请完整填写试卷信息并确保 Section B 题号解析成功'
    return
  }
  savingToChroma.value = true
  chromaMessage.value = ''
  try {
    const payload = {
      examPaperId: examPaperId.value.trim(),
      examPaperName: examPaperName.value.trim(),
      subject: examPaperSubject.value.trim(),
      document: structuredJsonB.value,
      blankNumbers: sectionBQuestionNumbers.value,
      questionType: '段落匹配',
      partId: 3,
      sectionId: 'B',
    }
    const response = await storeSectionBInChromaCET4(payload)
    chromaMessageType.value = 'success'
    const successMsg = response?.message || 'Section B 已写入 ChromaDB'
    chromaMessage.value = successMsg
    window.alert(successMsg || '保存成功')
  } catch (error) {
    chromaMessageType.value = 'error'
    chromaMessage.value = error.message || '写入 ChromaDB 失败，请稍后重试'
  } finally {
    savingToChroma.value = false
  }
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

.actions {
  margin: 18px 0 0;
  display: flex;
  justify-content: flex-end;
}

.query-bar {
  margin-top: 18px;
  display: flex;
  gap: 12px;
}

.query-input {
  flex: 1;
  border: 1px solid #cfd8dc;
  border-radius: 8px;
  padding: 10px 12px;
  font-size: 15px;
}

.query-button {
  padding: 10px 20px;
  border-radius: 999px;
  border: none;
  background: linear-gradient(135deg, #8e44ad 0%, #6c3483 100%);
  color: #fff;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.query-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  box-shadow: none;
}

.query-button:not(:disabled):hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 18px rgba(142, 68, 173, 0.35);
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

.section-block {
  margin-top: 16px;
}

.section-block h3 {
  margin-bottom: 8px;
  color: #2c3e50;
}

.question-no {
  font-weight: 600;
  margin-right: 8px;
  color: #2c3e50;
}

.option-mark {
  font-weight: 600;
  color: #2c3e50;
}

.option-text,
.question-text {
  color: #37474f;
}

.question-row {
  display: flex;
  flex-direction: column;
  gap: 6px;
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
  flex-wrap: wrap;
}

.section-actions {
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

.answer-summary {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.answer-summary li {
  display: flex;
  gap: 12px;
  align-items: center;
  border-bottom: 1px dashed #dfe6e9;
  padding-bottom: 6px;
}

.answer-choice {
  font-weight: 600;
  color: #2c3e50;
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

.reset-button {
  border: 1px solid #cfd8dc;
  background: #fff;
  color: #2c3e50;
  padding: 8px 16px;
  border-radius: 999px;
  cursor: pointer;
  font-weight: 600;
  transition: all 0.2s ease;
}

.reset-button:hover {
  border-color: #27ae60;
  color: #27ae60;
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

.options-block h3,
.answer-board h3 {
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

/* Section B 段落匹配样式 */
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

.statement-text {
  margin: 0;
  color: #37474f;
  line-height: 1.6;
  flex: 1;
}

.paragraph-select {
  width: 100%;
  max-width: 200px;
  padding: 10px 12px;
  border: 1px solid #cfd8dc;
  border-radius: 8px;
  font-size: 15px;
  background: #fff;
  cursor: pointer;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.paragraph-select:focus {
  outline: none;
  border-color: #43a047;
  box-shadow: 0 0 0 2px rgba(67, 160, 71, 0.15);
}

/* Section C 篇章阅读样式 */
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

/* 试卷列表样式 */
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

.selected-paper-info {
  color: #2e7d32;
}

/* Segment ID 徽章样式 */
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

/* 答案展示样式 */
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

.answers-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(80px, 1fr));
  gap: 12px;
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

/* 参考答案样式（写作和翻译） */
.reference-answer-section {
  margin-top: 20px;
  padding: 20px;
  background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);
  border-radius: 12px;
  border: 2px solid #fbbf24;
  box-shadow: 0 4px 12px rgba(251, 191, 36, 0.15);
}

.reference-answer-content {
  color: #78350f;
  line-height: 1.8;
  font-size: 15px;
  white-space: pre-wrap;
  word-break: break-word;
  background: #fff;
  padding: 16px;
  border-radius: 8px;
  border: 1px solid #fcd34d;
}

.concurrent-toggle {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 12px;
}

.parse-button {
  padding: 8px 16px;
  background: linear-gradient(135deg, #4f46e5 0%, #9333ea 100%);
  color: #fff;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  box-shadow: 0 4px 10px rgba(79, 70, 229, 0.35);
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}

.parse-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  box-shadow: none;
}

.parse-button:not(:disabled):hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 14px rgba(79, 70, 229, 0.45);
}

.mode-tag {
  font-size: 12px;
  padding: 4px 8px;
  background: #eef2ff;
  border: 1px solid #c7d2fe;
  border-radius: 8px;
  color: #3730a3;
}

.task-list-section {
  margin-top: 20px;
  padding: 16px;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  background-color: #fafafa;
}

.task-list-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.task-list-header h3 {
  margin: 0;
  font-size: 16px;
  color: #333;
}

.link-btn {
  background: none;
  border: none;
  color: #4CAF50;
  cursor: pointer;
  font-size: 14px;
  padding: 4px 8px;
}

.link-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.task-empty {
  text-align: center;
  color: #888;
  font-size: 14px;
  padding: 20px 0;
}

.task-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.task-item {
  display: flex;
  gap: 16px;
  align-items: center;
  justify-content: space-between;
  padding: 12px;
  background-color: #fff;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.task-info {
  flex: 1;
}

.task-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.task-name {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.task-status-pill {
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 12px;
  background-color: #e0e0e0;
  color: #333;
}

.task-status-pill.SUCCEEDED {
  background-color: #e8f5e9;
  color: #2e7d32;
}

.task-status-pill.RUNNING,
.task-status-pill.PENDING {
  background-color: #fff3cd;
  color: #8a6d3b;
}

.task-status-pill.FAILED {
  background-color: #fdecea;
  color: #c62828;
}

.task-meta {
  font-size: 13px;
  color: #666;
  margin: 6px 0;
}

.task-error {
  margin-top: 6px;
  color: #c62828;
  font-size: 13px;
}

.task-actions {
  display: flex;
  align-items: center;
}

.task-view-btn {
  padding: 6px 14px;
  border: none;
  border-radius: 4px;
  background-color: #4CAF50;
  color: #fff;
  cursor: pointer;
  font-size: 14px;
}

.task-view-btn.disabled {
  background-color: #ccc;
  cursor: not-allowed;
}

.task-view-btn + .task-view-btn {
  margin-left: 8px;
}

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
}
</style>

