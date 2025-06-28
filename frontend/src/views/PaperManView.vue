<template>
  <div class="app-container">
    <SideBarMenu />

    <div class="main-content">
      <div class="content">
        <div class="card">
          <h1 class="header">请选择试卷到用户名下</h1>

          <div class="paper-container">
            <div class="paper-section">
              <h3 class="sticky-title">可选试卷</h3>
              <div class="paper-list fixed-height">
                <table id="allPapersTable">
                  <thead>
                  <tr>
                    <th>试卷ID</th>
                    <th>试卷名称</th>
                    <th>试卷内容</th>
                    <th>难度</th>
                    <th>题目数量</th>
                    <th>试卷科目</th>
                  </tr>
                  </thead>
                  <tbody>
                  <tr v-for="paper in availablePapers"
                      :key="paper.id"
                      @click="highlightRow($event, 'allPapersTable')">
                    <td>{{ paper.id }}</td>
                    <td>{{ paper.examPaperName }}</td>
                    <td>{{ paper.examPaperContent }}</td>
                    <td>{{ paper.examPaperDifficulty }}</td>
                    <td>{{ paper.examPaperQuestionNumber }}</td>
                    <td>{{ paper.examPaperSubject }}</td>
                  </tr>
                  </tbody>
                </table>
              </div>
            </div>

            <div class="button-container">
              <button class="transfer-btn" @click="transferRight">→</button>
              <button class="transfer-btn" @click="transferLeft">←</button>
            </div>

            <div class="paper-section">
              <h3 class="sticky-title">我的试卷</h3>
              <div class="paper-list fixed-height">
                <table id="userPapersTable">
                  <thead>
                  <tr>
                    <th>试卷ID</th>
                    <th>试卷名称</th>
                    <th>试卷内容</th>
                    <th>难度</th>
                    <th>题目数量</th>
                    <th>试卷科目</th>
                  </tr>
                  </thead>
                  <tbody>
                  <tr v-for="paper in userPapers"
                      :key="paper.id"
                      @click="highlightRow($event, 'userPapersTable')">
                    <td>{{ paper.id }}</td>
                    <td>{{ paper.examPaperName }}</td>
                    <td>{{ paper.examPaperContent }}</td>
                    <td>{{ paper.examPaperDifficulty }}</td>
                    <td>{{ paper.examPaperQuestionNumber }}</td>
                    <td>{{ paper.examPaperSubject }}</td>
                  </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>

          <div class="submit-btn-container">
            <button class="submit-btn" @click="submitUserPapers">确认保存</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import SideBarMenu from '@/components/SideBarMenu.vue';
import { ElMessage } from 'element-plus';

import {
  getAvailableExamPapers,
  getUserExamPapers,
  saveUserExamPapers
} from '@/service/userExamPaperService';

export default {
  name: 'PaperMan',
  components: {
    SideBarMenu
  },
  data() {
    return {
      availablePapers: [],
      userPapers: [],
      currentUser: JSON.parse(localStorage.getItem('user') || '{}'),
      selectedLeftRow: null,
      selectedRightRow: null
    };
  },
  async created() {
    await this.fetchPapers();
    this.$nextTick(() => {
      this.highlightFirstRows();
    });
  },
  methods: {
    async fetchPapers() {
      try {
        const username = this.currentUser.username;
        this.availablePapers = await getAvailableExamPapers(username);
        this.userPapers = await getUserExamPapers(username);
      } catch (error) {
        console.error('获取试卷失败:', error);
        ElMessage.error('获取试卷失败');
      }
    },

    highlightFirstRows() {
      const leftFirstRow = document.querySelector('#allPapersTable tbody tr');
      const rightFirstRow = document.querySelector('#userPapersTable tbody tr');

      if (leftFirstRow) {
        this.highlightRow({ currentTarget: leftFirstRow }, 'allPapersTable');
      }
      if (rightFirstRow) {
        this.highlightRow({ currentTarget: rightFirstRow }, 'userPapersTable');
      }
    },

    highlightRow(event, tableId) {
      const row = event.currentTarget;
      const tbody = document.getElementById(tableId).querySelector('tbody');
      const rows = tbody.querySelectorAll('tr');

      rows.forEach(r => r.classList.remove('highlight'));
      row.classList.add('highlight');

      // 保存当前选中的行
      if (tableId === 'allPapersTable') {
        this.selectedLeftRow = row;
      } else {
        this.selectedRightRow = row;
      }
    },

    transferRight() {
      if (!this.selectedLeftRow) return;

      const paperId = this.selectedLeftRow.cells[0].textContent;
      const paper = this.availablePapers.find(p => p.id == paperId);

      if (paper) {
        // 从可用试卷中移除
        this.availablePapers = this.availablePapers.filter(p => p.id != paperId);
        // 添加到用户试卷
        this.userPapers.push(paper);

        // 更新选中行
        this.$nextTick(() => {
          const leftFirstRow = document.querySelector('#allPapersTable tbody tr');
          if (leftFirstRow) {
            this.highlightRow({ currentTarget: leftFirstRow }, 'allPapersTable');
          }
          const rightRows = document.querySelectorAll('#userPapersTable tbody tr');
          if (rightRows.length > 0) {
            this.highlightRow({ currentTarget: rightRows[rightRows.length - 1] }, 'userPapersTable');
          }
        });
      }
    },

    transferLeft() {
      if (!this.selectedRightRow) return;

      const paperId = this.selectedRightRow.cells[0].textContent;
      const paper = this.userPapers.find(p => p.id == paperId);

      if (paper) {
        // 从用户试卷中移除
        this.userPapers = this.userPapers.filter(p => p.id != paperId);
        // 添加到可用试卷
        this.availablePapers.push(paper);

        // 更新选中行
        this.$nextTick(() => {
          const rightFirstRow = document.querySelector('#userPapersTable tbody tr');
          if (rightFirstRow) {
            this.highlightRow({ currentTarget: rightFirstRow }, 'userPapersTable');
          }
          const leftRows = document.querySelectorAll('#allPapersTable tbody tr');
          if (leftRows.length > 0) {
            this.highlightRow({ currentTarget: leftRows[leftRows.length - 1] }, 'allPapersTable');
          }
        });
      }
    },

    async submitUserPapers() {
      try {
        const paperIds = this.userPapers.map(paper => paper.id);
        await saveUserExamPapers(this.currentUser.username, paperIds);

        ElMessage({
          message: '试卷保存成功',
          type: 'success',
          duration: 2000
        });
      } catch (error) {
        console.error('保存试卷失败:', error);
        ElMessage.error('保存试卷失败');
      }
    }
  }
};
</script>

<style scoped>
/* 保持原有的CSS样式不变 */
.app-container {
  display: flex;
  min-height: 100vh;
}

.main-content {
  flex: 1;
  padding: 20px;
  background-color: #f5f5f5;
  margin-left: 250px; /* 等于侧边栏宽度 */
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
  padding: 20px;
}

.paper-container {
  display: flex;
  justify-content: space-between;
  margin-top: 20px;
  gap: 20px;
  padding-left: 10px;
}

.paper-list {
  width: 100%;
  height: 400px;
  overflow: auto;
  border: 1px solid #ddd;
  background-color: #f0f8ff;
  border-radius: 4px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.1);
  white-space: nowrap;
  box-sizing: border-box;
}

.paper-section {
  position: relative;
  width: 45%;
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
  white-space: nowrap;
  box-sizing: border-box;
  width: 100%;
}

table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
  table-layout: auto;
}

thead tr {
  position: sticky;
  top: 0;
  z-index: 10;
  background-color: #f3e5ff !important;
  background-image: linear-gradient(to bottom, #f3e5ff, #e6d1f9);
  user-select: none;
}

thead th {
  padding: 12px 15px;
  text-align: left;
  background-color: #f8f9fa;
  color: #003366;
  font-weight: 600;
  border-bottom: 2px solid #d8bfd8;
  white-space: nowrap;
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

.paper-list::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}
</style>