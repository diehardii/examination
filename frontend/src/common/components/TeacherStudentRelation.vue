<template>

      <div class="content">
        <div class="page-header">
          <h2 class="page-title">师生关系维护</h2>
        </div>

        <!-- 筛选区域 -->
        <div class="filter-section">
          <div class="filter-item">
            <label class="filter-label">选择教师：</label>
            <el-select v-model="selectedTeacher" placeholder="请选择教师" @change="onTeacherChange" class="teacher-select">
              <el-option
                  v-for="teacher in teachers"
                  :key="teacher.id"
                  :label="teacher.realName || teacher.username"
                  :value="teacher.id"
              />
            </el-select>
          </div>
        </div>

        <!-- 学生表格区域 -->
        <div class="student-container" :class="{ 'single-table': !selectedTeacher }">
          <div class="student-section">
            <div class="sticky-title">{{ selectedTeacher ? '未分配学生' : '全部学生' }}</div>
            <div class="student-list fixed-height">
              <table id="allStudentsTable">
                <thead>
                <tr>
                  <th>学生ID</th>
                  <th>姓名</th>
                  <th>邮箱</th>
                </tr>
                </thead>
                <tbody>
                <tr
                    v-for="student in allStudents"
                    :key="student.id"
                    @click="highlightRow($event, 'allStudentsTable')"
                >
                  <td>{{ student.id }}</td>
                  <td>{{ student.realName }}</td>
                  <td>{{ student.email }}</td>
                </tr>
                </tbody>
              </table>
            </div>
          </div>

          <div class="button-container" v-if="selectedTeacher">
            <button class="transfer-btn" @click="transferRight">→</button>
            <button class="transfer-btn" @click="transferLeft">←</button>
          </div>

          <div class="student-section" v-if="selectedTeacher">
            <div class="sticky-title">该教师的学生</div>
            <div class="student-list fixed-height">
              <table id="teacherStudentsTable">
                <thead>
                <tr>
                  <th>学生ID</th>
                  <th>姓名</th>
                  <th>邮箱</th>
                </tr>
                </thead>
                <tbody>
                <tr
                    v-for="student in teacherStudents"
                    :key="student.id"
                    @click="highlightRow($event, 'teacherStudentsTable')"
                >
                  <td>{{ student.id }}</td>
                  <td>{{ student.realName }}</td>
                  <td>{{ student.email }}</td>
                </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>

        <!-- 保存按钮 -->
        <div class="submit-btn-container" v-if="selectedTeacher">
          <button class="submit-btn" @click="saveTeacherStudentRelation">保存师生关系</button>
        </div>
      </div>

</template>

<script>
import { ref, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

import { getAllUsers } from '@/common/service/userDetailService'
import { getTeacherStudents, saveTeacherStudentRelation as saveTeacherStudentRelationService } from '@/common/service/teacherStudentService'

export default {
  name: 'TeacherStudentRelation',

  setup() {
    const teachers = ref([])
    const allStudents = ref([]) // 左侧表格：未分配给该教师的学生
    const teacherStudents = ref([]) // 右侧表格：该教师的学生
    const allStudentsPool = ref([]) // 全量学生缓存

    const selectedTeacher = ref(null)
    
    const loadUsers = async () => {
      try {
        const users = await getAllUsers()
        teachers.value = users.filter(user => {
          if (user.role && user.role.roleName) {
            return user.role.roleName.toUpperCase() === 'TEACHER'
          }
          return user.roleId === 2
        })

        allStudentsPool.value = users.filter(user => {
          if (user.role && user.role.roleName) {
            return user.role.roleName.toUpperCase() === 'STUDENT'
          }
          return user.roleId === 3
        })

        allStudents.value = [...allStudentsPool.value]
        await nextTick()
        highlightFirstRows()
      } catch (error) {
        ElMessage.error('加载用户列表失败')
      }
    }

    const onTeacherChange = async () => {
      if (selectedTeacher.value) {
        try {
          teacherStudents.value = await getTeacherStudents(selectedTeacher.value)
          
          // 更新左侧表格：显示未分配给该教师的学生
          const teacherStudentIds = teacherStudents.value.map(student => student.id)
          allStudents.value = allStudentsPool.value.filter(student =>
            !teacherStudentIds.includes(student.id)
          )
          
          await nextTick()
          highlightFirstRows()
        } catch (error) {
          ElMessage.error('加载教师学生关系失败')
        }
      } else {
        // 如果没有选择教师，显示所有学生
        allStudents.value = [...allStudentsPool.value]
        teacherStudents.value = []
      }
    }

    const highlightFirstRows = () => {
      const leftFirstRow = document.querySelector('#allStudentsTable tbody tr')
      const rightFirstRow = document.querySelector('#teacherStudentsTable tbody tr')

      if (leftFirstRow) {
        highlightRow({ currentTarget: leftFirstRow }, 'allStudentsTable')
      }
      if (rightFirstRow) {
        highlightRow({ currentTarget: rightFirstRow }, 'teacherStudentsTable')
      }
    }

    const highlightRow = (event, tableId) => {
      const row = event.currentTarget
      const tbody = document.getElementById(tableId).querySelector('tbody')
      const rows = tbody.querySelectorAll('tr')

      rows.forEach(r => r.classList.remove('highlight'))
      row.classList.add('highlight')
    }

    const transferRight = () => {
      const leftTable = document.getElementById('allStudentsTable')
      const leftTbody = leftTable.querySelector('tbody')
      const highlightedRow = leftTbody.querySelector('tr.highlight')

      if (highlightedRow) {
        const index = highlightedRow.rowIndex - 1
        const student = allStudents.value[index]

        // 检查是否已经在右侧
        const exists = teacherStudents.value.find(s => s.id === student.id)
        if (exists) {
          ElMessage.warning('该学生已经是该教师的学生')
          return
        }

        // 添加到右侧列表
        teacherStudents.value.push(student)
        // 从左侧列表移除
        allStudents.value.splice(index, 1)

        // 自动高亮
        nextTick(() => {
          const leftRows = leftTbody.querySelectorAll('tr')
          if (leftRows.length > 0) {
            highlightRow({ currentTarget: leftRows[0] }, 'allStudentsTable')
          }

          const rightTbody = document.getElementById('teacherStudentsTable').querySelector('tbody')
          const rightRows = rightTbody.querySelectorAll('tr')
          if (rightRows.length > 0) {
            highlightRow({ currentTarget: rightRows[rightRows.length - 1] }, 'teacherStudentsTable')
          }
        })
      } else {
        ElMessage.warning('请先选择要移动的学生')
      }
    }

    const transferLeft = () => {
      const rightTable = document.getElementById('teacherStudentsTable')
      const rightTbody = rightTable.querySelector('tbody')
      const highlightedRow = rightTbody.querySelector('tr.highlight')

      if (highlightedRow) {
        const index = highlightedRow.rowIndex - 1
        const student = teacherStudents.value[index]

        // 添加到左侧列表
        allStudents.value.push(student)
        // 从右侧列表移除
        teacherStudents.value.splice(index, 1)

        // 自动高亮
        nextTick(() => {
          const rightRows = rightTbody.querySelectorAll('tr')
          if (rightRows.length > 0) {
            highlightRow({ currentTarget: rightRows[0] }, 'teacherStudentsTable')
          }

          const leftTbody = document.getElementById('allStudentsTable').querySelector('tbody')
          const leftRows = leftTbody.querySelectorAll('tr')
          if (leftRows.length > 0) {
            highlightRow({ currentTarget: leftRows[leftRows.length - 1] }, 'allStudentsTable')
          }
        })
      } else {
        ElMessage.warning('请先选择要移动的学生')
      }
    }

    const saveTeacherStudentRelation = async () => {
      try {
        const studentIds = teacherStudents.value.map(student => student.id)
        await saveTeacherStudentRelationService(selectedTeacher.value, studentIds)
        
        await ElMessageBox.alert('修改成功', '提示', {
          confirmButtonText: '确定',
          type: 'success'
        })
      } catch (error) {
        await ElMessageBox.alert('错误：' + (error.message || '保存失败'), '错误', {
          confirmButtonText: '确定',
          type: 'error'
        })
      }
    }

    onMounted(() => {
      loadUsers()
    })

    return {
      teachers,
      allStudents,
      teacherStudents,
      selectedTeacher,
      onTeacherChange,
      highlightRow,
      transferRight,
      transferLeft,
      saveTeacherStudentRelation
    }
  }
}
</script>

<style scoped>
.content {
  padding: 20px;
  background-color: #f5f5f5;
  max-width: 1400px;
  width: 100%;
  margin: 0 auto;
  box-sizing: border-box;
}

.page-header {
  margin-bottom: 20px;
}

.page-title {
  color: #333;
  margin: 0;
  font-size: 22px;
  font-weight: 600;
}

.filter-section {
  display: flex;
  margin-bottom: 20px;
  align-items: center;
  gap: 20px;
  flex-wrap: wrap;
  padding: 16px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.filter-item {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  min-width: 240px;
  max-width: 400px;
}

.filter-label {
  font-weight: 500;
  color: #333;
  white-space: nowrap;
  font-size: 14px;
}

.teacher-select {
  flex: 1;
}

:deep(.el-select) {
  width: 100%;
}

@media (max-width: 768px) {
  .filter-section {
    flex-direction: column;
    align-items: stretch;
  }
  
  .filter-item {
    max-width: none;
    margin-bottom: 10px;
  }
  
  .student-container {
    flex-direction: column;
    height: auto;
  }
  
  .student-section {
    max-width: none;
    margin-bottom: 20px;
  }
  
  .button-container {
    flex-direction: row;
    width: 100%;
    margin-top: 20px;
    justify-content: center;
  }
}

.student-container {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 20px;
  height: 420px;
  width: 100%;
  box-sizing: border-box;
}

.student-list {
  width: 100%;
  height: 380px;
  overflow: auto;
  border: 1px solid #e0e0e0;
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
  white-space: nowrap;
}

.student-section {
  position: relative;
  flex: 1;
  min-width: 0;
  max-width: calc(50% - 10px);
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
}

/* 当只有一个表格时的样式 */
.student-container.single-table .student-section {
  max-width: 100%;
}

.sticky-title {
  position: sticky;
  top: 0;
  left: 0;
  margin: 0;
  padding: 16px;
  background: linear-gradient(to bottom, #f8f9fa, #f0f1f3);
  border-bottom: 2px solid #e0e0e0;
  color: #333;
  z-index: 20;
  white-space: nowrap;
  text-align: center;
  font-weight: 600;
  font-size: 16px;
  width: 100%;
  box-sizing: border-box;
}

table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

thead {
  position: sticky;
  top: 0;
  z-index: 10;
}

thead tr {
  background-color: #f8f9fa;
}

thead th {
  padding: 14px 16px;
  text-align: left;
  background-color: #f8f9fa;
  color: #333;
  font-weight: 600;
  border-bottom: 2px solid #e0e0e0;
  font-size: 14px;
}

tbody tr {
  cursor: pointer;
  transition: background-color 0.2s;
}

tbody tr td {
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
  color: #333;
}

tbody tr.highlight {
  background-color: #e3f2fd !important;
  color: #1976d2 !important;
}

tbody tr:hover:not(.highlight) {
  background-color: #fafafa;
}

tbody tr:nth-child(even):not(.highlight) {
  background-color: #f9fafb;
}

.button-container {
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  align-items: center;
  flex-shrink: 0;
  width: 60px;
  margin-top: 140px;
  gap: 12px;
}

.transfer-btn {
  padding: 10px 14px;
  font-size: 18px;
  cursor: pointer;
  background-color: #fff;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  transition: all 0.3s;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  font-weight: 600;
  color: #333;
}

.transfer-btn:hover {
  background-color: #4CAF50;
  color: white;
  border-color: #4CAF50;
  box-shadow: 0 2px 6px rgba(76, 175, 80, 0.3);
  transform: translateY(-1px);
}

.submit-btn-container {
  display: flex;
  justify-content: center;
  width: 100%;
  margin-top: 24px;
}

.submit-btn {
  padding: 12px 32px;
  background-color: #4CAF50;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 15px;
  font-weight: 500;
  transition: all 0.3s;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.submit-btn:hover {
  background-color: #45a049;
  box-shadow: 0 4px 8px rgba(76, 175, 80, 0.3);
  transform: translateY(-1px);
}

.student-list::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

.student-list::-webkit-scrollbar-track {
  background: #f1f1f1;
}

.student-list::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 4px;
}

.student-list::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}
</style> 