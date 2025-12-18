<template>
  <div class="container">
    <Sidebar />
    <div class="main-content">
      <img src="@/assets/images/home.jpg" alt="系统功能示意图">
    </div>
  </div>
</template>

<script>
import Sidebar from '@/common/components/SideBarMenu.vue';
import { onMounted, onUnmounted } from 'vue'
import { useAuthStore } from '@/common/stores/auth'
import { useRouter } from 'vue-router'
// 暂时注释掉首页自动分析功能，避免每次打开Home时调用Coze
// import { startHomepageTutoringAnalysis } from '@/cet4/service/CET4tutoringAnalysisService'

export default {
  name: 'HomeView',
  components: {
    Sidebar
  },
 setup() {
      const authStore = useAuthStore()
      const router = useRouter()


      const handleMessage = (event) => {
        // 验证消息来源
        if (event.origin !== 'http://localhost:5173') return

        if (event.data.type === 'RETURN_FROM_ANALYSIS') {
          const token = event.data.token
          const isValid = authStore.verifyCrossSystemToken(token)

          if (isValid) {
            router.push('/home/from-analysis')
          } else {
            alert('会话已过期，请重新登录')
            router.push('/auth/login')
          }
        }
      }

      onMounted(() => {
        window.addEventListener('message', handleMessage)
        // 暂时注释掉首页自动分析功能
        // startHomepageTutoringAnalysis()
      })

      onUnmounted(() => {
        window.removeEventListener('message', handleMessage)
      })



  }


};
</script>

<style scoped>
.container {
  display: flex;
  min-height: 100vh;
}

.main-content {
  flex-grow: 1;
  padding: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  height: calc(100vh - 40px);
  margin-left: 260px; /* 等于侧边栏宽度 */
  width: calc(100% - 260px);
}

.main-content img {
  width: 800px;
  height: 600px;
  object-fit: contain;
  border-radius: 8px;
  box-shadow: 0 0 10px rgba(0,0,0,0.1);
}
</style>