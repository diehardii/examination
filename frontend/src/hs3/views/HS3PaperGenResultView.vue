<template>
  <div class="display-wrapper">
    <h1>高考试卷生成结果</h1>

    <!-- 试卷信息 -->
    <div class="paper-info-card">
      <div class="info-item">
        <span class="info-label">试卷ID:</span>
        <span class="info-value">{{ examPaperEnId || '待保存' }}</span>
      </div>
      <div class="info-item">
        <span class="info-label">题目段落数:</span>
        <span class="info-value">{{ segments.length }}</span>
      </div>
      <div class="info-item" v-if="part1Segments.length">
        <span class="info-label">听力单元数:</span>
        <span class="info-value">{{ part1Segments.length }}</span>
      </div>
      <div class="info-item" v-if="part2Segments.length">
        <span class="info-label">阅读理解单元数:</span>
        <span class="info-value">{{ part2Segments.length }}</span>
      </div>
      <div class="info-item" v-if="part3Segments.length">
        <span class="info-label">语言知识运用单元数:</span>
        <span class="info-value">{{ part3Segments.length }}</span>
      </div>
      <div class="info-item" v-if="part4Segments.length">
        <span class="info-label">写作单元数:</span>
        <span class="info-value">{{ part4Segments.length }}</span>
      </div>
    </div>

    <!-- 第一部分：听力 -->
    <div v-if="part1Segments.length" class="result-card structured exam-section">
      <div class="section-header">
        <div>
          <h2>第一部分 · 听力</h2>
          <p class="section-meta">共 {{ part1Segments.length }} 个单元</p>
        </div>
      </div>

      <div
        v-for="(segmentData, sIdx) in part1Segments"
        :key="`part1-${sIdx}`"
        class="passage-section"
      >
        <div class="passage-header">
          <h3>{{ getSegmentTitle(segmentData, sIdx) }}</h3>
          <span v-if="segmentData.topic" class="topic-badge">主题: {{ segmentData.topic }}</span>
        </div>

        <!-- 听力材料 -->
        <div v-if="segmentData.content" class="passage-content-block">
          <h4>听力原文</h4>
          <pre class="passage-text">{{ segmentData.content }}</pre>
        </div>

        <!-- 题目列表 -->
        <div v-if="segmentData.questions && segmentData.questions.items" class="questions-block">
          <div
            v-for="(q, qIdx) in segmentData.questions.items"
            :key="`part1-${sIdx}-q-${qIdx}`"
            class="question-item"
          >
            <div class="question-header">
              <span class="question-no">{{ q.question_number }}</span>
              <p class="question-text">{{ q.question_content }}</p>
            </div>
            <div class="options-list" v-if="q.options && q.options.length">
              <label
                v-for="opt in q.options"
                :key="opt.option_mark"
                class="radio-option"
                :class="{ 'correct-answer': opt.option_mark === q.answer }"
              >
                <input
                  type="radio"
                  :name="`part1-${sIdx}-q-${qIdx}`"
                  :value="opt.option_mark"
                  disabled
                />
                <span class="option-label">
                  <strong>{{ opt.option_mark }}.</strong> {{ opt.option_content }}
                </span>
                <span v-if="opt.option_mark === q.answer" class="answer-tag">正确答案</span>
              </label>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 第二部分：阅读理解 -->
    <div v-if="part2Segments.length" class="result-card structured exam-section">
      <div class="section-header">
        <div>
          <h2>第二部分 · 阅读理解</h2>
          <p class="section-meta">共 {{ part2Segments.length }} 个单元</p>
        </div>
      </div>

      <div
        v-for="(segmentData, sIdx) in part2Segments"
        :key="`part2-${sIdx}`"
        class="passage-section"
      >
        <div class="passage-header">
          <h3>{{ getSegmentTitle(segmentData, sIdx) }}</h3>
          <span v-if="segmentData.topic" class="topic-badge">主题: {{ segmentData.topic }}</span>
        </div>

        <!-- 阅读文章 -->
        <div v-if="segmentData.content" class="passage-content-block">
          <h4>阅读材料</h4>
          <pre class="passage-text">{{ segmentData.content }}</pre>
        </div>

        <!-- 七选五选项（仅显示一次） -->
        <div v-if="isSevenChooseFive(segmentData) && segmentData.questions && segmentData.questions.items && segmentData.questions.items.length" class="word-options-block">
          <h5>备选选项</h5>
          <div class="word-options-grid">
            <div v-for="opt in segmentData.questions.items[0].options" :key="opt.option_mark" class="word-option-item">
              <strong>{{ opt.option_mark }}.</strong> {{ opt.option_content }}
            </div>
          </div>
        </div>

        <!-- 题目列表 -->
        <div v-if="segmentData.questions && segmentData.questions.items" class="questions-block">
          <div
            v-for="(q, qIdx) in segmentData.questions.items"
            :key="`part2-${sIdx}-q-${qIdx}`"
            class="question-item"
          >
            <div class="question-header">
              <span class="question-no">{{ q.question_number }}</span>
              <p v-if="q.question_content" class="question-text">{{ q.question_content }}</p>
            </div>
            <!-- 非七选五题目显示选项 -->
            <div v-if="!isSevenChooseFive(segmentData) && q.options && q.options.length" class="options-list">
              <label
                v-for="opt in q.options"
                :key="opt.option_mark"
                class="radio-option"
                :class="{ 'correct-answer': opt.option_mark === q.answer }"
              >
                <input
                  type="radio"
                  :name="`part2-${sIdx}-q-${qIdx}`"
                  :value="opt.option_mark"
                  disabled
                />
                <span class="option-label">
                  <strong>{{ opt.option_mark }}.</strong> {{ opt.option_content }}
                </span>
                <span v-if="opt.option_mark === q.answer" class="answer-tag">正确答案</span>
              </label>
            </div>
            <!-- 七选五题目显示答案 -->
            <div v-if="isSevenChooseFive(segmentData)" class="correct-answer-display">
              <span class="answer-tag">正确答案: {{ q.answer }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 第三部分：语言知识运用 -->
    <div v-if="part3Segments.length" class="result-card structured exam-section">
      <div class="section-header">
        <div>
          <h2>第三部分 · 语言知识运用</h2>
          <p class="section-meta">共 {{ part3Segments.length }} 个单元</p>
        </div>
      </div>

      <div
        v-for="(segmentData, sIdx) in part3Segments"
        :key="`part3-${sIdx}`"
        class="passage-section"
      >
        <div class="passage-header">
          <h3>{{ getSegmentTitle(segmentData, sIdx) }}</h3>
          <span v-if="segmentData.topic" class="topic-badge">主题: {{ segmentData.topic }}</span>
        </div>

        <!-- 短文内容 -->
        <div v-if="segmentData.content" class="passage-content-block">
          <h4>{{ isCloze(segmentData) ? '完形填空短文' : '语法填空短文' }}</h4>
          <pre class="passage-text">{{ segmentData.content }}</pre>
        </div>

        <!-- 题目列表 -->
        <div v-if="segmentData.questions && segmentData.questions.items" class="questions-block">
          <div
            v-for="(q, qIdx) in segmentData.questions.items"
            :key="`part3-${sIdx}-q-${qIdx}`"
            class="question-item"
          >
            <div class="question-header">
              <span class="question-no">{{ q.question_number }}</span>
            </div>
            <!-- 完形填空显示选项 -->
            <div v-if="isCloze(segmentData) && q.options && q.options.length" class="options-list">
              <label
                v-for="opt in q.options"
                :key="opt.option_mark"
                class="radio-option"
                :class="{ 'correct-answer': opt.option_mark === q.answer }"
              >
                <input
                  type="radio"
                  :name="`part3-${sIdx}-q-${qIdx}`"
                  :value="opt.option_mark"
                  disabled
                />
                <span class="option-label">
                  <strong>{{ opt.option_mark }}.</strong> {{ opt.option_content }}
                </span>
                <span v-if="opt.option_mark === q.answer" class="answer-tag">正确答案</span>
              </label>
            </div>
            <!-- 语法填空显示答案 -->
            <div v-if="!isCloze(segmentData)" class="correct-answer-display">
              <span class="answer-tag">正确答案: {{ q.answer }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 第四部分：写作 -->
    <div v-if="part4Segments.length" class="result-card structured exam-section">
      <div class="section-header">
        <div>
          <h2>第四部分 · 写作</h2>
          <p class="section-meta">共 {{ part4Segments.length }} 个单元</p>
        </div>
      </div>

      <div
        v-for="(segmentData, sIdx) in part4Segments"
        :key="`part4-${sIdx}`"
        class="passage-section"
      >
        <div class="passage-header">
          <h3>{{ getSegmentTitle(segmentData, sIdx) }}</h3>
          <span v-if="segmentData.topic" class="topic-badge">主题: {{ segmentData.topic }}</span>
        </div>

        <!-- 写作题目/阅读材料 -->
        <div v-if="segmentData.content" class="passage-content-block">
          <h4>{{ isContinuedWriting(segmentData) ? '阅读材料与续写要求' : '写作题目' }}</h4>
          <pre class="passage-text">{{ segmentData.content }}</pre>
        </div>

        <!-- 范文 -->
        <div v-if="segmentData.questions && segmentData.questions.items && segmentData.questions.items.length" class="reference-writing-block">
          <h4>参考范文</h4>
          <pre class="passage-text">{{ segmentData.questions.items[0].answer }}</pre>
        </div>
      </div>
    </div>

    <!-- 操作按钮 -->
    <div class="action-buttons">
      <button @click="goBack" class="btn-secondary">返回</button>
      <button @click="handleStoreToLibrary" :disabled="isStoring" class="btn-success">
        {{ isStoring ? '存储中...' : '存储到试卷库' }}
      </button>
      <button @click="generateAnother" class="btn-primary">继续生成</button>
    </div>
    
    <!-- 存储状态提示 -->
    <div v-if="storeMessage" :class="['store-message', storeMessageType]">
      {{ storeMessage }}
    </div>

    <!-- 试卷元信息弹窗 -->
    <HS3SaveMetaModal
      :visible="showSaveDialog"
      :saving="savingPaperInfo"
      :name="saveForm.name"
      :description="saveForm.desc"
      :error="saveFormError"
      @update:name="val => (saveForm.name = val)"
      @update:description="val => (saveForm.desc = val)"
      @cancel="cancelSaveMeta"
      @confirm="confirmSaveMeta"
    />
  </div>
</template>

<script>
import axios from 'axios'
import { ElMessageBox } from 'element-plus'
import HS3SaveMetaModal from '@/hs3/components/HS3SaveMetaModal.vue'

export default {
  name: 'HS3PaperGenResult',
  components: {
    HS3SaveMetaModal
  },
  data() {
    return {
      examPaperEnId: '',
      segments: [],
      examPaperEnSource: '',
      isStoring: false,
      storeMessage: '',
      storeMessageType: '',
      examPaperName: '',
      examPaperDesc: '',
      showSaveDialog: false,
      savingPaperInfo: false,
      saveForm: { name: '', desc: '' },
      saveFormError: ''
    };
  },
  computed: {
    // 第一部分：听力（part_number = 1）
    part1Segments() {
      return this.segments.filter(seg => seg.part_number === 1);
    },
    // 第二部分：阅读理解（part_number = 2）
    part2Segments() {
      return this.segments.filter(seg => seg.part_number === 2);
    },
    // 第三部分：语言知识运用（part_number = 3）
    part3Segments() {
      return this.segments.filter(seg => seg.part_number === 3);
    },
    // 第四部分：写作（part_number = 4）
    part4Segments() {
      return this.segments.filter(seg => seg.part_number === 4);
    }
  },
  mounted() {
    const storedData = sessionStorage.getItem('hs3PaperGenResult');
    if (storedData) {
      try {
        const resultData = JSON.parse(storedData);
        this.examPaperEnId = resultData.examPaperEnId || '';
        this.examPaperEnSource = resultData.exam_paper_en_source || 'AIfromreal';
        
        if (Array.isArray(resultData.segments)) {
          this.segments = resultData.segments.map((seg, idx) => {
            // 解析output字段
            let parsedSeg = seg;
            if (seg.output) {
              try {
                const output = typeof seg.output === 'string' ? JSON.parse(seg.output) : seg.output;
                // 如果output包含segments数组，取第一个
                // 注意：先合并Coze输出的内容，再用原始seg的标识字段覆盖
                // 这样可以获取Coze生成的content/questions，同时保留正确的segment_name等标识
                if (output.segments && output.segments.length > 0) {
                  const cozeData = output.segments[0];
                  parsedSeg = {
                    ...cozeData,
                    // 保留原始的标识字段，不让Coze输出覆盖
                    segment_name: seg.segment_name,
                    segment_number: seg.segment_number,
                    part_name: seg.part_name,
                    part_number: seg.part_number,
                    section_name: seg.section_name,
                    section_number: seg.section_number,
                    // 保留其他原始数据
                    segment_index: seg.segment_index,
                    topic: seg.topic || cozeData.topic,
                    original_document: seg.original_document,
                    output: seg.output,
                    exam_paper_en_source: seg.exam_paper_en_source
                  };
                } else {
                  parsedSeg = {
                    ...output,
                    // 保留原始的标识字段
                    segment_name: seg.segment_name,
                    segment_number: seg.segment_number,
                    part_name: seg.part_name,
                    part_number: seg.part_number,
                    section_name: seg.section_name,
                    section_number: seg.section_number,
                    segment_index: seg.segment_index,
                    topic: seg.topic || output.topic,
                    original_document: seg.original_document,
                    output: seg.output,
                    exam_paper_en_source: seg.exam_paper_en_source
                  };
                }
              } catch (e) {
                console.error('解析segment output失败:', e);
              }
            }
            return parsedSeg;
          });
        }
        
        sessionStorage.removeItem('hs3PaperGenResult');
      } catch (e) {
        console.error('解析结果数据失败:', e);
        this.showError();
      }
    } else {
      this.showError();
    }
  },
  methods: {
    showError() {
      this.examPaperEnId = '未知';
      this.segments = [];
    },
    goBack() {
      this.$router.go(-1);
    },
    generateAnother() {
      this.$router.push({ name: 'HS3PaperGen' });
    },
    getSegmentTitle(segmentData, index) {
      if (segmentData.segment_name) {
        return segmentData.segment_name;
      }
      if (segmentData.section_name) {
        return `${segmentData.part_name || ''} - ${segmentData.section_name} ${segmentData.segment_number || (index + 1)}`;
      }
      return `段落 ${index + 1}`;
    },
    isSevenChooseFive(segmentData) {
      return segmentData.section_name === '七选五';
    },
    isCloze(segmentData) {
      return segmentData.section_name === '完形填空';
    },
    isContinuedWriting(segmentData) {
      return segmentData.section_name === '读后续写';
    },
    
    async handleStoreToLibrary() {
      if (!this.segments.length) {
        this.storeMessageType = 'error'
        this.storeMessage = '没有可存储的试卷数据'
        return
      }

      const defaultName = this.examPaperName || (this.examPaperEnId ? `高考模拟卷 ${this.examPaperEnId}` : '')
      this.saveForm = { name: defaultName, desc: this.examPaperDesc || '' }
      this.saveFormError = ''
      this.showSaveDialog = true
    },
    cancelSaveMeta() {
      this.showSaveDialog = false
    },
    async confirmSaveMeta() {
      const cleanedName = (this.saveForm.name || '').trim()
      const cleanedDesc = (this.saveForm.desc || '').trim()
      if (!cleanedName) {
        this.saveFormError = '试卷名称不能为空'
        return
      }
      this.saveFormError = ''
      this.showSaveDialog = false
      await this.performStoreToLibrary(cleanedName, cleanedDesc)
    },
    async performStoreToLibrary(cleanedName, cleanedDesc) {
      this.examPaperName = cleanedName
      this.examPaperDesc = cleanedDesc
      this.isStoring = true
      this.savingPaperInfo = true
      this.storeMessage = ''

      try {
        // 准备存储数据 - 参照 HS3PaperAnalysisView 的格式
        // 注意：HS3使用 segment_name 而不是 segment_id，使用 section_name 而不是 question_type
        const payload = {
          examPaperName: cleanedName,
          subject: '高考',
          examPaperSource: this.examPaperEnSource || 'AIfromreal',
          segments: this.segments.map(seg => {
            // 为每个segment添加必要的元数据
            return {
              ...seg,
              // HS3使用的字段名（驼峰命名，供后端兼容处理）
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
        
        console.log('[HS3存储] 准备存储数据:', payload)

        // 调用后端接口存储到关系型数据库和ChromaDB（一次性完成）
        const response = await axios.post(
          'http://localhost:8080/api/hs3/paper-analysis/store-to-chroma',
          payload,
          { withCredentials: true }
        )

        if (response.data.success) {
          this.examPaperEnId = String(response.data.examPaperId || '')
          this.storeMessageType = 'success'
          this.storeMessage = ''
          
          ElMessageBox.alert(
            `试卷已成功保存到试卷题库（共保存 ${response.data.segmentsCount || this.segments.length} 道题）`,
            '保存成功',
            {
              confirmButtonText: '确定',
              type: 'success'
            }
          )
        } else {
          throw new Error(response.data.message || '存储失败')
        }
      } catch (error) {
        this.storeMessageType = 'error'
        if (error.response?.data?.message) { 
          this.storeMessage = error.response.data.message 
        } else if (error.message) { 
          this.storeMessage = error.message 
        } else { 
          this.storeMessage = '存储失败，请稍后重试' 
        }
      } finally {
        this.isStoring = false
        this.savingPaperInfo = false
      }
    }
  }
};
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

.paper-info-card {
  background: #ffffff;
  padding: 20px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  margin-bottom: 24px;
}

.info-item {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 8px;
}

.info-label {
  font-weight: 600;
  color: #546e7a;
}

.info-value {
  color: #2c3e50;
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

.passage-section {
  margin-bottom: 30px;
  padding-bottom: 20px;
  border-bottom: 1px solid #e0e0e0;
}

.passage-section:last-child {
  border-bottom: none;
}

.passage-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.passage-header h3 {
  margin: 0;
  color: #2c3e50;
}

.topic-badge {
  background: linear-gradient(135deg, #4caf50 0%, #45a049 100%);
  color: white;
  padding: 4px 12px;
  border-radius: 16px;
  font-size: 12px;
}

.passage-content-block {
  border: 1px solid #e0e0e0;
  border-radius: 12px;
  padding: 18px;
  background: #f9fbfd;
  margin-bottom: 24px;
}

.passage-content-block h4 {
  margin-top: 0;
  margin-bottom: 12px;
  color: #2c3e50;
  font-size: 16px;
}

.passage-text {
  white-space: pre-wrap;
  word-break: break-word;
  margin: 0;
  color: #37474f;
  font-size: 15px;
  line-height: 1.8;
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

.question-no {
  font-weight: 600;
  margin-right: 8px;
  color: #2c3e50;
  min-width: 30px;
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
  transition: all 0.2s;
}

.radio-option.correct-answer {
  background: #e8f5e9;
  border-color: #4caf50;
}

.radio-option input[type="radio"]:disabled {
  cursor: default;
}

.option-label {
  flex: 1;
  color: #37474f;
  line-height: 1.6;
  display: flex;
  align-items: center;
  gap: 8px;
}

.option-label strong {
  color: #2e7d32;
  margin-right: 6px;
}

.answer-tag {
  display: inline-block;
  padding: 2px 8px;
  background: #4caf50;
  color: white;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
  margin-left: auto;
}

.correct-answer-display {
  margin-top: 8px;
  padding: 8px 12px;
  background: #e8f5e9;
  border-radius: 6px;
  border-left: 3px solid #4caf50;
}

.word-options-block {
  margin: 20px 0;
  padding: 16px;
  background: #f9fbfd;
  border: 1px solid #e0e0e0;
  border-radius: 12px;
}

.word-options-block h5 {
  margin: 0 0 12px 0;
  color: #2c3e50;
}

.word-options-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 12px;
}

.word-option-item {
  padding: 8px 12px;
  background: white;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
}

.word-option-item strong {
  color: #2e7d32;
  margin-right: 8px;
}

.reference-writing-block {
  margin-top: 20px;
  padding: 16px;
  background: #e8f5e9;
  border: 1px solid #4caf50;
  border-left: 4px solid #2e7d32;
  border-radius: 12px;
}

.reference-writing-block h4 {
  color: #2e7d32;
  margin-bottom: 12px;
  font-size: 16px;
  margin-top: 0;
}

.action-buttons {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin-top: 40px;
  padding-top: 20px;
  border-top: 1px solid #eee;
}

.btn-primary,
.btn-secondary {
  padding: 12px 32px;
  border: none;
  border-radius: 999px;
  cursor: pointer;
  font-size: 16px;
  font-weight: 600;
  transition: all 0.2s ease;
}

.btn-primary {
  background: linear-gradient(135deg, #4caf50 0%, #45a049 100%);
  color: white;
}

.btn-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(76, 175, 80, 0.35);
}

.btn-secondary {
  background: #f0f0f0;
  color: #333;
}

.btn-secondary:hover {
  background: #e0e0e0;
}

.btn-success {
  background: linear-gradient(135deg, #2e7d32 0%, #388e3c 100%);
  color: white;
  padding: 12px 32px;
  border: none;
  border-radius: 999px;
  cursor: pointer;
  font-size: 16px;
  font-weight: 600;
  transition: all 0.2s ease;
}

.btn-success:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(46, 125, 50, 0.35);
}

.btn-success:disabled {
  background: #cccccc;
  cursor: not-allowed;
  transform: none;
}

.store-message {
  margin-top: 20px;
  padding: 12px 20px;
  border-radius: 8px;
  text-align: center;
  font-size: 14px;
  font-weight: 500;
}

.store-message.success {
  background: #e8f5e9;
  color: #2e7d32;
  border: 1px solid #4caf50;
}

.store-message.error {
  background: #ffebee;
  color: #c62828;
  border: 1px solid #ef5350;
}

@media (max-width: 600px) {
  .display-wrapper {
    padding: 20px 15px 40px;
  }
  
  .options-list {
    margin-left: 0;
  }
}
</style>
