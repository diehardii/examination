<template>
  <div class="student-class-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2>学生班级关系维护</h2>
    </div>

    <!-- 主内容区 -->
    <div class="content-container">
      <!-- 左侧：学生列表 -->
      <div class="left-panel">
        <div class="panel-header">
          <h3>学生列表</h3>
          <div class="filter-controls">
            <el-radio-group v-model="studentFilter" @change="loadStudents">
              <el-radio-button value="all">全部学生</el-radio-button>
              <el-radio-button value="unassigned">未分配班级</el-radio-button>
            </el-radio-group>
          </div>
        </div>
        
        <div class="panel-body">
          <el-table 
            :data="students" 
            height="500"
            highlight-current-row
            @current-change="handleStudentSelect"
            v-loading="studentLoading">
            <el-table-column type="index" label="序号" width="60" />
            <el-table-column prop="studentNumber" label="学号" width="120" />
            <el-table-column prop="studentName" label="姓名" width="100" />
            <el-table-column prop="stageName" label="学段" width="80" />
            <el-table-column prop="gradeName" label="年级" width="80" />
            <el-table-column prop="classCode" label="当前班级" min-width="100">
              <template #default="scope">
                <el-tag v-if="scope.row.classCode" type="success" size="small">
                  {{ scope.row.classCode }}
                </el-tag>
                <el-tag v-else type="info" size="small">未分配</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100" fixed="right">
              <template #default="scope">
                <el-button 
                  v-if="scope.row.classId"
                  type="danger" 
                  size="small"
                  text
                  @click="removeStudentClass(scope.row)">
                  移除班级
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>

      <!-- 右侧：班级列表和学生分配 -->
      <div class="right-panel">
        <div class="panel-header">
          <h3>班级管理</h3>
        </div>

        <!-- 班级选择 -->
        <div class="class-selector">
          <el-select 
            v-model="selectedClassId" 
            placeholder="选择班级查看学生"
            @change="loadClassStudents"
            clearable
            filterable>
            <el-option
              v-for="cls in classes"
              :key="cls.classId"
              :label="`${cls.classCode} (${cls.studentCount}人)`"
              :value="cls.classId">
              <span style="float: left">{{ cls.classCode }}</span>
              <span style="float: right; color: var(--el-text-color-secondary); font-size: 13px">
                {{ cls.studentCount }}人
              </span>
            </el-option>
          </el-select>

          <el-button 
            type="primary"
            :disabled="!selectedStudent || !selectedClassId"
            @click="assignStudentToClass">
            分配选中学生到此班级
          </el-button>
        </div>

        <!-- 班级学生列表 -->
        <div class="panel-body" v-if="selectedClassId">
          <div class="class-info">
            <h4>{{ currentClassInfo.classCode }} - 学生列表</h4>
            <el-tag type="primary">
              共 {{ currentClassInfo.studentCount }} 名学生
            </el-tag>
          </div>

          <el-table 
            :data="classStudents" 
            height="380"
            v-loading="classLoading">
            <el-table-column type="index" label="序号" width="60" />
            <el-table-column prop="studentNumber" label="学号" width="120" />
            <el-table-column prop="studentName" label="姓名" width="100" />
            <el-table-column prop="username" label="用户名" width="120" />
            <el-table-column prop="email" label="邮箱" min-width="180" />
            <el-table-column label="操作" width="100" fixed="right">
              <template #default="scope">
                <el-button 
                  type="danger" 
                  size="small"
                  text
                  @click="removeStudentFromCurrentClass(scope.row)">
                  移除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <!-- 未选择班级提示 -->
        <div v-else class="empty-container">
          <el-empty description="请选择班级查看学生列表" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import studentClassService from '@/common/services/studentClassService';

// 数据
const students = ref([]);
const classes = ref([]);
const classStudents = ref([]);
const studentFilter = ref('all');
const selectedClassId = ref(null);
const selectedStudent = ref(null);
const currentClassInfo = ref({});
const studentLoading = ref(false);
const classLoading = ref(false);

// 加载学生列表
const loadStudents = async () => {
  studentLoading.value = true;
  try {
    if (studentFilter.value === 'all') {
      students.value = await studentClassService.getAllStudents();
    } else {
      students.value = await studentClassService.getUnassignedStudents();
    }
  } catch (error) {
    ElMessage.error('加载学生列表失败');
  } finally {
    studentLoading.value = false;
  }
};

// 加载班级列表
const loadClasses = async () => {
  try {
    classes.value = await studentClassService.getAllClassesWithStudentCount();
  } catch (error) {
    ElMessage.error('加载班级列表失败');
  }
};

// 加载班级学生
const loadClassStudents = async () => {
  if (!selectedClassId.value) {
    classStudents.value = [];
    currentClassInfo.value = {};
    return;
  }

  classLoading.value = true;
  try {
    const result = await studentClassService.getClassStudents(selectedClassId.value);
    currentClassInfo.value = result;
    classStudents.value = result.students || [];
  } catch (error) {
    ElMessage.error('加载班级学生失败');
  } finally {
    classLoading.value = false;
  }
};

// 处理学生选择
const handleStudentSelect = (row) => {
  selectedStudent.value = row;
};

// 分配学生到班级
const assignStudentToClass = async () => {
  if (!selectedStudent.value || !selectedClassId.value) {
    return;
  }

  try {
    await ElMessageBox.confirm(
      `确定将学生 ${selectedStudent.value.studentName} 分配到所选班级吗？`,
      '确认分配',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    );

    await studentClassService.assignClassToStudent(
      selectedStudent.value.studentId,
      selectedClassId.value
    );

    ElMessage.success('分配成功');
    await loadStudents();
    await loadClasses();
    await loadClassStudents();
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('分配失败');
    }
  }
};

// 移除学生班级
const removeStudentClass = async (student) => {
  try {
    await ElMessageBox.confirm(
      `确定要移除学生 ${student.studentName} 的班级分配吗？`,
      '确认移除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    );

    await studentClassService.removeStudentFromClass(student.studentId);
    ElMessage.success('移除成功');
    await loadStudents();
    await loadClasses();
    if (selectedClassId.value) {
      await loadClassStudents();
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('移除失败');
    }
  }
};

// 从当前班级移除学生
const removeStudentFromCurrentClass = async (student) => {
  await removeStudentClass(student);
};

// 初始化
onMounted(async () => {
  await Promise.all([loadStudents(), loadClasses()]);
});
</script>

<style scoped>
.student-class-page {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: calc(100vh - 60px);
}

/* 页面标题 */
.page-header {
  background: white;
  padding: 20px 24px;
  border-radius: 8px;
  margin-bottom: 20px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}

.page-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 500;
  color: #303133;
}

/* 主内容区 */
.content-container {
  display: flex;
  gap: 20px;
  height: calc(100vh - 180px);
}

/* 左右面板 */
.left-panel,
.right-panel {
  background: white;
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.left-panel {
  flex: 1;
}

.right-panel {
  flex: 1;
}

/* 面板标题 */
.panel-header {
  padding: 16px 20px;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.panel-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 500;
  color: #303133;
}

/* 筛选控件 */
.filter-controls {
  display: flex;
  gap: 12px;
}

/* 面板主体 */
.panel-body {
  flex: 1;
  padding: 20px;
  overflow: auto;
}

/* 班级选择器 */
.class-selector {
  padding: 20px;
  display: flex;
  gap: 12px;
  align-items: center;
  border-bottom: 1px solid #e4e7ed;
}

.class-selector :deep(.el-select) {
  flex: 1;
}

/* 班级信息 */
.class-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.class-info h4 {
  margin: 0;
  font-size: 14px;
  font-weight: 500;
  color: #606266;
}

/* 空状态 */
.empty-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
  padding: 40px;
}

/* 表格样式调整 */
:deep(.el-table) {
  font-size: 14px;
}

:deep(.el-table th) {
  background-color: #f5f7fa;
  color: #606266;
  font-weight: 500;
}

:deep(.el-table .el-table__row:hover) {
  background-color: #f5f7fa;
}

:deep(.el-table .current-row) {
  background-color: #ecf5ff !important;
}

/* 按钮样式 */
:deep(.el-button--primary) {
  background-color: #409eff;
  border-color: #409eff;
}

:deep(.el-button--primary:hover) {
  background-color: #66b1ff;
  border-color: #66b1ff;
}

:deep(.el-button.is-disabled) {
  opacity: 0.5;
}

/* 标签样式 */
:deep(.el-tag) {
  border-radius: 4px;
}
</style>
