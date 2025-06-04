import request from '@/utils/request'

export const documentApi = {
  // 获取所有文档
  getAll(params = {}) {
    return request({
      url: '/documents',
      method: 'get',
      params
    })
  },

  // 根据ID获取文档
  getById(id) {
    return request({
      url: `/documents/${id}`,
      method: 'get'
    })
  },

  // 创建新文档
  create(data) {
    return request({
      url: '/documents/create',
      method: 'post',
      data
    })
  },

  // 上传文档
  upload(file, onUploadProgress) {
    const formData = new FormData()
    formData.append('file', file)
    
    return request({
      url: '/documents/upload',
      method: 'post',
      data: formData,
      headers: {
        'Content-Type': 'multipart/form-data'
      },
      onUploadProgress
    })
  },

  // 批量上传文档
  uploadMultiple(files, onUploadProgress) {
    const formData = new FormData()
    files.forEach(file => {
      formData.append('files', file)
    })
    
    return request({
      url: '/documents/upload/multiple',
      method: 'post',
      data: formData,
      headers: {
        'Content-Type': 'multipart/form-data'
      },
      onUploadProgress
    })
  },

  // 更新文档信息
  update(id, data) {
    return request({
      url: `/documents/${id}`,
      method: 'put',
      data
    })
  },

  // 更新文档内容
  updateContent(id, data) {
    return request({
      url: `/documents/${id}/content`,
      method: 'put',
      data
    })
  },

  // 删除文档
  delete(id) {
    return request({
      url: `/documents/${id}`,
      method: 'delete'
    })
  },

  // 下载文档
  download(id) {
    return request({
      url: `/documents/${id}/download`,
      method: 'get',
      responseType: 'blob'
    })
  },

  // 获取文档预览URL
  getPreviewUrl(id) {
    return request({
      url: `/documents/${id}/preview`,
      method: 'get'
    })
  },

  // 搜索文档
  search(query, params = {}) {
    return request({
      url: '/documents/search',
      method: 'get',
      params: {
        q: query,
        ...params
      }
    })
  },

  // 根据标签获取文档
  getByTag(tagId, params = {}) {
    return request({
      url: `/documents/tag/${tagId}`,
      method: 'get',
      params
    })
  },

  // 为文档添加标签
  addTag(documentId, tagId) {
    return request({
      url: `/documents/${documentId}/tags`,
      method: 'post',
      data: { tagId }
    })
  },

  // 从文档移除标签
  removeTag(documentId, tagId) {
    return request({
      url: `/documents/${documentId}/tags/${tagId}`,
      method: 'delete'
    })
  },

  // 获取文档统计信息
  getStats() {
    return request({
      url: '/documents/stats',
      method: 'get'
    })
  },

  // 获取最近访问的文档
  getRecent(limit = 10) {
    return request({
      url: '/documents/recent',
      method: 'get',
      params: { limit }
    })
  }
} 