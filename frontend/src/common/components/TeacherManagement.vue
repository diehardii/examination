<template>
  <div class="content">
    <div class="page-header">
      <h2 class="page-title">老师管理</h2>
    </div>

    <el-table :data="teachers" stripe style="width: 100%" class="data-table" @row-click="onSelectTeacher">
      <el-table-column prop="realName" label="姓名" />
      <el-table-column prop="username" label="用户名" />
      <el-table-column prop="phone" label="电话" />
      <el-table-column prop="stageName" label="已配置学段">
        <template #default="scope">
          <span v-if="scope.row.stageName" class="stage-badge">{{ scope.row.stageName }}</span>
          <span v-else class="empty-text">未配置</span>
        </template>
      </el-table-column>
      <el-table-column width="200" label="操作" align="center">
        <template #default="scope">
          <el-button size="small" @click.stop="openStageDialog(scope.row)">配置学段</el-button>
          <el-button size="small" @click.stop="openClassDialog(scope.row)">配置班级</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog title="配置学段" v-model="stageDialogVisible" width="400px">
      <el-form label-width="100px">
        <el-form-item label="学段">
          <el-select v-model="stageForm.stageId" placeholder="请选择学段">
            <el-option v-for="stage in stages" :key="stage.stageId" :label="stage.displayName" :value="stage.stageId" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="stageDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingStage" @click="saveStage">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog title="配置班级" v-model="classDialogVisible" width="520px">
      <el-form label-width="120px">
        <el-form-item label="学段">
          <el-select v-model="classForm.stageId" placeholder="请选择学段" @change="loadClasses">
            <el-option v-for="stage in stages" :key="stage.stageId" :label="stage.displayName" :value="stage.stageId" />
          </el-select>
        </el-form-item>
        <el-form-item label="班级">
          <el-select v-model="classForm.classIds" multiple placeholder="请选择班级" :disabled="!classForm.stageId">
            <el-option v-for="c in stageClasses" :key="c.classId" :label="c.classCode" :value="c.classId" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="classDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingClasses" @click="saveClasses">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { fetchTeachers, assignTeacherStage, fetchTeacherClasses, assignTeacherClasses } from '@/common/service/teacherService';
import { fetchStages } from '@/common/service/educationStageService';
import { fetchClassesByStage } from '@/common/service/classService';

export default {
  name: 'TeacherManagement',
  setup() {
    const teachers = ref([]);
    const stages = ref([]);
    const stageDialogVisible = ref(false);
    const classDialogVisible = ref(false);
    const savingStage = ref(false);
    const savingClasses = ref(false);

    const currentTeacher = ref(null);
    const stageForm = reactive({ stageId: null });
    const classForm = reactive({ stageId: null, classIds: [] });
    const stageClasses = ref([]);

    const loadTeachers = async () => {
      teachers.value = await fetchTeachers();
    };

    const loadStages = async () => {
      stages.value = await fetchStages();
    };

    const onSelectTeacher = async (row) => {
      currentTeacher.value = row;
    };

    const openStageDialog = (row) => {
      currentTeacher.value = row;
      stageForm.stageId = row.stageId || null;
      stageDialogVisible.value = true;
    };

    const openClassDialog = async (row) => {
      currentTeacher.value = row;
      classForm.stageId = row.stageId || null;
      classForm.classIds = [];
      stageClasses.value = [];
      if (classForm.stageId) {
        await loadClasses(classForm.stageId);
        classForm.classIds = await fetchTeacherClasses(row.id);
      }
      classDialogVisible.value = true;
    };

    const saveStage = async () => {
      if (!stageForm.stageId) {
        ElMessage.warning('请选择学段');
        return;
      }
      savingStage.value = true;
      try {
        await assignTeacherStage(currentTeacher.value.id, stageForm.stageId);
        stageDialogVisible.value = false;
        await loadTeachers();
      } finally {
        savingStage.value = false;
      }
    };

    const loadClasses = async (stageId) => {
      if (!stageId) {
        stageClasses.value = [];
        classForm.classIds = [];
        return;
      }
      stageClasses.value = await fetchClassesByStage(stageId);
      const currentIds = await fetchTeacherClasses(currentTeacher.value.id);
      classForm.classIds = currentIds.filter(id => stageClasses.value.some(c => c.classId === id));
    };

    const saveClasses = async () => {
      if (!classForm.stageId) {
        ElMessage.warning('请先选择学段');
        return;
      }
      savingClasses.value = true;
      try {
        // Ensure backend knows the chosen stage before assigning classes
        if (currentTeacher.value.stageId !== classForm.stageId) {
          await assignTeacherStage(currentTeacher.value.id, classForm.stageId);
        }
        await assignTeacherClasses(currentTeacher.value.id, classForm.classIds);
        classDialogVisible.value = false;
        await loadTeachers();
      } finally {
        savingClasses.value = false;
      }
    };

    onMounted(async () => {
      await Promise.all([loadTeachers(), loadStages()]);
    });

    return {
      teachers,
      stages,
      stageDialogVisible,
      classDialogVisible,
      stageForm,
      classForm,
      stageClasses,
      savingStage,
      savingClasses,
      onSelectTeacher,
      openStageDialog,
      openClassDialog,
      saveStage,
      saveClasses,
      loadClasses,
    };
  }
};
</script>

<style scoped>
.content {
  padding: 20px;
  background-color: #f5f5f5;
  min-height: 100%;
}

.page-header {
  margin-bottom: 20px;
}

.page-title {
  margin: 0;
  font-size: 22px;
  color: #333;
  font-weight: 600;
}

.data-table {
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  background-color: #fff;
}

.stage-badge {
  display: inline-block;
  padding: 4px 12px;
  background-color: #e3f2fd;
  color: #1976d2;
  border-radius: 12px;
  font-size: 13px;
  font-weight: 500;
}

.empty-text {
  color: #999;
  font-style: italic;
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

:deep(.el-table__row) {
  cursor: pointer;
  transition: background-color 0.2s;
}

:deep(.el-dialog) {
  border-radius: 8px;
}

:deep(.el-form-item__label) {
  font-weight: 500;
  color: #333;
}
</style>
