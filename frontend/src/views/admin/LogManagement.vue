<template>
  <div class="log-management">
    <!-- 头部工具栏 -->
    <div class="log-header">
      <div class="header-left">
        <h2 class="page-title">日志管理</h2>
        <el-button @click="refreshStats" :loading="statsLoading" type="primary" size="small">
          <i class="el-icon-refresh"></i> 刷新统计
        </el-button>
      </div>
      <div class="header-right">
        <el-button @click="showCleanDialog = true" type="warning" size="small">
          <i class="el-icon-delete"></i> 清理日志
        </el-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-cards">
      <div class="stat-card">
        <div class="stat-icon application">
          <i class="el-icon-document"></i>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ logStats.applicationLogs || 0 }}</div>
          <div class="stat-label">应用日志</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon error">
          <i class="el-icon-warning"></i>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ logStats.errorLogs || 0 }}</div>
          <div class="stat-label">错误日志</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon operation">
          <i class="el-icon-operation"></i>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ logStats.operationLogs || 0 }}</div>
          <div class="stat-label">操作日志</div>
        </div>
      </div>
    </div>

    <!-- 主要内容区域 -->
    <div class="log-content">
      <!-- 左侧文件列表 -->
      <div class="file-list-panel">
        <div class="panel-header">
          <h3>日志文件</h3>
          <el-button @click="loadLogFiles" :loading="filesLoading" size="mini" type="text">
            <i class="el-icon-refresh"></i>
          </el-button>
        </div>
        <div class="file-list">
          <div 
            v-for="file in logFiles" 
            :key="file.name"
            :class="['file-item', { active: selectedFile?.name === file.name }]"
            @click="selectFile(file)"
          >
            <div class="file-icon" :class="file.type">
              <i :class="getFileIcon(file.type)"></i>
            </div>
            <div class="file-info">
              <div class="file-name">{{ file.name }}</div>
              <div class="file-meta">
                <span class="file-size">{{ formatFileSize(file.size) }}</span>
                <span class="file-time">{{ formatTime(file.lastModified) }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧日志内容 -->
      <div class="log-viewer-panel">
        <div class="panel-header">
          <div class="header-left">
            <h3 v-if="selectedFile">{{ selectedFile.name }}</h3>
            <span v-else class="no-file-text">请选择日志文件</span>
          </div>
          <div class="header-right" v-if="selectedFile">
            <el-input
              v-model="searchKeyword"
              placeholder="搜索日志内容..."
              size="small"
              style="width: 300px; margin-right: 10px;"
              @keyup.enter="searchLogs"
            >
              <template #suffix>
                <el-button @click="searchLogs" :loading="searchLoading" type="text" size="small">
                  <i class="el-icon-search"></i>
                </el-button>
              </template>
            </el-input>
            <el-button @click="loadLogContent" :loading="contentLoading" size="small">
              <i class="el-icon-refresh"></i> 刷新
            </el-button>
          </div>
        </div>
        
        <div class="log-content-area" v-if="selectedFile">
          <!-- 搜索结果 -->
          <div v-if="searchResults.length > 0" class="search-results">
            <div class="search-header">
              <span>搜索结果: {{ searchResults.length }} 条匹配</span>
              <el-button @click="clearSearch" size="mini" type="text">清除搜索</el-button>
            </div>
            <div class="search-list">
              <div 
                v-for="result in searchResults" 
                :key="result.lineNumber"
                class="search-item"
                @click="scrollToLine(result.lineNumber)"
              >
                <span class="line-number">{{ result.lineNumber }}</span>
                <span class="log-line" v-html="highlightKeyword(result.content)"></span>
              </div>
            </div>
          </div>
          
          <!-- 日志内容 -->
          <div v-else class="log-lines" ref="logContainer">
            <div 
              v-for="(line, index) in logLines" 
              :key="index"
              :class="['log-line', getLogLevelClass(line)]"
            >
              <span class="line-number">{{ currentPage > 1 ? ((currentPage - 1) * pageSize + index + 1) : (index + 1) }}</span>
              <span class="log-content">{{ line }}</span>
            </div>
          </div>
          
          <!-- 分页 -->
          <div class="log-pagination" v-if="!searchResults.length">
            <el-pagination
              v-model:current-page="currentPage"
              :page-size="pageSize"
              :total="totalLines"
              layout="prev, pager, next, jumper, total"
              @current-change="handlePageChange"
              small
            />
          </div>
        </div>
        
        <div v-else class="no-content">
          <i class="el-icon-document"></i>
          <p>选择一个日志文件查看内容</p>
        </div>
      </div>
    </div>

    <!-- 清理日志对话框 -->
    <el-dialog v-model="showCleanDialog" title="清理日志文件" width="400px">
      <div>
        <p>请选择要保留的日志天数：</p>
        <el-input-number 
          v-model="keepDays" 
          :min="1" 
          :max="365" 
          style="width: 100%;"
        />
        <p class="warning-text" style="margin-top: 10px; color: #f56c6c; font-size: 12px;">
          * 将删除 {{ keepDays }} 天前的所有日志文件，此操作不可恢复
        </p>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showCleanDialog = false">取消</el-button>
          <el-button type="primary" @click="confirmCleanLogs" :loading="cleanLoading">
            确认清理
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getLogFiles, readLogFile, searchLogs as searchLogsApi, getTodayLogStats, cleanOldLogs, getTodayStats, getLogFileInfo } from '@/api/log'

// 响应式数据
const logFiles = ref([])
const selectedFile = ref(null)
const logLines = ref([])
const searchResults = ref([])
const searchKeyword = ref('')
const logStats = reactive({
  applicationLogs: 0,
  errorLogs: 0,
  operationLogs: 0
})

// 分页数据
const currentPage = ref(1)
const pageSize = ref(100)
const totalLines = ref(0)

// 加载状态
const filesLoading = ref(false)
const contentLoading = ref(false)
const searchLoading = ref(false)
const statsLoading = ref(false)
const cleanLoading = ref(false)

// 对话框状态
const showCleanDialog = ref(false)
const keepDays = ref(30)

// DOM引用
const logContainer = ref(null)

// 组件挂载
onMounted(() => {
  loadLogFiles()
  loadTodayStats()
})

// 加载日志文件列表
const loadLogFiles = async () => {
  filesLoading.value = true
  try {
    const response = await getLogFiles()
    logFiles.value = response.data
  } catch (error) {
    ElMessage.error('加载日志文件列表失败: ' + error.message)
  } finally {
    filesLoading.value = false
  }
}

// 选择文件
const selectFile = (file) => {
  selectedFile.value = file
  currentPage.value = 1
  clearSearch()
  loadLogContent()
}

// 加载日志内容
const loadLogContent = async () => {
  if (!selectedFile.value) return
  
  contentLoading.value = true
  try {
    const response = await readLogFile(selectedFile.value.name, currentPage.value, pageSize.value)
    if (response.data.success) {
      logLines.value = response.data.content
      totalLines.value = response.data.totalLines
    } else {
      ElMessage.error(response.data.message)
    }
  } catch (error) {
    ElMessage.error('加载日志内容失败: ' + error.message)
  } finally {
    contentLoading.value = false
  }
}

// 搜索日志
const searchLogs = async () => {
  if (!selectedFile.value || !searchKeyword.value.trim()) return
  
  searchLoading.value = true
  try {
    const response = await searchLogsApi(selectedFile.value.name, searchKeyword.value.trim())
    if (response.data.success) {
      searchResults.value = response.data.matches
      ElMessage.success(`找到 ${response.data.matches.length} 条匹配结果`)
    } else {
      ElMessage.error(response.data.message)
    }
  } catch (error) {
    ElMessage.error('搜索日志失败: ' + error.message)
  } finally {
    searchLoading.value = false
  }
}

// 清除搜索
const clearSearch = () => {
  searchResults.value = []
  searchKeyword.value = ''
}

// 加载今日统计
const loadTodayStats = async () => {
  try {
    const response = await getTodayStats()
    if (response.data.success) {
      todayStats.value = response.data
    } else {
      ElMessage.error(response.data.message)
    }
  } catch (error) {
    ElMessage.error('加载统计数据失败: ' + error.message)
  }
}

// 刷新统计
const refreshStats = () => {
  loadTodayStats()
}

// 分页变化处理
const handlePageChange = (page) => {
  currentPage.value = page
  loadLogContent()
}

// 确认清理日志
const confirmCleanLogs = async () => {
  try {
    await ElMessageBox.confirm(
      `确定要删除 ${keepDays.value} 天前的日志文件吗？此操作不可恢复！`,
      '警告',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )
    
    cleanLoading.value = true
    const response = await cleanOldLogs(keepDays.value)
    if (response.data.success) {
      ElMessage.success(response.data.message)
      showCleanDialog.value = false
      loadLogFiles() // 重新加载文件列表
    } else {
      ElMessage.error(response.data.message)
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('清理日志失败: ' + error.message)
    }
  } finally {
    cleanLoading.value = false
  }
}

// 查看文件详情
const showFileInfo = async (file) => {
  try {
    const response = await getLogFileInfo(file.name)
    if (response.data.success) {
      ElMessageBox.alert(
        `文件名: ${response.data.name}
文件大小: ${(response.data.size / 1024 / 1024).toFixed(2)} MB
总行数: ${response.data.totalLines}
最后修改: ${new Date(response.data.lastModified).toLocaleString()}
日志级别统计: ${JSON.stringify(response.data.levelStats, null, 2)}`,
        '文件详情',
        {
          confirmButtonText: '确定',
          type: 'info'
        }
      )
    } else {
      ElMessage.error(response.data.message)
    }
  } catch (error) {
    ElMessage.error('获取文件信息失败: ' + error.message)
  }
}

// 清理过期日志
const cleanLogs = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要清理30天前的过期日志文件吗？此操作不可恢复！',
      '确认清理',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    const response = await cleanOldLogs(30)
    if (response.data.success) {
      ElMessage.success(response.data.message)
      loadLogFiles() // 重新加载文件列表
    } else {
      ElMessage.error(response.data.message)
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('清理日志失败: ' + error.message)
    }
  }
}

// 工具函数
const getFileIcon = (type) => {
  switch (type) {
    case 'error': return 'el-icon-warning'
    case 'operation': return 'el-icon-operation'
    default: return 'el-icon-document'
  }
}

const formatFileSize = (size) => {
  if (size < 1024) return size + ' B'
  if (size < 1024 * 1024) return (size / 1024).toFixed(1) + ' KB'
  return (size / (1024 * 1024)).toFixed(1) + ' MB'
}

const formatTime = (timestamp) => {
  return new Date(timestamp).toLocaleString()
}

const getLogLevelClass = (line) => {
  if (line.includes('ERROR')) return 'error'
  if (line.includes('WARN')) return 'warn'
  if (line.includes('INFO')) return 'info'
  if (line.includes('DEBUG')) return 'debug'
  return ''
}

const highlightKeyword = (content) => {
  if (!searchKeyword.value) return content
  const regex = new RegExp(`(${searchKeyword.value})`, 'gi')
  return content.replace(regex, '<mark>$1</mark>')
}

const scrollToLine = (lineNumber) => {
  ElMessage.info(`跳转到第 ${lineNumber} 行（功能开发中）`)
}
</script>

<style scoped>
.log-management {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--bg-dark, #1a1a1a);
  color: var(--text-dark, #ffffff);
}

.log-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  background: var(--surface-dark, #2a2a2a);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.page-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  background: linear-gradient(135deg, var(--color-purple, #b794f6) 0%, var(--color-blue, #63b3ed) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.stats-cards {
  display: flex;
  gap: 16px;
  padding: 16px 20px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: var(--surface-dark, #2a2a2a);
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  flex: 1;
  min-width: 150px;
}

.stat-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
}

.stat-icon.application {
  background: linear-gradient(135deg, #4299e1, #3182ce);
  color: white;
}

.stat-icon.error {
  background: linear-gradient(135deg, #f56565, #e53e3e);
  color: white;
}

.stat-icon.operation {
  background: linear-gradient(135deg, #48bb78, #38a169);
  color: white;
}

.stat-content {
  flex: 1;
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: var(--text-dark, #ffffff);
}

.stat-label {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
  margin-top: 4px;
}

.log-content {
  display: flex;
  flex: 1;
  overflow: hidden;
}

.file-list-panel {
  width: 300px;
  border-right: 1px solid rgba(255, 255, 255, 0.1);
  background: var(--surface-dark, #2a2a2a);
  display: flex;
  flex-direction: column;
}

.log-viewer-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: var(--bg-dark, #1a1a1a);
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  background: var(--surface-dark, #2a2a2a);
}

.panel-header h3 {
  margin: 0;
  font-size: 14px;
  font-weight: 500;
  color: var(--text-dark, #ffffff);
}

.no-file-text {
  color: rgba(255, 255, 255, 0.5);
  font-size: 14px;
}

.file-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.file-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
  margin-bottom: 4px;
}

.file-item:hover {
  background: rgba(255, 255, 255, 0.05);
}

.file-item.active {
  background: rgba(99, 179, 237, 0.2);
  border: 1px solid rgba(99, 179, 237, 0.3);
}

.file-icon {
  width: 24px;
  height: 24px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
}

.file-icon.application {
  background: #4299e1;
  color: white;
}

.file-icon.error {
  background: #f56565;
  color: white;
}

.file-icon.operation {
  background: #48bb78;
  color: white;
}

.file-info {
  flex: 1;
}

.file-name {
  font-size: 13px;
  color: var(--text-dark, #ffffff);
  margin-bottom: 2px;
}

.file-meta {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.5);
  display: flex;
  gap: 8px;
}

.log-content-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.search-results {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.search-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: var(--surface-dark, #2a2a2a);
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  font-size: 13px;
}

.search-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.search-item {
  display: flex;
  gap: 12px;
  padding: 8px 12px;
  border-radius: 4px;
  cursor: pointer;
  margin-bottom: 4px;
  border: 1px solid transparent;
}

.search-item:hover {
  background: rgba(255, 255, 255, 0.05);
  border-color: rgba(255, 255, 255, 0.1);
}

.log-lines {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
  font-family: 'Fira Code', monospace;
  font-size: 12px;
  line-height: 1.4;
}

.log-line {
  display: flex;
  margin-bottom: 2px;
  padding: 2px 0;
}

.log-line.error {
  background: rgba(245, 101, 101, 0.1);
  border-left: 3px solid #f56565;
  padding-left: 8px;
}

.log-line.warn {
  background: rgba(237, 137, 54, 0.1);
  border-left: 3px solid #ed8936;
  padding-left: 8px;
}

.log-line.info {
  color: #4299e1;
}

.log-line.debug {
  color: rgba(255, 255, 255, 0.6);
}

.line-number {
  width: 60px;
  color: rgba(255, 255, 255, 0.4);
  text-align: right;
  margin-right: 12px;
  flex-shrink: 0;
  font-size: 11px;
}

.log-content {
  flex: 1;
  word-break: break-all;
  white-space: pre-wrap;
}

.log-pagination {
  padding: 12px 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  background: var(--surface-dark, #2a2a2a);
}

.no-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: rgba(255, 255, 255, 0.4);
}

.no-content i {
  font-size: 48px;
  margin-bottom: 16px;
}

.warning-text {
  margin-top: 10px;
  color: #f56c6c;
  font-size: 12px;
}

/* 高亮样式 */
:deep(mark) {
  background: #ffd700;
  color: #000;
  padding: 1px 2px;
  border-radius: 2px;
}

/* 滚动条样式 */
::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.1);
}

::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.3);
  border-radius: 3px;
}

::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.5);
}
</style> 