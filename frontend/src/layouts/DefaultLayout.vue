<template>
  <div class="default-layout">
    <!-- 顶部导航栏 -->
    <el-header class="header">
      <div class="logo-container">
        <router-link to="/">
          <h1 class="logo">知识管理平台</h1>
        </router-link>
      </div>
      
      <div class="header-menu">
        <el-menu
          mode="horizontal"
          :ellipsis="false"
          :router="true"
          class="horizontal-menu"
        >
          <el-menu-item index="/">首页</el-menu-item>
          <el-menu-item index="/documents">文档</el-menu-item>
        </el-menu>
      </div>
      
      <div class="header-right">
        <el-input
          v-model="searchQuery"
          placeholder="搜索内容..."
          class="search-input"
          @keyup.enter="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        
        <template v-if="userStore.isLoggedIn()">
          <el-dropdown trigger="click" @command="handleCommand">
            <div class="avatar-container">
              <el-avatar 
                :size="32" 
                :src="userInfo?.avatar || ''" 
                class="user-avatar"
              >
                {{ userInfo?.username?.charAt(0).toUpperCase() || 'U' }}
              </el-avatar>
              <span class="username">{{ userInfo?.username || '用户' }}</span>
              <el-icon><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="settings">设置</el-dropdown-item>
                <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
        
        <template v-else>
          <el-button-group>
            <el-button type="primary" @click="navigateTo('/login')">登录</el-button>
            <el-button @click="navigateTo('/register')">注册</el-button>
          </el-button-group>
        </template>
      </div>
    </el-header>
    
    <!-- 主体内容 -->
    <el-container class="main-container">
      <!-- 侧边栏 -->
      <el-aside width="240px" class="sidebar">
        <el-menu
          default-active="1"
          :router="true"
          class="sidebar-menu"
        >
          <el-sub-menu index="1">
            <template #title>
              <el-icon><Document /></el-icon>
              <span>文档管理</span>
            </template>
            <el-menu-item index="/documents">全部文档</el-menu-item>
            <el-menu-item index="/documents/recent">最近文档</el-menu-item>
            <el-menu-item index="/documents/favorites">收藏文档</el-menu-item>
          </el-sub-menu>
          
          <el-menu-item index="/tags">
            <el-icon><PriceTag /></el-icon>
            <span>标签管理</span>
          </el-menu-item>
          
          <el-menu-item index="/help">
            <el-icon><QuestionFilled /></el-icon>
            <span>帮助中心</span>
          </el-menu-item>
        </el-menu>
      </el-aside>
      
      <!-- 内容区域 -->
      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  Search,
  ArrowDown,
  Document,
  PriceTag,
  QuestionFilled
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const searchQuery = ref('')

const userInfo = computed(() => userStore.userInfo)

// 在组件挂载时获取用户信息
onMounted(async () => {
  if (userStore.isLoggedIn() && !userStore.userInfo) {
    try {
      await userStore.getUserProfile()
    } catch (error) {
      console.error('获取用户信息失败', error)
    }
  }
})

// 处理搜索
const handleSearch = () => {
  if (!searchQuery.value.trim()) return
  
  router.push({
    path: '/search',
    query: { q: searchQuery.value }
  })
  searchQuery.value = ''
}

// 处理下拉菜单命令
const handleCommand = async (command) => {
  switch (command) {
    case 'profile':
      router.push('/profile')
      break
    case 'settings':
      router.push('/settings')
      break
    case 'logout':
      await handleLogout()
      break
  }
}

// 处理退出登录
const handleLogout = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要退出登录吗？',
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await userStore.logout()
    ElMessage.success('已退出登录')
    router.push('/')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('退出登录失败', error)
    }
  }
}

// 页面导航
const navigateTo = (path) => {
  router.push(path)
}
</script>

<style lang="scss" scoped>
.default-layout {
  display: flex;
  flex-direction: column;
  height: 100vh;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  height: 60px;
  background-color: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  position: relative;
  z-index: 10;
}

.logo-container {
  a {
    text-decoration: none;
  }
  
  .logo {
    margin: 0;
    font-size: 20px;
    color: var(--el-color-primary);
  }
}

.header-menu {
  flex: 1;
  margin: 0 20px;
  
  .horizontal-menu {
    border-bottom: none;
  }
}

.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
  
  .search-input {
    width: 200px;
    transition: width 0.3s;
    
    &:focus-within {
      width: 280px;
    }
  }
  
  .avatar-container {
    display: flex;
    align-items: center;
    gap: 8px;
    cursor: pointer;
    padding: 6px 8px;
    border-radius: 4px;
    
    &:hover {
      background-color: var(--el-fill-color-light);
    }
    
    .username {
      font-size: 14px;
      color: var(--el-text-color-primary);
    }
  }
}

.main-container {
  flex: 1;
  overflow: hidden;
}

.sidebar {
  background-color: #fff;
  border-right: 1px solid var(--el-border-color-light);
  transition: width 0.3s;
  
  .sidebar-menu {
    height: 100%;
    border-right: none;
  }
}

.main-content {
  padding: 20px;
  overflow-y: auto;
  background-color: var(--el-fill-color-light);
}
</style> 