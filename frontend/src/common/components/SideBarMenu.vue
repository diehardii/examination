<template>
  <div class="sidebar">
    <h2>智能考试管理系统</h2>
    
    <!-- 科目选择下拉框 -->
    <div class="subject-selector">
      <label for="subject-select">选择科目：</label>
      <select 
        id="subject-select" 
        v-model="selectedSubject" 
        @change="handleSubjectChange"
        class="subject-dropdown"
      >
        <option value="">请选择科目</option>
        <option 
          v-for="subject in subjects" 
          :key="subject.id" 
          :value="subject.subjectEnName"
        >
          {{ subject.subjectEnName }}
        </option>
      </select>
    </div>
    
    <ul class="menu">
      <li v-for="item in menuItems" :key="item.path">
        <a v-if="item.path === '/paper-gen'" href="javascript:void(0)" @click="goToPaperGen" class="menu-link">
          <span class="icon">{{ item.icon }}</span>
          <span class="menu-text">{{ item.name }}</span>
        </a>
        <router-link v-else :to="item.path">
          <span class="icon">{{ item.icon }}</span>
          <span class="menu-text">{{ item.name }}</span>
        </router-link>
      </li>
      <!-- 新增图表分析菜单项 -->

      <li v-if="!isAdmin">
        <a href="javascript:void(0)" @click="goToKnowledgeManagement" class="menu-link">
          <span class="icon">📘</span>
          <span class="menu-text">真题解析</span>
        </a>
      </li>

      <li v-if="!isAdmin">
        <router-link to="/wrong-questions">
          <span class="icon">❌</span>
          <span class="menu-text">错题答疑</span>
        </router-link>
      </li>

      <li v-if="!isAdmin">
        <router-link to="/smart-qa">
          <span class="icon">💡</span>
          <span class="menu-text">智能问答</span>
        </router-link>
      </li>

      <li v-if="isTeacher">
        <router-link to="/learning-analysis">
          <span class="icon">📉</span>
          <span class="menu-text">学情分析</span>
        </router-link>
      </li>

      <li v-if="isTeacher">
        <router-link to="/user-man">
          <span class="icon">👨‍💼</span>
          <span class="menu-text">用户管理</span>
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
import { useAuthStore } from '@/common/stores/auth';
import menuApi from '@/common/api/menu';
import { subjectsEnApi } from '@/common/api/subjectsEn';



export default {
  name: 'SideBarView',
  setup() {
    const menuItems = ref([]);
    const subjects = ref([]);
    const selectedSubject = ref('');
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

    const fetchSubjects = async () => {
      try {
        const response = await subjectsEnApi.getAllSubjects();
        console.log('API返回的科目数据:', response);
        subjects.value = response || [];
      } catch (error) {
        console.error('获取科目列表失败:', error);
        subjects.value = [];
      }
    };

    const handleSubjectChange = () => {
      // 将选中的科目存储到sessionStorage
      if (selectedSubject.value) {
        sessionStorage.setItem('selectedSubject', selectedSubject.value);
        console.log('已选择科目:', selectedSubject.value);
      } else {
        sessionStorage.removeItem('selectedSubject');
      }
    };

    // 根据选择的科目跳转到对应的试卷生成页面
    const goToPaperGen = () => {
      const subject = selectedSubject.value;
      
      // 高考相关科目跳转到 HS3 试卷生成页面
      if (subject === '高考' || subject === '高考英语') {
        router.push({ path: '/paper-gen-hs3', query: { subject: '高考' } });
      } else if (subject === 'CET4' || subject === '大学英语四级') {
        // CET4 跳转到试卷生成页面
        router.push({ path: '/paper-gen', query: { subject: 'CET4' } });
      } else if (subject === 'CET6' || subject === '大学英语六级') {
        // CET6 跳转到试卷生成页面
        router.push({ path: '/paper-gen', query: { subject: 'CET6' } });
      } else {
        // 其他科目跳转到默认试卷生成页面
        router.push('/paper-gen');
      }
    };

    // 根据选择的科目跳转到对应的真题解析页面
    const goToKnowledgeManagement = () => {
      const subject = selectedSubject.value;
      
      // 高考相关科目跳转到 HS3 页面
      if (subject === '高考' || subject === '高考英语') {
        router.push({ path: '/paper-analysis-hs3', query: { subject: '高考' } });
      } else if (subject === 'CET4' || subject === '大学英语四级') {
        // CET4 直接跳转到试卷解析页面
        router.push({ path: '/paper-analysis-cet4', query: { subject: 'CET4' } });
      } else if (subject === 'CET6' || subject === '大学英语六级') {
        // CET6 跳转到试卷解析页面
        router.push({ path: '/paper-analysis-cet4', query: { subject: 'CET6' } });
      } else {
        // 其他科目跳转到原有页面
        router.push('/knowledge-management');
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

    const openKnowledgeManager = () => {
      try {
        // 打开教学知识管理系统
        const knowledgeUrl = 'http://localhost:5172/'
        window.open(knowledgeUrl, '_blank')
      } catch (error) {
        console.error('打开教学知识管理系统失败:', error)
        alert('无法打开教学知识管理系统')
      }
    };

    const isTeacher = computed(() => {
      try {
        const user = JSON.parse(localStorage.getItem('user'));
          return user?.roleName == "TEACHER";
      } catch {
        return false;
      }
    });

    const isAdmin = computed(() => {
      try {
        const user = JSON.parse(localStorage.getItem('user'));
        return user?.roleName == "ADMIN";
      } catch {
        return false;
      }
    });



    onMounted(() => {
      fetchMenuItems();
      fetchSubjects();
      // 移除自动恢复之前选择的科目，保持初始为空以显示占位选项
      // const savedSubject = sessionStorage.getItem('selectedSubject');
      // if (savedSubject) {
      //   selectedSubject.value = savedSubject;
      // }
      selectedSubject.value = '';
    });

    return {
      menuItems,
      subjects,
      selectedSubject,
      logout,
      openDataAnalysis,
      openKnowledgeManager,
      goToKnowledgeManagement,
      goToPaperGen,
      isTeacher,
      isAdmin,
      handleSubjectChange,
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
  left: 0;
  /* 确保菜单始终在最上层，不会被内容遮盖 */
  z-index: 100;
  /* 添加滚动条，防止菜单内容过多时溢出 */
  overflow-y: auto;
}

.sidebar h2 {
  text-align: center;
  margin-bottom: 20px;
  color: #fff;
}

.subject-selector {
  margin-bottom: 20px;
  padding: 15px;
  background-color: #34495e;
  border-radius: 6px;
}

.subject-selector label {
  display: block;
  margin-bottom: 8px;
  color: #ecf0f1;
  font-size: 14px;
  font-weight: 500;
}

.subject-dropdown {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #5a6c7d;
  border-radius: 4px;
  background-color: #2c3e50;
  color: #ecf0f1;
  font-size: 14px;
  cursor: pointer;
  transition: border-color 0.3s, background-color 0.3s;
}

.subject-dropdown:focus {
  outline: none;
  border-color: #3498db;
  background-color: #2c3e50;
}

.subject-dropdown option {
  background-color: #2c3e50;
  color: #ecf0f1;
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