import request from '@/utils/request'

export const noteApi = {
  // 获取所有笔记
  getAll(params = {}) {
    return request({
      url: '/notes',
      method: 'get',
      params
    })
  },

  // 根据ID获取笔记
  getById(id) {
    return request({
      url: `/notes/${id}`,
      method: 'get'
    })
  },

  // 创建笔记
  create(data) {
    return request({
      url: '/notes',
      method: 'post',
      data
    })
  },

  // 更新笔记
  update(id, data) {
    return request({
      url: `/notes/${id}`,
      method: 'put',
      data
    })
  },

  // 删除笔记
  delete(id) {
    return request({
      url: `/notes/${id}`,
      method: 'delete'
    })
  },

  // 搜索笔记
  search(query, params = {}) {
    return request({
      url: '/notes/search',
      method: 'get',
      params: {
        q: query,
        ...params
      }
    })
  },

  // 根据标签获取笔记
  getByTag(tagId, params = {}) {
    return request({
      url: `/notes/tag/${tagId}`,
      method: 'get',
      params
    })
  },

  // 为笔记添加标签
  addTag(noteId, tagId) {
    return request({
      url: `/notes/${noteId}/tags`,
      method: 'post',
      data: { tagId }
    })
  },

  // 从笔记移除标签
  removeTag(noteId, tagId) {
    return request({
      url: `/notes/${noteId}/tags/${tagId}`,
      method: 'delete'
    })
  },

  // 获取笔记统计信息
  getStats() {
    return request({
      url: '/notes/stats',
      method: 'get'
    })
  },

  // 获取最近编辑的笔记
  getRecent(limit = 10) {
    return request({
      url: '/notes/recent',
      method: 'get',
      params: { limit }
    })
  },

  // 导出笔记为Markdown
  exportMarkdown(id) {
    return request({
      url: `/notes/${id}/export/markdown`,
      method: 'get',
      responseType: 'blob'
    })
  },

  // 导出笔记为PDF
  exportPdf(id) {
    return request({
      url: `/notes/${id}/export/pdf`,
      method: 'get',
      responseType: 'blob'
    })
  }
} 