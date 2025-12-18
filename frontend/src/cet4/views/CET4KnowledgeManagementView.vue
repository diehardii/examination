<template>
  <div class="km-page">
    <!-- 顶部导航（对齐 knowledge-manager-frontend 的 HomeView 风格） -->
    <header class="km-header">
      <div class="km-header-inner">
        <div class="km-brand" @click="$router.push('/home')">
          <span class="km-logo">🎓</span>
          <span class="km-title">知识管理中心</span>
        </div>
        <nav class="km-nav">
          <button class="km-nav-btn" @click="goToPaperManagement" :disabled="!selectedSubject">试卷管理</button>
          <button class="km-nav-btn" @click="goToPaperDisplay">试卷展示</button>
        </nav>
      </div>
    </header>

    <main class="km-container">
      <h1 class="km-page-title">请选择功能</h1>
      <div class="cards-grid">
      <!-- 试卷管理卡片 -->
      <div class="select-card">
        <div class="icon">📚</div>
        <h2>试卷管理</h2>
        <div class="form-field">
          <label>考试科目</label>
          <select v-model="selectedSubject" class="subject-select" :disabled="loadingSubjects">
            <option value="">{{ loadingSubjects ? '加载中...' : '请选择科目' }}</option>
            <option v-for="subject in subjectOptions" :key="subject.value" :value="subject.value">
              {{ subject.label }}
            </option>
          </select>
        </div>
        <button 
          class="manage-button" 
          :disabled="!selectedSubject"
          @click="goToPaperManagement"
        >
          进入试卷管理
        </button>
      </div>

      <!-- 试卷展示卡片 -->
      <div class="select-card">
        <div class="icon">📄</div>
        <h2>试卷展示</h2>
        <p class="card-desc">通过输入试卷号或试卷名称，查询并展示 ChromaDB 中的试卷数据</p>
        <button 
          class="manage-button paper-button"
          @click="goToPaperDisplay"
        >
          进入试卷展示
        </button>
      </div>
    </div>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import { getSubjectsEn } from '@/common/service/subjectService'

const router = useRouter()
const selectedSubject = ref('')
const subjectOptions = ref([])
const loadingSubjects = ref(false)

onMounted(async () => {
  await loadSubjects()
})

const loadSubjects = async () => {
  loadingSubjects.value = true
  try {
    const subjects = await getSubjectsEn()
    subjectOptions.value = (subjects || []).map(subject => ({
      label: subject.subjectEnName || subject.subjectName, // 兼容旧字段
      value: subject.subjectEnName || subject.subjectName
    }))
  } catch (error) {
    subjectOptions.value = []
  } finally {
    loadingSubjects.value = false
  }
}

const goToPaperManagement = async () => {
  if (!selectedSubject.value) return
  
  try {
    // 将subject存入session（使用CET4专用API）
    await axios.post('http://localhost:8080/api/cet4/knowledge-manager/set-subject', {
      subject: selectedSubject.value
    }, {
      withCredentials: true
    })
    
    // 根据subject跳转到对应的页面（route 未定义路径参数，使用 query 避免 vue-router 丢弃 params 警告）
    router.push({ name: 'PaperAnalysisCET4', query: { subject: selectedSubject.value } })
  } catch (error) {
    alert('设置科目失败，请重试')
  }
}

const goToPaperDisplay = () => {
  router.push({ name: 'PaperDisplay' })
}
</script>

<style scoped>
.km-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f7fb;
}

.km-header {
  position: sticky;
  top: 0;
  z-index: 10;
  background: #ffffff;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.06);
}

.km-header-inner {
  max-width: 1080px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 20px;
  box-sizing: border-box;
}

.km-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
}

.km-logo {
  font-size: 22px;
}

.km-title {
  font-size: 18px;
  font-weight: 700;
  color: #2c3e50;
}

.km-nav {
  display: flex;
  align-items: center;
  gap: 10px;
}

.km-nav-btn {
  padding: 8px 14px;
  background: #ffffff;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  color: #34495e;
  cursor: pointer;
  transition: all 0.2s ease;
}

.km-nav-btn:hover:not(:disabled) {
  background: #f5f7fb;
  transform: translateY(-1px);
}

.km-nav-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.km-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 32px 20px 48px;
  box-sizing: border-box;
  max-width: 1080px;
  margin: 0 auto;
  width: 100%;
}

.km-page-title {
  margin: 0 0 20px 0;
  color: #2c3e50;
  font-size: 22px;
  font-weight: 700;
}

h1 {
  margin-bottom: 40px;
  color: #2c3e50;
}

.cards-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 320px));
  gap: 28px;
  justify-content: center;
  align-content: start;
  justify-items: center;
  width: 100%;
  max-width: 960px;
  margin: 0 auto;
  padding: 8px 0; /* 轻微内边距，避免贴边感 */
}

.select-card {
  background: #ffffff;
  padding: 16px 18px;
  border-radius: 10px;
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.08);
  text-align: center;
  max-width: 320px;
  width: 100%;
}

.icon {
  font-size: 32px;
  margin-bottom: 10px;
}

.card-desc {
  color: #546e7a;
  line-height: 1.6;
  margin: 16px 0;
  min-height: 48px;
}

.form-field {
  margin: 12px 0;
  text-align: left;
}

.form-field label {
  display: block;
  margin-bottom: 8px;
  font-weight: 600;
  color: #2c3e50;
}

.subject-select {
  width: 100%;
  padding: 8px 10px;
  border: 1px solid #cfd8dc;
  border-radius: 8px;
  font-size: 13px;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.subject-select:focus {
  outline: none;
  border-color: #3498db;
  box-shadow: 0 0 0 2px rgba(52, 152, 219, 0.15);
}

.manage-button {
  display: inline-block;
  margin-top: 12px;
  padding: 8px 18px;
  background: linear-gradient(135deg, #3498db 0%, #2980b9 100%);
  color: #fff;
  border: none;
  border-radius: 999px;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.manage-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.manage-button:not(:disabled):hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 24px rgba(52, 152, 219, 0.35);
}

.paper-button {
  background: linear-gradient(135deg, #e67e22 0%, #d35400 100%);
}

.paper-button:not(:disabled):hover {
  box-shadow: 0 10px 24px rgba(230, 126, 34, 0.35);
}

p {
  color: #546e7a;
  line-height: 1.6;
}

@media (max-width: 600px) {
  .select-card {
    padding: 14px 16px;
  }
}
</style>



