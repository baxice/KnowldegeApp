<template>
  <div id="app">
    <router-view />
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useThemeStore } from '@/stores/theme'
import { useAuthStore } from '@/stores/auth'

const themeStore = useThemeStore()
const authStore = useAuthStore()

onMounted(() => {
  // 初始化主题
  themeStore.initTheme()
  
  // 初始化认证状态
  authStore.initAuth()
})
</script>

<style>
/* 全局样式已在obsidian-theme.css中定义 */
#app {
  height: 100vh;
  overflow: hidden;
}
</style> 