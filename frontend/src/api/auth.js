import request from '@/utils/request'

export const authApi = {
  // 用户登录
  login(credentials) {
    return request({
      url: '/auth/login',
      method: 'post',
      data: credentials
    })
  },

  // 用户注册
  register(userData) {
    return request({
      url: '/auth/register',
      method: 'post',
      data: userData
    })
  },

  // 用户登出
  logout() {
    return request({
      url: '/auth/logout',
      method: 'post'
    })
  },

  // 刷新token
  refreshToken() {
    return request({
      url: '/auth/refresh',
      method: 'post'
    })
  },

  // 获取当前用户信息
  getCurrentUser() {
    return request({
      url: '/users/profile',
      method: 'get'
    })
  },

  // 更新用户信息
  updateProfile(userData) {
    return request({
      url: '/auth/profile',
      method: 'put',
      data: userData
    })
  },

  // 修改密码
  changePassword(passwordData) {
    return request({
      url: '/auth/change-password',
      method: 'post',
      data: passwordData
    })
  },

  // 忘记密码
  forgotPassword(email) {
    return request({
      url: '/auth/forgot-password',
      method: 'post',
      data: { email }
    })
  },

  // 重置密码
  resetPassword(resetData) {
    return request({
      url: '/auth/reset-password',
      method: 'post',
      data: resetData
    })
  }
}

// 单独导出函数，供store使用
export const login = authApi.login
export const register = authApi.register
export const logout = authApi.logout
export const getUserProfile = authApi.getCurrentUser 