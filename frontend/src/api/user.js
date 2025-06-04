import request from '@/utils/request'

export const userApi = {
  // 更新用户资料
  updateProfile(userData) {
    return request({
      url: '/users/profile',
      method: 'put',
      data: userData
    })
  },

  // 上传用户头像
  uploadAvatar(formData) {
    return request({
      url: '/users/avatar',
      method: 'post',
      data: formData,
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
  },

  // 获取用户资料
  getProfile() {
    return request({
      url: '/users/profile',
      method: 'get'
    })
  },

  // 修改密码
  changePassword(passwordData) {
    return request({
      url: '/users/change-password',
      method: 'post',
      data: passwordData
    })
  },

  // 获取用户设置
  getSettings() {
    return request({
      url: '/users/settings',
      method: 'get'
    })
  },

  // 更新用户设置
  updateSettings(settingsData) {
    return request({
      url: '/users/settings',
      method: 'put',
      data: settingsData
    })
  },

  // 删除用户账户
  deleteAccount() {
    return request({
      url: '/users/account',
      method: 'delete'
    })
  }
}

// 单独导出函数，供store使用
export const updateUserProfile = userApi.updateProfile
export const uploadAvatar = userApi.uploadAvatar
export const getUserSettings = userApi.getSettings
export const updateUserSettings = userApi.updateSettings 