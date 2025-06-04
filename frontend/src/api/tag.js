import request from '@/utils/request'

export const tagApi = {
  // 获取所有标签
  getAll(params = {}) {
    return request({
      url: '/tags',
      method: 'get',
      params
    })
  },

  // 根据ID获取标签
  getById(id) {
    return request({
      url: `/tags/${id}`,
      method: 'get'
    })
  },

  // 创建标签
  create(data) {
    return request({
      url: '/tags',
      method: 'post',
      data
    })
  },

  // 更新标签
  update(id, data) {
    return request({
      url: `/tags/${id}`,
      method: 'put',
      data
    })
  },

  // 删除标签
  delete(id) {
    return request({
      url: `/tags/${id}`,
      method: 'delete'
    })
  },

  // 搜索标签
  search(query) {
    return request({
      url: '/tags/search',
      method: 'get',
      params: { q: query }
    })
  },

  // 获取标签统计信息
  getStats() {
    return request({
      url: '/tags/stats',
      method: 'get'
    })
  },

  // 获取热门标签
  getPopular(limit = 10) {
    return request({
      url: '/tags/popular',
      method: 'get',
      params: { limit }
    })
  }
} 