<template>
  <div class="university-management">
    <div class="page-header">
      <h2 class="page-title">大学管理</h2>
      <el-button type="primary" @click="openAddModal" class="add-button">
        <span class="button-icon">+</span> 添加大学
      </el-button>
    </div>

    <el-table :data="universities" style="width: 100%" stripe class="data-table">
      <el-table-column prop="universityName" label="大学名称" />
      <el-table-column label="操作" width="200" align="center">
        <template #default="scope">
          <el-button size="small" @click="openEditModal(scope.row)">编辑</el-button>
          <el-button size="small" type="danger" @click="delUniversity(scope.row.universityId)">
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 添加/编辑模态框 -->
    <el-dialog v-model="modalVisible" :title="modalTitle" width="30%">
      <el-form :model="currentUniversity">
        <el-form-item label="大学名称">
          <el-input v-model="currentUniversity.universityName" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="modalVisible = false">取消</el-button>
        <el-button type="primary" @click="saveUniversity">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUniversities, createUniversity, updateUniversity, deleteUniversity } from '@/common/service/universityService'

export default {
  setup() {
    const universities = ref([])
    const modalVisible = ref(false)
    const isEditing = ref(false)
    const currentUniversity = ref({
      universityId: null,
      universityName: ''
    })

    const modalTitle = ref('添加大学')

    const fetchData = async () => {
      try {
        universities.value = await getUniversities()
      } catch (error) {
        ElMessage.error('获取大学数据失败')
      }
    }

    const openAddModal = () => {
      isEditing.value = false
      currentUniversity.value = { universityId: null, universityName: '' }
      modalTitle.value = '添加大学'
      modalVisible.value = true
    }

    const openEditModal = (university) => {
      isEditing.value = true
      currentUniversity.value = { ...university }
      modalTitle.value = '编辑大学'
      modalVisible.value = true
    }

    const saveUniversity = async () => {
      try {
        if (isEditing.value) {
          console.log('before'+currentUniversity.value.universityName+currentUniversity.value.universityId)
          await  updateUniversity(currentUniversity.value.universityId, currentUniversity.value)
          ElMessage.success('大学更新成功')
        } else {
          await createUniversity(currentUniversity.value)
          ElMessage.success('大学添加成功')
        }
        modalVisible.value = false
        await fetchData()
      } catch (error) {
        ElMessage.error('操作失败')
      }
    }

    const delUniversity = async (id) => {
      try {
        await ElMessageBox.confirm('确定删除该大学及其所有院系吗?', '警告', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })

        await deleteUniversity(id)
        ElMessage.success('删除成功')
        await fetchData()
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error('删除失败')
        }
      }
    }

    onMounted(fetchData)

    return {
      universities,
      modalVisible,
      currentUniversity,
      modalTitle,
      openAddModal,
      openEditModal,
      saveUniversity,
      delUniversity
    }
  }
}
</script>

<style scoped>
.university-management {
  padding: 20px;
  background: #f5f5f5;
  min-height: 100%;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-title {
  margin: 0;
  font-size: 22px;
  color: #333;
  font-weight: 600;
}

.add-button {
  background-color: #4CAF50;
  border-color: #4CAF50;
  font-size: 14px;
  padding: 10px 20px;
  border-radius: 4px;
  transition: all 0.3s;
}

.add-button:hover {
  background-color: #45a049;
  border-color: #45a049;
}

.button-icon {
  font-size: 16px;
  font-weight: bold;
  margin-right: 4px;
}

.data-table {
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
</style>