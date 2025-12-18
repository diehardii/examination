<template>
  <div class="register-page">
    <div class="register-container">
      <!-- 左侧系统介绍（与登录界面保持一致） -->
      <div class="system-intro">
        <h1 class="system-title">智能考试管理系统</h1>
        <p class="system-description">
          我们应用最先进的大模型来按照您的要求生成各类试卷，并提供试卷的测试，评分，
          应用大模型对您的考试结果进行多维度的分析。为广大学生提供便捷的自主学习工具。
        </p>
      </div>

      <!-- 右侧注册表单 -->
      <div class="register-form">
        <h2>注册</h2>
        <form @submit.prevent="handleRegister">
          <div class="form-group">
            <label for="username">用户名</label>
            <input
                id="username"
                v-model="form.username"
                type="text"
                required
                placeholder="请输入用户名"
                @input="validateUsername"
            />
            <p v-if="usernameError" class="input-error">{{ usernameError }}</p>
          </div>
          <div class="form-group">
            <label for="password">密码</label>
            <input
                id="password"
                v-model="form.password"
                type="password"
                required
                placeholder="请输入密码(至少6位)"
                @input="validatePassword"
            />
            <p v-if="passwordError" class="input-error">{{ passwordError }}</p>
          </div>
          <div class="form-group">
            <label for="confirmPassword">确认密码</label>
            <input
                id="confirmPassword"
                v-model="form.confirmPassword"
                type="password"
                required
                placeholder="请再次输入密码"
                @input="validateConfirmPassword"
            />
            <p v-if="confirmPasswordError" class="input-error">{{ confirmPasswordError }}</p>
          </div>
          <div class="form-group">
            <label for="phone">手机号</label>
            <input
                id="phone"
                v-model="form.phone"
                type="tel"
                required
                placeholder="请输入手机号"
                @input="validatePhone"
            />
            <p v-if="phoneError" class="input-error">{{ phoneError }}</p>
          </div>
          <button type="submit" :disabled="loading || hasErrors">
            {{ loading ? '注册中...' : '注册' }}
          </button>
          <p v-if="submitError" class="submit-error">{{ submitError }}</p>
        </form>
        <p class="login-link">
          已有账号？<router-link :to="{ name: 'login' }">立即登录</router-link>
        </p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/common/stores/auth';

const authStore = useAuthStore();
const router = useRouter();

const form = ref({
  username: '',
  password: '',
  confirmPassword: '',
  phone: ''
});

const errors = ref({
  username: '',
  password: '',
  confirmPassword: '',
  phone: ''
});

const loading = ref(false);
const submitError = ref('');

// 计算属性检查是否有错误
const hasErrors = computed(() => {
  return Object.values(errors.value).some(error => error !== '');
});

const validateUsername = () => {
  if (!form.value.username) {
    errors.value.username = '用户名不能为空';
  } else if (form.value.username.length < 3) {
    errors.value.username = '用户名至少3个字符';
  } else {
    errors.value.username = '';
  }
};

const validatePassword = () => {
  if (!form.value.password) {
    errors.value.password = '密码不能为空';
  } else if (form.value.password.length < 6) {
    errors.value.password = '密码至少6个字符';
  } else {
    errors.value.password = '';
  }
};

const validateConfirmPassword = () => {
  if (!form.value.confirmPassword) {
    errors.value.confirmPassword = '请确认密码';
  } else if (form.value.password !== form.value.confirmPassword) {
    errors.value.confirmPassword = '两次输入的密码不一致';
  } else {
    errors.value.confirmPassword = '';
  }
};

const validatePhone = () => {
  const phoneRegex = /^1[3-9]\d{9}$/;
  if (!form.value.phone) {
    errors.value.phone = '手机号不能为空';
  } else if (!phoneRegex.test(form.value.phone)) {
    errors.value.phone = '请输入有效的手机号';
  } else {
    errors.value.phone = '';
  }
};

const handleRegister = async () => {
  // 验证所有字段
  validateUsername();
  validatePassword();
  validateConfirmPassword();
  validatePhone();

  if (hasErrors.value) {
    return;
  }

  try {
    loading.value = true;
    submitError.value = '';

    // 调用注册接口
    await authStore.register({
      username: form.value.username,
      password: form.value.password,
      phone: form.value.phone
    });

    // 注册成功后自动登录
    await authStore.login({
      username: form.value.username,
      password: form.value.password
    });

    // 跳转到首页
    router.push('/home');
  } catch (err) {
    submitError.value = err.response?.data?.message || err.message || '注册失败，请检查输入信息';
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
/* 保持与登录界面相同的样式 */
.register-page {
  position: relative;
  height: 100vh;
  width: 100vw;
  background-color: #f0f2f5;
  display: flex;
  justify-content: center;
  align-items: center;
}

.register-container {
  width: 80%;
  max-width: 1200px;
  height: 80%;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  background-image: url('@/assets/images/login.jpg');
  background-size: cover;
  background-position: center;
  border-radius: 15px;
  padding: 20px 40px 40px;
  box-shadow: 0 0 30px rgba(0, 0, 0, 0.2);
  position: relative;
  overflow: hidden;
}

.system-intro {
  width: 50%;
  padding: 0 40px;
  position: relative;
  z-index: 1;
  margin-top: 70px;
}

.system-title {
  font-size: 2.5rem;
  font-weight: bold;
  margin-bottom: 10px;
  color: #1a237e;
  text-shadow: 1px 1px 2px rgba(255, 255, 255, 0.5);
}

.system-description {
  font-size: 1.1rem;
  line-height: 1.6;
  color: #283593;
  margin-top: 5px;
  text-shadow: 1px 1px 2px rgba(255, 255, 255, 0.5);
}

.register-form {
  width: 300px;
  height: auto;
  padding: 40px;
  border-radius: 10px;
  background-color: rgba(255, 255, 255, 0.95);
  box-shadow: 0 0 20px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
  justify-content: center;
  position: relative;
  z-index: 1;
  margin-top: 20px;
}

.register-form h2 {
  text-align: center;
  font-size: 28px;
  margin-bottom: 30px;
  color: #333;
}

.form-group {
  margin-bottom: 15px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-size: 16px;
  color: #555;
}

.form-group input {
  width: 100%;
  padding: 12px;
  box-sizing: border-box;
  border: 1px solid #ddd;
  border-radius: 5px;
  font-size: 16px;
}

.input-error {
  color: #e74c3c;
  font-size: 12px;
  margin-top: 4px;
  height: 16px;
}

button {
  width: 100%;
  padding: 14px;
  background-color: #42b983;
  color: white;
  border: none;
  border-radius: 5px;
  cursor: pointer;
  font-size: 16px;
  transition: background-color 0.3s;
  margin-top: 10px;
}

button:hover:not(:disabled) {
  background-color: #3aa876;
}

button:disabled {
  background-color: #ccc;
  cursor: not-allowed;
}

.submit-error {
  color: #e74c3c;
  margin-top: 15px;
  font-size: 14px;
  text-align: center;
}

.login-link {
  text-align: center;
  margin-top: 20px;
  font-size: 14px;
  color: #666;
}

.login-link a {
  color: #42b983;
  text-decoration: none;
  font-weight: 500;
}

.login-link a:hover {
  text-decoration: underline;
}

@media (max-width: 768px) {
  .register-container {
    flex-direction: column;
    width: 90%;
    height: auto;
    background-position: left center;
    padding: 15px 20px 30px;
  }

  .system-intro {
    width: 100%;
    padding: 20px;
    text-align: center;
    margin-bottom: 15px;
    margin-top: -40px;
  }

  .register-form {
    width: 100%;
    height: auto;
    margin-top: 0;
  }
}
</style>