<template>
  <div class="content">
    <div class="page-header">
      <h2 class="page-title">班级管理</h2>
      <el-button type="primary" @click="openDialog()" class="add-button">
        <span class="button-icon">+</span> 新增班级
      </el-button>
    </div>

    <el-table :data="classes" stripe style="width: 100%" class="data-table">
      <el-table-column prop="classCode" label="班级代码" />
      <el-table-column prop="stageName" label="阶段" />
      <el-table-column prop="gradeName" label="年级" />
      <el-table-column prop="departmentName" label="院系" />
      <el-table-column label="操作" width="180" align="center">
        <template #default="scope">
          <el-button size="small" @click="openDialog(scope.row)">编辑</el-button>
          <el-button size="small" type="danger" @click="remove(scope.row.classId)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="500px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="阶段" prop="stageId">
          <el-select v-model="form.stageId" placeholder="请选择阶段" @change="loadGrades">
            <el-option v-for="stage in stages" :key="stage.stageId" :label="stage.displayName" :value="stage.stageId" />
          </el-select>
        </el-form-item>
        <el-form-item label="年级" prop="gradeId">
          <el-select v-model="form.gradeId" placeholder="请选择年级">
            <el-option v-for="grade in grades" :key="grade.gradeId" :label="grade.gradeName" :value="grade.gradeId" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="isUniversityStage" label="院系" prop="departmentId">
          <el-select v-model="form.departmentId" placeholder="请选择院系" clearable>
            <el-option v-for="dept in departments" :key="dept.departmentId" :label="dept.departmentName" :value="dept.departmentId" />
          </el-select>
        </el-form-item>
        <el-form-item label="班级代码" prop="classCode">
          <el-input v-model="form.classCode" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, reactive, computed, onMounted } from 'vue';
import { fetchClasses, createClass, updateClass, deleteClass } from '@/common/service/classService';
import { fetchStages } from '@/common/service/educationStageService';
import { fetchGrades } from '@/common/service/gradeService';
import { getDepartments } from '@/common/service/departmentService';

export default {
  name: 'ClassManagement',
  setup() {
    const classes = ref([]);
    const stages = ref([]);
    const grades = ref([]);
    const departments = ref([]);
    const dialogVisible = ref(false);
    const dialogTitle = ref('新增班级');
    const saving = ref(false);
    const formRef = ref(null);
    const form = reactive({
      classId: null,
      stageId: null,
      gradeId: null,
      departmentId: null,
      classCode: ''
    });

    const rules = {
      stageId: [{ required: true, message: '请选择阶段', trigger: 'change' }],
      gradeId: [{ required: true, message: '请选择年级', trigger: 'change' }],
      classCode: [{ required: true, message: '请输入班级代码', trigger: 'blur' }]
    };

    // 判断当前选择的阶段是否为大学
    const isUniversityStage = computed(() => {
      if (!form.stageId || stages.value.length === 0) return false;
      const currentStage = stages.value.find(s => s.stageId === form.stageId);
      if (!currentStage) return false;
      // 检查 stageName 或 displayName 是否包含"大学"或"university"
      const stageName = (currentStage.stageName || '').toLowerCase();
      const displayName = (currentStage.displayName || '').toLowerCase();
      return stageName.includes('university') || stageName.includes('大学') ||
             displayName.includes('university') || displayName.includes('大学');
    });

    const loadClasses = async () => {
      classes.value = await fetchClasses();
    };

    const loadStages = async () => {
      stages.value = await fetchStages();
    };

    const loadDepartments = async () => {
      departments.value = await getDepartments();
    };

    const loadGrades = async (stageId) => {
      if (!stageId) {
        grades.value = [];
        form.gradeId = null;
        return;
      }
      grades.value = await fetchGrades(stageId);
      if (form.gradeId && !grades.value.find(g => g.gradeId === form.gradeId)) {
        form.gradeId = null;
      }
      // 如果阶段不是大学，清空院系选择
      if (!isUniversityStage.value) {
        form.departmentId = null;
      }
    };

    const openDialog = (row) => {
      if (row) {
        dialogTitle.value = '编辑班级';
        Object.assign(form, row);
        loadGrades(row.stageId);
      } else {
        dialogTitle.value = '新增班级';
        Object.assign(form, { classId: null, stageId: null, gradeId: null, departmentId: null, classCode: '' });
        grades.value = [];
      }
      dialogVisible.value = true;
    };

    const submit = async () => {
      await formRef.value.validate();
      saving.value = true;
      try {
        // 如果不是大学阶段，清空院系ID
        const submitData = { ...form };
        if (!isUniversityStage.value) {
          submitData.departmentId = null;
        }
        if (form.classId) {
          await updateClass(submitData);
        } else {
          await createClass(submitData);
        }
        dialogVisible.value = false;
        await loadClasses();
      } finally {
        saving.value = false;
      }
    };

    const remove = async (id) => {
      await deleteClass(id);
      await loadClasses();
    };

    onMounted(async () => {
      await Promise.all([loadStages(), loadClasses(), loadDepartments()]);
    });

    return {
      classes,
      stages,
      grades,
      departments,
      dialogVisible,
      dialogTitle,
      form,
      formRef,
      rules,
      saving,
      isUniversityStage,
      openDialog,
      submit,
      remove,
      loadGrades
    };
  }
};
</script>

<style scoped>
.content {
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
