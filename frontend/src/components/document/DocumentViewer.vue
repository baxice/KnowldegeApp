<template>
  <div class="document-viewer">
    <div class="document-header">
      <div class="document-info">
        <div class="document-title">
          <i :class="getFileIcon(document.fileType)" class="file-icon"></i>
          <h2>{{ document.title }}</h2>
        </div>
        <div class="document-meta">
          <span class="meta-item">
            <i class="fas fa-calendar"></i>
            创建时间: {{ formatDate(document.createdAt) }}
          </span>
          <span class="meta-item">
            <i class="fas fa-hdd"></i>
            文件大小: {{ formatFileSize(document.fileSize) }}
          </span>
          <span class="meta-item">
            <i class="fas fa-file"></i>
            文件类型: {{ document.fileType }}
          </span>
        </div>
        <div class="document-description" v-if="document.description">
          <p>{{ document.description }}</p>
        </div>
        <div class="document-tags" v-if="document.tags && document.tags.length">
          <span 
            v-for="tag in document.tags" 
            :key="tag.id"
            class="tag"
            :style="{ backgroundColor: tag.color }"
          >
            {{ tag.name }}
          </span>
        </div>
      </div>
      
      <div class="document-actions">
        <button v-if="canEdit" class="btn btn-primary" @click="editDocument" title="编辑">
          <i class="fas fa-edit"></i>
          编辑
        </button>
        <button v-if="canDelete" class="btn btn-danger" @click="deleteDocument" title="删除">
          <i class="fas fa-trash"></i>
          删除
        </button>
      </div>
    </div>
    
    <div class="document-content">
      <!-- 加载状态 -->
      <div v-if="isLoading" class="loading-state">
        <i class="fas fa-spinner fa-spin"></i>
        <p>正在加载内容...</p>
      </div>
      
      <!-- 文档预览区域 -->
      <div v-else class="preview-container">
        <!-- Markdown预览 -->
        <div v-if="isMarkdown" class="markdown-preview">
          <div v-if="markdownContent" class="markdown-body" v-html="markdownContent"></div>
          <div v-else class="empty-content">
            <i class="fas fa-file-alt"></i>
            <h3>空的Markdown文档</h3>
            <p>这是一个空的Markdown文档，点击编辑按钮开始编写内容。</p>
            <button v-if="canEdit" class="btn btn-primary" @click="editDocument">
              <i class="fas fa-edit"></i>
              开始编辑
            </button>
          </div>
        </div>
        
        <!-- 图片预览 -->
        <div v-else-if="isImage" class="image-preview">
          <img :src="documentUrl" :alt="document.title" />
        </div>
        
        <!-- PDF预览 -->
        <div v-else-if="isPdf" class="pdf-preview">
          <iframe :src="documentUrl" width="100%" height="600px"></iframe>
        </div>
        
        <!-- 文本文件预览 -->
        <div v-else-if="isText" class="text-preview">
          <pre v-if="textContent" class="text-content">{{ textContent }}</pre>
          <div v-else class="empty-content">
            <i class="fas fa-file-alt"></i>
            <h3>无法加载文本内容</h3>
            <p>文件内容无法显示，可能是编码问题或文件损坏。</p>
          </div>
        </div>
        
        <!-- 其他文件类型提示 -->
        <div v-else class="file-info">
          <div class="file-icon-large">
            <i :class="getFileIcon(document.fileType)"></i>
          </div>
          <h3>{{ document.title }}</h3>
          <p>此文件类型暂不支持在线预览。</p>
          <p class="file-type-info">文件类型: {{ document.fileType }}</p>
          <p class="file-size-info">文件大小: {{ formatFileSize(document.fileSize) }}</p>
        </div>
      </div>
    </div>
    
    <!-- 编辑对话框 -->
    <div v-if="showEditDialog" class="modal-overlay" @click="closeEditDialog">
      <div class="modal-dialog" @click.stop>
        <div class="modal-header">
          <h3>编辑文档信息</h3>
          <button class="close-btn" @click="closeEditDialog">×</button>
        </div>
        <div class="modal-body">
          <form @submit.prevent="saveDocument">
            <div class="form-group">
              <label>文档标题</label>
              <input 
                type="text" 
                v-model="editForm.title" 
                class="form-control"
                required
              />
            </div>
            <div class="form-group">
              <label>文档描述</label>
              <textarea 
                v-model="editForm.description" 
                class="form-control"
                rows="3"
              ></textarea>
            </div>
            <div class="form-group">
              <label>标签</label>
              <input 
                type="text" 
                v-model="tagInput"
                @keydown.enter.prevent="addTag"
                class="form-control"
                placeholder="输入标签名称，按回车添加"
              />
              <div class="tags-container">
                <span 
                  v-for="(tag, index) in editForm.tags" 
                  :key="index"
                  class="tag editable"
                >
                  {{ tag }}
                  <button type="button" @click="removeTag(index)">×</button>
                </span>
              </div>
            </div>
          </form>
        </div>
        <div class="modal-footer">
          <button type="button" class="btn" @click="closeEditDialog">取消</button>
          <button type="button" class="btn btn-primary" @click="saveDocument">保存</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { marked } from 'marked'
import hljs from 'highlight.js'
import { documentApi } from '@/api/document'

// 配置marked
marked.setOptions({
  highlight: function(code, lang) {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return hljs.highlight(code, { language: lang }).value
      } catch (err) {}
    }
    return hljs.highlightAuto(code).value
  },
  breaks: true,
  gfm: true
})

// Props
const props = defineProps({
  document: {
    type: Object,
    required: true
  },
  canEdit: {
    type: Boolean,
    default: false
  },
  canDelete: {
    type: Boolean,
    default: false
  }
})

// Emits
const emit = defineEmits(['edit', 'delete'])

// 响应式数据
const documentUrl = ref('')
const textContent = ref('')
const markdownContent = ref('')
const showEditDialog = ref(false)
const tagInput = ref('')
const isLoading = ref(false)

const editForm = ref({
  title: '',
  description: '',
  tags: []
})

// 计算属性
const isImage = computed(() => {
  if (!props.document.fileType) return false
  return props.document.fileType.startsWith('image/')
})

const isPdf = computed(() => {
  return props.document.fileType === 'application/pdf'
})

const isText = computed(() => {
  if (!props.document.fileType) return false
  return props.document.fileType.startsWith('text/') && !isMarkdown.value
})

const isMarkdown = computed(() => {
  return props.document.fileType === 'text/markdown' || 
         props.document.fileType === 'text/x-markdown' ||
         props.document.title?.toLowerCase().endsWith('.md')
})

// 检查是否有内容需要显示
const hasContent = computed(() => {
  return props.document.content && props.document.content.trim()
})

// 方法
const getFileIcon = (fileType) => {
  if (!fileType) return 'fas fa-file-alt'
  
  if (fileType.startsWith('image/')) return 'fas fa-file-image'
  if (fileType === 'application/pdf') return 'fas fa-file-pdf'
  if (fileType.startsWith('text/')) return 'fas fa-file-alt'
  if (fileType.includes('word')) return 'fas fa-file-word'
  if (fileType.includes('excel') || fileType.includes('spreadsheet')) return 'fas fa-file-excel'
  if (fileType.includes('powerpoint') || fileType.includes('presentation')) return 'fas fa-file-powerpoint'
  
  return 'fas fa-file'
}

const formatDate = (dateString) => {
  if (!dateString) return '未知时间'
  const date = new Date(dateString)
  return date.toLocaleString('zh-CN')
}

const formatFileSize = (bytes) => {
  if (!bytes) return '0 B'
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(1024))
  return Math.round(bytes / Math.pow(1024, i) * 100) / 100 + ' ' + sizes[i]
}

const loadDocumentContent = async () => {
  try {
    isLoading.value = true
    
    // 如果是Markdown并且有content字段，直接使用
    if (isMarkdown.value && hasContent.value) {
      markdownContent.value = marked(props.document.content)
      return
    }
    
    // 如果有文件路径，获取文档URL和内容
    if (props.document.filePath) {
      const response = await documentApi.getDocument(props.document.id)
      documentUrl.value = response.fileUrl || ''
      
      // 如果是文本文件，尝试加载内容
      if (isText.value || isMarkdown.value) {
        if (documentUrl.value) {
          const contentResponse = await fetch(documentUrl.value)
          const content = await contentResponse.text()
          
          if (isMarkdown.value) {
            markdownContent.value = marked(content)
          } else {
            textContent.value = content
          }
        }
      }
    }
  } catch (error) {
    console.error('加载文档内容失败:', error)
  } finally {
    isLoading.value = false
  }
}

const editDocument = () => {
  emit('edit', props.document)
}

const deleteDocument = () => {
  if (confirm(`确定要删除文档 "${props.document.title}" 吗？此操作不可恢复。`)) {
    emit('delete', props.document)
  }
}

const closeEditDialog = () => {
  showEditDialog.value = false
  tagInput.value = ''
}

const addTag = () => {
  const tag = tagInput.value.trim()
  if (tag && !editForm.value.tags.includes(tag)) {
    editForm.value.tags.push(tag)
    tagInput.value = ''
  }
}

const removeTag = (index) => {
  editForm.value.tags.splice(index, 1)
}

const saveDocument = async () => {
  try {
    const updatedDocument = {
      ...props.document,
      ...editForm.value
    }
    
    await documentApi.update(updatedDocument.id, updatedDocument)
    emit('edit', updatedDocument)
    closeEditDialog()
  } catch (error) {
    console.error('保存文档失败:', error)
    alert('保存失败，请重试')
  }
}

const downloadDocument = () => {
  // 实际下载逻辑
  if (documentUrl.value) {
    const link = document.createElement('a')
    link.href = documentUrl.value
    link.download = props.document.title
    link.click()
  }
}

// 生命周期
onMounted(() => {
  loadDocumentContent()
})
</script>

<style scoped>
@import 'highlight.js/styles/vs2015.css';

.document-viewer {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--bg-color);
  overflow: hidden;
}

.document-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 20px 24px;
  border-bottom: 1px solid var(--border-color);
  background: var(--surface-color);
  flex-shrink: 0;
}

.document-info {
  flex: 1;
}

.document-title {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.file-icon {
  font-size: 24px;
  color: var(--color-blue);
}

.document-title h2 {
  margin: 0;
  font-size: 1.8em;
  font-weight: 600;
  color: var(--text-color);
}

.document-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 24px;
  margin-bottom: 16px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--text-muted);
}

.meta-item i {
  color: var(--color-blue);
}

.document-description {
  margin-bottom: 16px;
}

.document-description p {
  margin: 0;
  color: var(--text-muted);
  line-height: 1.5;
}

.document-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.tag {
  padding: 4px 8px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
  color: white;
  background: var(--color-blue);
}

.document-actions {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border: 1px solid var(--border-color);
  border-radius: 6px;
  background: var(--surface-color-2);
  color: var(--text-color);
  cursor: pointer;
  transition: all 0.2s;
  font-size: 13px;
  text-decoration: none;
}

.btn:hover {
  background: var(--color-blue);
  color: white;
  border-color: var(--color-blue);
}

.btn-primary {
  background: var(--color-blue);
  color: white;
  border-color: var(--color-blue);
}

.btn-primary:hover {
  background: hsl(170, 100%, 65%);
  border-color: hsl(170, 100%, 65%);
}

.btn-danger {
  background: var(--color-red);
  color: white;
  border-color: var(--color-red);
}

.btn-danger:hover {
  background: #dc2626;
  border-color: #dc2626;
}

.document-content {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.loading-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--text-muted);
}

.loading-state i {
  font-size: 32px;
  margin-bottom: 16px;
  color: var(--color-blue);
}

.preview-container {
  flex: 1;
  overflow: auto;
  background: var(--bg-color);
}

.markdown-preview {
  padding: 24px;
  height: 100%;
}

.markdown-body {
  color: var(--text-color);
  line-height: 1.6;
  font-family: var(--font-main);
}

.markdown-body h1,
.markdown-body h2,
.markdown-body h3,
.markdown-body h4,
.markdown-body h5,
.markdown-body h6 {
  color: var(--color-blue);
  margin-top: 24px;
  margin-bottom: 16px;
  font-weight: 600;
}

.markdown-body h1 {
  font-size: 2em;
  border-bottom: 2px solid var(--border-color);
  padding-bottom: 8px;
}

.markdown-body h2 {
  font-size: 1.5em;
}

.markdown-body h3 {
  font-size: 1.25em;
}

.markdown-body p {
  margin-bottom: 16px;
}

.markdown-body ul,
.markdown-body ol {
  margin-bottom: 16px;
  padding-left: 24px;
}

.markdown-body li {
  margin-bottom: 8px;
}

.markdown-body blockquote {
  border-left: 4px solid var(--color-blue);
  padding-left: 16px;
  margin: 16px 0;
  color: var(--text-muted);
  font-style: italic;
}

.markdown-body code {
  background: var(--surface-color-2);
  color: var(--color-orange);
  padding: 2px 4px;
  border-radius: 3px;
  font-family: var(--font-code);
  font-size: 0.9em;
}

.markdown-body pre {
  background: var(--surface-color-2);
  padding: 16px;
  border-radius: 6px;
  overflow-x: auto;
  margin: 16px 0;
  border: 1px solid var(--border-color);
}

.markdown-body pre code {
  background: none;
  color: inherit;
  padding: 0;
}

.markdown-body table {
  border-collapse: collapse;
  width: 100%;
  margin: 16px 0;
}

.markdown-body th,
.markdown-body td {
  border: 1px solid var(--border-color);
  padding: 8px 12px;
  text-align: left;
}

.markdown-body th {
  background: var(--surface-color-2);
  font-weight: 600;
}

.markdown-body a {
  color: var(--color-blue);
  text-decoration: none;
}

.markdown-body a:hover {
  text-decoration: underline;
}

.markdown-body img {
  max-width: 100%;
  height: auto;
  border-radius: 4px;
  margin: 8px 0;
}

.empty-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 300px;
  text-align: center;
  color: var(--text-muted);
}

.empty-content i {
  font-size: 48px;
  margin-bottom: 16px;
  opacity: 0.5;
  color: var(--color-blue);
}

.empty-content h3 {
  margin: 0 0 8px 0;
  color: var(--text-color);
}

.empty-content p {
  margin: 0 0 20px 0;
  line-height: 1.5;
}

.image-preview {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 24px;
  height: 100%;
}

.image-preview img {
  max-width: 100%;
  max-height: 100%;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.pdf-preview {
  padding: 24px;
  height: 100%;
}

.pdf-preview iframe {
  border: 1px solid var(--border-color);
  border-radius: 6px;
}

.text-preview {
  padding: 24px;
  height: 100%;
}

.text-content {
  background: var(--surface-color-2);
  padding: 16px;
  border-radius: 6px;
  color: var(--text-color);
  font-family: var(--font-code);
  font-size: 13px;
  line-height: 1.5;
  margin: 0;
  white-space: pre-wrap;
  overflow: auto;
  border: 1px solid var(--border-color);
}

.file-info {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  text-align: center;
  padding: 40px;
  color: var(--text-muted);
}

.file-icon-large {
  margin-bottom: 24px;
}

.file-icon-large i {
  font-size: 80px;
  color: var(--color-blue);
  opacity: 0.5;
}

.file-info h3 {
  margin: 0 0 16px 0;
  color: var(--text-color);
  font-size: 1.5em;
}

.file-info p {
  margin: 8px 0;
  line-height: 1.5;
}

.file-type-info,
.file-size-info {
  font-size: 0.9em;
  color: var(--text-muted);
}

/* 响应式调整 */
@media (max-width: 768px) {
  .document-header {
    flex-direction: column;
    gap: 16px;
    padding: 16px;
  }
  
  .document-actions {
    width: 100%;
    justify-content: center;
  }
  
  .document-meta {
    flex-direction: column;
    gap: 8px;
  }
  
  .markdown-preview,
  .text-preview,
  .file-info {
    padding: 16px;
  }
  
  .file-icon-large i {
    font-size: 60px;
  }
}
</style> 