import request from '@/utils/request'

// 获取日志文件列表
export function getLogFiles() {
  return request({
    url: '/logs/files',
    method: 'get'
  })
}

// 读取日志文件内容
export function readLogFile(fileName, page = 1, size = 100) {
  return request({
    url: '/logs/content',
    method: 'get',
    params: {
      fileName,
      page,
      size
    }
  })
}

// 搜索日志内容
export function searchLogs(fileName, keyword) {
  return request({
    url: '/logs/search',
    method: 'get',
    params: {
      fileName,
      keyword
    }
  })
}

// 获取今日日志统计
export function getTodayStats() {
  return request({
    url: '/logs/stats/today',
    method: 'get'
  })
}

// 获取日志文件详细信息
export function getLogFileInfo(fileName) {
  return request({
    url: '/logs/info',
    method: 'get',
    params: {
      fileName
    }
  })
}

// 清理过期日志文件
export function cleanOldLogs(daysToKeep = 30) {
  return request({
    url: '/logs/clean',
    method: 'delete',
    params: {
      daysToKeep
    }
  })
} 