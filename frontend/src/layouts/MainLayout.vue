<template>
  <div class="main-layout">
    <!-- 顶部导航栏 -->
    <header class="header">
      <div class="header-left">
        <h1 class="logo">知识库</h1>
      </div>
      <div class="header-center">
        <el-input
          v-model="searchQuery"
          placeholder="搜索文档..."
          class="search-input"
          clearable
        >
          <template #prefix>
            <el-icon><search /></el-icon>
          </template>
        </el-input>
      </div>
      <div class="header-right">
        <template v-if="userStore.isAuthenticated">
          <el-dropdown>
            <el-avatar :size="32" :src="userStore.user?.avatar">
              {{ userStore.user?.username?.charAt(0) }}
            </el-avatar>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="handleProfile">个人中心</el-dropdown-item>
                <el-dropdown-item @click="handleSettings">设置</el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
        <template v-else>
          <el-button type="primary" @click="handleLogin">登录</el-button>
          <el-button @click="handleRegister">注册</el-button>
        </template>
      </div>
    </header>

    <!-- 主要内容区 -->
    <div class="main-content">
      <!-- 左侧导航 -->
      <aside class="sidebar">
        <el-menu
          default-active="1"
          class="sidebar-menu"
        >
          <el-menu-item index="1">
            <el-icon><document /></el-icon>
            <span>我的文档</span>
          </el-menu-item>
          <el-menu-item index="2">
            <el-icon><star /></el-icon>
            <span>收藏夹</span>
          </el-menu-item>
          <el-menu-item index="3">
            <el-icon><clock /></el-icon>
            <span>最近文档</span>
          </el-menu-item>
        </el-menu>
      </aside>

      <!-- 中间内容区 -->
      <main class="content">
        <router-view></router-view>
      </main>

      <!-- 右侧预览/属性面板 -->
      <aside class="preview-panel" v-if="showPreview">
        <div class="preview-header">
          <h3>预览</h3>
          <el-button type="text" @click="closePreview">
            <el-icon><close /></el-icon>
          </el-button>
        </div>
        <div class="preview-content">
          <!-- 预览内容 -->
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { Search, Document, Star, Clock, Close } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()
const searchQuery = ref('')
const showPreview = ref(false)

// 处理登录
const handleLogin = () => {
  router.push('/login')
}

// 处理注册
const handleRegister = () => {
  router.push('/register')
}

// 处理个人中心
const handleProfile = () => {
  router.push('/profile')
}

// 处理设置
const handleSettings = () => {
  router.push('/settings')
}

// 处理登出
const handleLogout = async () => {
  try {
    await userStore.userLogout()
    router.push('/')
  } catch (error) {
    console.error('登出失败:', error)
  }
}

// 关闭预览面板
const closePreview = () => {
  showPreview.value = false
}
</script>

<style scoped>
.main-layout {
  height: 100vh;
  display: flex;
  flex-direction: column;
}

.header {
  height: 60px;
  padding: 0 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid var(--el-border-color-light);
  background-color: var(--el-bg-color);
}

.header-left {
  display: flex;
  align-items: center;
}

.logo {
  font-size: 24px;
  margin: 0;
  color: var(--el-color-primary);
}

.header-center {
  flex: 1;
  max-width: 600px;
  margin: 0 20px;
}

.search-input {
  width: 100%;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.main-content {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.sidebar {
  width: 240px;
  border-right: 1px solid var(--el-border-color-light);
  background-color: var(--el-bg-color);
}

.sidebar-menu {
  border-right: none;
}

.content {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  background-color: var(--el-bg-color-page);
}

.preview-panel {
  width: 320px;
  border-left: 1px solid var(--el-border-color-light);
  background-color: var(--el-bg-color);
  display: flex;
  flex-direction: column;
}

.preview-header {
  height: 48px;
  padding: 0 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid var(--el-border-color-light);
}

.preview-content {
  flex: 1;
  padding: 16px;
  overflow-y: auto;
}
</style> 