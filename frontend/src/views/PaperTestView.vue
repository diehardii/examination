<template>
  <div class="app-container">
    <SideBarMenu />
    <div class="main-content">
      <div class="paper-container">
        <h1 class="header">请选择你要考的试卷</h1>
        <div class="paper-section">
          <h3 class="sticky-title">我的试卷（您可以双击选中的试卷查看该试卷历史测试情况）</h3>
          <div class="paper-list">
            <table>
              <thead>
              <tr>
                <th>试卷ID</th>
                <th>试卷名称</th>
                <th>试卷内容</th>
                <th>难度</th>
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
                <td>{{ paper.examPaperName }}</td>
                <td>{{ paper.examPaperContent }}</td>
                <td>{{ paper.examPaperDifficulty }}</td>
                <td>{{ paper.examPaperSubject }}</td>
              </tr>
              </tbody>
            </table>
          </div>
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
              <el-table-column prop="testScore" label="得分" width="100" />

            </el-table>

            <div v-if="selectedTestRecord" class="test-details">
              <HistoryTestResult
                  v-if="showTestResult"
                  :testId="selectedTestRecord.testId"
                  :key="selectedTestRecord.testId"
                  :onClose="closeDialog"
              />
            </div>
          </div>
        </el-dialog>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted,nextTick } from 'vue'
import { useRouter } from 'vue-router'
import {ElMessage, ElMessageBox} from 'element-plus'
import SideBarMenu from '@/components/SideBarMenu.vue'
import HistoryTestResult from '@/components/HistoryTestResult.vue'
import { getUserExamPapers } from '@/service/userExamPaperService'
import { getUserTestRecordsByExamPaperId } from '@/service/userTestRecordDetailService'

export default {
  components: {
    SideBarMenu,
    HistoryTestResult
  },
  setup() {
    const router = useRouter()
    const papers = ref([])
    const selectedPaperId = ref(null)
    const currentUser = ref(JSON.parse(localStorage.getItem('user') || {}))
    const showRecordsDialog = ref(false)
    const testRecords = ref([])
    const selectedPaper = ref(null)
    const selectedTestRecord = ref(null)
    const showTestResult = ref(false)

    onMounted(async () => {
      const username = currentUser.value.username
      papers.value = await getUserExamPapers(username)
      if (papers.value.length > 0) {
        selectedPaperId.value = papers.value[0].id
       // await loadTestRecords(papers.value[0].id)
      }
    })

    const selectPaper = (paperId) => {
      selectedPaperId.value = paperId
    }

    const showTestRecords = async (paperId) => {
      selectedPaper.value = papers.value.find(p => p.id === paperId);
      await loadTestRecords(paperId);


      // 添加以下代码：在对话框显示后自动点击第一行
      nextTick(() => {
        if (testRecords.value.length > 0) {
          // 模拟点击第一行
          showTestDetails(testRecords.value[0])

        }
      })
    }




    const loadTestRecords = async (paperId) => {

      try {
        const userId = currentUser.value.id;
        const records = await getUserTestRecordsByExamPaperId(userId, paperId);

        // 双重保障：处理可能的null/undefined
        testRecords.value = Array.isArray(records) ? records : [];

        if (testRecords.value.length > 0) {
          selectedTestRecord.value = testRecords.value[0];
          showRecordsDialog.value = true;
        } else {
          await ElMessageBox.confirm('该试卷暂无历史考试记录', '提示', {
            confirmButtonText: '知道了',
            cancelButtonText: '', // 设为空字符串可以隐藏取消按钮
            type: 'info',
            showCancelButton: false,
            customClass: 'custom-message-box', // 明确不显示取消按钮
          });
        }
      } catch (error) {

        await ElMessageBox.confirm('该试卷暂无历史考试记录', '提示', {
          confirmButtonText: '知道了',
          cancelButtonText: '', // 设为空字符串可以隐藏取消按钮
          type: 'info',
          showCancelButton: false,
          customClass: 'custom-message-box', // 明确不显示取消按钮
        });
      }
    }

    const showTestDetails = async (record) => {
      console.log("Records:", JSON.stringify(record, null, 2))
      selectedTestRecord.value = record
      showTestResult.value = true



    }

    const formatDateTime = (dateTime) => {
      return dateTime ? new Date(dateTime).toLocaleString() : ''
    }

    const confirmSelection = () => {
      if (!selectedPaperId.value) {
        ElMessage.error('请先选择一份试卷')
        return
      }
      router.push({ name: 'QuestionAnswer', params: { paperId: selectedPaperId.value } })
    }

    const tableRowClassName = ({ row }) => {
      return selectedTestRecord.value?.testId === row.testId ? 'highlight-row' : ''
    }

    const closeDialog = () => {
      showRecordsDialog.value = false;
    };

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
      showTestDetails,
      formatDateTime,
      confirmSelection,
      tableRowClassName,
      closeDialog,
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