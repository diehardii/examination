<template>
  <div class="content">
    <div class="page-header">
      <h2 class="page-title">老师管理</h2>
    </div>

    <el-table :data="teachers" stripe style="width: 100%" class="data-table">
      <el-table-column prop="realName" label="姓名" width="120" />
      <el-table-column prop="username" label="用户名" width="150" />
      <el-table-column prop="phone" label="电话" width="130" />
      <el-table-column label="已配置学段" min-width="200">
        <template #default="scope">
          <div v-if="scope.row.stages && scope.row.stages.length > 0" class="stages-container">
            <el-tag
              v-for="stage in scope.row.stages"
              :key="stage.stageId"
              class="stage-tag"
              type="info"
            >
              {{ stage.stageName }} ({{ stage.classCount }}个班级)
            </el-tag>
          </div>
          <span v-else class="empty-text">未配置</span>
        </template>
      </el-table-column>
      <el-table-column width="120" label="操作" align="center" fixed="right">
        <template #default="scope">
          <el-button size="small" type="primary" @click="openConfigDialog(scope.row)">
            配置学段班级
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 配置学段和班级的对话框 -->
    <el-dialog 
      title="配置学段和班级" 
      v-model="configDialogVisible" 
      width="800px"
      :close-on-click-modal="false"
    >
      <div class="config-container">
        <div class="teacher-info">
          <strong>教师：</strong>{{ currentTeacher?.realName }} ({{ currentTeacher?.username }})
        </div>

        <!-- 学段选择区域 -->
        <div class="stage-selector">
          <el-button
            v-for="stage in allStages"
            :key="stage.stageId"
            :type="selectedStages.includes(stage.stageId) ? 'primary' : ''"
            :class="['stage-btn', { active: selectedStages.includes(stage.stageId) }]"
            @click="toggleStage(stage.stageId)"
          >
            {{ stage.displayName }}
            <span v-if="selectedStages.includes(stage.stageId)" class="check-icon">✓</span>
          </el-button>
        </div>

        <!-- 各学段的班级配置 -->
        <div class="stages-config">
          <div
            v-for="stageId in selectedStages"
            :key="stageId"
            class="stage-section"
          >
            <div class="stage-header">
              <span class="stage-title">{{ getStageName(stageId) }}</span>
              <el-button size="small" link type="danger" @click="removeStage(stageId)">
                删除此学段
              </el-button>
            </div>
            <el-select
              v-model="stageClassesMap[stageId]"
              multiple
              placeholder="请选择班级"
              class="class-select"
              :loading="loadingClasses[stageId]"
            >
              <el-option
                v-for="cls in classesMap[stageId] || []"
                :key="cls.classId"
                :label="`${cls.classCode} - ${cls.gradeName}`"
                :value="cls.classId"
              />
            </el-select>
          </div>
        </div>

        <div v-if="selectedStages.length === 0" class="empty-hint">
          请先选择要配置的学段
        </div>
      </div>

      <template #footer>
        <el-button @click="configDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveConfiguration">
          保存配置
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { 
  fetchTeachers, 
  fetchTeacherClassesByStage, 
  assignTeacherClassesByStage,
  fetchTeacherAllAssignments
} from '@/common/service/teacherService';
import { fetchStages } from '@/common/service/educationStageService';
import { fetchClassesByStage } from '@/common/service/classService';

export default {
  name: 'TeacherManagement',
  setup() {
    const teachers = ref([]);
    const allStages = ref([]);
    const configDialogVisible = ref(false);
    const saving = ref(false);
    const currentTeacher = ref(null);
    
    const selectedStages = ref([]);
    const stageClassesMap = reactive({});
    const classesMap = reactive({});
    const loadingClasses = reactive({});

    const loadTeachers = async () => {
      teachers.value = await fetchTeachers();
    };

    const loadStages = async () => {
      allStages.value = await fetchStages();
    };

    const getStageName = (stageId) => {
      const stage = allStages.value.find(s => s.stageId === stageId);
      return stage ? stage.displayName : '';
    };

    const openConfigDialog = async (teacher) => {
      currentTeacher.value = teacher;
      configDialogVisible.value = true;
      
      // 重置状态
      selectedStages.value = [];
      Object.keys(stageClassesMap).forEach(key => delete stageClassesMap[key]);
      Object.keys(classesMap).forEach(key => delete classesMap[key]);
      
      // 加载教师现有的配置
      try {
        const assignments = await fetchTeacherAllAssignments(teacher.id);
        
        // assignments 是 { stageId: [classId1, classId2, ...] } 格式
        for (const [stageId, classIds] of Object.entries(assignments)) {
          const sid = parseInt(stageId);
          selectedStages.value.push(sid);
          stageClassesMap[sid] = classIds;
          
          // 加载该学段的所有班级选项
          await loadStageClasses(sid);
        }
      } catch (error) {
        console.error('加载教师配置失败:', error);
      }
    };

    const toggleStage = async (stageId) => {
      const index = selectedStages.value.indexOf(stageId);
      if (index > -1) {
        // 取消选择该学段
        selectedStages.value.splice(index, 1);
        delete stageClassesMap[stageId];
      } else {
        // 选择该学段
        selectedStages.value.push(stageId);
        stageClassesMap[stageId] = [];
        
        // 加载该学段的班级列表
        await loadStageClasses(stageId);
      }
    };

    const removeStage = (stageId) => {
      const index = selectedStages.value.indexOf(stageId);
      if (index > -1) {
        selectedStages.value.splice(index, 1);
        delete stageClassesMap[stageId];
      }
    };

    const loadStageClasses = async (stageId) => {
      if (classesMap[stageId]) return; // 已加载过
      
      loadingClasses[stageId] = true;
      try {
        const classes = await fetchClassesByStage(stageId);
        classesMap[stageId] = classes;
      } catch (error) {
        ElMessage.error(`加载学段班级失败: ${error.message}`);
      } finally {
        loadingClasses[stageId] = false;
      }
    };

    const saveConfiguration = async () => {
      if (!currentTeacher.value) return;
      
      saving.value = true;
      try {
        // 为每个选中的学段保存班级配置
        const promises = selectedStages.value.map(stageId => {
          const classIds = stageClassesMap[stageId] || [];
          return assignTeacherClassesByStage(
            currentTeacher.value.id,
            stageId,
            classIds
          );
        });
        
        await Promise.all(promises);
        
        ElMessage.success('配置保存成功');
        configDialogVisible.value = false;
        await loadTeachers();
      } catch (error) {
        ElMessage.error(`保存失败: ${error.message || '未知错误'}`);
      } finally {
        saving.value = false;
      }
    };

    onMounted(async () => {
      await Promise.all([loadTeachers(), loadStages()]);
    });

    return {
      teachers,
      allStages,
      configDialogVisible,
      saving,
      currentTeacher,
      selectedStages,
      stageClassesMap,
      classesMap,
      loadingClasses,
      getStageName,
      openConfigDialog,
      toggleStage,
      removeStage,
      saveConfiguration,
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

.stages-container {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.stage-tag {
  font-size: 13px;
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

/* 对话框内容样式 */
.config-container {
  padding: 10px 0;
}

.teacher-info {
  padding: 12px;
  background-color: #f5f7fa;
  border-radius: 4px;
  margin-bottom: 20px;
  font-size: 14px;
}

.stage-selector {
  margin-bottom: 24px;
  padding: 16px;
  background-color: #fafafa;
  border-radius: 8px;
}

.stage-selector::before {
  content: '选择学段：';
  display: block;
  font-weight: 600;
  color: #333;
  margin-bottom: 12px;
}

.stage-btn {
  margin: 0 8px 8px 0;
  position: relative;
  padding-right: 32px;
}

.stage-btn.active {
  background-color: #409EFF;
  border-color: #409EFF;
  color: white;
}

.check-icon {
  position: absolute;
  right: 8px;
  font-weight: bold;
}

.stages-config {
  max-height: 400px;
  overflow-y: auto;
}

.stage-section {
  margin-bottom: 20px;
  padding: 16px;
  background-color: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
}

.stage-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid #e4e7ed;
}

.stage-title {
  font-size: 15px;
  font-weight: 600;
  color: #333;
}

.class-select {
  width: 100%;
}

.empty-hint {
  text-align: center;
  padding: 40px;
  color: #909399;
  font-size: 14px;
}

:deep(.el-dialog) {
  border-radius: 8px;
}

:deep(.el-dialog__body) {
  padding: 20px 20px;
}
</style>
