<template>
  <div class="app-container">
    <SideBarMenu />
    <div class="main-layout">
      <!-- 顶部菜单 -->
      <div class="top-menu">
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
import SideBarMenu from '@/components/SideBarMenu.vue'
import WrongExamPaperGen from '@/components/WrongExamPaperGen.vue'
import PaperGenFromWrong from '@/components/PaperGenFromWrong.vue'

export default {
  name: 'WrongQuestionManView',
  components: {
    SideBarMenu,
    WrongExamPaperGen,
    PaperGenFromWrong
  },
  data() {
    return {
      activeMenu: 'generateFromWrong',
      menuItems: [
        {
          name: 'generateFromWrong',
          label: '从错题库中生成错题集试卷',
          component: 'WrongExamPaperGen'
        },
        {
          name: 'generateSimilar',
          label: '根据错题集AI生成类似试卷',
          component: 'PaperGenFromWrong'
        }
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
  margin-left: 290px;
  width: calc(100% - 290px);
}

.top-menu {
  display: flex;
  padding: 0 20px;
  height: 60px;
  background-color: #3498db;
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
  background-color: #f5f5f5;
  overflow: auto;
}
</style>