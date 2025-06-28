<template>
  <div class="content">
    <div class="card">
      <h1 class="header">错题管理</h1>

      <div class="filter-section">
        <el-input
            v-model="paperName"
            placeholder="请输入试卷名称"
            style="width: 300px; margin-right: 20px"
        />

        <el-select
            v-model="selectedSubject"
            placeholder="请选择科目"
            style="width: 200px"
            @change="loadWrongQuestions"
        >
          <el-option
              v-for="subject in subjects"
              :key="subject.subjectId"
              :label="subject.subjectName"
              :value="subject.subjectId"
          />
        </el-select>
      </div>

      <div class="question-container">
        <div class="question-section">
          <div class="question-list fixed-height">
            <table id="allQuestionsTable">
              <caption class="sticky-title">错题列表</caption>
              <thead>
              <tr>
                <th>试卷名称</th>
                <th>题目内容</th>
                <th>正确答案</th>
                <th>用户答案</th>
              </tr>
              </thead>
              <tbody>
              <tr
                  v-for="question in wrongQuestions"
                  :key="question.id"
                  @click="highlightRow($event, 'allQuestionsTable')"
              >
                <td>{{ question.examPaperName }}</td>
                <td>{{ question.content }}</td>
                <td>{{ question.correctAnswer }}</td>
                <td>{{ question.userAnswer}}</td>
              </tr>
              </tbody>
            </table>
          </div>
        </div>

        <div class="button-container">
          <button class="transfer-btn" @click="transferRight">→</button>
          <button class="transfer-btn" @click="transferLeft">←</button>
        </div>

        <div class="question-section">
          <div class="question-list fixed-height">
            <table id="selectedQuestionsTable">
              <caption class="sticky-title">已选试题</caption>
              <thead>
              <tr>
                <th>试卷名称</th>
                <th>题目内容</th>
                <th>正确答案</th>
                <th>用户答案</th>
              </tr>
              </thead>
              <tbody>
              <tr
                  v-for="question in selectedQuestions"
                  :key="question.id"
                  @click="highlightRow($event, 'selectedQuestionsTable')"
              >
                <td>{{ question.examPaperName }}</td>
                <td>{{ question.content }}</td>
                <td>{{ question.correctAnswer }}</td>
                <td>{{ question.userAnswer }}</td>
              </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <div class="submit-btn-container">
        <button class="submit-btn" @click="generatePaper">生成错题集</button>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted,nextTick  } from 'vue'
import { ElMessage } from 'element-plus'

import {
  getWrongQuestions,
  generateWrongExamPaper
} from '@/service/wrongExamPaperGenService.js'
import {getSubjects} from '@/service/subjectService.js'
import { useRouter } from 'vue-router';
export default {
  name: 'WrongExamPaperGenView',

  setup() {
    const paperName = ref('')
    const selectedSubject = ref(null)
    const subjects = ref([])
    const wrongQuestions = ref([])
    const selectedQuestions = ref([])
    const currentUser = JSON.parse(localStorage.getItem('user') || '{}')

    const router = useRouter();

    const loadSubjects = async () => {
      try {
        subjects.value = await getSubjects()
      } catch (error) {
        ElMessage.error('加载科目失败')
      }
    }

    const loadWrongQuestions = async () => {
      if (!selectedSubject.value) return

      try {
        wrongQuestions.value = await getWrongQuestions(
            selectedSubject.value,
            currentUser.id
        )
        // 如果有错题数据，自动选中第一条
        if (wrongQuestions.value.length > 0) {
          await nextTick() // 等待DOM更新
          const firstRow = document.getElementById('allQuestionsTable')
              .querySelector('tbody tr:first-child')
          if (firstRow) {
            highlightRow({ currentTarget: firstRow }, 'allQuestionsTable')
          }
        }
      } catch (error) {
        ElMessage.error('获取错题失败！')
      }
    }

    const highlightRow = (eventOrElement, tableId) => {
      const row = eventOrElement.currentTarget || eventOrElement
      const tbody = document.getElementById(tableId).querySelector('tbody')
      const rows = tbody.querySelectorAll('tr')

      rows.forEach(r => r.classList.remove('highlight'))
      row.classList.add('highlight')
    }

    const transferRight = () => {
      const leftTable = document.getElementById('allQuestionsTable')
      const leftTbody = leftTable.querySelector('tbody')
      const highlightedRow = leftTbody.querySelector('tr.highlight')

      if (highlightedRow) {
        const index = highlightedRow.rowIndex - 1
        const question = wrongQuestions.value[index]

        // 添加到右侧列表
        selectedQuestions.value.push(question)
        // 从左侧列表移除
        wrongQuestions.value.splice(index, 1)

        // 自动高亮右侧列表的第一条记录（如果有）
        nextTick(() => {
          const rightTbody = document.getElementById('selectedQuestionsTable').querySelector('tbody')
          const rightRows = rightTbody.querySelectorAll('tr')
          if (rightRows.length > 0) {
            highlightRow(rightRows[0], 'selectedQuestionsTable')
          }

          // 自动高亮左侧列表的新第一条记录（如果有剩余记录）
          const leftRows = leftTbody.querySelectorAll('tr')
          if (leftRows.length > 0) {
            highlightRow(leftRows[0], 'allQuestionsTable')
          }
        })
      } else {
        ElMessage.warning('请先选择要移动的题目')
      }
    }

    const transferLeft = () => {
      const rightTable = document.getElementById('selectedQuestionsTable')
      const rightTbody = rightTable.querySelector('tbody')
      const highlightedRow = rightTbody.querySelector('tr.highlight')

      if (highlightedRow) {
        const index = highlightedRow.rowIndex - 1
        const question = selectedQuestions.value[index]

        // 添加到左侧列表
        wrongQuestions.value.push(question)
        // 从右侧列表移除
        selectedQuestions.value.splice(index, 1)

        // 自动高亮左侧列表的第一条记录（如果有）
        nextTick(() => {
          const leftTbody = document.getElementById('allQuestionsTable').querySelector('tbody')
          const leftRows = leftTbody.querySelectorAll('tr')
          if (leftRows.length > 0) {
            highlightRow(leftRows[0], 'allQuestionsTable')
          }

          // 自动高亮右侧列表的新第一条记录（如果有剩余记录）
          const rightRows = rightTbody.querySelectorAll('tr')
          if (rightRows.length > 0) {
            highlightRow(rightRows[0], 'selectedQuestionsTable')
          }
        })
      } else {
        ElMessage.warning('请先选择要移动的题目')
      }
    }

    const generatePaper = async () => {
      if (!paperName.value || !selectedSubject.value) {
        ElMessage.warning('请填写试卷名称并选择科目')
        return
      }

      if (selectedQuestions.value.length === 0) {
        ElMessage.warning('请至少选择一道题目')
        return
      }

      try {
        const ids = [];
        for (let i = 0; i < selectedQuestions.value.length; i++) {
          ids.push(selectedQuestions.value[i].questionId);
        }

        const paperData=  await generateWrongExamPaper({
          paperName: paperName.value,
          subjectId: selectedSubject.value,
          userId: currentUser.id,
          questionIds: ids
        });
        console.log('响应数据:', JSON.stringify(paperData, null, 2));
        console.log(paperData.id);
        if (paperData?.id) {
          console.log('Begin PaperShow:', paperData.id);
          router.push({  // 使用 router 而不是 this.$router
            name: 'PaperShow',
            params: { id: paperData.id }
          });
        } else {
          throw new Error('生成的试卷数据不完整，缺少ID');
        }


      } catch (error) {
        ElMessage.error('生成错题集失败')
      }
    }

    onMounted(() => {
      loadSubjects()
    })

    return {
      paperName,
      selectedSubject,
      subjects,
      wrongQuestions,
      selectedQuestions,
      loadWrongQuestions,
      highlightRow,
      transferRight,
      transferLeft,
      generatePaper
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
  background-color: #f5f5f5;
  margin-left: 250px;
  width: calc(100% - 250px);
}

.content {
  padding: 20px;
}

.header {
  color: #333;
  margin-bottom: 20px;
  font-size: 24px;
  text-align: center;
}

.card {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  padding: 15px;
}

.filter-section {
  display: flex;
  margin-bottom: 20px;
  align-items: center;
}

.question-container {
  display: flex;
  justify-content: space-between;
  margin-top: 20px;
  gap: 20px;
  height: 355px;
}

.question-list {
  width: 100%;
  height: 350px;
  overflow: auto;
  border: 1px solid #ddd;
  background-color: #f0f8ff;
  border-radius: 4px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.1);
  white-space: nowrap;
}

.question-section {
  position: relative;
  width: 45%;
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
  white-space: nowrap;
  text-align: center;
  font-weight: bold;
  font-size: 1.17em;
}

table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

thead {
  position: sticky;
  top: 52px; /* 标题高度 + 内边距 */
  z-index: 10;
}

thead tr {
  background-color: #f3e5ff !important;
  background-image: linear-gradient(to bottom, #f3e5ff, #e6d1f9);
}

thead th {
  padding: 12px 15px;
  text-align: left;
  background-color: #f8f9fa;
  color: #003366;
  font-weight: 600;
  border-bottom: 2px solid #d8bfd8;
}

tbody tr {
  cursor: pointer;
  transition: background-color 0.2s;
}

tbody tr td {
  padding: 12px 15px;
  border-bottom: 1px solid #dee2e6;
  color:#333;
}

tbody tr.highlight {
  background-color: #e6f7ff !important;
  color: #003366 !important;
}

tbody tr:hover:not(.highlight) {
  background-color: #f8f9fa;
}

.button-container {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  width: 10%;
}

.transfer-btn {
  margin: 10px 0;
  padding: 8px 12px;
  font-size: 16px;
  cursor: pointer;
  background-color: #f8f9fa;
  border: 1px solid #ddd;
  border-radius: 4px;
  transition: all 0.2s;
}

.transfer-btn:hover {
  background-color: #e9ecef;
  border-color: #adb5bd;
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

/* 滚动条样式 */
.question-list::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

.question-list::-webkit-scrollbar-track {
  background: #f1f1f1;
}

.question-list::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 4px;
}

.question-list::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}
</style>