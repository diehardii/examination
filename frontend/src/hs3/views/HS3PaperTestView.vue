<template>
  <div class="app-container">
    <SideBarMenu />
    <div class="main-content">
      <header class="page-header">
        <div>
          <h1 class="title">请选择你要考的高考英语试卷</h1>
          <p class="user-hint" v-if="currentUserName">当前账号：{{ currentUserName }}</p>
        </div>
      </header>

      <section class="paper-panel">
        <h3 class="sticky-title">试卷列表</h3>
        <div class="paper-list" v-if="filteredPapers.length">
          <table>
            <thead>
              <tr>
                <th>ID</th>
                <th>试卷名称</th>
                <th>科目</th>
                <th>来源</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="paper in filteredPapers"
                :key="paper.id"
                :class="{ highlight: String(selectedPaperId) === String(paper.id) }"
                @click="selectPaper(paper.id)"
              >
                <td>{{ paper.id }}</td>
                <td>{{ paper.examPaperEnName }}</td>
                <td>{{ paper.examPaperEnSubject || 'GAOKAO' }}</td>
                <td>{{ paper.examPaperEnSource || 'real' }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <el-empty v-else :image-size="180" description="暂无可用试卷" />

        <div class="submit-btn-container">
          <button class="submit-btn" @click="confirmSelection">确认</button>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { ElMessage } from 'element-plus'
import { useRouter, useRoute } from 'vue-router'
import SideBarMenu from '@/common/components/SideBarMenu.vue'
import { useAuthStore } from '@/common/stores/auth'
import { getExamPaperList } from '@/hs3/service/HS3paperAnalysisService'

const authStore = useAuthStore()
const { user: storeUser } = storeToRefs(authStore)
const router = useRouter()
const route = useRoute()

const cachedUser = ref(null)
try {
  const storedUser = localStorage.getItem('user')
  cachedUser.value = storedUser ? JSON.parse(storedUser) : null
} catch (error) {
  cachedUser.value = null
}

const currentUser = computed(() => storeUser.value || cachedUser.value)
const currentUserName = computed(() => currentUser.value?.username || '')

const papers = ref([])
const selectedPaperId = ref(null)

function canAccessPaper(paper) {
  const source = String(paper.examPaperEnSource || paper.source || '').toLowerCase()
  if (source === 'real') return true

  const ownerId = paper.ownerId ?? paper.userId ?? paper.teacherId ?? paper.createdBy
  const currentId = currentUser.value?.id
  if (ownerId && currentId) {
    return String(ownerId) === String(currentId)
  }
  return true
}

const normalizeSubject = (val) => {
  const s = String(val || '').trim()
  if (!s) return ''
  const upper = s.toUpperCase()
  if (upper === 'GAOKAO' || s === '高考' || s === '高考英语') return 'GAOKAO'
  return upper
}

const subjectFilter = computed(() => {
  const fromRoute = route.query.subject || route.params.subject || ''
  const fromSession = sessionStorage.getItem('selectedSubject') || ''
  return normalizeSubject(fromRoute || fromSession)
})

const filteredPapers = computed(() => {
  const target = subjectFilter.value
  return papers.value
    .filter(canAccessPaper)
    .filter((p) => {
      if (!target) return true
      const subj = normalizeSubject(p.examPaperEnSubject || p.subject || '')
      // 若选中高考，则允许试卷未填科目（默认高考）
      if (target === 'GAOKAO' && !subj) return true
      return subj === target
    })
})

const confirmSelection = () => {
  if (!selectedPaperId.value) {
    ElMessage.error('请先选择一份试卷')
    return
  }
  router.push({
    path: `/question-answer-hs3/${selectedPaperId.value}`,
    query: { subject: '高考' }
  }).catch(() => {})
}

onMounted(() => {
  loadPapers()
})

watch(filteredPapers, (list) => {
  if (!list.length) {
    selectedPaperId.value = null
    return
  }
  const existing = list.find((paper) => String(paper.id) === String(selectedPaperId.value))
  if (!existing) {
    selectedPaperId.value = list[0].id
  }
}, { immediate: true })

const loadPapers = async () => {
  try {
    const data = await getExamPaperList()
    if (Array.isArray(data)) {
      papers.value = data
    } else {
      papers.value = []
      ElMessage.error('获取试卷列表失败')
    }
  } catch (error) {
    papers.value = []
    ElMessage.error(error.response?.data?.message || '加载试卷列表失败')
  }
}

const selectPaper = (paperId) => {
  selectedPaperId.value = paperId
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
  box-sizing: border-box;
  width: calc(100% - 290px);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.title {
  margin: 0;
  font-size: 22px;
  color: #2c3e50;
}

.user-hint {
  margin: 6px 0 0;
  color: #666;
}

.panels {
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: 18px;
}

.paper-panel {
  background: #fff;
  border: 1px solid #e6e6e6;
  border-radius: 8px;
  padding: 12px;
  height: calc(100vh - 120px);
  overflow: hidden;
}

.sticky-title {
  position: sticky;
  top: 0;
  margin: 0;
  padding: 12px;
  background-color: #f8f9fa;
  border-bottom: 1px solid #dee2e6;
  z-index: 10;
  color: #003366;
}

.paper-list {
  margin-top: 10px;
  max-height: calc(100% - 48px);
  overflow: auto;
  border: 1px solid #ddd;
  border-radius: 6px;
  background: #f9fcff;
}

table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

th, td {
  padding: 10px 12px;
  border-bottom: 1px solid #e6e6e6;
}

th {
  background: #f1f4f8;
  color: #003366;
  text-align: left;
}

tbody tr {
  cursor: pointer;
  transition: background-color 0.2s ease;
}

tbody tr.highlight {
  background: #e6f7ff;
  color: #003366;
}

tbody tr:hover:not(.highlight) {
  background: #f8f9fa;
}

.display-panel {
  background: #fff;
  border: 1px solid #e6e6e6;
  border-radius: 8px;
  padding: 16px;
  min-height: calc(100vh - 120px);
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
}

.panel-header {
  border-bottom: 1px solid #eee;
  padding-bottom: 10px;
  margin-bottom: 12px;
}

.paper-title {
  margin: 0;
  font-size: 20px;
  color: #2c3e50;
}

.paper-meta {
  display: flex;
  gap: 12px;
  color: #607d8b;
  font-size: 14px;
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
  flex: 1;
  overflow: auto;
  padding-right: 6px;
}

.segment-block {
  border: 1px solid #e6e6e6;
  border-radius: 10px;
  padding: 16px;
  margin-bottom: 16px;
  background: #fff;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.04);
}

.segment-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.segment-number {
  font-weight: 600;
  color: #2e7d32;
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
}

.question-block, .questions-block, .statements-block {
  margin-bottom: 12px;
}

.question-row, .grammar-fill-row, .statement-row {
  display: flex;
  align-items: center;
  gap: 8px;
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
  padding: 8px;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  background: #fff;
}

.grammar-fill-grid, .grammar-answers-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 8px;
}

.grammar-fill-input {
  flex: 1;
  padding: 6px 8px;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
}

.answers-section {
  border-top: 1px solid #eee;
  padding-top: 10px;
}

.answers-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 8px;
}

.answer-item, .grammar-answer-item {
  background: #f7f9fb;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  padding: 8px 10px;
  display: flex;
  justify-content: space-between;
  align-items: center;
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
}

.options-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 6px 12px;
  margin-top: 6px;
}

.radio-option {
  display: flex;
  align-items: center;
  gap: 6px;
  background: #f9fbfd;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  padding: 8px;
}

.submit-bar {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
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

.submit-btn:disabled {
  background: #9e9e9e;
  cursor: not-allowed;
}

.submit-btn:not(:disabled):hover {
  background: #43a047;
}
</style>
