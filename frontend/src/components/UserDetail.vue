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

            <!-- 大学-院系级联选择 -->
            <el-form-item label="大学" prop="universityId">
              <el-select
                  v-model="form.universityId"
                  placeholder="请选择大学"
                  @change="handleUniversityChange"
                  :loading="loading.universities"
              >
                <el-option
                    v-for="univ in universities"
                    :key="univ.universityId"
                    :label="univ.universityName"
                    :value="univ.universityId"
                />
              </el-select>
            </el-form-item>

            <el-form-item label="院系" prop="departmentId">
              <el-select
                  v-model="form.departmentId"
                  placeholder="请先选择大学"
                  :disabled="!form.universityId"
                  :loading="loading.departments"
              >
                <el-option
                    v-for="dept in departments"
                    :key="dept.departmentId"
                    :label="dept.departmentName"
                    :value="dept.departmentId"
                />
              </el-select>
            </el-form-item>

            <el-form-item label="职业" prop="occupation">
              <el-select v-model="form.occupation" placeholder="请选择">
                <el-option label="学生" value="学生"></el-option>
                <el-option label="老师" value="老师"></el-option>
              </el-select>
            </el-form-item>

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
import { getUserDetail, saveUserDetail } from '@/service/userDetailService';
import { getUniversities } from '@/service/universityService';
import { getDepartmentsByUniversity } from '@/service/departmentService';


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
        universityId: null,
        departmentId: null,
        occupation: ''
      },
      universities: [],
      departments: [],
      loading: {
        universities: false,
        departments: false
      },
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
        universityId: [
          { required: true, message: '请选择大学', trigger: 'change' }
        ]
      }
    };
  },
  async created() {
    await this.loadInitialData();
  },
  methods: {
    async loadInitialData() {
      try {
        // 1. 加载大学列表
        this.loading.universities = true;
        this.universities = await getUniversities();

        // 2. 加载当前用户详情
        const user = JSON.parse(localStorage.getItem('user') || {});
        this.form.userId = user.id;

        if (this.form.userId) {
          const detail = await getUserDetail(this.form.userId);
          console.log(detail.data);
          Object.assign(this.form, detail.data);

          // 3. 如果已有大学选择，加载对应院系
          if (this.form.universityId) {
            await this.loadDepartments(this.form.universityId);
          }
        }
      } catch (error) {
        this.$message.error('请补充您的资料信息');
        console.error(error);
      } finally {
        this.loading.universities = false;
      }
    },

    async handleUniversityChange(universityId) {
      this.form.departmentId = null; // 重置院系选择
      if (universityId) {
        await this.loadDepartments(universityId);
      } else {
        this.departments = [];
      }
    },

    async loadDepartments(universityId) {
      this.loading.departments = true;
      try {
        this.departments = await getDepartmentsByUniversity(universityId);
      } catch (error) {
        this.$message.error('院系列载失败');
        console.error(error);
      } finally {
        this.loading.departments = false;
      }
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