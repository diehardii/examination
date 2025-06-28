<template>
  <div class="subject-management">
    <div class="header">
      <h2>科目管理</h2>
      <button class="add-btn" @click="openAddModal">+ 添加科目</button>
    </div>

    <div class="table-container">
      <table>
        <thead>
        <tr>
          <th>科目名称</th>
          <th>操作</th>
        </tr>
        </thead>
        <tbody>
        <tr v-for="subject in subjects" :key="subject.subjectId">
          <td>{{ subject.subjectName }}</td>
          <td class="action-buttons">
            <button class="edit-btn" @click="openUpdateModal(subject)">修改</button>
            <button class="delete-btn" @click="delSubject(subject)">删除</button>
          </td>
        </tr>
        <tr v-if="subjects.length === 0">
          <td colspan="2" class="no-data">暂无科目数据</td>
        </tr>
        </tbody>
      </table>
    </div>

    <!-- 添加模态框 -->
    <el-dialog v-model="addModalVisible" title="添加科目" width="30%">
      <el-form :model="addForm">
        <el-form-item label="科目名称">
          <el-input v-model="addForm.subjectName" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addModalVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAdd">确认添加</el-button>
      </template>
    </el-dialog>

    <!-- 修改模态框 -->
    <el-dialog v-model="updateModalVisible" title="修改科目" width="30%">
      <el-form :model="updateForm">
        <el-form-item label="科目名称">
          <el-input v-model="updateForm.subjectName" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="updateModalVisible = false">取消</el-button>
        <el-button type="primary" @click="handleUpdate">确认修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { getSubjects, addSubject, updateSubject, deleteSubject } from '@/service/subjectService'
import { ElMessage, ElMessageBox } from 'element-plus'

export default {
  setup() {
    const subjects = ref([])
    const addModalVisible = ref(false)
    const updateModalVisible = ref(false)

    const addForm = ref({
      subjectName: ''
    })

    const updateForm = ref({
      subjectId: null,
      subjectName: '',
      oldSubjectName: '',
    })

    const loadSubjects = async () => {
      try {
        subjects.value = await getSubjects()
      } catch (error) {
        ElMessage.error('获取科目列表失败')
      }
    }

    const openAddModal = () => {
      addForm.value = { subjectName: '' }
      addModalVisible.value = true
    }

    const openUpdateModal = (subject) => {
      updateForm.value = {
        subjectId: subject.subjectId,
        oldSubjectName: subject.subjectName,
        subjectName: subject.subjectName
      }
      updateModalVisible.value = true
    }

    const handleAdd = async () => {
      try {
        await addSubject(addForm.value)
        addModalVisible.value = false
        ElMessage.success('添加成功')
        await loadSubjects()
      } catch (error) {
        ElMessage.error('添加失败')
      }
    }

    const handleUpdate = async () => {
      try {
        await updateSubject(updateForm.value.oldSubjectName,updateForm.value.subjectName)
        updateModalVisible.value = false
        ElMessage.success('更新成功')
        await loadSubjects()
      } catch (error) {
        ElMessage.error('更新失败')
      }
    }

    const delSubject = async (subject) => {
      try {
        await ElMessageBox.confirm(`确定删除科目 "${subject.subjectName}" 吗？`, '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })

        await deleteSubject(subject.subjectName)
        ElMessage.success('删除成功')
        await loadSubjects()
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error('删除失败')
        }
      }
    }

    onMounted(loadSubjects)

    return {
      subjects,
      addModalVisible,
      updateModalVisible,
      addForm,
      updateForm,
      openAddModal,
      openUpdateModal,
      handleAdd,
      handleUpdate,
      delSubject
    }
  }
}
</script>

<style scoped>
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.add-btn {
  background-color: #4CAF50;
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 4px;
  cursor: pointer;
}

.table-container {
  width: 100%;
  border: 1px solid #ddd;
  border-radius: 4px;
  overflow: hidden;
}

table {
  width: 100%;
  border-collapse: collapse;
}

th {
  background-color: #e0e0e0;
  padding: 12px 15px;
  text-align: left;
}

td {
  padding: 12px 15px;
  border-bottom: 1px solid #eee;
}

tr:nth-child(even) {
  background-color: #f9f9f9;
}

.action-buttons {
  display: flex;
  gap: 8px;
}

.edit-btn, .delete-btn {
  padding: 6px 12px;
  border: none;
  border-radius: 3px;
  cursor: pointer;
}

.edit-btn {
  background-color: #1890ff;
  color: white;
}

.delete-btn {
  background-color: #ff4d4f;
  color: white;
}

.no-data {
  text-align: center;
  padding: 20px;
  color: #999;
}
</style>