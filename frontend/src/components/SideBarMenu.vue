<template>
  <div class="sidebar">
    <h2>智能考试管理系统</h2>
    <ul class="menu">
      <li v-for="item in menuItems" :key="item.path">
        <router-link :to="item.path">
          <span class="icon">{{ item.icon }}</span>
          <span class="menu-text">{{ item.name }}</span>
        </router-link>
      </li>
      <!-- 新增图表分析菜单项 -->

      <li v-if="isAdmin">
        <a @click="openDataAnalysis" style="cursor: pointer;">
          <span class="icon">📉</span>
          <span class="menu-text">考试分析</span>
        </a>
      </li>
      <li v-if="isAdmin">
        <router-link to="/user-man">
          <span class="icon">👨‍💼</span>
          <span class="menu-text">用户管理</span>
        </router-link>
      </li>

      <li v-if="isAdmin">
        <router-link to="/system-man">
          <span class="icon">⚙️</span>
          <span class="menu-text">系统维护</span>
        </router-link>
      </li>
      <li>

        <button @click="logout" class="logout-btn">
          <span class="icon">🚪</span>
          <span class="menu-text">退出登录</span>
        </button>
      </li>
    </ul>
  </div>
</template>

<script>
import { ref, onMounted,computed } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import menuApi from '@/api/menu';



export default {
  name: 'SideBarView',
  setup() {
    const menuItems = ref([]);
    const router = useRouter();
    const authStore = useAuthStore();

    const fetchMenuItems = async () => {
      try {
        const response = await menuApi.getMenuItems();
        menuItems.value = response.data;
      } catch (error) {
        console.error('获取菜单失败:', error);
      }
    };

    const logout = async () => {
      try {

        router.push('/auth/login');
      } catch (error) {
        console.error('退出登录失败:', error);
      }
    };



    const openDataAnalysis = async () => {
      try {
        const token = await authStore.generateCrossSystemToken()
        if (!token) throw new Error('无法生成认证令牌')

        // 打开新窗口并传递token
        const analysisUrl = `http://localhost:5173?token=${encodeURIComponent(token)}`
        window.open(analysisUrl, '_blank')
      } catch (error) {
        console.error('打开数据分析失败:', error)
        alert('无法打开数据分析系统，请检查登录状态')
      }
    };

    const isAdmin = computed(() => {
      try {
        const user = JSON.parse(localStorage.getItem('user'));
        console.log(user);
        return user?.roleName == "ADMIN";
      } catch {
        return false;
      }
    });



    onMounted(() => {
      fetchMenuItems();
    });

    return {
      menuItems,
      logout,
      openDataAnalysis,
      isAdmin,
    };
  }
};
</script>

<style scoped>
.sidebar {
  width: 250px;
  height: 100vh;
  background-color: #2c3e50;
  color: white;
  padding: 20px;
  box-shadow: 2px 0 5px rgba(0, 0, 0, 0.1);
  position: fixed;
  top: 0;
}

.sidebar h2 {
  text-align: center;
  margin-bottom: 30px;
  color: #fff;
}

.menu {
  list-style: none;
  padding: 0;
}

.menu li {
  margin-bottom: 15px;
}

.menu a,
.logout-btn {
  display: flex;
  align-items: center;
  color: #ecf0f1;
  text-decoration: none;
  padding: 10px;
  border-radius: 4px;
  transition: background-color 0.3s;
  width: 100%;
  font-size: 1em; /* 统一字体大小 */
  line-height: 1.5; /* 统一行高 */
}

.menu-text {
  font-size: inherit; /* 继承父元素的字体大小 */
}

.menu a:hover {
  background-color: #34495e;
}

.menu a.router-link-active {
  background-color: #3498db;
  color: white;
}

.icon {
  margin-right: 10px;
  font-size: 1.2em;
}

.logout-btn {
  display: flex;
  align-items: center;
  background: none;
  border: none;
  color: #ecf0f1;
  cursor: pointer;
  padding: 10px;
  width: 100%;
  text-align: left;
  border-radius: 4px;
  transition: background-color 0.3s;
}

.logout-btn:hover {
  background-color: #e74c3c;
}
</style>