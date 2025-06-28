<template>
  <div class="department-management">
    <div class="header">
      <h2>院系管理</h2>
      <div class="controls">
        <el-select v-model="selectedUniversity" placeholder="请选择大学" @change="loadDepartments">
          <el-option
              v-for="university in universities"
              :key="university.universityId"
              :label="university.universityName"
              :value="university.universityId"
          />
        </el-select>
        <el-button type="primary" @click="openAddModal" :disabled="!selectedUniversity" style="background-color:#5daf34">
          + 添加院系
        </el-button>
      </div>
    </div>

    <el-table
        :data="departments"
        style="width: 100%"
        v-if="selectedUniversity"
    >
      <el-table-column prop="departmentName" label="院系名称" />
      <el-table-column label="操作" width="200">
        <template #default="scope">
          <el-button size="small" @click="openEditModal(scope.row)">编辑</el-button>
          <el-button size="small" type="danger" @click="delDepartment(scope.row.departmentId)">
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 未选择大学时的提示 -->
    <div v-else class="no-university-tip">
      <el-empty description=" " />
      <div class="custom-tip-text">请先选择您所在的高校</div>
    </div>

    <!-- 添加/编辑模态框 -->
    <el-dialog v-model="modalVisible" :title="modalTitle" width="30%">
      <el-form :model="currentDepartment">
        <el-form-item label="院系名称">
          <el-input v-model="currentDepartment.departmentName" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="modalVisible = false">取消</el-button>
        <el-button type="primary" @click="saveDepartment">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { ElMessage, ElEmpty } from 'element-plus'
import { getUniversities } from '@/service/universityService'
import { getDepartmentsByUniversity, createDepartment, updateDepartment, deleteDepartment } from '@/service/departmentService'

export default {
  setup() {
    const universities = ref([])
    const departments = ref([])
    const selectedUniversity = ref(null)
    const modalVisible = ref(false)
    const isEditing = ref(false)
    const currentDepartment = ref({
      departmentId: null,
      departmentName: '',
      universityId: null
    })

    const modalTitle = ref('添加院系')

    const loadUniversities = async () => {
      try {
        universities.value = await getUniversities()
      } catch (error) {
        ElMessage.error('获取大学数据失败')
      }
    }

    const loadDepartments = async () => {
      if (!selectedUniversity.value) return

      try {
        departments.value = await getDepartmentsByUniversity(selectedUniversity.value)
      } catch (error) {
        ElMessage.error('获取院系数据失败')
      }
    }

    const openAddModal = () => {
      isEditing.value = false
      currentDepartment.value = {
        departmentId: null,
        departmentName: '',
        universityId: selectedUniversity.value
      }
      modalTitle.value = '添加院系'
      modalVisible.value = true
    }

    const openEditModal = (department) => {
      isEditing.value = true
      currentDepartment.value = { ...department }
      modalTitle.value = '编辑院系'
      modalVisible.value = true
    }

    const saveDepartment = async () => {
      try {
        if (isEditing.value) {
          await updateDepartment(currentDepartment.value.departmentId,currentDepartment.value)
          ElMessage.success('院系更新成功')
        } else {
          await createDepartment(currentDepartment.value)
          ElMessage.success('院系添加成功')
        }
        modalVisible.value = false
        await loadDepartments()
      } catch (error) {
        ElMessage.error('操作失败')
      }
    }

    const delDepartment = async (id) => {
      try {
        await deleteDepartment(id)
        ElMessage.success('删除成功')
        await loadDepartments()
      } catch (error) {
        ElMessage.error('删除失败')
      }
    }

    onMounted(loadUniversities)

    return {
      universities,
      departments,
      selectedUniversity,
      modalVisible,
      currentDepartment,
      modalTitle,
      loadDepartments,
      openAddModal,
      openEditModal,
      saveDepartment,
      delDepartment
    }
  }
}
</script>

<style scoped>
.department-management {
  padding: 20px;
}

.header {
  margin-bottom: 20px;
}

.header h2 {
  font-size: 22px;
  color: #333;
}

.controls {
  display: flex;
  gap: 10px;
  margin-top: 15px;
}

.el-select {
  flex: 1;
}

/* 未选择大学提示样式 */
.no-university-tip {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  height: 400px; /* 高度为原来的2倍 */
  background-color: #f5f7fa;
  border-radius: 8px;
  margin-top: 20px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.custom-tip-text {
  font-size: 22px; /* 加大字体 */
  font-weight: 600; /* 加粗 */
  color: #333; /* 黑色 */
  margin-top: -40px;
  text-align: center;
}

/* 表格样式 */
.el-table {
  margin-top: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

/* 按钮样式 */
.el-button--primary {
  background-color: #5daf34;
  border-color: #5daf34;
}

.el-button--primary:hover {
  background-color: #4d9a2b;
  border-color: #4d9a2b;
}

/* 对话框样式 */
.el-dialog {
  border-radius: 8px;
}

.el-form-item__label {
  font-weight: 500;
}
</style>