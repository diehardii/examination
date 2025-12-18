<template>
  <div class="login-page">
    <div class="login-container">
      <!-- 左侧系统介绍 -->
      <div class="system-intro">
        <h1 class="system-title">智能考试管理系统</h1>
        <p class="system-description">
          我们应用最先进的大模型来按照您的要求生成各类试卷，并提供试卷的测试，评分，
          应用大模型对您的考试结果进行多维度的分析。为广大学生提供便捷的自主学习工具。
        </p>
      </div>

      <!-- 右侧登录表单 -->
      <div class="login-form">
        <h2>登录</h2>
        <form @submit.prevent="handleLogin">
          <div class="form-group">
            <label for="username">用户名</label>
            <input
                id="username"
                v-model="username"
                type="text"
                required
                placeholder="请输入用户名"
            />
          </div>
          <div class="form-group">
            <label for="password">密码</label>
            <input
                id="password"
                v-model="password"
                type="password"
                required
                placeholder="请输入密码"
            />
          </div>
          <button type="submit" :disabled="loading">
            {{ loading ? '登录中...' : '登录' }}
          </button>
          <p v-if="error" class="error">{{ error }}</p>
        </form>
        <p class="register-link">
          还没有账号？<router-link :to="{ name: 'register' }">立即注册</router-link>
        </p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/common/stores/auth';

const authStore = useAuthStore();
const router = useRouter();

const username = ref('');
const password = ref('');
const loading = ref(false);
const error = ref('');

const handleLogin = async () => {
  try {
    loading.value = true;
    error.value = '';
    await authStore.login({
      username: username.value,
      password: password.value
    });
    router.push('/home');
  } catch (err) {
    error.value = err.response?.data?.message || '登录失败，请检查用户名和密码';
  } finally {
    loading.value = false;
  }
};
</script>

<!-- 保持原有的样式不变 -->
<style scoped>
.login-page {
  position: relative;
  height: 100vh;
  width: 100vw;
  background-color: #f0f2f5;
  display: flex;
  justify-content: center;
  align-items: center;
}

.login-container {
  width: 80%;
  max-width: 1200px;
  height: 80%;
  display: flex;
  justify-content: space-between;
  align-items: flex-start; /* 改为顶部对齐 */
  background-image: url('@/assets/images/login.jpg');
  background-size: cover;
  background-position: center;
  border-radius: 15px;
  padding: 20px 40px 40px; /* 顶部内边距减少 */
  box-shadow: 0 0 30px rgba(0, 0, 0, 0.2);
  position: relative;
  overflow: hidden;
}

.system-intro {
  width: 50%;
  padding: 0 40px;
  position: relative;
  z-index: 1;
  margin-top: 70px; /* 进一步加大上移距离 */
}

.system-title {
  font-size: 2.5rem;
  font-weight: bold;
  margin-bottom: 10px; /* 减小下边距 */
  color: #1a237e;
  text-shadow: 1px 1px 2px rgba(255, 255, 255, 0.5);
}

.system-description {
  font-size: 1.1rem;
  line-height: 1.6;
  color: #283593;
  margin-top: 5px; /* 减小上边距 */
  text-shadow: 1px 1px 2px rgba(255, 255, 255, 0.5);
}

.login-form {
  width: 300px;
  height: 400px;
  padding: 40px;
  border-radius: 10px;
  background-color: rgba(255, 255, 255, 0.95);
  box-shadow: 0 0 20px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
  justify-content: center;
  position: relative;
  z-index: 1;
  margin-top: 20px; /* 表单也稍微上移保持平衡 */
}

.login-form h2 {
  text-align: center;
  font-size: 28px;
  margin-bottom: 30px;
  color: #333;
}

.form-group {
  margin-bottom: 25px;
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

.error {
  color: #e74c3c;
  margin-top: 15px;
  font-size: 14px;
  text-align: center;
}

.register-link {
  text-align: center;
  margin-top: 20px;
  font-size: 14px;
  color: #666;
}

.register-link a {
  color: #42b983;
  text-decoration: none;
  font-weight: 500;
}

.register-link a:hover {
  text-decoration: underline;
}

@media (max-width: 768px) {
  .login-container {
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

  .login-form {
    width: 100%;
    height: auto;
    margin-top: 0;
  }
}
</style>