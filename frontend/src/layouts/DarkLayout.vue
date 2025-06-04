<template>
  <div class="dark-layout">
    <!-- 主体内容 -->
    <div class="main-container">
      <!-- 侧边栏 -->
      <div class="sidebar">
        <div class="sidebar-controls">
          <el-tooltip content="文件管理" placement="right">
            <el-icon class="sidebar-icon active"><Document /></el-icon>
          </el-tooltip>
          <el-tooltip content="搜索" placement="right">
            <el-icon class="sidebar-icon" @click="togglePopup('search')"><Search /></el-icon>
          </el-tooltip>
          <el-tooltip content="收藏夹" placement="right" class="not-implemented">
            <el-icon class="sidebar-icon"><Star /></el-icon>
          </el-tooltip>
          <el-tooltip content="标签管理" placement="right" class="not-implemented">
            <el-icon class="sidebar-icon"><PriceTag /></el-icon>
          </el-tooltip>
          <el-tooltip content="设置" placement="right" class="not-implemented">
            <el-icon class="sidebar-icon"><Setting /></el-icon>
          </el-tooltip>
        </div>
        
        <div class="sidebar-content">
          <!-- 文件浏览视图始终显示 -->
          <div class="view-content file-view">
            <div class="view-header">
              <span>文件</span>
              <div class="view-actions">
                <el-tooltip content="新建文件" placement="bottom">
                  <el-icon class="action-icon" @click="createNewDocument"><Plus /></el-icon>
                </el-tooltip>
                <el-tooltip content="新建文件夹" placement="bottom">
                  <el-icon class="action-icon" @click="createNewFolder"><FolderAdd /></el-icon>
                </el-tooltip>
              </div>
            </div>
            
            <div class="folder-tree">
              <div 
                v-for="folder in folders" 
                :key="folder.id" 
                class="folder-item expandable"
                :class="{ active: folder.expanded }"
                @click="toggleFolder(folder.id)"
              >
                <span>{{ folder.name }}</span>
                <div v-if="folder.expanded" class="folder-actions">
                  <el-tooltip content="添加文件" placement="top">
                    <el-icon class="folder-action-icon" @click.stop="addFileToFolder(folder.id)"><Plus /></el-icon>
                  </el-tooltip>
                </div>
                <div v-if="folder.expanded" class="sub-items">
                  <div 
                    v-for="file in folder.files" 
                    :key="file.id"
                    class="file-item" 
                    :class="{ active: activeDocument === file.id }" 
                    @click.stop="openDocument(file.id)"
                  >
                    <span class="file-name">{{ file.title }} <span class="tag" v-if="file.type">{{ file.type }}</span></span>
                  </div>
                  <div v-if="folder.files.length === 0" class="empty-folder">
                    <span>文件夹为空</span>
                  </div>
                </div>
              </div>
              <div v-if="folders.length === 0" class="empty-folders">
                <span>没有文件夹</span>
                <el-button size="small" type="primary" @click="createNewFolder">创建文件夹</el-button>
              </div>
            </div>
          </div>
          
          <!-- 弹出窗口容器 -->
          <div v-if="activePopup" class="popup-container" @click.self="closePopup">
            <!-- 搜索弹出窗口 -->
            <div v-if="activePopup === 'search'" class="popup-content search-popup">
              <div class="popup-header">
                <span>搜索</span>
                <el-icon class="close-popup-icon" @click="closePopup"><Close /></el-icon>
              </div>
              
              <div class="search-container">
                <el-input 
                  v-model="searchQuery" 
                  placeholder="搜索文档..." 
                  clearable
                  class="search-input"
                  @keyup.enter="performSearch"
                >
                  <template #prefix>
                    <el-icon><Search /></el-icon>
                  </template>
                </el-input>
                
                <div class="search-options">
                  <el-checkbox v-model="searchOptions.title">标题</el-checkbox>
                  <el-checkbox v-model="searchOptions.content">内容</el-checkbox>
                  <el-checkbox v-model="searchOptions.tags" class="not-implemented">标签</el-checkbox>
                </div>
                
                <div v-if="searchResults.length > 0" class="search-results">
                  <div class="result-count">找到 {{ searchResults.length }} 个结果</div>
                  <div 
                    v-for="(result, index) in searchResults" 
                    :key="index" 
                    class="search-result-item"
                    @click="openDocument(result.id)"
                  >
                    <div class="result-title">{{ result.title }}</div>
                    <div class="result-preview">{{ result.preview }}</div>
                  </div>
                </div>
                
                <div v-else-if="hasSearched" class="no-results">
                  未找到匹配的结果
                </div>
              </div>
            </div>
          </div>
          
          <div class="sidebar-footer">
            <div class="footer-left">
              <el-dropdown trigger="click">
                <div class="avatar-container">
                  <el-avatar 
                    :size="24" 
                    :src="userAvatar" 
                    class="user-avatar"
                  >
                    {{ userInitial }}
                  </el-avatar>
                  <span class="username">{{ userName }}</span>
                </div>
                <template #dropdown>
                  <el-dropdown-menu class="dark-dropdown">
                    <template v-if="isUserLoggedIn">
                      <el-dropdown-item @click="goToProfile">
                        <el-icon><User /></el-icon>
                        <span>个人中心</span>
                      </el-dropdown-item>
                      <el-dropdown-item class="not-implemented">
                        <el-icon><Setting /></el-icon>
                        <span>设置</span>
                      </el-dropdown-item>
                      <el-dropdown-item divided @click="handleLogout">
                        <el-icon><Switch /></el-icon>
                        <span>退出登录</span>
                      </el-dropdown-item>
                    </template>
                    <template v-else>
                      <el-dropdown-item @click="goToLogin">
                        <el-icon><Key /></el-icon>
                        <span>登录</span>
                      </el-dropdown-item>
                      <el-dropdown-item @click="goToRegister">
                        <el-icon><Avatar /></el-icon>
                        <span>注册</span>
                      </el-dropdown-item>
                    </template>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
            <div class="footer-actions">
              <el-tooltip content="同步" placement="top" class="not-implemented">
                <el-icon class="footer-icon"><RefreshRight /></el-icon>
              </el-tooltip>
              <el-tooltip content="设置" placement="top">
                <el-icon class="footer-icon"><Setting /></el-icon>
              </el-tooltip>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 内容区域 -->
      <div class="content-area">
        <!-- 标签页区域 -->
        <div class="tabs-container" v-if="openedTabs.length > 0">
          <div 
            v-for="tab in openedTabs" 
            :key="tab.id"
            class="tab-item"
            :class="{ active: activeDocument === tab.id }"
            @click="switchToTab(tab.id)"
          >
            <el-icon class="tab-icon"><Document /></el-icon>
            <span class="tab-title">{{ tab.title }}</span>
            <el-icon class="close-icon" @click.stop="closeTab(tab.id)"><Close /></el-icon>
          </div>
          <el-icon class="add-tab-icon" @click="createNewDocument"><Plus /></el-icon>
        </div>
        
        <!-- 主内容区域 -->
        <div class="main-content" :class="{ 'has-tabs': openedTabs.length > 0 }">
          <router-view v-if="hasActiveDocument" />
          
          <div v-else class="empty-state">
            <h2>知识管理平台</h2>
            <p>集中管理您的知识、笔记和文档</p>
            
            <div class="quick-actions">
              <div class="action-group">
                <h3>开始使用</h3>
                <div class="action-buttons">
                  <div class="action-button" @click="createNewDocument">
                    <el-icon><Document /></el-icon>
                    <span>创建新文档</span>
                  </div>
                  <div class="action-button" @click="openExistingDocument">
                    <el-icon><FolderOpened /></el-icon>
                    <span>打开文档</span>
                  </div>
                </div>
              </div>
              
              <div class="action-group">
                <h3>最近文档</h3>
                <div v-if="recentDocuments.length > 0" class="recent-documents">
                  <div 
                    v-for="doc in recentDocuments" 
                    :key="doc.id" 
                    class="recent-document-item"
                    @click="openDocument(doc.id)"
                  >
                    <el-icon><Document /></el-icon>
                    <span>{{ doc.title }}</span>
                  </div>
                </div>
                <div v-else class="no-recent">
                  没有最近的文档
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  Document,
  Search,
  Star,
  PriceTag,
  Setting,
  Plus,
  FolderAdd,
  FolderOpened,
  User,
  RefreshRight,
  Close,
  Key,
  Avatar,
  CirclePlus,
  Switch
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

// 导入默认头像
const defaultAvatar = new URL('@/assets/images/default-avatar.svg', import.meta.url).href

const router = useRouter()
const route = useRoute()
const activeDocument = ref('')
const searchQuery = ref('')
const searchOptions = ref({
  title: true,
  content: true,
  tags: false
})
const hasSearched = ref(false)
const searchResults = ref([])

// 弹出窗口控制
const activePopup = ref(null)

// 添加userStore
const userStore = useUserStore()

// 用户信息
const userInfo = ref({
  name: '',
  avatar: ''
})

// 文件夹和文件数据
const folders = ref([
  { 
    id: 'folder1', 
    name: '我的文档', 
    expanded: true,
    files: [
      { id: 'new', title: '新文档', type: 'MD' }
    ]
  }
])

// 已打开的标签页
const openedTabs = ref([])

// 计算属性：获取用户头像
const userAvatar = computed(() => {
  console.log('用户登录状态:', userStore.isAuthenticated, '有token:', !!userStore.token);
  return userStore.isAuthenticated && userStore.user?.avatar 
    ? userStore.user.avatar 
    : defaultAvatar
})

// 计算属性：是否已登录
const isUserLoggedIn = computed(() => {
  return userStore.isAuthenticated;
})

// 计算属性：获取用户名
const userName = computed(() => {
  return userStore.user?.username || '未登录'
})

// 计算属性：获取用户名首字母
const userInitial = computed(() => {
  return userStore.user?.username ? userStore.user.username.charAt(0).toUpperCase() : '?'
})

// 计算属性：是否有活动文档
const hasActiveDocument = computed(() => activeDocument.value !== '')

// 模拟最近文档
const recentDocuments = ref([
  { id: 'programming', title: '编程语言', lastModified: '2023-11-20' },
  { id: 'frameworks', title: '框架与库', lastModified: '2023-11-15' }
])

// 切换弹出窗口
const togglePopup = (popup) => {
  if (activePopup.value === popup) {
    activePopup.value = null
  } else {
    activePopup.value = popup
  }
}

// 关闭弹出窗口
const closePopup = () => {
  activePopup.value = null
}

// 切换文件夹展开/折叠状态
const toggleFolder = (folderId) => {
  const folder = folders.value.find(f => f.id === folderId)
  if (folder) {
    folder.expanded = !folder.expanded
  }
}

// 创建新文件夹
const createNewFolder = () => {
  // 生成唯一ID
  const folderId = `folder${Date.now()}`
  
  // 弹出输入框让用户输入文件夹名称
  ElMessageBox.prompt('请输入文件夹名称', '新建文件夹', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    inputPattern: /\S+/,
    inputErrorMessage: '文件夹名称不能为空'
  }).then(({ value }) => {
    // 创建新文件夹
    folders.value.push({
      id: folderId,
      name: value,
      expanded: true,
      files: []
    })
    
    ElMessage({
      type: 'success',
      message: `文件夹 "${value}" 创建成功`
    })
  }).catch(() => {
    // 用户取消
  })
}

// 向文件夹中添加文件
const addFileToFolder = (folderId) => {
  createNewDocument(folderId)
}

// 打开文档
const openDocument = (id) => {
  activeDocument.value = id
  
  // 添加到打开的标签页中（如果不存在）
  const existingTab = openedTabs.value.find(tab => tab.id === id)
  if (!existingTab) {
    // 查找文档标题
    let title = '文档'
    
    // 在所有文件夹中查找文件
    for (const folder of folders.value) {
      const file = folder.files.find(f => f.id === id)
      if (file) {
        title = file.title
        break
      }
    }
    
    openedTabs.value.push({ id, title })
  }
  
  // 导航到文档页面
  router.push({ name: 'Document', params: { id } })
}

// 创建新文档
const createNewDocument = (folderId = null) => {
  // 生成唯一ID
  const docId = `doc${Date.now()}`
  
  // 如果指定了文件夹ID，则添加到该文件夹
  if (folderId) {
    const folder = folders.value.find(f => f.id === folderId)
    if (folder) {
      folder.files.push({
        id: docId,
        title: '新文档',
        type: 'MD'
      })
    }
  } else {
    // 如果未指定文件夹，则添加到第一个文件夹
    if (folders.value.length > 0) {
      folders.value[0].files.push({
        id: docId,
        title: '新文档',
        type: 'MD'
      })
    } else {
      // 如果没有文件夹，先创建一个
      ElMessageBox.confirm('没有可用的文件夹，是否创建一个新文件夹？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'info'
      }).then(() => {
        createNewFolder()
      }).catch(() => {
        // 用户取消
      })
      return
    }
  }
  
  // 添加到标签页
  if (!openedTabs.value.find(tab => tab.id === docId)) {
    openedTabs.value.push({ id: docId, title: '新文档' })
  }
  
  // 设置为活动文档
  activeDocument.value = docId
  
  // 导航到文档页面
  router.push({ name: 'Document', params: { id: docId } })
}

// 打开现有文档
const openExistingDocument = () => {
  // 实现打开文档逻辑
  togglePopup('search')
}

// 切换到指定标签页
const switchToTab = (id) => {
  activeDocument.value = id
  router.push({ name: 'Document', params: { id } })
}

// 关闭标签页
const closeTab = (id) => {
  const index = openedTabs.value.findIndex(tab => tab.id === id)
  if (index !== -1) {
    openedTabs.value.splice(index, 1)
    
    // 如果关闭的是当前活动标签页，则切换到最后一个标签页
    if (activeDocument.value === id) {
      if (openedTabs.value.length > 0) {
        const lastTab = openedTabs.value[openedTabs.value.length - 1]
        switchToTab(lastTab.id)
      } else {
        // 如果没有标签页了，返回空白状态
        activeDocument.value = ''
        router.push('/')
      }
    }
  }
}

// 执行搜索
const performSearch = () => {
  if (!searchQuery.value) return
  
  hasSearched.value = true
  
  // 收集所有文件
  const allFiles = []
  folders.value.forEach(folder => {
    folder.files.forEach(file => {
      allFiles.push({
        id: file.id,
        title: file.title
      })
    })
  })
  
  // 模拟搜索结果
  searchResults.value = allFiles
    .filter(file => file.title.toLowerCase().includes(searchQuery.value.toLowerCase()))
    .map(file => ({
      id: file.id,
      title: file.title,
      preview: `包含"${searchQuery.value}"的文档...`
    }))
}

// 导航到登录页
const goToLogin = () => {
  router.push('/login')
}

// 导航到注册页
const goToRegister = () => {
  router.push('/register')
}

// 导航到个人中心
const goToProfile = () => {
  if (userStore.isAuthenticated) {
    router.push('/profile')
  } else {
    router.push('/login')
  }
}

// 处理登出
const handleLogout = async () => {
  try {
    await userStore.logout()
    ElMessage.success('退出登录成功')
    router.push('/')
  } catch (error) {
    console.error('退出登录失败:', error)
    ElMessage.error('退出登录失败')
  }
}

// 监听路由变化，同步活动文档
watch(() => route.params.id, (newId) => {
  if (newId) {
    activeDocument.value = newId
    
    // 如果标签页中不存在，添加到标签页
    if (!openedTabs.value.find(tab => tab.id === newId)) {
      openDocument(newId)
    }
  }
}, { immediate: true })

// 页面加载时，根据路由参数设置活动文档
onMounted(() => {
  if (route.params.id) {
    openDocument(route.params.id)
  }
  
  // 检查登录状态
  console.log('组件挂载时登录状态:', userStore.isAuthenticated);
  console.log('Token值:', userStore.token);
  console.log('用户信息:', userStore.user);
  
  // 如果有token但没有用户信息，尝试重新获取
  if (userStore.token && !userStore.user) {
    userStore.getUserProfile().catch(err => {
      console.error('获取用户信息失败:', err);
    });
  }
})
</script>

<style lang="scss" scoped>
.dark-layout {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background-color: #1e1e1e;
  color: #e8e8e8;
  font-family: 'Segoe UI', Arial, sans-serif;
}

// 深色下拉菜单样式
:deep(.dark-dropdown) {
  background-color: #252526;
  border: 1px solid #3c3c3c;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
  
  .el-dropdown-menu__item {
    color: #e8e8e8;
    display: flex;
    align-items: center;
    gap: 8px;
    
    &:hover {
      background-color: #2a2d2e;
      color: #8a2be2;
    }
    
    &.is-disabled {
      color: #666;
      cursor: not-allowed;
    }
    
    .el-icon {
      margin-right: 2px;
    }
  }
  
  .el-dropdown-menu__item--divided::before {
    background-color: #3c3c3c;
  }
}

// 主体内容
.main-container {
  display: flex;
  flex: 1;
  overflow: hidden;
}

// 侧边栏
.sidebar {
  display: flex;
  width: 240px;
  background-color: #252526;
  border-right: 1px solid #3c3c3c;
  position: relative;
  z-index: 10;
  
  .sidebar-controls {
    display: flex;
    flex-direction: column;
    width: 40px;
    background-color: #333333;
    padding-top: 15px;
    
    .sidebar-icon {
      color: #858585;
      font-size: 16px;
      margin-bottom: 20px;
      cursor: pointer;
      display: flex;
      justify-content: center;
      transition: color 0.2s ease;
      
      &:hover {
        color: #e8e8e8;
      }
      
      &.active {
        color: #8a2be2;
      }
    }
  }
  
  .sidebar-content {
    flex: 1;
    display: flex;
    flex-direction: column;
    position: relative;
    
    // 弹出窗口容器
    .popup-container {
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background-color: rgba(0, 0, 0, 0.5);
      z-index: 100;
      display: flex;
      align-items: center;
      justify-content: center;
      
      .popup-content {
        width: 90%;
        max-height: 80%;
        background-color: #252526;
        border-radius: 4px;
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
        overflow: hidden;
        display: flex;
        flex-direction: column;
        
        .popup-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          padding: 10px 15px;
          border-bottom: 1px solid #3c3c3c;
          
          .close-popup-icon {
            cursor: pointer;
            transition: color 0.2s ease;
            
            &:hover {
              color: #8a2be2;
            }
          }
        }
      }
      
      .search-popup {
        height: 400px;
        
        .search-container {
          flex: 1;
          padding: 15px;
          overflow-y: auto;
          
          .search-input {
            margin-bottom: 15px;
            
            :deep(.el-input__wrapper) {
              background-color: #2d2d2d;
              box-shadow: 0 0 0 1px #3c3c3c inset;
              
              &:hover {
                box-shadow: 0 0 0 1px #8a2be2 inset;
              }
              
              &.is-focus {
                box-shadow: 0 0 0 1px #8a2be2 inset;
              }
            }
            
            :deep(.el-input__inner) {
              color: #e8e8e8;
              
              &::placeholder {
                color: #858585;
              }
            }
            
            :deep(.el-input__prefix-inner .el-icon) {
              color: #858585;
            }
          }
          
          .search-options {
            display: flex;
            gap: 15px;
            margin-bottom: 20px;
            
            .el-checkbox {
              color: #cccccc;
              
              :deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
                background-color: #8a2be2;
                border-color: #8a2be2;
              }
              
              :deep(.el-checkbox__input.is-checked + .el-checkbox__label) {
                color: #e8e8e8;
              }
            }
          }
          
          .search-results {
            background-color: #252526;
            border-radius: 4px;
            padding: 10px;
            border: 1px solid #3c3c3c;
            
            .result-count {
              font-size: 12px;
              color: #858585;
              margin-bottom: 10px;
              padding-bottom: 8px;
              border-bottom: 1px solid #3c3c3c;
            }
            
            .search-result-item {
              padding: 10px;
              border-radius: 4px;
              margin-bottom: 8px;
              cursor: pointer;
              transition: all 0.2s ease;
              border: 1px solid transparent;
              
              &:hover {
                background-color: #2a2d2e;
                border-color: #3c3c3c;
                transform: translateX(2px);
              }
              
              .result-title {
                font-size: 14px;
                margin-bottom: 5px;
                color: #8a2be2;
                font-weight: 500;
              }
              
              .result-preview {
                font-size: 12px;
                color: #cccccc;
                white-space: nowrap;
                overflow: hidden;
                text-overflow: ellipsis;
                line-height: 1.4;
              }
            }
          }
          
          .no-results {
            text-align: center;
            padding: 20px 0;
            color: #858585;
            font-style: italic;
            background-color: #252526;
            border-radius: 4px;
            margin-top: 10px;
          }
        }
      }
    }
    
    .view-content {
      flex: 1;
      display: flex;
      flex-direction: column;
      
      .view-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 0 15px;
        height: 40px;
        border-bottom: 1px solid #3c3c3c;
        font-size: 14px;
        position: relative;
        z-index: 5;
        
        .view-actions {
          display: flex;
          align-items: center;
          gap: 10px;
          
          .action-icon {
            font-size: 14px;
            cursor: pointer;
            transition: color 0.2s ease;
            
            &:hover {
              color: #8a2be2;
            }
          }
        }
      }
    }
    
    .folder-tree {
      flex: 1;
      padding: 10px 0;
      overflow-y: auto;
      overflow-x: hidden;
      
      .folder-item {
        position: relative;
        padding: 6px 15px;
        cursor: pointer;
        font-size: 13px;
        transition: background-color 0.2s ease;
        
        &:hover {
          background-color: #2a2d2e;
        }
        
        &.expandable {
          position: relative;
          
          &:before {
            content: '▶';
            position: absolute;
            left: 5px;
            color: #858585;
            font-size: 8px;
            transition: transform 0.2s ease, color 0.2s ease;
          }
        }
        
        &.active {
          &:before {
            content: '▼';
            color: #8a2be2;
          }
        }
        
        .folder-actions {
          position: absolute;
          right: 8px;
          top: 50%;
          transform: translateY(-50%);
          display: none;
          
          .folder-action-icon {
            font-size: 12px;
            color: #858585;
            margin-left: 6px;
            transition: color 0.2s ease;
            
            &:hover {
              color: #8a2be2;
            }
          }
        }
        
        &:hover .folder-actions {
          display: flex;
        }
      }
      
      .sub-items {
        margin-left: 10px;
        
        .file-item {
          padding: 6px 15px;
          cursor: pointer;
          font-size: 13px;
          transition: background-color 0.2s ease, color 0.2s ease;
          
          &:hover {
            background-color: #2a2d2e;
          }
          
          &.active {
            background-color: #37373d;
            color: #8a2be2;
          }
          
          .file-name {
            display: block;
            word-break: break-word;
            white-space: normal;
            line-height: 1.4;
          }
          
          .tag {
            background-color: #4d4d4d;
            padding: 1px 4px;
            border-radius: 3px;
            font-size: 11px;
            margin-left: 5px;
          }
        }
        
        .empty-folder {
          padding: 10px 15px;
          font-style: italic;
          color: #858585;
          font-size: 12px;
        }
      }
      
      .empty-folders {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 15px;
        padding: 20px;
        color: #858585;
        font-style: italic;
        font-size: 13px;
      }
    }
    
    .sidebar-footer {
      height: 30px;
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 0 15px;
      background-color: #2d2d2d;
      font-size: 12px;
      color: #858585;
      
      .footer-left {
        display: flex;
        align-items: center;
        
        .avatar-container {
          display: flex;
          align-items: center;
          gap: 8px;
          cursor: pointer;
          
          .user-avatar {
            border: 1px solid #3c3c3c;
          }
          
          .username {
            max-width: 120px;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
          }
        }
      }
      
      .footer-actions {
        display: flex;
        gap: 10px;
        
        .footer-icon {
          cursor: pointer;
          font-size: 14px;
          transition: color 0.2s ease;
          
          &:hover {
            color: #e8e8e8;
          }
        }
      }
    }
  }
}

// 内容区域
.content-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  background-color: #1e1e1e;
  
  // 标签页容器
  .tabs-container {
    display: flex;
    height: 36px;
    background-color: #252526;
    border-bottom: 1px solid #3c3c3c;
    overflow-x: auto;
    
    // 隐藏滚动条
    &::-webkit-scrollbar {
      display: none;
    }
    
    .tab-item {
      display: flex;
      align-items: center;
      min-width: 120px;
      max-width: 200px;
      height: 100%;
      padding: 0 8px;
      background-color: #2d2d2d;
      border-right: 1px solid #3c3c3c;
      cursor: pointer;
      transition: background-color 0.2s ease;
      
      &.active {
        background-color: #1e1e1e;
        border-top: 1px solid #8a2be2;
      }
      
      .tab-icon {
        font-size: 14px;
        margin-right: 6px;
        color: #858585;
      }
      
      .tab-title {
        flex: 1;
        font-size: 13px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
      
      .close-icon {
        font-size: 14px;
        color: #858585;
        visibility: hidden;
        transition: color 0.2s ease;
        
        &:hover {
          color: #e8e8e8;
        }
      }
      
      &:hover .close-icon {
        visibility: visible;
      }
    }
    
    .add-tab-icon {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 36px;
      height: 36px;
      color: #858585;
      font-size: 14px;
      cursor: pointer;
      transition: all 0.2s ease;
      
      &:hover {
        color: #e8e8e8;
        background-color: #2a2d2e;
      }
    }
  }
  
  // 主内容区域
  .main-content {
    flex: 1;
    overflow-y: auto;
    
    &.has-tabs {
      background-color: #1e1e1e;
    }
    
    .empty-state {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      height: 100%;
      padding: 40px;
      text-align: center;
      
      h2 {
        font-size: 28px;
        font-weight: normal;
        margin-bottom: 10px;
        color: #e8e8e8;
      }
      
      p {
        font-size: 16px;
        color: #858585;
        margin-bottom: 30px;
      }
      
      .quick-actions {
        display: flex;
        flex-direction: column;
        gap: 30px;
        
        .action-group {
          h3 {
            font-size: 18px;
            font-weight: normal;
            margin-bottom: 15px;
            color: #cccccc;
          }
          
          .action-buttons {
            display: flex;
            justify-content: center;
            gap: 20px;
            
            .action-button {
              display: flex;
              flex-direction: column;
              align-items: center;
              gap: 8px;
              padding: 15px;
              width: 140px;
              border-radius: 6px;
              background-color: #2d2d2d;
              cursor: pointer;
              transition: background-color 0.2s ease, transform 0.2s ease;
              border: 1px solid #3c3c3c;
              
              &:hover {
                background-color: #3c3c3c;
                transform: translateY(-2px);
              }
              
              .el-icon {
                font-size: 24px;
                color: #8a2be2;
              }
              
              span {
                font-size: 14px;
              }
            }
          }
          
          .recent-documents {
            display: flex;
            flex-direction: column;
            gap: 10px;
            
            .recent-document-item {
              display: flex;
              align-items: center;
              gap: 8px;
              padding: 8px 12px;
              border-radius: 4px;
              cursor: pointer;
              
              &:hover {
                background-color: #2a2d2e;
              }
              
              .el-icon {
                color: #8a2be2;
              }
            }
          }
          
          .no-recent {
            color: #858585;
            font-style: italic;
          }
        }
      }
    }
  }
}

// 标记未完成功能
.not-implemented {
  position: relative;
  opacity: 0.7;
  
  &:after {
    content: '(未实现)';
    position: absolute;
    top: -2px;
    right: -50px;
    color: #ff6b6b;
    font-size: 12px;
    white-space: nowrap;
  }
}
</style> 