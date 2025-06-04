import { defineStore } from 'pinia'
import { authApi } from '@/api/auth'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: null,
    token: null,
    isAuthenticated: false,
    loading: false
  }),
  
  getters: {
    userInitials: (state) => {
      if (!state.user || !state.user.username) return 'GU'
      return state.user.username.substring(0, 2).toUpperCase()
    },
    
    userName: (state) => {
      return state.user?.username || 'Guest'
    },
    
    userEmail: (state) => {
      return state.user?.email || 'guest@example.com'
    }
  },
  
  actions: {
    async login(credentials) {
      this.loading = true
      try {
        const response = await authApi.login(credentials)
        const { token, user } = response.data
        
        this.token = token
        this.user = user
        this.isAuthenticated = true
        
        // 保存到localStorage
        localStorage.setItem('token', token)
        localStorage.setItem('user', JSON.stringify(user))
        
        // 设置axios默认header
        this.setAuthHeader(token)
        
        return { success: true, user }
      } catch (error) {
        console.error('登录失败:', error)
        return { 
          success: false, 
          message: error.response?.data?.message || '登录失败，请检查用户名和密码' 
        }
      } finally {
        this.loading = false
      }
    },
    
    async register(userData) {
      this.loading = true
      try {
        const response = await authApi.register(userData)
        const { token, user } = response.data
        
        this.token = token
        this.user = user
        this.isAuthenticated = true
        
        // 保存到localStorage
        localStorage.setItem('token', token)
        localStorage.setItem('user', JSON.stringify(user))
        
        // 设置axios默认header
        this.setAuthHeader(token)
        
        return { success: true, user }
      } catch (error) {
        console.error('注册失败:', error)
        return { 
          success: false, 
          message: error.response?.data?.message || '注册失败，请稍后重试' 
        }
      } finally {
        this.loading = false
      }
    },
    
    async logout() {
      try {
        // 调用后端登出接口（如果需要）
        if (this.token) {
          await authApi.logout()
        }
      } catch (error) {
        console.error('登出请求失败:', error)
      } finally {
        // 清除本地状态
        this.clearAuth()
      }
    },
    
    clearAuth() {
      this.user = null
      this.token = null
      this.isAuthenticated = false
      
      // 清除localStorage
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      
      // 清除axios默认header
      this.clearAuthHeader()
    },
    
    async refreshToken() {
      try {
        const response = await authApi.refreshToken()
        const { token } = response.data
        
        this.token = token
        localStorage.setItem('token', token)
        this.setAuthHeader(token)
        
        return true
      } catch (error) {
        console.error('刷新token失败:', error)
        this.clearAuth()
        return false
      }
    },
    
    async getCurrentUser() {
      if (!this.token) return null
      
      try {
        const response = await authApi.getCurrentUser()
        this.user = response.data
        localStorage.setItem('user', JSON.stringify(this.user))
        return this.user
      } catch (error) {
        console.error('获取用户信息失败:', error)
        this.clearAuth()
        return null
      }
    },
    
    async updateProfile(userData) {
      this.loading = true
      try {
        const response = await authApi.updateProfile(userData)
        this.user = { ...this.user, ...response.data }
        localStorage.setItem('user', JSON.stringify(this.user))
        
        return { success: true, user: this.user }
      } catch (error) {
        console.error('更新用户信息失败:', error)
        return { 
          success: false, 
          message: error.response?.data?.message || '更新失败，请稍后重试' 
        }
      } finally {
        this.loading = false
      }
    },
    
    setAuthHeader(token) {
      if (token) {
        // 这里需要导入axios实例并设置header
        // axios.defaults.headers.common['Authorization'] = `Bearer ${token}`
      }
    },
    
    clearAuthHeader() {
      // 清除axios默认header
      // delete axios.defaults.headers.common['Authorization']
    },
    
    initAuth() {
      // 从localStorage恢复认证状态
      const token = localStorage.getItem('token')
      const userStr = localStorage.getItem('user')
      
      if (token && userStr) {
        try {
          this.token = token
          this.user = JSON.parse(userStr)
          this.isAuthenticated = true
          this.setAuthHeader(token)
          
          // 验证token是否仍然有效
          this.getCurrentUser()
        } catch (error) {
          console.error('恢复认证状态失败:', error)
          this.clearAuth()
        }
      }
    },
    
    // 检查token是否即将过期
    checkTokenExpiry() {
      if (!this.token) return false
      
      try {
        // 解析JWT token获取过期时间
        const payload = JSON.parse(atob(this.token.split('.')[1]))
        const currentTime = Date.now() / 1000
        
        // 如果token在5分钟内过期，尝试刷新
        if (payload.exp - currentTime < 300) {
          this.refreshToken()
        }
        
        return payload.exp > currentTime
      } catch (error) {
        console.error('检查token过期时间失败:', error)
        return false
      }
    }
  }
}) 