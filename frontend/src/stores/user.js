import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import request from '@/utils/request'
import { login, register, logout, getUserProfile } from '@/api/auth'
import { updateUserProfile, uploadAvatar as apiUploadAvatar } from '@/api/user'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(null)
  
  // 计算属性：是否已登录
  const isAuthenticated = computed(() => !!token.value)
  
  // 计算属性：用户信息
  const user = computed(() => userInfo.value)
  
  // 初始化函数，在应用启动时调用
  const initUser = async () => {
    if (token.value) {
      try {
        await fetchUserProfile()
        console.log('用户信息获取成功:', userInfo.value)
      } catch (error) {
        console.error('获取用户信息失败:', error)
        // 如果获取用户信息失败，可能是token过期，清除登录状态
        token.value = ''
        userInfo.value = null
        localStorage.removeItem('token')
      }
    }
  }

  // 登录
  const loginUser = async (credentials) => {
    try {
      const response = await login(credentials)
      token.value = response.token
      localStorage.setItem('token', response.token)
      
      userInfo.value = {
        id: response.id,
        username: response.username,
        email: response.email,
        avatar: response.avatar,
        roles: response.roles
      }
      
      return response
    } catch (error) {
      console.error('登录失败:', error)
      throw error
    }
  }

  // 注册
  const registerUser = async (userData) => {
    const response = await register(userData)
    return response
  }

  // 登出
  const logoutUser = async () => {
    try {
      await logout()
    } finally {
      token.value = ''
      userInfo.value = null
      localStorage.removeItem('token')
    }
  }

  // 获取用户信息
  const fetchUserProfile = async () => {
    console.log('开始获取用户信息，当前token:', token.value?.substring(0, 10) + '...')
    try {
      if (!token.value) {
        console.error('获取用户信息失败: token不存在')
        throw new Error('未登录')
      }

      const response = await getUserProfile()
      console.log('获取用户信息成功:', response)
      
      if (!response) {
        throw new Error('获取用户信息失败: 响应为空')
      }
      
      userInfo.value = response
      return response
    } catch (error) {
      console.error('获取用户信息失败:', error)
      
      // 处理不同类型的错误
      if (error.response?.status === 401) {
        console.error('认证失败，清除登录状态')
        token.value = ''
        userInfo.value = null
        localStorage.removeItem('token')
      } else if (error.response?.status === 403) {
        console.error('权限错误，可能是token无效或已过期')
      }
      
      throw error
    }
  }

  // 更新用户信息
  const updateProfile = async (profileData) => {
    const response = await updateUserProfile(profileData)
    if (response && userInfo.value) {
      Object.assign(userInfo.value, response)
    }
    return response
  }

  // 上传头像
  const uploadAvatar = async (formData) => {
    const response = await apiUploadAvatar(formData)
    if (userInfo.value && response.avatar) {
      userInfo.value.avatar = response.avatar
    }
    return response
  }

  // 修改密码
  const changePassword = async (passwordData) => {
    const response = await request.put('/api/v1/users/password', passwordData)
    return response.data
  }

  // 请求重置密码
  const requestPasswordReset = async (email) => {
    const response = await request.post('/api/v1/auth/password/reset-request', { email })
    return response.data
  }

  // 验证重置令牌
  const validateResetToken = async (token) => {
    const response = await request.get(`/api/v1/auth/password/reset-validate/${token}`)
    return response.data
  }

  // 重置密码
  const resetUserPassword = async (token, password) => {
    const response = await request.post('/api/v1/auth/password/reset', {
      token,
      password
    })
    return response.data
  }

  // 检查是否已登录（保留旧方法以兼容现有代码）
  const isLoggedIn = () => {
    return !!token.value
  }

  return {
    token,
    userInfo,
    user,
    isAuthenticated,
    initUser,
    login: loginUser,
    register: registerUser,
    logout: logoutUser,
    getUserProfile: fetchUserProfile,
    updateProfile,
    uploadAvatar,
    changePassword,
    requestPasswordReset,
    validateResetToken,
    resetPassword: resetUserPassword,
    isLoggedIn
  }
}) 