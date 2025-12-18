<template>
  <div class="display-wrapper">
    <h1>试卷生成结果</h1>

    <!-- 试卷信息 -->
    <div class="paper-info-card">
      <div class="info-item">
        <span class="info-label">试卷ID:</span>
        <span class="info-value">{{ examPaperEnId }}</span>
      </div>
      <div class="info-item">
        <span class="info-label">题目单元数:</span>
        <span class="info-value">{{ units.length }}</span>
      </div>
      <div class="info-item" v-if="part1Units.length">
        <span class="info-label">Part 1单元数:</span>
        <span class="info-value">{{ part1Units.length }}</span>
      </div>
      <div class="info-item" v-if="part2Units.length">
        <span class="info-label">Part 2单元数:</span>
        <span class="info-value">{{ part2Units.length }}</span>
      </div>
      <div class="info-item" v-if="part3Units.length">
        <span class="info-label">Part 3单元数:</span>
        <span class="info-value">{{ part3Units.length }}</span>
      </div>
      <div class="info-item" v-if="part4Units.length">
        <span class="info-label">Part 4单元数:</span>
        <span class="info-value">{{ part4Units.length }}</span>
      </div>
    </div>

    <!-- Part I · Writing -->
    <div v-if="part1Units.length" class="result-card structured exam-section">
      <div class="section-header">
        <div>
          <h2>Part I · Writing</h2>
          <p class="section-meta">共 {{ part1Units.length }} 个单元</p>
        </div>
      </div>

      <div
        v-for="(unitData, uIdx) in part1Units"
        :key="`part1-${uIdx}`"
        class="passage-section"
      >
        <div class="passage-header">
          <h3>{{ getUnitTitle(unitData, uIdx) }}</h3>
        </div>

        <!-- 写作题目 -->
        <div v-if="unitData.output" class="writing-block">
          <!-- 判断是否有writing对象 -->
          <div v-if="unitData.output.writing" class="writing-content">
            <!-- 显示题目和要求 -->
            <div v-if="unitData.output.writing.passage" class="passage-content-block">
              <h4>写作题目</h4>
              <pre class="passage-text">{{ unitData.output.writing.passage }}</pre>
            </div>
            
          </div>
          
          <!-- 如果直接有writing_prompt字段（兼容旧格式） -->
          <div v-else-if="unitData.output.writing_prompt" class="writing-content">
            <div class="passage-content-block">
              <h4>写作题目</h4>
              <pre class="passage-text">{{ unitData.output.writing_prompt }}</pre>
            </div>
            
            <div v-if="unitData.output.requirements" class="requirements-block">
              <h4>写作要求</h4>
              <ul class="requirements-list">
                <li v-for="(req, idx) in unitData.output.requirements" :key="idx">{{ req }}</li>
              </ul>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Part II · Listening Comprehension -->
    <div v-if="part2Units.length" class="result-card structured exam-section">
      <div class="section-header">
        <div>
          <h2>Part II · Listening Comprehension</h2>
          <p class="section-meta">共 {{ part2Units.length }} 个单元</p>
        </div>
      </div>

      <div
        v-for="(unitData, uIdx) in part2Units"
        :key="`part2-${uIdx}`"
        class="passage-section"
      >
        <div class="passage-header">
          <h3>{{ getUnitTitle(unitData, uIdx) }}</h3>
        </div>

        <!-- 听力材料 -->
        <div class="passage-content-block" v-if="unitData.output && unitData.output.listening_content">
          <pre class="passage-text">{{ unitData.output.listening_content }}</pre>
        </div>

       <!-- 题目列表 -->
        <div class="questions-block" v-if="unitData.output && unitData.output.question_and_options && unitData.output.question_and_options.length">
          <div
            v-for="(q, qIdx) in unitData.output.question_and_options"
            :key="`part2-${uIdx}-q-${qIdx}`"
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
                  :name="`part2-${uIdx}-q-${qIdx}`"
                  :value="opt.option_mark || opt.mark"
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

    <!-- Part III · Reading Comprehension -->
    <div v-if="part3Units.length" class="result-card structured exam-section">
      <div class="section-header">
        <div>
          <h2>Part III · Reading Comprehension</h2>
          <p class="section-meta">共 {{ part3Units.length }} 个单元</p>
        </div>
      </div>

      <div
        v-for="(unitData, uIdx) in part3Units"
        :key="`part3-${uIdx}`"
        class="passage-section"
      >
        <div class="passage-header">
          <h3>{{ getUnitTitle(unitData, uIdx) }}</h3>
        </div>

        <!-- Section A 选词填空 -->
        <div v-if="unitData.output && unitData.output.reading_comprehension && unitData.output.reading_comprehension.section_a" class="section-a-block">
          <h4>Section A - {{ unitData.output.reading_comprehension.section_a.question_type }}</h4>
          <div class="passage-content-block">
            <h5>题干</h5>
            <pre class="passage-text">{{ unitData.output.reading_comprehension.section_a.passage }}</pre>
          </div>
          
          <!-- 备选词汇 -->
          <div v-if="unitData.output.reading_comprehension.section_a.word_options" class="word-options-block">
            <h5>备选词汇</h5>
            <div class="word-options-grid">
              <div v-for="opt in unitData.output.reading_comprehension.section_a.word_options" :key="opt.letter" class="word-option-item">
                <strong>{{ opt.letter }})</strong> {{ opt.word }}
              </div>
            </div>
          </div>
          
        </div>

        <!-- Section B 段落匹配 -->
        <div v-if="unitData.output && unitData.output.reading_comprehension && unitData.output.reading_comprehension.section_b" class="section-b-block">
          <h4>Section B - {{ unitData.output.reading_comprehension.section_b.question_type }}</h4>
          
          <!-- 文章段落 -->
          <div class="article-paragraphs">
            <div v-for="para in unitData.output.reading_comprehension.section_b.article" :key="para.paragraph_mark" class="paragraph-block">
              <div class="paragraph-header">
                <strong>{{ para.paragraph_mark }}</strong>
              </div>
              <p class="paragraph-content">{{ para.paragraph_content }}</p>
            </div>
          </div>
          
          <!-- 题目列表 -->
          <div v-if="unitData.output.reading_comprehension.section_b.statements" class="statements-block">
            <h5>题目</h5>
            <div class="questions-block">
              <div v-for="stmt in unitData.output.reading_comprehension.section_b.statements" :key="stmt.question_number" class="question-item">
                <div class="question-header">
                  <span class="question-no">{{ stmt.question_number }}</span>
                  <p class="question-text">{{ stmt.statement_content }}</p>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Section C 篇章阅读 -->
        <div v-if="unitData.output && unitData.output.passage_content && unitData.output.questions" class="section-c-block">
          <h4>Section C - {{ unitData.output.question_type || '篇章阅读' }}</h4>
          <div v-if="unitData.output.passage_mark" class="passage-mark">
            <strong>{{ unitData.output.passage_mark }}</strong>
          </div>
          
          <!-- 文章内容 -->
          <div class="passage-content-block">
            <pre class="passage-text">{{ unitData.output.passage_content }}</pre>
          </div>
          
          <!-- 题目列表 -->
          <div class="questions-block">
            <div v-for="q in unitData.output.questions" :key="q.question_number" class="question-item">
              <div class="question-header">
                <span class="question-no">{{ q.question_number }}</span>
                <p class="question-text">{{ q.question_content }}</p>
              </div>
              <div class="options-list">
                <label
                  v-for="opt in q.options"
                  :key="`part3-${uIdx}-q-${q.question_number}-${opt.option_mark}`"
                  class="radio-option"
                >
                  <input
                    type="radio"
                    :name="`part3-${uIdx}-q-${q.question_number}`"
                    :value="opt.option_mark"
                    disabled
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

    <!-- Part IV · Translation -->
    <div v-if="part4Units.length" class="result-card structured exam-section">
      <div class="section-header">
        <div>
          <h2>Part IV · Translation</h2>
          <p class="section-meta">共 {{ part4Units.length }} 个单元</p>
        </div>
      </div>

      <div
        v-for="(unitData, uIdx) in part4Units"
        :key="`part4-${uIdx}`"
        class="passage-section"
      >
        <div class="passage-header">
          <h3>{{ getUnitTitle(unitData, uIdx) }}</h3>
        </div>

        <!-- 翻译题目 -->
        <div v-if="unitData.output && unitData.output.translation" class="translation-block">
          <!-- 显示翻译题目（包含中文原文） -->
          <div v-if="unitData.output.translation.passage" class="passage-content-block">
            <h4>翻译题目</h4>
            <pre class="passage-text">{{ unitData.output.translation.passage }}</pre>
          </div>
          
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

    <!-- 试卷元信息弹窗（复用强化训练预览样式） -->
    <CET4SaveMetaModal
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
import { storeWritingInChromaCET4, storeTranslationInChromaCET4, storePart2ABInChromaCET4, storePart2CInChromaCET4, storeSectionAInChromaCET4, storeSectionBInChromaCET4, storeSectionCInChromaCET4 } from '@/cet4/service/CET4paperAnalysisServiceCET4'
import CET4SaveMetaModal from '@/cet4/components/CET4SaveMetaModal.vue'

export default {
  name: 'PaperGenResult',
  components: {
    CET4SaveMetaModal
  },
  data() {
    return {
      examPaperEnId: '',
      units: [], // 存储所有units
      // 新增：从生成结果传递的来源（AIfromreal/AIfromself/real）
      examPaperEnSource: '',
      isStoring: false,
      storeMessage: '',
      storeMessageType: '', // 'success' 或 'error'
      examPaperName: '',
      examPaperDesc: '',
      showSaveDialog: false,
      savingPaperInfo: false,
      saveForm: { name: '', desc: '' },
      saveFormError: ''
    };
  },
  computed: {
    // Part 1的单元
    part1Units() {
      return this.units.filter(unit => unit.part_id === 1);
    },
    // Part 2的单元
    part2Units() {
      return this.units.filter(unit => unit.part_id === 2);
    },
    // Part 3的单元
    part3Units() {
      return this.units.filter(unit => unit.part_id === 3);
    },
    // Part 4的单元
    part4Units() {
      return this.units.filter(unit => unit.part_id === 4);
    }
  },
  mounted() {
    // 从sessionStorage读取数据
    const storedData = sessionStorage.getItem('paperGenResult');
    if (storedData) {
      try {
        const resultData = JSON.parse(storedData);
        this.examPaperEnId = resultData.examPaperEnId || '';
        // 新增：保存来源，默认 AIfromreal（与生成页一致），若不存在则回退为 'AIfromreal'
        this.examPaperEnSource = resultData.exam_paper_en_source || 'AIfromreal';
        // 处理units数组
        if (Array.isArray(resultData.units)) {
          this.units = resultData.units.map((unit, idx) => {
            let output = unit.output;
            if (typeof output === 'string') {
              try { output = JSON.parse(output); } catch (e) { output = null; }
            }
            return {
              part_id: unit.part_id || 0,
              unit_index: unit.unit_index,
              segment_id: unit.segment_id,
              output: output,
              answers: unit.answers || {}
            };
          });
        }
        // 读取后清除sessionStorage
        sessionStorage.removeItem('paperGenResult');
      } catch (e) {
        this.showError();
      }
    } else {
      this.showError();
    }
  },
  methods: {
    showError() {
      this.examPaperEnId = '未知';
      this.units = [];
    },
    goBack() {
      this.$router.go(-1);
    },
    generateAnother() {
      this.$router.push({ name: 'PaperGen' });
    },
    getUnitTitle(unitData, index) {
      if (unitData.output && unitData.output.unit_type) {
        return unitData.output.unit_type;
      }
      const partNames = {
        1: 'Writing',
        2: 'Listening',
        3: 'Reading',
        4: 'Translation'
      };
      const partName = partNames[unitData.part_id] || 'Unknown';
      return `${partName} Unit ${index + 1} (Segment ${unitData.segment_id || ''})`;
    },
    optionsOf(q) {
      try {
        const std = q && q['选项'];
        const preferredOrder = ['A', 'B', 'C', 'D'];
        if (std && typeof std === 'object' && !Array.isArray(std)) {
          const arr = [];
          preferredOrder.forEach((k) => {
            if (std[k] && String(std[k]).trim()) {
              arr.push({ mark: k, text: String(std[k]).trim() });
            }
          });
          if (!arr.length) {
            Object.keys(std).forEach((k) => {
              const v = std[k];
              if (v && String(v).trim()) {
                arr.push({ mark: String(k).trim(), text: String(v).trim() });
              }
            });
          }
          if (arr.length) return arr;
        }
      } catch (e) {}
      return [];
    },
    async handleStoreToLibrary() {
      if (!this.units.length) {
        this.storeMessageType = 'error'
        this.storeMessage = '没有可存储的试卷数据'
        return
      }

      const defaultName = this.examPaperName || (this.examPaperEnId ? `Generated Paper ${this.examPaperEnId}` : '')
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
        const examPaperPayload = {
          examPaperEnName: cleanedName,
          examPaperEnDesc: cleanedDesc || null,
          examPaperEnSubject: 'CET4',
          examPaperEnSource: this.examPaperEnSource || 'AIfromreal'
        }

        const examPaperResponse = await axios.post(
          'http://localhost:8080/api/cet4/exam-paper-en/',
          examPaperPayload,
          { withCredentials: true }
        )

        if (!examPaperResponse.data?.success || !examPaperResponse.data?.id) {
          const msg = examPaperResponse.data?.message || '创建试卷记录失败'
          throw new Error(msg)
        }

        const newExamPaperId = String(examPaperResponse.data.id)
        this.examPaperEnId = newExamPaperId

        const partsToSave = []
        const examPaperName = cleanedName
        const examPaperId = newExamPaperId

        const part1Units = this.part1Units
        if (part1Units.length > 0) {
          part1Units.forEach(unit => {
            if (unit.output) {
              partsToSave.push({
                type: 'writing',
                payload: {
                  examPaperId,
                  examPaperName,
                  subject: 'CET4',
                  document: typeof unit.output === 'string' ? unit.output : JSON.stringify(unit.output),
                  questionType: '写作',
                  partId: 1,
                  examPaperEnSource: this.examPaperEnSource
                },
                save: storeWritingInChromaCET4
              })
            }
          })
        }

        const part2Units = this.part2Units
        if (part2Units.length > 0) {
          const allListeningUnits = []
          const allBlankNumbers = []
          part2Units.forEach(unit => {
            if (unit.output) {
              allListeningUnits.push(unit.output)
              if (unit.output.question_and_options && Array.isArray(unit.output.question_and_options)) {
                unit.output.question_and_options.forEach(q => { if (q.question_number) { allBlankNumbers.push(q.question_number) } })
              }
            }
          })
          if (allListeningUnits.length > 0) {
            const listeningDocument = { units: allListeningUnits }
            partsToSave.push({
              type: 'listening',
              payload: {
                examPaperId,
                examPaperName,
                subject: 'CET4',
                document: JSON.stringify(listeningDocument),
                blankNumbers: allBlankNumbers,
                questionType: '听力',
                partId: 2,
                sectionId: 'AB',
                examPaperEnSource: this.examPaperEnSource
              },
              save: storePart2ABInChromaCET4
            })
          }
        }

        const part3Units = this.part3Units
        if (part3Units.length > 0) {
          part3Units.forEach((unit, idx) => {
            if (unit.output) {
              let sectionId = ''
              if (unit.segment_id && unit.segment_id.includes('section')) {
                const parts = unit.segment_id.split('section'); if (parts.length > 1) { sectionId = parts[1] }
              }
              let saveFunc = null
              let questionType = ''
              let blankNumbers = []
              if (unit.output.reading_comprehension) {
                if (unit.output.reading_comprehension.section_a) {
                  saveFunc = storeSectionAInChromaCET4
                  questionType = '选词填空'
                  const sectionA = unit.output.reading_comprehension.section_a
                  if (sectionA.blank_numbers && Array.isArray(sectionA.blank_numbers)) { blankNumbers = sectionA.blank_numbers }
                  else if (sectionA.blank_count) { const startNum = sectionA.start_number || 26; const count = parseInt(sectionA.blank_count); for (let i = 0; i < count; i++) { blankNumbers.push(String(startNum + i)) } }
                } else if (unit.output.reading_comprehension.section_b) {
                  saveFunc = storeSectionBInChromaCET4
                  questionType = '段落匹配'
                  const sectionB = unit.output.reading_comprehension.section_b
                  if (sectionB.statements && Array.isArray(sectionB.statements)) { blankNumbers = sectionB.statements.map(s => s.question_number) }
                } else if (unit.output.reading_comprehension.section_c1 || unit.output.reading_comprehension.section_c2) {
                  saveFunc = storeSectionCInChromaCET4
                  questionType = '篇章阅读'
                  const c1 = unit.output.reading_comprehension.section_c1
                  const c2 = unit.output.reading_comprehension.section_c2
                  if (c1 && c1.questions && Array.isArray(c1.questions)) { blankNumbers.push(...c1.questions.map(q => q.question_number)) }
                  if (c2 && c2.questions && Array.isArray(c2.questions)) { blankNumbers.push(...c2.questions.map(q => q.question_number)) }
                }
              } else if (unit.output.passage_content && unit.output.questions) {
                saveFunc = storeSectionCInChromaCET4
                questionType = '篇章阅读'
                if (Array.isArray(unit.output.questions)) { blankNumbers = unit.output.questions.map(q => q.question_number) }
              }
              if (saveFunc && blankNumbers.length > 0) {
                partsToSave.push({
                  type: `section${sectionId}`,
                  payload: {
                    examPaperId,
                    examPaperName,
                    subject: 'CET4',
                    document: typeof unit.output === 'string' ? unit.output : JSON.stringify(unit.output),
                    blankNumbers: blankNumbers,
                    questionType: questionType,
                    partId: 3,
                    sectionId: sectionId,
                    examPaperEnSource: this.examPaperEnSource
                  },
                  save: saveFunc
                })
              }
            }
          })
        }

        const part4Units = this.part4Units
        if (part4Units.length > 0) {
          part4Units.forEach((unit, idx) => {
            if (unit.output) {
              partsToSave.push({
                type: 'translation',
                payload: {
                  examPaperId,
                  examPaperName,
                  subject: 'CET4',
                  document: typeof unit.output === 'string' ? unit.output : JSON.stringify(unit.output),
                  questionType: '翻译',
                  partId: 4,
                  examPaperEnSource: this.examPaperEnSource
                },
                save: storeTranslationInChromaCET4
              })
            }
          })
        }

        if (partsToSave.length === 0) { throw new Error('没有可保存的试卷内容') }

        // 计算实际保存的题目数量
        let totalSavedCount = 0
        for (const part of partsToSave) {
          await part.save(part.payload)
          // 根据类型计算题目数量
          switch (part.type) {
            case 'writing':
            case 'translation':
              totalSavedCount += 1
              break
            case 'listening':
              // 听力：按 units 数量计算
              try {
                const doc = JSON.parse(part.payload.document)
                const unitsCount = Array.isArray(doc?.units) ? doc.units.length : 0
                totalSavedCount += unitsCount || 1
              } catch {
                totalSavedCount += 1
              }
              break
            case 'sectionA':
              // 选词填空：1篇
              totalSavedCount += 1
              break
            case 'sectionB':
              // 段落匹配：1篇
              totalSavedCount += 1
              break
            case 'sectionC':
              // 篇章阅读：根据 C1/C2 数量
              try {
                const doc = JSON.parse(part.payload.document)
                let cCount = 0
                if (doc?.reading_comprehension?.section_c1) cCount++
                if (doc?.reading_comprehension?.section_c2) cCount++
                // 如果是单篇格式 (passage_content)
                if (doc?.passage_content) cCount = 1
                totalSavedCount += cCount || 1
              } catch {
                totalSavedCount += 1
              }
              break
            default:
              totalSavedCount += 1
          }
        }

        this.storeMessageType = 'success'
        this.storeMessage = ''
        
        // 使用弹窗提示
        ElMessageBox.alert(
          `试卷已成功保存到试卷题库（共保存 ${totalSavedCount} 道题）`,
          '保存成功',
          {
            confirmButtonText: '确定',
            type: 'success'
          }
        )
      } catch (error) {
        this.storeMessageType = 'error'
        if (error.response?.data?.message) { this.storeMessage = error.response.data.message }
        else if (error.message) { this.storeMessage = error.message }
        else { this.storeMessage = '存储失败，请稍后重试' }
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
}

.passage-content-block {
  border: 1px solid #e0e0e0;
  border-radius: 12px;
  padding: 18px;
  background: #f9fbfd;
  margin-bottom: 24px;
}

.passage-content-block h3 {
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

.answers-summary {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 12px;
  margin-top: 16px;
}

.answer-item-summary {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 14px;
  background: #f9fbfd;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
}

.answers-summary-inline {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #e0e0e0;
}

.answers-summary-inline h3 {
  margin: 0 0 12px 0;
  color: #2c3e50;
  font-size: 16px;
}

.answers-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 12px;
}

/* Part 3 Section A 样式 */
.section-a-block,
.section-b-block,
.section-c-block {
  margin-top: 20px;
}

.section-a-block h4,
.section-b-block h4,
.section-c-block h4 {
  color: #2e7d32;
  margin-bottom: 16px;
  font-size: 18px;
}

.section-a-block h5,
.section-b-block h5,
.section-c-block h5 {
  color: #2c3e50;
  margin: 12px 0 8px 0;
  font-size: 16px;
}

.passage-mark {
  margin-bottom: 12px;
}

.passage-mark strong {
  display: inline-block;
  background: #2e7d32;
  color: white;
  padding: 6px 16px;
  border-radius: 6px;
  font-size: 16px;
}

.word-options-block {
  margin: 20px 0;
  padding: 16px;
  background: #f9fbfd;
  border: 1px solid #e0e0e0;
  border-radius: 12px;
}

.word-options-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 12px;
  margin-top: 12px;
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

/* Part 3 Section B 样式 */
.article-paragraphs {
  margin: 20px 0;
}

.paragraph-block {
  margin-bottom: 16px;
  padding: 16px;
  background: #f9fbfd;
  border-left: 4px solid #2e7d32;
  border-radius: 8px;
}

.paragraph-header {
  margin-bottom: 8px;
}

.paragraph-header strong {
  display: inline-block;
  background: #2e7d32;
  color: white;
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 14px;
}

.paragraph-content {
  margin: 0;
  color: #37474f;
  line-height: 1.8;
  font-size: 15px;
}

.statements-block {
  margin-top: 24px;
}

.correct-answer-display {
  margin-top: 8px;
  padding: 8px 12px;
  background: #e8f5e9;
  border-radius: 6px;
  border-left: 3px solid #4caf50;
}

.correct-answer-display .answer-tag {
  background: transparent;
  color: #2e7d32;
  padding: 0;
  font-weight: 600;
}

/* Part 1 Writing 样式 */
.writing-block {
  margin-top: 20px;
}

.writing-block h4 {
  color: #2e7d32;
  margin-bottom: 12px;
  font-size: 16px;
}

.topic-block {
  margin-bottom: 16px;
  padding: 12px 16px;
  background: linear-gradient(135deg, #2e7d32 0%, #4caf50 100%);
  border-radius: 8px;
}

.topic-block h4 {
  color: white;
  margin: 0 0 8px 0;
  font-size: 14px;
}

.topic-text {
  color: white;
  font-size: 18px;
  font-weight: 600;
  margin: 0;
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
}

.requirements-block {
  margin-top: 20px;
  padding: 16px;
  background: #f9fbfd;
  border: 1px solid #e0e0e0;
  border-radius: 12px;
}

.requirements-list {
  margin: 8px 0;
  padding-left: 24px;
}

.requirements-list li {
  color: #37474f;
  line-height: 1.8;
  margin-bottom: 8px;
}

/* Part 4 Translation 样式 */
.translation-block {
  margin-top: 20px;
}

.translation-block h4 {
  color: #2e7d32;
  margin-bottom: 12px;
  font-size: 16px;
}

.reference-translation-block {
  margin-top: 20px;
  padding: 16px;
  background: #e8f5e9;
  border: 1px solid #4caf50;
  border-radius: 12px;
}

.reference-translation-block h4 {
  color: #2e7d32;
  margin-bottom: 12px;
  font-size: 16px;
}

.question-num-summary {
  font-weight: 600;
  color: #2e7d32;
}

.answer-text-summary {
  color: #37474f;
  font-weight: 600;
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
  
  .answers-summary {
    grid-template-columns: repeat(auto-fill, minmax(100px, 1fr));
  }
}
</style>
