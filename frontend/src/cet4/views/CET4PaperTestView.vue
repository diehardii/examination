<template>
  <div class="app-container">
    <SideBarMenu />
    <div class="main-content">
      <div class="paper-container">
        <h1 class="header">请选择你要考的试卷</h1>
        <p class="user-hint" v-if="currentUser && currentUser.username">
          当前账号：{{ currentUser.username }}
        </p>
        <div class="paper-section">
          <h3 class="sticky-title">英文试卷列表（双击查看历史测试情况）</h3>
          <div class="paper-list" v-if="papers.length > 0">
            <table>
              <thead>
              <tr>
                <th>试卷ID</th>
                <th>试卷名称</th>
                <th>科目</th>
              </tr>
              </thead>
              <tbody>
              <tr
                  v-for="paper in papers"
                  :key="paper.id"
                  :class="{ highlight: selectedPaperId === paper.id }"
                  @click="selectPaper(paper.id)"
                  @dblclick="showTestRecords(paper.id)"
              >
                <td>{{ paper.id }}</td>
                <td>{{ paper.examPaperEnName }}</td>
                <td>{{ paper.examPaperEnSubject }}</td>
              </tr>
              </tbody>
            </table>
            </div>
            <el-empty
              v-else
              description="暂无可用试卷，请先生成或联系老师分配"
              :image-size="200"
            />
          <div class="submit-btn-container">
            <button class="submit-btn" @click="confirmSelection">确认</button>
          </div>
        </div>

        <!-- 测试记录弹窗（使用Element Plus但保持原样式） -->
        <el-dialog
            v-model="showRecordsDialog"
            width="90%"
            top="3vh"
            class="custom-dialog"
            style="max-width: 1200px; height: 85vh"
        >
          <div v-if="selectedPaper" class="record-dialog">
            <h3 style="text-align: center">{{ selectedPaper.examPaperName }} - 历史考试记录</h3>
            <el-table
                :data="testRecords"
                class="record-table custom-el-table"
                @row-click="showTestDetails"
                :row-class-name="tableRowClassName"
            >
              <el-table-column prop="testTime" label="考试时间" width="180">
                <template #default="{ row }">
                  {{ formatDateTime(row.testTime) }}
                </template>
              </el-table-column>
              <el-table-column prop="examPaper.examPaperName" label="试卷名称" width="180" />
              <el-table-column prop="examPaper.examPaperContent" label="试卷内容" width="200" />
              <el-table-column prop="examPaper.examPaperDifficulty" label="试卷难度" width="120" />
              <el-table-column prop="examPaper.examPaperSubject" label="科目" width="120" />
              <el-table-column prop="examPaper.examPaperQuestionNumber" label="题目总数" width="120" />
              <el-table-column prop="correctNumber" label="正确题数" width="120" />
              <el-table-column prop="testScore" label="得分" width="100">
                <template #default="{ row }">
                  {{ formatScore(row.testScore || row.testEnScore) }}
                </template>
              </el-table-column>

            </el-table>

            <div v-if="selectedTestRecord" class="test-details">
              <!-- HistoryTestResult 组件已删除，相关引用已清理 -->
            </div>
          </div>
        </el-dialog>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import SideBarMenu from '@/common/components/SideBarMenu.vue'
import axios from 'axios'

export default {
  components: {
    SideBarMenu
  },
  setup() {
    const router = useRouter()
    const papers = ref([])
    const selectedPaperId = ref(null)
    let parsedUser = null
    try {
      const storedUser = localStorage.getItem('user')
      parsedUser = storedUser ? JSON.parse(storedUser) : null
    } catch (error) {
    }
    const currentUser = ref(parsedUser)
    const showRecordsDialog = ref(false)
    const testRecords = ref([])
    const selectedPaper = ref(null)
    const selectedTestRecord = ref(null)
    const showTestResult = ref(false)

    onMounted(async () => {
      // 如果选择了高考科目，直接跳转到 HS3 试卷测试页
      const selectedSubject = sessionStorage.getItem('selectedSubject') || ''
      const isGaokao = ['高考', '高考英语', 'GAOKAO'].includes(selectedSubject)
      if (isGaokao) {
        router.replace({ path: '/paper-test-hs3', query: { subject: '高考' } }).catch(() => {})
        return
      }

      // 默认加载 CET4 试卷列表
      await loadExamPaperList()
    })

    const loadExamPaperList = async () => {
      try {
        const response = await axios.get('http://localhost:8080/api/cet4/exam-paper/papers-en', {
          withCredentials: true
        })
        
        if (Array.isArray(response.data)) {
          papers.value = response.data
          if (papers.value.length > 0) {
            selectedPaperId.value = papers.value[0].id
          }
        } else {
          ElMessage.error('获取试卷列表失败')
        }
      } catch (error) {
        if (error.response?.status === 401) {
          papers.value = []
          selectedPaperId.value = null
          ElMessage.error('登录信息已失效，请重新登录后查看试卷')
        } else {
          ElMessage.error('加载试卷列表失败，请稍后重试')
        }
      }
    }

    const selectPaper = (paperId) => {
      selectedPaperId.value = paperId
    }

    const showTestRecords = async (paperId) => {
      // 暂时禁用历史记录功能，因为需要UserTestRecordEn表
      ElMessage.info('英文试卷历史记录功能暂未实现')
    }

    const formatDateTime = (dateTime) => {
      return dateTime ? new Date(dateTime).toLocaleString() : ''
    }

    const confirmSelection = () => {
      if (!selectedPaperId.value) {
        ElMessage.error('请先选择一份试卷')
        return
      }
      // 跳转到答题页面，使用路径跳转而不是命名路由
      const paperId = String(selectedPaperId.value)
      
      router.push(`/question-answer-en/${paperId}`).then(() => {
        // 路由跳转成功
      }).catch(err => {
      })
    }

    const tableRowClassName = ({ row }) => {
      return selectedTestRecord.value?.testId === row.testId ? 'highlight-row' : ''
    }

    const closeDialog = () => {
      showRecordsDialog.value = false;
    };

    // 格式化分数，保留两位小数
    const formatScore = (score) => {
      if (score === null || score === undefined) {
        return '0.00'
      }
      const numScore = typeof score === 'number' ? score : parseFloat(score)
      return isNaN(numScore) ? '0.00' : numScore.toFixed(2)
    }

    return {
      papers,
      selectedPaperId,
      showRecordsDialog,
      testRecords,
      selectedPaper,
      selectedTestRecord,
      showTestResult,
      selectPaper,
      showTestRecords,
      formatDateTime,
      formatScore,
      confirmSelection,
      tableRowClassName,
      closeDialog,
      currentUser,
    }
  }
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
  margin-top: 50px;
}

.header {
  color: #333;
  margin-bottom: 20px;
  font-size: 24px;
  text-align: center;
}

.paper-section {
  position: relative;
  width: 100%;
  box-sizing: border-box;
}

.user-hint {
  margin: 4px 0 16px;
  color: #666;
  text-align: center;
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

table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

thead tr {
  position: sticky;
  top: 0;
  z-index: 10;
  background-color: #f8f9fa !important;
  background-image: linear-gradient(to bottom, #f8f9fa, #f8f9fa);
}

thead th {
  padding: 12px 15px;
  text-align: left;
  color: #003366;
  font-weight: 600;
  border-bottom: 2px solid #dee2e6;
}

tbody tr {
  cursor: pointer;
  transition: background-color 0.2s;
}

tbody tr td {
  padding: 12px 15px;
  border-bottom: 1px solid #dee2e6;
  color: #333;
}

tbody tr.highlight {
  background-color: #e6f7ff !important;
  color: #003366 !important;
}

tbody tr:hover:not(.highlight) {
  background-color: #f8f9fa;
}

.submit-btn-container {
  display: flex;
  justify-content: center;
  width: 100%;
  margin-top: 20px;
}

.submit-btn {
  margin-top: 20px;
  padding: 10px 20px;
  background-color: #4CAF50;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 16px;
  transition: background-color 0.2s;
}

.submit-btn:hover {
  background-color: #45a049;
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

/* 弹窗样式修改 */
.custom-dialog {

  height: 85vh !important;  /* 从80vh增加到85vh */
  max-height: 85vh !important;
  width: 90% !important;
  max-width: 1200px !important;  /* 添加最大宽度限制 */
}

.record-dialog {

  max-height: 80vh;  /* 从70vh增加到80vh */
  overflow-y: auto;
  width: 100%;
}

.record-table {
  margin-top: 20px;
  width: 100%;
  border: 1px solid #ddd;
  border-radius: 4px;
}

.test-details {
  width: 94%;
  margin-top: 30px;
  padding: 20px;
  border: 1px solid #eee;
  border-radius: 4px;
}

/* 自定义 Element Table 样式以匹配原表格 */
.custom-el-table {
  width: 98%;
  border-collapse: collapse;
  font-size: 14px;
  background-color: #f0f8ff;
  border: 1px solid #ddd;
  border-radius: 4px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.1);
}

.custom-el-table :deep(.el-table__header-wrapper) {
  position: sticky;
  top: 0;
  z-index: 10;
  width: 100% !important;
}

.custom-el-table :deep(.el-table__header th) {
  padding: 12px 15px;
  text-align: left;
  color: #003366;
  font-weight: 600;
  border-bottom: 2px solid #dee2e6;
  background-color: #f8f9fa !important;
}

.custom-el-table :deep(.el-table__body tr) {
  cursor: pointer;
  transition: background-color 0.2s;
}

.custom-el-table :deep(.el-table__body td) {
  padding: 12px 15px;
  border-bottom: 1px solid #dee2e6;
  color: #333;
}

.custom-el-table :deep(.el-table__body tr:hover) {
  background-color: #f8f9fa;
}

.custom-el-table :deep(.el-table__body tr.highlight-row) {
  background-color: #e6f7ff !important;
  color: #003366 !important;
}

/* 滚动条样式 */
.custom-el-table :deep(.el-table__body-wrapper)::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

.custom-el-table :deep(.el-table__body-wrapper)::-webkit-scrollbar-track {
  background: #f1f1f1;
}

.custom-el-table :deep(.el-table__body-wrapper)::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 4px;
}

.custom-el-table :deep(.el-table) {
  width: 100% !important;
}
/* 让确认按钮变成绿色 */
.custom-message-box .el-button--primary {
  background-color: #67C23A !important;
  border-color: #67C23A !important;
}

/* 鼠标悬停效果 */
.custom-message-box .el-button--primary:hover {
  background-color: #5daf34 !important;
  border-color: #5daf34 !important;
}

/* 点击效果 */
.custom-message-box .el-button--primary:active {
  background-color: #529b2e !important;
  border-color: #529b2e !important;
}

:deep(.el-message-box__btns .el-button--primary) {
  background-color: #67C23A !important;
  border-color: #67C23A !important;
}
</style>