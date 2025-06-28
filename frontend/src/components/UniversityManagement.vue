<template>
  <div class="university-management">
    <div class="header">
      <h2>大学管理</h2>
      <button class="add-btn" @click="openAddModal">+ 添加大学</button>
    </div>

    <el-table :data="universities" style="width: 100%">
      <el-table-column prop="universityName" label="大学名称" />
      <el-table-column label="操作" width="200">
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
import { getUniversities, createUniversity, updateUniversity, deleteUniversity } from '@/service/universityService'

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
</style>