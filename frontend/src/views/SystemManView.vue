<template>
  <div class="app-container">
    <!-- 新增左侧菜单栏 -->
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
      <component :is="activeComponent" />
    </div>
  </div>
  </div>
</template>

<script>
import UniversityManagement from '@/components/UniversityManagement.vue'
import DepartmentManagement from '@/components/DepartmentManagement.vue'
import SubjectManagement from '@/components/SubjectManagement.vue'
import RoleManagement from '@/components/RoleManagement.vue'  // 新增导入
import PaperClear from '@/components/PaperClear.vue'  //
import SideBarMenu from '@/components/SideBarMenu.vue'  // 新增导入
export default {
  components: {
    UniversityManagement,
    DepartmentManagement,
    SubjectManagement,
    RoleManagement,
    PaperClear,
    SideBarMenu,
  },
  data() {
    return {
      activeMenu: 'university',
      menuItems: [
        { name: 'university', label: '大学维护', component: 'UniversityManagement' },
        { name: 'department', label: '院系维护', component: 'DepartmentManagement' },
        { name: 'subject', label: '科目维护', component: 'SubjectManagement' },
        { name: 'role', label: '权限管理', component: 'RoleManagement' },
        { name: 'paperClear', label: '试卷清理', component: 'PaperClear' } // 新增菜单项
      ]
    }
  },
  computed: {
    activeComponent() {
      const item = this.menuItems.find(i => i.name === this.activeMenu)
      return item ? item.component : null
    }
  },
  methods: {
    setActiveMenu(menu) {
      this.activeMenu = menu
    }
  }
}
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
  margin-left: 290px; /* 与左侧菜单栏宽度一致 */
  width: calc(100% - 290px);
}

/* 调整顶部菜单样式 */
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
/* 调整内容区域样式 */
.main-content {
  flex: 1;
  background-color: #f5f5f5;
}


.content {
  flex: 1;
  padding: 20px;
  overflow: auto;
}
</style>