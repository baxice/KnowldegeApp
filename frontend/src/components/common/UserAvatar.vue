<template>
  <div class="user-avatar-container" ref="userAvatarRef">
    <!-- 用户头像按钮 -->
    <div class="user-avatar-button" @click="toggleDropdown">
      <div class="user-avatar" :class="{ 'guest': !isAuthenticated }">
        {{ userInitials }}
      </div>
      <div class="user-info" v-if="isAuthenticated">
        <div class="user-name">{{ userName }}</div>
        <div class="user-email">{{ userEmail }}</div>
      </div>
      <div class="user-info" v-else>
        <div class="user-name">游客</div>
        <div class="user-email">点击登录</div>
      </div>
      <i class="fas fa-caret-down dropdown-arrow" :class="{ 'open': showDropdown }"></i>
    </div>
    
    <!-- 下拉菜单 -->
    <div class="user-dropdown" v-show="showDropdown" @click.stop>
      <!-- 未登录用户菜单 -->
      <template v-if="!isAuthenticated">
        <div class="dropdown-header">
          <div class="guest-avatar">
            <i class="fas fa-user-circle"></i>
          </div>
          <div class="guest-info">
            <div class="guest-title">欢迎访问</div>
            <div class="guest-subtitle">请登录以使用完整功能</div>
          </div>
        </div>
        <div class="dropdown-divider"></div>
        <div class="dropdown-item" @click="goToLogin">
          <i class="fas fa-sign-in-alt"></i>
          <span>登录</span>
        </div>
        <div class="dropdown-item" @click="goToRegister">
          <i class="fas fa-user-plus"></i>
          <span>注册</span>
        </div>
      </template>
      
      <!-- 已登录用户菜单 -->
      <template v-else>
        <div class="dropdown-header">
          <div class="user-avatar-large">
            {{ userInitials }}
          </div>
          <div class="user-details">
            <div class="user-name-large">{{ userName }}</div>
            <div class="user-email-large">{{ userEmail }}</div>
          </div>
        </div>
        <div class="dropdown-divider"></div>
        <div class="dropdown-item" @click="goToProfile">
          <i class="fas fa-user"></i>
          <span>个人资料</span>
        </div>
        <div class="dropdown-item" @click="goToSettings">
          <i class="fas fa-cog"></i>
          <span>设置</span>
        </div>
        <div class="dropdown-item" v-if="isAdmin" @click="goToAdmin">
          <i class="fas fa-shield-alt"></i>
          <span>管理后台</span>
        </div>
        <div class="dropdown-divider"></div>
        <div class="dropdown-item logout" @click="handleLogout">
          <i class="fas fa-sign-out-alt"></i>
          <span>退出登录</span>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const authStore = useAuthStore()
const userStore = useUserStore()

// 响应式数据
const showDropdown = ref(false)
const userAvatarRef = ref(null)

// 计算属性
const isAuthenticated = computed(() => authStore.isAuthenticated || userStore.isAuthenticated)
const userName = computed(() => authStore.user?.username || userStore.user?.username || '游客')
const userEmail = computed(() => authStore.user?.email || userStore.user?.email || 'guest@example.com')
const userInitials = computed(() => {
  if (!isAuthenticated.value) return '?'
  const name = userName.value
  return name ? name.substring(0, 2).toUpperCase() : 'GU'
})

const isAdmin = computed(() => {
  const user = authStore.user || userStore.user
  return user?.roles?.includes('ADMIN') || false
})

// 方法
const toggleDropdown = () => {
  showDropdown.value = !showDropdown.value
  
  // 如果菜单打开，调整位置
  if (showDropdown.value) {
    setTimeout(() => {
      adjustDropdownPosition()
    }, 0)
  }
}

const closeDropdown = () => {
  showDropdown.value = false
}

// 调整下拉菜单位置，确保不溢出
const adjustDropdownPosition = () => {
  if (!userAvatarRef.value) return
  
  const dropdown = userAvatarRef.value.querySelector('.user-dropdown')
  if (!dropdown) return
  
  const rect = userAvatarRef.value.getBoundingClientRect()
  const dropdownRect = dropdown.getBoundingClientRect()
  const sidebarWidth = 280 // var(--sidebar-width)
  
  // 重置样式
  dropdown.style.left = '0px'
  dropdown.style.right = 'auto'
  
  // 检查是否超出侧边栏右边界
  if (rect.left + dropdownRect.width > sidebarWidth) {
    dropdown.style.left = 'auto'
    dropdown.style.right = '0px'
  }
  
  // 检查是否超出屏幕上边界
  if (rect.top - dropdownRect.height < 0) {
    dropdown.style.bottom = 'auto'
    dropdown.style.top = '100%'
    dropdown.style.marginTop = '8px'
    dropdown.style.marginBottom = '0'
  }
}

const goToLogin = () => {
  closeDropdown()
  router.push('/login')
}

const goToRegister = () => {
  closeDropdown()
  router.push('/register')
}

const goToProfile = () => {
  closeDropdown()
  // TODO: 实现个人资料页面
  ElMessage.info('个人资料功能正在开发中')
}

const goToSettings = () => {
  closeDropdown()
  // TODO: 实现设置页面
  ElMessage.info('设置功能正在开发中')
}

const goToAdmin = () => {
  closeDropdown()
  router.push('/admin')
}

const handleLogout = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要退出登录吗？',
      '确认退出',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )
    
    closeDropdown()
    
    // 优先使用userStore的登出方法
    if (userStore.logoutUser) {
      await userStore.logoutUser()
    } else if (authStore.logout) {
      await authStore.logout()
    }
    
    ElMessage.success('已退出登录')
    
    // 刷新页面或重定向到首页
    window.location.reload()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('退出登录失败:', error)
      ElMessage.error('退出登录失败，请重试')
    }
  }
}

// 点击外部关闭下拉菜单
const handleClickOutside = (event) => {
  if (userAvatarRef.value && !userAvatarRef.value.contains(event.target)) {
    closeDropdown()
  }
}

// 生命周期
onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<style scoped>
.user-avatar-container {
  position: relative;
}

.user-avatar-button {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  padding: 8px 12px;
  border-radius: 8px;
  transition: background-color 0.2s ease;
}

.user-avatar-button:hover {
  background-color: var(--surface-color-2, rgba(255, 255, 255, 0.05));
}

.user-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: linear-gradient(45deg, var(--color-blue, #66d9ff), var(--color-purple, #a855f7));
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  color: white;
  font-size: 14px;
  flex-shrink: 0;
}

.user-avatar.guest {
  background: linear-gradient(45deg, #6b7280, #9ca3af);
}

.user-info {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.user-name {
  font-weight: 600;
  font-size: 14px;
  color: var(--text-color, #f2f2f0);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-email {
  font-size: 12px;
  color: var(--text-muted, #888);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.dropdown-arrow {
  color: var(--text-muted, #888);
  font-size: 12px;
  transition: transform 0.2s ease;
  flex-shrink: 0;
}

.dropdown-arrow.open {
  transform: rotate(180deg);
}

.user-dropdown {
  position: absolute;
  bottom: 100%;
  left: 0;
  margin-bottom: 8px;
  background-color: var(--surface-color, #242730);
  border: 1px solid var(--border-color, #404040);
  border-radius: 8px;
  box-shadow: 0 -8px 32px rgba(0, 0, 0, 0.3);
  width: 260px;
  z-index: 1000;
  overflow: hidden;
  /* 确保菜单不会超出侧边栏 */
  max-width: calc(var(--sidebar-width, 280px) - 20px);
  /* 添加动画效果 */
  animation: dropdownSlideUp 0.2s ease-out;
}

@keyframes dropdownSlideUp {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.dropdown-header {
  padding: 16px;
  background-color: var(--surface-color-2, rgba(255, 255, 255, 0.02));
  display: flex;
  align-items: center;
  gap: 12px;
}

.guest-avatar {
  font-size: 48px;
  color: var(--text-muted, #888);
}

.guest-info {
  flex: 1;
}

.guest-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-color, #f2f2f0);
  margin-bottom: 4px;
}

.guest-subtitle {
  font-size: 12px;
  color: var(--text-muted, #888);
}

.user-avatar-large {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: linear-gradient(45deg, var(--color-blue, #66d9ff), var(--color-purple, #a855f7));
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  color: white;
  font-size: 18px;
  flex-shrink: 0;
}

.user-details {
  flex: 1;
}

.user-name-large {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-color, #f2f2f0);
  margin-bottom: 4px;
}

.user-email-large {
  font-size: 12px;
  color: var(--text-muted, #888);
}

.dropdown-divider {
  height: 1px;
  background-color: var(--border-color, #404040);
  margin: 0;
}

.dropdown-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  cursor: pointer;
  transition: background-color 0.2s ease;
  font-size: 14px;
  color: var(--text-color, #f2f2f0);
}

.dropdown-item:hover {
  background-color: var(--surface-color-2, rgba(255, 255, 255, 0.05));
}

.dropdown-item.logout {
  color: var(--color-red, #ef4444);
}

.dropdown-item.logout:hover {
  background-color: rgba(239, 68, 68, 0.1);
}

.dropdown-item i {
  width: 16px;
  text-align: center;
  color: inherit;
}

/* 响应式调整 */
@media (max-width: 768px) {
  .user-dropdown {
    width: calc(100vw - 40px);
    max-width: 240px;
    left: 0;
    right: auto;
  }
  
  .user-info {
    display: none;
  }
  
  .user-avatar-button {
    padding: 8px;
    justify-content: center;
  }
}
</style> 