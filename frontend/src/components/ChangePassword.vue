<template>

      <div class="change-password-container">
        <div class="card">
          <h1 class="header">修改密码</h1>

          <el-form
              :model="form"
              label-width="120px"
              :rules="rules"
              ref="passwordForm"
          >
            <el-form-item label="当前密码" prop="currentPassword">
              <el-input
                  v-model="form.currentPassword"
                  type="password"
                  show-password
                  placeholder="请输入当前密码"
              ></el-input>
            </el-form-item>

            <el-form-item label="新密码" prop="newPassword">
              <el-input
                  v-model="form.newPassword"
                  type="password"
                  show-password
                  placeholder="请输入新密码"
              ></el-input>
            </el-form-item>

            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input
                  v-model="form.confirmPassword"
                  type="password"
                  show-password
                  placeholder="请再次输入新密码"
              ></el-input>
            </el-form-item>

            <el-form-item>
              <el-button
                  type="primary"
                  @click="submitForm"
                  :loading="submitting"
                  style="background-color:#5daf34"
              >
                确认修改
              </el-button>
              <el-button @click="resetForm">重置</el-button>
            </el-form-item>
          </el-form>
        </div>
      </div>
</template>

<script>
import { changePassword } from '@/service/authService';

export default {
  name: 'ChangePasswordView',
  data() {
    const validateConfirmPassword = (rule, value, callback) => {
      if (value !== this.form.newPassword) {
        callback(new Error('两次输入的密码不一致'));
      } else {
        callback();
      }
    };

    return {
      form: {
        currentPassword: '',
        newPassword: '',
        confirmPassword: ''
      },
      submitting: false,
      rules: {
        currentPassword: [
          { required: true, message: '请输入当前密码', trigger: 'blur' },
          { min: 6, message: '密码长度至少为6位', trigger: 'blur' }
        ],
        newPassword: [
          { required: true, message: '请输入新密码', trigger: 'blur' },
          { min: 6, message: '密码长度至少为6位', trigger: 'blur' },
          {
            validator: (rule, value, callback) => {
              if (value === this.form.currentPassword) {
                callback(new Error('新密码不能与当前密码相同'));
              } else {
                callback();
              }
            },
            trigger: 'blur'
          }
        ],
        confirmPassword: [
          { required: true, message: '请确认新密码', trigger: 'blur' },
          { validator: validateConfirmPassword, trigger: 'blur' }
        ]
      }
    };
  },
  methods: {
    submitForm() {
      this.$refs.passwordForm.validate(async (valid) => {
        if (valid) {
          this.submitting = true;
          try {
            await changePassword(this.form);
            this.$message.success('密码修改成功');
            this.resetForm();
          } catch (error) {
            this.$message.error(error.message || '密码修改失败');
          } finally {
            this.submitting = false;
          }
        }
      });
    },
    resetForm() {
      this.$refs.passwordForm.resetFields();
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