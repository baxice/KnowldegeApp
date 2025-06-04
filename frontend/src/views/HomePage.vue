<template>
  <div class="app-container">
    <!-- 顶部导航栏 -->
    <header class="header">
      <div class="logo">
        <i class="fas fa-brain logo-icon"></i>
        <span>KnowledgeBase</span>
      </div>
      
      <div class="search-bar">
        <input 
          type="text" 
          placeholder="搜索文档和笔记..."
          v-model="searchQuery"
          @input="handleSearch"
        >
      </div>
      
      <div class="actions">
        <button class="btn" @click="toggleTheme">
          <i :class="isDarkTheme ? 'fas fa-sun' : 'fas fa-moon'"></i>
          <span>主题</span>
        </button>
        <button class="btn">
          <i class="fas fa-cog"></i>
          <span>设置</span>
        </button>
        
        <!-- 新建按钮（带下拉菜单） -->
        <div class="dropdown" ref="newDropdown">
          <button class="btn btn-primary" @click="toggleNewDropdown">
            <i class="fas fa-plus"></i>
            <span>新建</span>
            <i class="fas fa-caret-down" style="margin-left: 4px;"></i>
          </button>
          <div class="dropdown-content" v-show="showNewDropdown">
            <a href="#" @click="createNewDocument">
              <i class="fas fa-file-alt"></i>
              <span>新建文档</span>
            </a>
            <a href="#" @click="createNewNote">
              <i class="fas fa-sticky-note"></i>
              <span>新建笔记</span>
            </a>
            <a href="#" @click="uploadDocument">
              <i class="fas fa-upload"></i>
              <span>上传文档</span>
            </a>
          </div>
        </div>
      </div>
    </header>
    
    <!-- 主内容区 -->
    <div class="main-content">
      <!-- 左侧边栏 -->
      <div class="sidebar">
        <div class="sidebar-section">
          <div class="sidebar-title">
            <i class="fas fa-folder"></i>
            <span>文档库</span>
          </div>
          <ul class="file-tree">
            <li 
              v-for="doc in documents" 
              :key="doc.id"
              class="file-item"
            >
              <div class="file-item-content" @click="openDocument(doc)">
                <i :class="getFileIcon(doc.fileType)" class="file-icon"></i>
                <span class="file-name">{{ doc.title }}</span>
              </div>
              <button 
                class="delete-btn" 
                @click.stop="deleteDocument(doc)"
                title="删除文档"
              >
                <i class="fas fa-trash"></i>
              </button>
            </li>
          </ul>
        </div>
        
        <div class="sidebar-section">
          <div class="sidebar-title">
            <i class="fas fa-sticky-note"></i>
            <span>我的笔记</span>
          </div>
          <ul class="file-tree">
            <li 
              v-for="note in notes" 
              :key="note.id"
              class="file-item"
            >
              <div class="file-item-content" @click="openNote(note)">
                <i class="fas fa-file-alt file-icon"></i>
                <span class="file-name">{{ note.title }}</span>
              </div>
              <button 
                class="delete-btn" 
                @click.stop="deleteNote(note)"
                title="删除笔记"
              >
                <i class="fas fa-trash"></i>
              </button>
            </li>
          </ul>
        </div>
        
        <div class="sidebar-section">
          <div class="sidebar-title">
            <i class="fas fa-tags"></i>
            <span>标签</span>
          </div>
          <ul class="file-tree">
            <li 
              v-for="tag in tags" 
              :key="tag.id"
              class="file-item"
              @click="filterByTag(tag)"
            >
              <i class="fas fa-tag" :style="{ color: tag.color }"></i>
              <span>{{ tag.name }}</span>
            </li>
          </ul>
        </div>
        
        <!-- 用户信息 -->
        <div class="user-info">
          <UserAvatar />
        </div>
      </div>
      
      <!-- 主编辑区 -->
      <div class="editor-container">
        <!-- 标签页容器 -->
        <div class="tabs-container">
          <div 
            v-for="tab in openTabs" 
            :key="tab.id"
            :class="['tab', { active: tab.id === activeTabId }]"
            @click="switchTab(tab.id)"
          >
            <i :class="getTabIcon(tab.type)"></i>
            <span>{{ tab.title }}</span>
            <button class="close-tab" @click.stop="closeTab(tab.id)">×</button>
          </div>
          <button class="btn" style="border-radius: 0; border-right: 1px solid var(--border-color);" @click="createNewNote">
            <i class="fas fa-plus"></i>
          </button>
        </div>
        
        <!-- 内容显示区域 -->
        <div class="content-area">
          <div v-if="activeTab" class="editor-container">
            <!-- Markdown笔记编辑器 -->
            <MarkdownEditor 
              v-if="activeTab.type === 'note'"
              :note-id="activeTab.noteId"
              :initial-content="activeTab.content"
              :title="activeTab.title"
              @save="handleNoteSave"
              @content-change="(content) => activeTab.content = content"
            />
            
            <!-- 文档查看器 -->
            <DocumentViewer 
              v-else-if="activeTab.type === 'document'"
              :document="activeTab.document"
              :can-edit="true"
              :can-delete="true"
              @edit="switchToEditMode"
              @delete="deleteDocument"
              @download="(doc) => console.log('下载文档:', doc)"
            />
            
            <!-- 新的文档编辑器 -->
            <DocumentEditor 
              v-else-if="activeTab.type === 'documentEditor'"
              :document="activeTab.document"
              @save="handleDocumentSave"
              @delete="deleteDocument"
            />
            
            <!-- 欢迎页面 -->
            <div v-else-if="activeTab.type === 'welcome'" class="welcome-page">
              <div class="welcome-content">
                <div class="welcome-header">
                  <div class="welcome-icon">
                    <i class="fas fa-brain"></i>
                  </div>
                  <h1>欢迎使用 KnowledgeBase</h1>
                  <p class="welcome-subtitle">专业的知识管理平台，让您的想法有序绽放</p>
                </div>
                
                <div class="feature-grid">
                  <div class="feature-card">
                    <div class="feature-icon">
                      <i class="fas fa-edit"></i>
                    </div>
                    <h3>Markdown 编辑</h3>
                    <p>类似VSCode的专业编辑体验，支持实时预览、语法高亮、智能提示</p>
                    <div class="feature-tags">
                      <span class="tag">实时预览</span>
                      <span class="tag">语法高亮</span>
                      <span class="tag">快捷键</span>
                    </div>
                  </div>
                  
                  <div class="feature-card">
                    <div class="feature-icon">
                      <i class="fas fa-folder-open"></i>
                    </div>
                    <h3>文档管理</h3>
                    <p>支持多种文档格式，智能分类，全文搜索，让知识触手可及</p>
                    <div class="feature-tags">
                      <span class="tag">多格式</span>
                      <span class="tag">智能搜索</span>
                      <span class="tag">云存储</span>
                    </div>
                  </div>
                  
                  <div class="feature-card">
                    <div class="feature-icon">
                      <i class="fas fa-tags"></i>
                    </div>
                    <h3>标签分类</h3>
                    <p>灵活的标签系统，多维度组织内容，建立知识网络结构</p>
                    <div class="feature-tags">
                      <span class="tag">多标签</span>
                      <span class="tag">颜色编码</span>
                      <span class="tag">快速筛选</span>
                    </div>
                  </div>
                  
                  <div class="feature-card">
                    <div class="feature-icon">
                      <i class="fas fa-chart-line"></i>
                    </div>
                    <h3>数据洞察</h3>
                    <p>可视化展示知识库统计，了解创作趋势，提升效率</p>
                    <div class="feature-tags">
                      <span class="tag">统计图表</span>
                      <span class="tag">趋势分析</span>
                      <span class="tag">效率提升</span>
                    </div>
                  </div>
                </div>
                
                <div class="quick-actions">
                  <h3>开始您的知识之旅</h3>
                  <div class="action-grid">
                    <button class="action-card primary" @click="createNewDocument">
                      <div class="action-icon">
                        <i class="fas fa-file-alt"></i>
                      </div>
                      <div class="action-content">
                        <h4>创建文档</h4>
                        <p>开始编写Markdown文档</p>
                      </div>
                      <div class="action-shortcut">Ctrl+N</div>
                    </button>
                    
                    <button class="action-card" @click="createNewNote">
                      <div class="action-icon">
                        <i class="fas fa-sticky-note"></i>
                      </div>
                      <div class="action-content">
                        <h4>快速笔记</h4>
                        <p>记录临时想法和灵感</p>
                      </div>
                      <div class="action-shortcut">Ctrl+Shift+N</div>
                    </button>
                    
                    <button class="action-card" @click="uploadDocument">
                      <div class="action-icon">
                        <i class="fas fa-upload"></i>
                      </div>
                      <div class="action-content">
                        <h4>上传文档</h4>
                        <p>导入现有文档和资料</p>
                      </div>
                      <div class="action-shortcut">Ctrl+U</div>
                    </button>
                    
                    <button class="action-card" @click="focusSearch">
                      <div class="action-icon">
                        <i class="fas fa-search"></i>
                      </div>
                      <div class="action-content">
                        <h4>搜索内容</h4>
                        <p>在知识库中快速查找</p>
                      </div>
                      <div class="action-shortcut">Ctrl+K</div>
                    </button>
                  </div>
                </div>
                
                <div class="recent-activity" v-if="recentItems.length > 0">
                  <h3>最近活动</h3>
                  <div class="activity-list">
                    <div 
                      v-for="item in recentItems" 
                      :key="`${item.type}_${item.id}`"
                      class="activity-item"
                      @click="openRecentItem(item)"
                    >
                      <div class="activity-icon">
                        <i :class="getActivityIcon(item.type)"></i>
                      </div>
                      <div class="activity-content">
                        <h4>{{ item.title }}</h4>
                        <p>{{ getActivityDescription(item) }}</p>
                      </div>
                      <div class="activity-time">{{ formatTimeAgo(item.createdAt) }}</div>
                    </div>
                  </div>
                </div>
                
                <div class="tips-section">
                  <h3>💡 使用技巧</h3>
                  <div class="tips-grid">
                    <div class="tip-item">
                      <strong>快捷键:</strong> 使用 Ctrl+S 快速保存，Ctrl+/ 切换预览模式
                    </div>
                    <div class="tip-item">
                      <strong>标签管理:</strong> 为文档添加标签可以更好地组织和检索内容
                    </div>
                    <div class="tip-item">
                      <strong>搜索技巧:</strong> 支持标题、内容、标签的全文搜索
                    </div>
                    <div class="tip-item">
                      <strong>编辑器:</strong> 支持分屏预览，实时渲染，代码高亮等专业功能
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
          
          <!-- 空状态 -->
          <div v-else class="empty-state">
            <div class="empty-content">
              <i class="fas fa-folder-open"></i>
              <h3>没有打开的标签页</h3>
              <p>点击左侧文档或创建新内容开始工作</p>
              <div class="empty-actions">
                <button class="btn btn-primary" @click="createNewDocument">
                  <i class="fas fa-plus"></i>
                  创建文档
                </button>
                <button class="btn" @click="createNewNote">
                  <i class="fas fa-sticky-note"></i>
                  创建笔记
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 状态栏 -->
    <div class="status-bar">
      <div class="status-item">
        <i class="fas fa-file-alt"></i>
        <span>{{ openTabs.length }} 个标签页</span>
      </div>
      <div class="status-item">
        <i class="fas fa-clock"></i>
        <span>{{ currentTime }}</span>
      </div>
    </div>
    
    <!-- 文件上传模态框 -->
    <FileUploadModal 
      :visible="showUploadModal"
      @close="closeUploadModal"
      @success="handleUploadSuccess"
      @error="handleUploadError"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useThemeStore } from '@/stores/theme'
import { useAuthStore } from '@/stores/auth'
import { useUserStore } from '@/stores/user'
import { documentApi } from '@/api/document'
import { noteApi } from '@/api/note'
import { tagApi } from '@/api/tag'

// 导入新组件
import MarkdownEditor from '@/components/note/MarkdownEditor.vue'
import DocumentViewer from '@/components/document/DocumentViewer.vue'
import DocumentEditor from '@/components/document/DocumentEditor.vue'
import FileUploadModal from '@/components/upload/FileUploadModal.vue'
import UserAvatar from '@/components/common/UserAvatar.vue'

// 状态管理
const themeStore = useThemeStore()
const authStore = useAuthStore()
const userStore = useUserStore()

// 响应式数据
const searchQuery = ref('')
const showNewDropdown = ref(false)
const showUploadModal = ref(false)
const uploadFiles = ref([])
const currentTime = ref('')

// 数据列表
const documents = ref([])
const notes = ref([])
const tags = ref([])

// 标签页管理
const openTabs = ref([
  {
    id: 'welcome',
    title: '欢迎',
    type: 'welcome',
    content: ''
  }
])
const activeTabId = ref('welcome')

// 计算属性
const isDarkTheme = computed(() => themeStore.isDarkTheme)
const currentUser = computed(() => authStore.user || { username: 'Guest', email: 'guest@example.com' })
const userInitials = computed(() => {
  const user = currentUser.value
  return user.username ? user.username.substring(0, 2).toUpperCase() : 'GU'
})

const activeTab = computed(() => {
  return openTabs.value.find(tab => tab.id === activeTabId.value)
})

const renderedMarkdown = computed(() => {
  if (activeTab.value && activeTab.value.type === 'note') {
    // 这里应该使用markdown解析器，暂时返回简单的HTML
    return activeTab.value.content.replace(/\n/g, '<br>')
  }
  return ''
})

// 最近活动项目
const recentItems = computed(() => {
  const allItems = [
    ...documents.value.map(doc => ({ ...doc, type: 'document' })),
    ...notes.value.map(note => ({ ...note, type: 'note' }))
  ]
  
  return allItems
    .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
    .slice(0, 6)
})

// 方法
const toggleTheme = () => {
  themeStore.toggleTheme()
}

const toggleNewDropdown = () => {
  showNewDropdown.value = !showNewDropdown.value
}

const handleSearch = async () => {
  if (!searchQuery.value.trim()) return
  
  try {
    // 搜索文档和笔记
    const [docResults, noteResults] = await Promise.all([
      documentApi.search(searchQuery.value),
      noteApi.search(searchQuery.value)
    ])
    
    // 更新搜索结果
    documents.value = docResults.data
    notes.value = noteResults.data
    
    console.log('搜索结果:', { documents: docResults.data, notes: noteResults.data })
  } catch (error) {
    console.error('搜索失败:', error)
  }
}

const createNewDocument = async () => {
  showNewDropdown.value = false
  
  try {
    const docData = {
      title: '新建文档',
      description: '新建的markdown文档'
    }
    
    console.log('开始创建文档:', docData)
    const response = await documentApi.create(docData)
    console.log('文档创建响应:', response)
    
    // 注意：request.js已经解包了数据，所以response就是文档对象
    const newDoc = response
    console.log('新创建的文档:', newDoc)
    
    const newTab = {
      id: `doc_${newDoc.id}`,
      title: newDoc.title,
      type: 'document',
      document: newDoc
    }
    
    openTabs.value.push(newTab)
    activeTabId.value = newTab.id
    
    // 刷新文档列表
    await loadDocuments()
    console.log('文档创建成功，已打开新标签页')
  } catch (error) {
    console.error('创建文档失败:', error)
    console.error('错误详情:', error.response?.data || error.message)
  }
}

const createNewNote = async () => {
  showNewDropdown.value = false
  
  try {
    const noteData = {
      title: '新建笔记',
      content: '# 新建笔记\n\n开始编写您的内容...'
    }
    
    console.log('开始创建笔记:', noteData)
    const response = await noteApi.create(noteData)
    console.log('笔记创建响应:', response)
    
    // 注意：request.js已经解包了数据，所以response就是笔记对象
    const newNote = response
    console.log('新创建的笔记:', newNote)
    
    const newTab = {
      id: `note_${newNote.id}`,
      title: newNote.title,
      type: 'note',
      content: newNote.content,
      noteId: newNote.id
    }
    
    openTabs.value.push(newTab)
    activeTabId.value = newTab.id
    
    // 刷新笔记列表
    await loadNotes()
    console.log('笔记创建成功，已打开新标签页')
  } catch (error) {
    console.error('创建笔记失败:', error)
    console.error('错误详情:', error.response?.data || error.message)
  }
}

const uploadDocument = () => {
  showNewDropdown.value = false
  showUploadModal.value = true
}

const openDocument = (doc) => {
  const existingTab = openTabs.value.find(tab => tab.id === `doc_${doc.id}`)
  if (existingTab) {
    activeTabId.value = existingTab.id
  } else {
    // 判断是否为Markdown文档，如果是则直接打开编辑模式
    const isMarkdown = doc.fileType === 'text/markdown' || 
                       doc.fileType === 'text/x-markdown' || 
                       doc.title.toLowerCase().endsWith('.md') ||
                       !doc.filePath // 空文档（新建的文档）
    
    const newTab = {
      id: `doc_${doc.id}`,
      title: doc.title,
      type: isMarkdown ? 'documentEditor' : 'document',
      document: doc
    }
    openTabs.value.push(newTab)
    activeTabId.value = newTab.id
  }
}

const openNote = (note) => {
  const existingTab = openTabs.value.find(tab => tab.id === `note_${note.id}`)
  if (existingTab) {
    activeTabId.value = existingTab.id
  } else {
    const newTab = {
      id: `note_${note.id}`,
      title: note.title,
      type: 'note',
      content: note.content || '',
      noteId: note.id
    }
    openTabs.value.push(newTab)
    activeTabId.value = newTab.id
  }
}

const switchTab = (tabId) => {
  activeTabId.value = tabId
}

const closeTab = (tabId) => {
  const index = openTabs.value.findIndex(tab => tab.id === tabId)
  if (index > -1) {
    if (openTabs.value[index].id === activeTabId.value && openTabs.value.length > 1) {
      const newActiveIndex = index === 0 ? 1 : index - 1
      activeTabId.value = openTabs.value[newActiveIndex].id
    }
    openTabs.value.splice(index, 1)
    
    if (openTabs.value.length === 0) {
      openTabs.value.push({
        id: 'welcome',
        title: '欢迎',
        type: 'welcome',
        content: ''
      })
      activeTabId.value = 'welcome'
    }
  }
}

const updateTabContent = () => {
  // 自动保存笔记内容
  if (activeTab.value && activeTab.value.type === 'note') {
    // 这里可以添加防抖保存逻辑
    console.log('保存笔记内容')
  }
}

const getFileIcon = (fileType) => {
  if (!fileType) return 'fas fa-file-alt' // 默认为文本文件图标
  
  const type = fileType.toLowerCase()
  
  // Markdown类型
  if (type === 'text/markdown' || type === 'text/x-markdown') return 'fas fa-file-alt'
  
  // 图片类型
  if (type.startsWith('image/')) return 'fas fa-file-image'
  
  // 文档类型
  if (type === 'application/pdf') return 'fas fa-file-pdf'
  if (type.startsWith('text/')) return 'fas fa-file-alt'
  
  // Office文档
  if (type.includes('word') || type.includes('msword') || type.includes('wordprocessingml')) {
    return 'fas fa-file-word'
  }
  if (type.includes('excel') || type.includes('spreadsheet') || type.includes('ms-excel')) {
    return 'fas fa-file-excel'
  }
  if (type.includes('powerpoint') || type.includes('presentation') || type.includes('ms-powerpoint')) {
    return 'fas fa-file-powerpoint'
  }
  
  // 其他常见类型
  if (type.includes('json')) return 'fas fa-file-code'
  if (type.includes('xml')) return 'fas fa-file-code'
  if (type.includes('zip') || type.includes('rar') || type.includes('archive')) return 'fas fa-file-archive'
  
  // 对于 application/octet-stream，根据文件名判断
  if (type === 'application/octet-stream') {
    console.log('遇到 application/octet-stream 类型，可能是文件类型识别问题')
    return 'fas fa-file-alt' // 默认返回文本文件图标
  }
  
  // 只在遇到未知文件类型时输出调试信息
  console.log('未匹配的文件类型，使用默认图标:', type)
  return 'fas fa-file-alt' // 改为文本文件图标而不是通用文件图标
}

const getTabIcon = (type) => {
  switch (type) {
    case 'note': return 'fas fa-sticky-note'
    case 'document': return 'fas fa-file-alt'
    case 'welcome': return 'fas fa-home'
    default: return 'fas fa-file'
  }
}

const formatFileSize = (bytes) => {
  if (!bytes) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

const filterByTag = async (tag) => {
  try {
    // 按标签过滤文档和笔记
    const [docResults, noteResults] = await Promise.all([
      documentApi.getByTag(tag.id),
      noteApi.getByTag(tag.id)
    ])
    
    // 注意：request.js已经解包了数据，所以结果就是数据数组
    const documentsData = Array.isArray(docResults) ? docResults : docResults.data || []
    const notesData = Array.isArray(noteResults) ? noteResults : noteResults.data || []
    
    documents.value = documentsData
    notes.value = notesData
    
    console.log(`按标签"${tag.name}"过滤结果:`, { documents: documentsData, notes: notesData })
  } catch (error) {
    console.error('按标签过滤失败:', error)
  }
}

const downloadDocument = (doc) => {
  // 实现文档下载功能
  console.log('下载文档:', doc.title)
}

const previewDocument = (doc) => {
  // 实现文档预览功能
  console.log('预览文档:', doc.title)
}

const closeUploadModal = () => {
  showUploadModal.value = false
  uploadFiles.value = []
}

const handleFileDrop = (e) => {
  e.preventDefault()
  const files = Array.from(e.dataTransfer.files)
  uploadFiles.value.push(...files)
}

const handleFileSelect = (e) => {
  const files = Array.from(e.target.files)
  uploadFiles.value.push(...files)
}

const removeUploadFile = (index) => {
  uploadFiles.value.splice(index, 1)
}

const uploadSelectedFiles = async () => {
  // 实现文件上传功能
  for (const file of uploadFiles.value) {
    try {
      await documentApi.upload(file)
      console.log('上传成功:', file.name)
    } catch (error) {
      console.error('上传失败:', file.name, error)
    }
  }
  closeUploadModal()
  loadDocuments() // 重新加载文档列表
}

// 添加缺失的方法
const handleUploadSuccess = async (uploadResult) => {
  console.log('文件上传成功事件触发:', uploadResult)
  
  // 重新加载文档列表
  console.log('开始重新加载文档列表...')
  await loadDocuments()
  console.log('文档列表重新加载完成，当前文档数量:', documents.value.length)
  
  // 如果上传成功，自动打开第一个上传的文档
  if (uploadResult.results && uploadResult.results.length > 0) {
    const successfulUploads = uploadResult.results.filter(result => result.success)
    console.log('成功上传的文档数量:', successfulUploads.length)
    
    if (successfulUploads.length > 0) {
      // 延迟一下让文档列表先更新，然后根据文档名称匹配
      setTimeout(() => {
        // 尝试匹配刚上传的文档
        const uploadedFileNames = successfulUploads.map(upload => upload.filename)
        console.log('上传的文件名列表:', uploadedFileNames)
        
        // 从文档列表中找到匹配的文档（通过文件名匹配）
        const matchedDocs = documents.value.filter(doc => {
          return uploadedFileNames.some(fileName => doc.title === fileName || doc.title.includes(fileName.split('.')[0]))
        })
        
        console.log('匹配到的文档:', matchedDocs)
        
        if (matchedDocs.length > 0) {
          // 自动打开第一个匹配的文档
          const firstDoc = matchedDocs[0]
          openDocument(firstDoc)
          console.log(`自动打开文档: "${firstDoc.title}" (ID: ${firstDoc.id})`)
        } else {
          // 如果没有匹配到，尝试打开最新的文档
          if (documents.value.length > 0) {
            const latestDoc = documents.value[0] // 假设已按时间排序
            openDocument(latestDoc)
            console.log(`没有精确匹配，打开最新文档: "${latestDoc.title}" (ID: ${latestDoc.id})`)
          }
        }
      }, 800) // 增加延迟时间，确保后端数据已更新
    }
  }
  
  // 显示上传结果提示
  const message = uploadResult.failed === 0 
    ? `成功上传 ${uploadResult.success} 个文档`
    : `上传完成：${uploadResult.success} 成功，${uploadResult.failed} 失败`
  console.log('上传结果:', message)
}

const handleUploadError = (error) => {
  console.error('文件上传失败:', error)
  // 可以显示错误提示
  // ElMessage.error('文件上传失败，请重试')
}

// 删除文档
const deleteDocument = async (doc) => {
  if (!confirm(`确定要删除文档 "${doc.title}" 吗？此操作不可恢复。`)) {
    return
  }
  
  try {
    console.log('开始删除文档:', doc.title, doc.id)
    await documentApi.delete(doc.id)
    console.log('文档删除成功:', doc.title)
    
    // 关闭已打开的文档标签页
    const openDocTab = openTabs.value.find(tab => tab.id === `doc_${doc.id}`)
    if (openDocTab) {
      closeTab(openDocTab.id)
    }
    
    // 重新加载文档列表
    await loadDocuments()
    
    console.log('文档删除完成，列表已更新')
  } catch (error) {
    console.error('删除文档失败:', error)
    console.error('错误详情:', error.response?.data || error.message)
    alert('删除文档失败，请重试')
  }
}

// 删除笔记
const deleteNote = async (note) => {
  if (!confirm(`确定要删除笔记 "${note.title}" 吗？此操作不可恢复。`)) {
    return
  }
  
  try {
    console.log('开始删除笔记:', note.title, note.id)
    await noteApi.delete(note.id)
    console.log('笔记删除成功:', note.title)
    
    // 关闭已打开的笔记标签页
    const openNoteTab = openTabs.value.find(tab => tab.id === `note_${note.id}`)
    if (openNoteTab) {
      closeTab(openNoteTab.id)
    }
    
    // 重新加载笔记列表
    await loadNotes()
    
    console.log('笔记删除完成，列表已更新')
  } catch (error) {
    console.error('删除笔记失败:', error)
    console.error('错误详情:', error.response?.data || error.message)
    alert('删除笔记失败，请重试')
  }
}

const handleNoteSave = async (noteData) => {
  try {
    // 保存笔记
    await noteApi.update(noteData.id, noteData)
    console.log('笔记保存成功:', noteData.title)
    
    // 更新标签页标题
    const tab = openTabs.value.find(tab => tab.noteId === noteData.id)
    if (tab) {
      tab.title = noteData.title
      tab.content = noteData.content
    }
    
    // 重新加载笔记列表
    await loadNotes()
  } catch (error) {
    console.error('笔记保存失败:', error)
  }
}

// 切换到编辑模式
const switchToEditMode = (document) => {
  console.log('切换到编辑模式:', document)
  
  // 找到当前标签页并修改类型
  const currentTab = activeTab.value
  if (currentTab && currentTab.type === 'document') {
    currentTab.type = 'documentEditor'
    // 确保文档数据是最新的
    currentTab.document = document
  }
}

// 处理文档保存
const handleDocumentSave = async (documentData) => {
  try {
    console.log('文档保存成功:', documentData.title)
    
    // 更新标签页信息
    const tab = openTabs.value.find(tab => tab.id === `doc_${documentData.id}`)
    if (tab) {
      tab.title = documentData.title
      tab.document = documentData
    }
    
    // 重新加载文档列表
    await loadDocuments()
  } catch (error) {
    console.error('文档保存处理失败:', error)
  }
}

// 聚焦搜索框
const focusSearch = () => {
  const searchInput = document.querySelector('.search-bar input')
  if (searchInput) {
    searchInput.focus()
  }
}

// 打开最近活动项目
const openRecentItem = (item) => {
  if (item.type === 'document') {
    openDocument(item)
  } else if (item.type === 'note') {
    openNote(item)
  }
}

// 获取活动图标
const getActivityIcon = (type) => {
  switch (type) {
    case 'document':
      return 'fas fa-file-alt'
    case 'note':
      return 'fas fa-sticky-note'
    default:
      return 'fas fa-file'
  }
}

// 获取活动描述
const getActivityDescription = (item) => {
  if (item.type === 'document') {
    return `文档 • ${formatFileSize(item.fileSize || 0)}`
  } else if (item.type === 'note') {
    const wordCount = item.content ? item.content.replace(/\s+/g, '').length : 0
    return `笔记 • ${wordCount} 字`
  }
  return ''
}

// 格式化时间为相对时间
const formatTimeAgo = (dateString) => {
  if (!dateString) return '未知时间'
  
  const now = new Date()
  const date = new Date(dateString)
  const diffMs = now - date
  const diffMins = Math.floor(diffMs / (1000 * 60))
  const diffHours = Math.floor(diffMs / (1000 * 60 * 60))
  const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24))
  
  if (diffMins < 1) return '刚刚'
  if (diffMins < 60) return `${diffMins}分钟前`
  if (diffHours < 24) return `${diffHours}小时前`
  if (diffDays < 7) return `${diffDays}天前`
  
  return date.toLocaleDateString('zh-CN', {
    month: 'short',
    day: 'numeric'
  })
}

// 数据加载
const loadDocuments = async () => {
  try {
    console.log('开始调用文档API...')
    const response = await documentApi.getAll()
    console.log('文档API响应:', response)
    // 注意：request.js已经解包了数据，所以response就是数据数组
    const documentsData = Array.isArray(response) ? response : response.data || []
    console.log('文档数据:', documentsData)
    documents.value = documentsData
    console.log('文档列表已更新，数量:', documents.value.length)
    
    // 打印前几个文档的信息用于调试
    if (documents.value.length > 0) {
      console.log('最新的3个文档:', documents.value.slice(0, 3).map(doc => ({
        id: doc.id,
        title: doc.title,
        createdAt: doc.createdAt
      })))
    }
  } catch (error) {
    console.error('加载文档失败:', error)
    console.error('错误详情:', error.response?.data || error.message)
    documents.value = []
  }
}

const loadNotes = async () => {
  try {
    const response = await noteApi.getAll()
    // 注意：request.js已经解包了数据，所以response就是数据数组
    const notesData = Array.isArray(response) ? response : response.data || []
    notes.value = notesData
    console.log('笔记列表已更新，数量:', notes.value.length)
  } catch (error) {
    console.error('加载笔记失败:', error)
    console.error('错误详情:', error.response?.data || error.message)
    notes.value = []
  }
}

const loadTags = async () => {
  try {
    const response = await tagApi.getAll()
    // 注意：request.js已经解包了数据，所以response就是数据数组
    const tagsData = Array.isArray(response) ? response : response.data || []
    tags.value = tagsData
    console.log('标签列表已更新，数量:', tags.value.length)
  } catch (error) {
    console.error('加载标签失败:', error)
    console.error('错误详情:', error.response?.data || error.message)
    tags.value = []
  }
}

// 时间更新
const updateTime = () => {
  const now = new Date()
  currentTime.value = now.toLocaleTimeString('zh-CN')
}

// 生命周期
onMounted(async () => {
  // 初始化主题
  themeStore.initTheme()
  
  // 初始化用户认证状态
  authStore.initAuth()
  userStore.initUser()
  
  // 加载数据
  await Promise.all([
    loadDocuments(),
    loadNotes(),
    loadTags()
  ])
  
  // 启动时间更新
  updateTime()
  const timeInterval = setInterval(updateTime, 1000)
  
  // 清理定时器
  onUnmounted(() => {
    clearInterval(timeInterval)
  })
})

// 响应式处理点击外部关闭下拉菜单
watch(showNewDropdown, (newValue) => {
  if (newValue) {
    const closeDropdown = () => {
      showNewDropdown.value = false
      document.removeEventListener('click', closeDropdown)
    }
    setTimeout(() => {
      document.addEventListener('click', closeDropdown)
    }, 0)
  }
})
</script>

<style scoped>
/* 导入全局样式变量 */
@import '@/assets/styles/obsidian-theme.css';

.app-container {
  display: flex;
  height: 100vh;
  flex-direction: column;
  background-color: var(--bg-color);
  color: var(--text-color);
}

/* 顶部导航栏 */
.header {
  height: var(--header-height);
  background-color: var(--surface-color);
  border-bottom: 1px solid var(--border-color);
  display: flex;
  align-items: center;
  padding: 0 16px;
  justify-content: space-between;
}

.logo {
  display: flex;
  align-items: center;
  gap: 12px;
  font-weight: 600;
  font-size: 18px;
}

.logo-icon {
  color: var(--color-blue);
  font-size: 24px;
}

.search-bar {
  flex: 1;
  max-width: 500px;
  margin: 0 20px;
}

.search-bar input {
  width: 100%;
  background-color: var(--surface-color-2);
  border: 1px solid var(--border-color);
  border-radius: 4px;
  padding: 8px 12px;
  color: var(--text-color);
  outline: none;
}

.actions {
  display: flex;
  gap: 16px;
}

.btn {
  background: none;
  border: none;
  color: var(--text-color);
  cursor: pointer;
  padding: 6px 10px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  position: relative;
}

.btn:hover {
  background-color: var(--surface-color-2);
}

.btn-primary {
  background-color: var(--color-blue);
  color: var(--bg-dark);
  font-weight: 500;
}

.btn-primary:hover {
  background-color: hsl(170, 100%, 65%);
}

/* 下拉菜单 */
.dropdown {
  position: relative;
  display: inline-block;
}

.dropdown-content {
  position: absolute;
  background-color: var(--surface-color);
  min-width: 180px;
  box-shadow: 0 8px 16px rgba(0,0,0,0.3);
  z-index: 100;
  border-radius: 4px;
  overflow: hidden;
  border: 1px solid var(--border-color);
  top: 100%;
  left: 0;
  margin-top: 4px;
}

.dropdown-content a {
  color: var(--text-color);
  padding: 10px 16px;
  text-decoration: none;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
}

.dropdown-content a:hover {
  background-color: var(--surface-color-2);
}

/* 主内容区 */
.main-content {
  display: flex;
  flex: 1;
  overflow: hidden;
}

/* 左侧边栏 */
.sidebar {
  width: var(--sidebar-width);
  background-color: var(--surface-color);
  border-right: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  overflow-x: visible; /* 水平方向允许溢出（下拉菜单） */
  overflow-y: auto; /* 垂直方向允许滚动 */
}

.sidebar-section {
  padding: 16px;
}

.sidebar-title {
  font-size: 14px;
  text-transform: uppercase;
  letter-spacing: 1px;
  margin-bottom: 12px;
  color: var(--color-blue);
  display: flex;
  align-items: center;
  gap: 8px;
}

.file-tree {
  list-style: none;
}

.file-item {
  padding: 8px 12px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  position: relative;
}

.file-item:hover {
  background-color: var(--surface-color-2);
}

.file-item-content {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  cursor: pointer;
}

.file-name {
  flex: 1;
  font-size: 14px;
}

.delete-btn {
  background: none;
  border: none;
  color: var(--text-color);
  cursor: pointer;
  opacity: 0;
  transition: opacity 0.2s;
  padding: 4px;
  border-radius: 4px;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
}

.file-item:hover .delete-btn {
  opacity: 0.7;
}

.delete-btn:hover {
  opacity: 1;
  background-color: rgba(255, 82, 82, 0.2);
  color: #ff5252;
}

.file-icon {
  color: var(--color-blue);
}

/* 用户信息 */
.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  border-top: 1px solid var(--border-color);
  background-color: var(--surface-color);
  margin-top: auto;
  position: relative; /* 为下拉菜单提供定位上下文 */
  overflow: visible; /* 允许下拉菜单溢出 */
}

/* 主编辑区 */
.editor-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* 标签页容器 */
.tabs-container {
  display: flex;
  height: var(--tab-height);
  background: var(--surface-color);
  border-bottom: 1px solid var(--border-color);
  overflow-x: auto;
  scrollbar-width: thin;
}

/* 单个标签页 */
.tab {
  display: flex;
  align-items: center;
  padding: 0 16px;
  height: 100%;
  border-right: 1px solid var(--border-color);
  cursor: pointer;
  transition: background-color 0.2s;
  white-space: nowrap;
  font-size: 14px;
  gap: 8px;
}

.tab.active {
  background: var(--bg-color);
  border-bottom: 2px solid var(--color-blue);
}

.tab:hover:not(.active) {
  background: var(--surface-color-2);
}

.close-tab {
  margin-left: 4px;
  background: none;
  border: none;
  color: var(--text-color);
  cursor: pointer;
  opacity: 0.7;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.close-tab:hover {
  background: rgba(255, 255, 255, 0.1);
  opacity: 1;
}

/* 内容显示区域 */
.content-area {
  flex: 1;
  display: flex;
  overflow: auto;
  padding: 16px;
  background-color: var(--bg-color);
}

.editor-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* Markdown笔记编辑器 */
.markdown-editor {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.markdown-input, .preview {
  flex: 1;
  height: 100%;
  padding: 16px;
  border: 1px solid var(--border-color);
  border-radius: var(--window-border-radius);
  background-color: var(--surface-color);
  overflow: auto;
}

.markdown-input {
  font-family: var(--font-code);
  resize: none;
  outline: none;
  color: var(--text-color);
  background-color: var(--surface-color);
}

.preview {
  font-family: var(--font-main);
  line-height: 1.6;
}

/* 文档查看器 */
.document-viewer {
  flex: 1;
  padding: 24px;
  background-color: var(--surface-color);
  border-radius: var(--window-border-radius);
  border: 1px solid var(--border-color);
}

.document-info h2 {
  margin-bottom: 16px;
  color: var(--color-blue);
}

.document-actions {
  margin-top: 24px;
  display: flex;
  gap: 12px;
}

/* 欢迎页面 */
.welcome-page {
  flex: 1;
  overflow-y: auto;
  background: var(--bg-color);
}

.welcome-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 40px 24px;
}

/* 欢迎页头部 */
.welcome-header {
  text-align: center;
  margin-bottom: 60px;
}

.welcome-icon {
  margin-bottom: 24px;
}

.welcome-icon i {
  font-size: 80px;
  background: linear-gradient(135deg, var(--color-blue), var(--color-purple));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.welcome-content h1 {
  font-size: 3.5em;
  font-weight: 700;
  margin: 0 0 16px 0;
  background: linear-gradient(135deg, var(--color-pink), var(--color-orange));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.welcome-subtitle {
  font-size: 1.3em;
  color: var(--text-muted);
  margin: 0;
  font-weight: 300;
}

/* 功能网格 */
.feature-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 24px;
  margin-bottom: 60px;
}

.feature-card {
  background: var(--surface-color);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  padding: 28px;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

.feature-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, var(--color-blue), var(--color-purple));
  opacity: 0;
  transition: opacity 0.3s ease;
}

.feature-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.15);
  border-color: var(--color-blue);
}

.feature-card:hover::before {
  opacity: 1;
}

.feature-icon {
  width: 60px;
  height: 60px;
  background: linear-gradient(135deg, var(--color-blue), var(--color-purple));
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20px;
}

.feature-icon i {
  font-size: 28px;
  color: white;
}

.feature-card h3 {
  font-size: 1.4em;
  font-weight: 600;
  color: var(--text-color);
  margin: 0 0 12px 0;
}

.feature-card p {
  color: var(--text-muted);
  line-height: 1.6;
  margin: 0 0 16px 0;
}

.feature-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.tag {
  background: var(--surface-color-2);
  color: var(--color-blue);
  padding: 4px 8px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
  border: 1px solid rgba(102, 217, 255, 0.3);
}

/* 快速操作 */
.quick-actions {
  margin-bottom: 60px;
}

.quick-actions h3 {
  text-align: center;
  font-size: 2em;
  font-weight: 600;
  color: var(--color-blue);
  margin: 0 0 32px 0;
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 20px;
}

.action-card {
  background: var(--surface-color);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  cursor: pointer;
  transition: all 0.3s ease;
  text-align: left;
  position: relative;
  overflow: hidden;
}

.action-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(90deg, var(--color-green), var(--color-blue));
  opacity: 0;
  transition: opacity 0.3s ease;
}

.action-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.1);
  border-color: var(--color-blue);
}

.action-card:hover::before {
  opacity: 1;
}

.action-card.primary {
  background: linear-gradient(135deg, var(--color-blue), var(--color-purple));
  color: white;
  border: none;
}

.action-card.primary .action-icon {
  background: rgba(255, 255, 255, 0.2);
}

.action-card.primary .action-content h4,
.action-card.primary .action-content p {
  color: white;
}

.action-card.primary .action-shortcut {
  background: rgba(255, 255, 255, 0.2);
  color: white;
}

.action-icon {
  width: 48px;
  height: 48px;
  background: var(--surface-color-2);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.action-icon i {
  font-size: 20px;
  color: var(--color-blue);
}

.action-content {
  flex: 1;
}

.action-content h4 {
  font-size: 1.1em;
  font-weight: 600;
  color: var(--text-color);
  margin: 0 0 4px 0;
}

.action-content p {
  font-size: 0.9em;
  color: var(--text-muted);
  margin: 0;
}

.action-shortcut {
  background: var(--surface-color-2);
  color: var(--text-muted);
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 500;
  font-family: var(--font-code);
  flex-shrink: 0;
}

/* 最近活动 */
.recent-activity {
  margin-bottom: 40px;
}

.recent-activity h3 {
  font-size: 1.5em;
  font-weight: 600;
  color: var(--color-blue);
  margin: 0 0 20px 0;
}

.activity-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.activity-item {
  background: var(--surface-color);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  padding: 16px;
  display: flex;
  align-items: center;
  gap: 16px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.activity-item:hover {
  background: var(--surface-color-2);
  border-color: var(--color-blue);
  transform: translateX(4px);
}

.activity-icon {
  width: 40px;
  height: 40px;
  background: var(--surface-color-2);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.activity-icon i {
  color: var(--color-blue);
  font-size: 16px;
}

.activity-content {
  flex: 1;
  min-width: 0;
}

.activity-content h4 {
  font-size: 1em;
  font-weight: 600;
  color: var(--text-color);
  margin: 0 0 4px 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.activity-content p {
  font-size: 0.85em;
  color: var(--text-muted);
  margin: 0;
}

.activity-time {
  color: var(--text-muted);
  font-size: 0.8em;
  flex-shrink: 0;
}

/* 使用技巧 */
.tips-section {
  margin-bottom: 40px;
}

.tips-section h3 {
  font-size: 1.5em;
  font-weight: 600;
  color: var(--color-blue);
  margin: 0 0 20px 0;
}

.tips-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 16px;
}

.tip-item {
  background: var(--surface-color);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  padding: 16px;
  border-left: 4px solid var(--color-blue);
}

.tip-item strong {
  color: var(--color-blue);
}

/* 空状态 */
.empty-state {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-color);
}

.empty-content {
  text-align: center;
  max-width: 400px;
  padding: 40px;
}

.empty-content i {
  font-size: 64px;
  color: var(--text-muted);
  margin-bottom: 24px;
  opacity: 0.5;
}

.empty-content h3 {
  font-size: 1.5em;
  color: var(--text-color);
  margin: 0 0 12px 0;
}

.empty-content p {
  color: var(--text-muted);
  margin: 0 0 24px 0;
  line-height: 1.6;
}

.empty-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}

/* 状态栏 */
.status-bar {
  height: 24px;
  background-color: var(--surface-color);
  border-top: 1px solid var(--border-color);
  display: flex;
  align-items: center;
  padding: 0 12px;
  font-size: 12px;
  justify-content: space-between;
}

.status-item {
  display: flex;
  align-items: center;
  gap: 6px;
}

/* 响应式调整 */
@media (max-width: 768px) {
  .sidebar {
    width: 220px;
  }
  
  .markdown-editor {
    flex-direction: column;
  }
  
  .markdown-input, .preview {
    min-height: 300px;
  }
  
  .search-bar {
    max-width: 200px;
  }
  
  /* 移动端删除按钮始终显示 */
  .file-item .delete-btn {
    opacity: 0.7;
  }
  
  .delete-btn {
    width: 28px;
    height: 28px;
    font-size: 14px;
  }
  
  /* 欢迎页面响应式 */
  .welcome-content {
    padding: 20px 16px;
  }
  
  .welcome-content h1 {
    font-size: 2.5em;
  }
  
  .welcome-subtitle {
    font-size: 1.1em;
  }
  
  .welcome-icon i {
    font-size: 60px;
  }
  
  .feature-grid {
    grid-template-columns: 1fr;
    gap: 16px;
    margin-bottom: 40px;
  }
  
  .feature-card {
    padding: 20px;
  }
  
  .action-grid {
    grid-template-columns: 1fr;
    gap: 12px;
  }
  
  .action-card {
    padding: 16px;
  }
  
  .quick-actions h3 {
    font-size: 1.5em;
  }
  
  .tips-grid {
    grid-template-columns: 1fr;
  }
  
  .activity-item {
    padding: 12px;
  }
  
  .activity-content h4 {
    font-size: 0.9em;
  }
  
  .empty-content {
    padding: 20px;
  }
  
  .empty-content i {
    font-size: 48px;
  }
}
</style> 