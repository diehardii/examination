<template>
  <div class="app-container">
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
import UserDetail from '@/common/components/UserDetail.vue'
import ChangePassword from '@/common/components/ChangePassword.vue'
import SideBarMenu from '@/common/components/SideBarMenu.vue'

export default {
  components: {
    UserDetail,
    ChangePassword,
    SideBarMenu
  },
  data() {
    return {
      activeMenu: 'userDetail',
      menuItems: [
        { name: 'userDetail', label: '用户资料变更', component: 'UserDetail' },
        { name: 'changePassword', label: '密码修改', component: 'ChangePassword' }
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

.content {
  flex: 1;
  padding: 20px;
  overflow: auto;
  background-color: #f5f5f5;
}
</style>