<template>
  <div class="auth-container">
    <router-view></router-view>
  </div>
</template>

<script setup>
// 不需要额外逻辑，router-view会自动渲染匹配的子路由
</script>

<style scoped>
.auth-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  width: 100vw;
  background-color: #f0f2f5;
}
</style>
<style scoped>
.auth-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
}
</style>