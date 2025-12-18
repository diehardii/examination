<template>
  <div class="app-container">
    <!-- 左侧菜单栏 -->
    <SideBarMenu />
    <div class="main-layout">
      <!-- 顶部菜单 -->
      <div class="top-menu" style="background-color: #3498db;">
        <button
          v-for="item in menuItems"
          :key="item.name"
          :class="['menu-item', { active: activeMenu === item.name }]"
          @click="setActiveMenu(item.name)"
        >
          {{ item.label }}
        </button>
      </div>

      <!-- 内容区域 -->
      <div class="content">
        <component 
          :is="activeComponent" 
          :class-id="selectedClassId"
          :class-name="selectedClassName"
          @view-student="handleViewStudent"
        />
      </div>
    </div>
  </div>
</template>

<script>
import { markRaw } from 'vue';
import SideBarMenu from '@/common/components/SideBarMenu.vue';
import ClassInfoAnalysis from '@/common/components/ClassInfoAnalysis.vue';
import CET4StudentAnalysis from '@/common/components/CET4StudentAnalysis.vue';
import CET4ClassAnalysis from '@/common/components/CET4ClassAnalysis.vue';
import CET4ExamPaperAnalysis from '@/common/components/CET4ExamPaperAnalysis.vue';

export default {
  name: 'LearningAnalysisView',
  components: {
    SideBarMenu,
    ClassInfoAnalysis,
    CET4StudentAnalysis,
    CET4ClassAnalysis,
    CET4ExamPaperAnalysis,
  },
  data() {
    return {
      activeMenu: 'classInfo',
      selectedClassId: null,
      selectedClassName: '',
      menuItems: [
        { name: 'classInfo', label: '班级信息', component: markRaw(ClassInfoAnalysis) },
        { name: 'cet4Student', label: '学生分析', component: markRaw(CET4StudentAnalysis) },
        { name: 'cet4Class', label: '班级分析', component: markRaw(CET4ClassAnalysis) },
        { name: 'cet4ExamPaper', label: '试卷分析', component: markRaw(CET4ExamPaperAnalysis) },
      ],
    };
  },
  computed: {
    activeComponent() {
      const item = this.menuItems.find(i => i.name === this.activeMenu);
      return item ? item.component : null;
    },
  },
  methods: {
    setActiveMenu(menu) {
      console.log('切换菜单到:', menu);
      this.activeMenu = menu;
      console.log('当前活动组件:', this.activeComponent);
    },
    // 从班级分析跳转到学生分析
    handleViewStudent(studentInfo) {
      this.selectedClassId = studentInfo.classId;
      this.selectedClassName = studentInfo.className || '';
      this.activeMenu = 'cet4Student';
      // 通过事件传递学生ID给学生分析组件
      this.$nextTick(() => {
        // 可以通过其他方式传递studentId，如store或provide/inject
      });
    },
  },
};
</script>

<style scoped>
.app-container {
  display: flex;
  min-height: 100vh;
}

.main-layout {
  display: flex;
  flex-direction: column;
  flex: 1;
  margin-left: 290px;
  width: calc(100% - 290px);
}

.top-menu {
  display: flex;
  padding: 0 20px;
  height: 60px;
  position: sticky;
  top: 0;
  z-index: 100;
}

.menu-item {
  background: none;
  border: none;
  color: white;
  font-size: 16px;
  padding: 0 20px;
  cursor: pointer;
  height: 100%;
  transition: background-color 0.3s;
}

.menu-item:hover {
  background-color: rgba(255, 255, 255, 0.2);
}

.menu-item.active {
  background-color: rgba(255, 255, 255, 0.3);
  font-weight: bold;
}

.content {
  flex: 1;
  padding: 20px;
  overflow: auto;
  background-color: #f5f5f5;
}
</style>
