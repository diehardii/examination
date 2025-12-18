<template>
  <div class="class-info-container">
    <div class="selection-area">
      <el-form :inline="true">
        <el-form-item label="选择班级">
          <el-select 
            v-model="selectedClassId" 
            placeholder="请选择班级"
            @change="onClassChange"
            style="width: 200px;"
          >
            <el-option
              v-for="cls in teacherClasses"
              :key="cls.classId"
              :label="cls.displayName"
              :value="cls.classId"
            />
          </el-select>
        </el-form-item>
      </el-form>
    </div>

    <div class="class-summary" v-if="classInfo">
      <el-descriptions title="班级信息" :column="4" border>
        <el-descriptions-item label="班级代码">{{ classInfo.classCode }}</el-descriptions-item>
        <el-descriptions-item label="学段">{{ classInfo.stageName }}</el-descriptions-item>
        <el-descriptions-item label="年级">{{ classInfo.gradeName }}</el-descriptions-item>
        <el-descriptions-item label="学生人数">{{ classInfo.studentCount }}</el-descriptions-item>
      </el-descriptions>
    </div>

    <div class="student-list" v-if="classInfo">
      <el-table :data="classInfo.students" stripe style="width: 100%">
        <el-table-column prop="studentId" label="学生ID" width="100" />
        <el-table-column prop="username" label="用户名" width="150" />
        <el-table-column prop="studentName" label="姓名" width="150" />
        <el-table-column prop="email" label="邮箱" />
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="viewStudentDetail(row)">
              查看学情
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-empty v-if="!selectedClassId" description="请选择班级查看学生信息" />
    <el-empty v-else-if="selectedClassId && classInfo && classInfo.students.length === 0" description="该班级暂无学生" />
  </div>
</template>

<script>
import { ref, onMounted, computed } from 'vue';
import { ElMessage } from 'element-plus';
import { fetchTeacherAllAssignments, fetchClassStudents, fetchClassesByStage } from '@/common/service/learningAnalysisService';

export default {
  name: 'ClassInfoAnalysis',
  setup() {
    const selectedClassId = ref(null);
    const classInfo = ref(null);
    const teacherClasses = ref([]);
    const loading = ref(false);

    // 获取当前用户信息
    const getCurrentUser = () => {
      const userStr = localStorage.getItem('user');
      if (userStr) {
        return JSON.parse(userStr);
      }
      return null;
    };

    // 加载教师的班级列表
    const loadTeacherClasses = async () => {
      const user = getCurrentUser();
      if (!user || !user.id) {
        ElMessage.warning('未获取到用户信息');
        return;
      }

      try {
        loading.value = true;
        // 获取教师所有学段的班级分配 { stageId: [classId1, classId2, ...] }
        const assignments = await fetchTeacherAllAssignments(user.id);
        
        // 收集所有班级ID
        const allClassIds = new Set();
        for (const stageId in assignments) {
          const classIds = assignments[stageId];
          classIds.forEach(id => allClassIds.add(id));
        }

        // 获取每个学段的班级详细信息
        const allClasses = [];
        for (const stageId in assignments) {
          const classIds = assignments[stageId];
          if (classIds && classIds.length > 0) {
            // 获取该学段的所有班级，然后筛选出教师分配的班级
            const stageClasses = await fetchClassesByStage(parseInt(stageId));
            const teacherStageClasses = stageClasses.filter(cls => classIds.includes(cls.classId));
            
            // 添加显示名称
            teacherStageClasses.forEach(cls => {
              cls.displayName = `${cls.stageName} - ${cls.gradeName} - ${cls.classCode}`;
            });
            
            allClasses.push(...teacherStageClasses);
          }
        }

        teacherClasses.value = allClasses;
      } catch (error) {
        console.error('加载班级列表失败:', error);
        ElMessage.error('加载班级列表失败');
      } finally {
        loading.value = false;
      }
    };

    // 班级选择变化时加载学生列表
    const onClassChange = async () => {
      if (!selectedClassId.value) {
        classInfo.value = null;
        return;
      }

      try {
        loading.value = true;
        const data = await fetchClassStudents(selectedClassId.value);
        classInfo.value = data;
      } catch (error) {
        console.error('加载学生列表失败:', error);
        ElMessage.error('加载学生列表失败');
        classInfo.value = null;
      } finally {
        loading.value = false;
      }
    };

    // 查看学生详细学情
    const viewStudentDetail = (student) => {
      ElMessage.info(`查看学生 ${student.studentName} 的学情分析（功能待实现）`);
      // TODO: 跳转到学生学情详情页面
    };

    onMounted(() => {
      loadTeacherClasses();
    });

    return {
      selectedClassId,
      classInfo,
      teacherClasses,
      loading,
      onClassChange,
      viewStudentDetail,
    };
  },
};
</script>

<style scoped>
.class-info-container {
  padding: 20px;
}

.selection-area {
  margin-bottom: 20px;
  padding: 15px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.class-summary {
  margin-bottom: 20px;
}

.student-list {
  background-color: #fff;
  border-radius: 4px;
}
</style>
