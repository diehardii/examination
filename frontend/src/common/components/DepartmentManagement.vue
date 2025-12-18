<template>
  <div class="department-management">
    <div class="page-header">
      <h2 class="page-title">院系管理</h2>
      <div class="controls">
        <el-select v-model="selectedUniversity" placeholder="请选择大学" @change="loadDepartments" class="university-select">
          <el-option
              v-for="university in universities"
              :key="university.universityId"
              :label="university.universityName"
              :value="university.universityId"
          />
        </el-select>
        <el-button type="primary" @click="openAddModal" :disabled="!selectedUniversity" class="add-button">
          <span class="button-icon">+</span> 添加院系
        </el-button>
      </div>
    </div>

    <el-table
        :data="departments"
        style="width: 100%"
        v-if="selectedUniversity"
        stripe
        class="data-table"
    >
      <el-table-column prop="departmentName" label="院系名称" />
      <el-table-column label="操作" width="200" align="center">
        <template #default="scope">
          <el-button size="small" @click="openEditModal(scope.row)">编辑</el-button>
          <el-button size="small" type="danger" @click="delDepartment(scope.row.departmentId)">
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 未选择大学时的提示 -->
    <div v-else class="empty-container">
      <el-empty description=" " />
      <div class="empty-text">请先选择您所在的高校</div>
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
import { getUniversities } from '@/common/service/universityService'
import { getDepartmentsByUniversity, createDepartment, updateDepartment, deleteDepartment } from '@/common/service/departmentService'

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
  background: #f5f5f5;
  min-height: 100%;
}

.page-header {
  margin-bottom: 20px;
}

.page-title {
  margin: 0 0 15px 0;
  font-size: 22px;
  color: #333;
  font-weight: 600;
}

.controls {
  display: flex;
  gap: 12px;
  align-items: center;
}

.university-select {
  flex: 1;
  max-width: 300px;
}

.add-button {
  background-color: #4CAF50;
  border-color: #4CAF50;
  font-size: 14px;
  padding: 10px 20px;
  border-radius: 4px;
  transition: all 0.3s;
}

.add-button:hover:not(:disabled) {
  background-color: #45a049;
  border-color: #45a049;
}

.button-icon {
  font-size: 16px;
  font-weight: bold;
  margin-right: 4px;
}

/* 未选择大学提示样式 */
.empty-container {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  height: 400px;
  background-color: #ffffff;
  border-radius: 8px;
  margin-top: 20px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.empty-text {
  font-size: 18px;
  font-weight: 500;
  color: #666;
  margin-top: -40px;
  text-align: center;
}

/* 表格样式 */
.data-table {
  margin-top: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  background-color: #fff;
}

:deep(.el-button--small) {
  padding: 5px 12px;
  font-size: 13px;
}

:deep(.el-table th) {
  background-color: #f8f9fa;
  color: #333;
  font-weight: 600;
}

:deep(.el-table td) {
  color: #333;
}

:deep(.el-dialog) {
  border-radius: 8px;
}

:deep(.el-form-item__label) {
  font-weight: 500;
  color: #333;
}

:deep(.el-select) {
  width: 100%;
}
</style>