<template>
      <div class="change-password-container">
        <div class="card">
          <h1 class="header">用户详细信息</h1>

          <el-form
              :model="form"
              label-width="120px"
              :rules="rules"
              ref="userForm"
          >
            <el-form-item label="真实姓名" prop="realName">
              <el-input v-model="form.realName"></el-input>
            </el-form-item>
            <!-- 基本信息 -->
            <el-form-item label="身份证号" prop="identityId">
              <el-input v-model="form.identityId"></el-input>
            </el-form-item>

            <el-form-item label="性别" prop="gender">
              <el-select v-model="form.gender" placeholder="请选择">
                <el-option label="男" value="男"></el-option>
                <el-option label="女" value="女"></el-option>
              </el-select>
            </el-form-item>

            <!-- 新增手机号码输入框 -->
            <el-form-item label="手机号码" prop="phoneNumber">
              <el-input v-model="form.phoneNumber" placeholder="请输入手机号码"></el-input>
            </el-form-item>

            <el-form-item label="邮箱" prop="email">
              <el-input v-model="form.email" type="email"></el-input>
            </el-form-item>

            <el-form-item label="地址" prop="address">
              <el-input v-model="form.address"></el-input>
            </el-form-item>

            <!-- 学生专属字段 -->
            <template v-if="isStudent">
              <el-divider content-position="left">学生信息</el-divider>
              
              <el-form-item label="学号" prop="studentNumber">
                <el-input v-model="form.studentNumber" placeholder="请输入学号"></el-input>
              </el-form-item>

              <el-form-item label="学段" prop="stageId">
                <el-select v-model="form.stageId" placeholder="请选择学段" @change="onStageChange">
                  <el-option
                      v-for="stage in stageOptions"
                      :key="stage.stageId"
                      :label="stage.displayName"
                      :value="stage.stageId"
                  ></el-option>
                </el-select>
              </el-form-item>

              <el-form-item label="年级" prop="gradeId">
                <el-select v-model="form.gradeId" placeholder="请选择年级" :disabled="!form.stageId" @change="onGradeChange">
                  <el-option
                      v-for="grade in gradeOptions"
                      :key="grade.gradeId"
                      :label="grade.gradeName"
                      :value="grade.gradeId"
                  ></el-option>
                </el-select>
              </el-form-item>

              <el-form-item label="班级" prop="classId">
                <el-select v-model="form.classId" placeholder="请选择班级" :disabled="!form.stageId">
                  <el-option
                      v-for="cls in classOptions"
                      :key="cls.classId"
                      :label="cls.classCode"
                      :value="cls.classId"
                  ></el-option>
                </el-select>
              </el-form-item>
            </template>

            

            <el-form-item>
              <el-button
                  type="primary"
                  @click="submitForm"
                  :loading="submitting"
                  style="background-color:#5daf34"
              >
                保存
              </el-button>
            </el-form-item>
          </el-form>
        </div>
      </div>
</template>

<script>
import { getUserDetail, saveUserDetail, getEducationStages, getGrades, getClasses } from '@/common/service/userDetailService';


export default {
  name: 'UserDetailView',
  data() {

    // 手机号码验证函数
    const validatePhone = (rule, value, callback) => {
      if (!value) {
        return callback(new Error('请输入手机号码'));
      }
      // 简单的手机号正则验证（11位数字，1开头）
      const reg = /^1[3-9]\d{9}$/;
      if (!reg.test(value)) {
        return callback(new Error('请输入有效的手机号码'));
      }
      callback();
    };


    return {
      form: {
        realName: '',
        userId: null,
        identityId: '',
        gender: '',
        phoneNumber: '', // 新增手机号码字段
        email: '',
        address: '',
        roleId: null,
        // 学生相关字段
        studentNumber: '',
        stageId: null,
        gradeId: null,
        classId: null
      },
      // 下拉选项数据
      stageOptions: [],
      gradeOptions: [],
      classOptions: [],
      allGrades: [],
      allClasses: [],
      loading: {},
      submitting: false,
      rules: {
        identityId: [
          { required: true, message: '请输入身份证号', trigger: 'blur' },
          { pattern: /^\d{17}[\dXx]$/, message: '请输入有效的身份证号' }
        ],
        gender: [
          { required: true, message: '请选择性别', trigger: 'change' }
        ],
        phoneNumber: [
          { validator: validatePhone, trigger: 'blur' }
        ],
        email: [
          { type: 'email', message: '请输入有效的邮箱地址', trigger: 'blur' }
        ],
        studentNumber: [
          { required: true, message: '请输入学号', trigger: 'blur' }
        ],
        stageId: [
          { required: true, message: '请选择学段', trigger: 'change' }
        ],
        gradeId: [
          { required: true, message: '请选择年级', trigger: 'change' }
        ],
        classId: [
          { required: true, message: '请选择班级', trigger: 'change' }
        ]
      }
    };
  },
  computed: {
    isTeacher() {
      try {
        const user = JSON.parse(localStorage.getItem('user') || '{}');
        console.log('User data in isTeacher:', user);
        console.log('Role Name:', user.roleName, 'Type:', typeof user.roleName);
        const result = user.roleName === 'TEACHER';
        console.log('Is teacher result:', result);
        return result;
      } catch (error) {
        console.error('Error in isTeacher computed:', error);
        return false;
      }
    },
    isStudent() {
      // 判断是否为学生角色 (roleId = 2)
      return this.form.roleId === 2;
    }
  },
  async created() {
    await this.loadInitialData();
  },
  methods: {
    async loadInitialData() {
      try {
        const user = JSON.parse(localStorage.getItem('user') || {});
        this.form.userId = user.id;

        if (this.form.userId) {
          const detail = await getUserDetail(this.form.userId);
          console.log(detail.data);
          Object.assign(this.form, detail.data);
          
          // 如果是学生，加载学段、年级、班级选项
          if (this.form.roleId === 2) {
            await this.loadStudentOptions();
          }
        }
      } catch (error) {
        this.$message.error('请补充您的资料信息');
        console.error(error);
      }
    },

    async loadStudentOptions() {
      try {
        // 加载学段列表
        const stagesRes = await getEducationStages();
        this.stageOptions = stagesRes.data || stagesRes;
        
        // 加载所有年级
        const gradesRes = await getGrades();
        this.allGrades = gradesRes.data || gradesRes;
        
        // 加载所有班级
        const classesRes = await getClasses();
        this.allClasses = classesRes.data || classesRes;
        
        // 如果已有学段，过滤年级和班级选项
        if (this.form.stageId) {
          this.filterGradesByStage(this.form.stageId);
          this.filterClassesByStage(this.form.stageId);
        }
      } catch (error) {
        console.error('加载学生选项数据失败:', error);
      }
    },

    onStageChange(stageId) {
      // 学段变化时，重置年级和班级
      this.form.gradeId = null;
      this.form.classId = null;
      this.filterGradesByStage(stageId);
      this.filterClassesByStage(stageId);
    },

    onGradeChange(gradeId) {
      // 年级变化时，可以根据需要过滤班级
      // 这里保持按学段过滤班级的逻辑
    },

    filterGradesByStage(stageId) {
      this.gradeOptions = this.allGrades.filter(g => g.stageId === stageId);
    },

    filterClassesByStage(stageId) {
      this.classOptions = this.allClasses.filter(c => c.stageId === stageId);
    },

    async submitForm() {
      try {
        await this.$refs.userForm.validate();

        this.submitting = true;
        await saveUserDetail(this.form);

        this.$message.success('用户信息保存成功');
      } catch (error) {
        if (error !== 'validate') {
          this.$message.error('保存失败: ' + (error.message || error));
        }
      } finally {
        this.submitting = false;
      }
    }
  }
};
</script>

<style scoped>

.change-password-container {
  padding: 20px;
}

.card {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  padding: 20px;
  max-width: 600px;
  margin: 0 auto;
}

.header {
  color: #333;
  margin-bottom: 20px;
  font-size: 24px;
  text-align: center;
}
</style>