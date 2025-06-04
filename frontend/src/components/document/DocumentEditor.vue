<template>
  <div class="document-editor">
    <!-- 工具栏 -->
    <div class="editor-toolbar">
      <div class="toolbar-left">
        <input 
          v-model="localTitle" 
          class="title-input"
          placeholder="文档标题"
          @blur="saveTitle"
          @keyup.enter="$event.target.blur()"
        />
        <span class="status-indicator" :class="{ saving: isSaving, saved: !isDirty }">
          <i class="fas fa-circle"></i>
          {{ statusText }}
        </span>
      </div>
      
      <div class="toolbar-center">
        <!-- Markdown工具按钮 -->
        <div class="markdown-tools">
          <button class="tool-btn" @click="insertMarkdown('**', '**')" title="粗体 (Ctrl+B)">
            <i class="fas fa-bold"></i>
          </button>
          <button class="tool-btn" @click="insertMarkdown('*', '*')" title="斜体 (Ctrl+I)">
            <i class="fas fa-italic"></i>
          </button>
          <button class="tool-btn" @click="insertMarkdown('`', '`')" title="行内代码">
            <i class="fas fa-code"></i>
          </button>
          <button class="tool-btn" @click="insertHeading" title="标题">
            <i class="fas fa-heading"></i>
          </button>
          <button class="tool-btn" @click="insertList" title="列表">
            <i class="fas fa-list-ul"></i>
          </button>
          <button class="tool-btn" @click="insertLink" title="链接">
            <i class="fas fa-link"></i>
          </button>
          <button class="tool-btn" @click="insertTable" title="表格">
            <i class="fas fa-table"></i>
          </button>
          <button class="tool-btn" @click="insertCodeBlock" title="代码块">
            <i class="fas fa-code"></i>
          </button>
        </div>
      </div>
      
      <div class="toolbar-right">
        <button 
          class="view-btn" 
          :class="{ active: viewMode === 'edit' }"
          @click="setViewMode('edit')"
          title="编辑模式"
        >
          <i class="fas fa-edit"></i>
          编辑
        </button>
        <button 
          class="view-btn" 
          :class="{ active: viewMode === 'split' }"
          @click="setViewMode('split')"
          title="分屏模式"
        >
          <i class="fas fa-columns"></i>
          分屏
        </button>
        <button 
          class="view-btn" 
          :class="{ active: viewMode === 'preview' }"
          @click="setViewMode('preview')"
          title="预览模式"
        >
          <i class="fas fa-eye"></i>
          预览
        </button>
        
        <div class="toolbar-divider"></div>
        
        <button class="action-btn" @click="saveDocument" :disabled="isSaving" title="保存 (Ctrl+S)">
          <i class="fas fa-save"></i>
          保存
        </button>
        <button class="action-btn danger" @click="deleteDocument" title="删除">
          <i class="fas fa-trash"></i>
        </button>
      </div>
    </div>
    
    <!-- 编辑器主体 -->
    <div class="editor-main" :class="`view-${viewMode}`">
      <!-- 编辑器区域 -->
      <div v-show="viewMode === 'edit' || viewMode === 'split'" class="editor-pane">
        <div class="editor-wrapper">
          <div class="line-numbers" v-if="showLineNumbers">
            <div 
              v-for="n in lineCount" 
              :key="n" 
              class="line-number"
              :class="{ active: n === currentLine }"
            >
              {{ n }}
            </div>
          </div>
          <textarea
            ref="editorTextarea"
            v-model="localContent"
            class="editor-textarea"
            :placeholder="placeholder"
            @input="handleInput"
            @scroll="syncScroll"
            @keydown="handleKeydown"
            @click="updateCursorPosition"
            @keyup="updateCursorPosition"
            spellcheck="false"
          ></textarea>
        </div>
      </div>
      
      <!-- 预览区域 -->
      <div v-show="viewMode === 'preview' || viewMode === 'split'" class="preview-pane">
        <div 
          ref="previewContainer"
          class="preview-content markdown-body"
          v-html="renderedMarkdown"
          @scroll="syncPreviewScroll"
        ></div>
      </div>
    </div>
    
    <!-- 状态栏 -->
    <div class="editor-status">
      <div class="status-left">
        <span class="status-item">
          <i class="fas fa-file-text"></i>
          {{ wordCount }} 字 / {{ charCount }} 字符
        </span>
        <span class="status-item">
          <i class="fas fa-list-ol"></i>
          第 {{ currentLine }} 行
        </span>
        <span class="status-item" v-if="selectedText">
          已选择 {{ selectedText.length }} 字符
        </span>
      </div>
      <div class="status-right">
        <span class="status-item">
          <i class="fas fa-code"></i>
          Markdown
        </span>
        <span class="status-item">
          <i class="fas fa-clock"></i>
          {{ lastSaved }}
        </span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { marked } from 'marked'
import hljs from 'highlight.js'
import 'highlight.js/styles/vs2015.css' // 使用暗色主题
import { documentApi } from '@/api/document'

// Props
const props = defineProps({
  document: {
    type: Object,
    required: true
  }
})

// Emits
const emit = defineEmits(['save', 'delete'])

// 响应式数据
const editorTextarea = ref(null)
const previewContainer = ref(null)
const localTitle = ref('')
const localContent = ref('')
const viewMode = ref('split') // edit, split, preview
const isSaving = ref(false)
const isDirty = ref(false)
const showLineNumbers = ref(true)
const currentLine = ref(1)
const selectedText = ref('')
const lastSaved = ref('未保存')

// 自动保存相关
let saveTimeout = null
const autoSaveDelay = 2000 // 2秒自动保存

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

// 计算属性
const placeholder = computed(() => {
  return viewMode.value === 'edit' ? '开始编写您的Markdown内容...\n\n支持语法：\n# 标题\n**粗体** *斜体*\n- 列表\n```代码块```\n[链接](url)' : ''
})

const lineCount = computed(() => {
  return localContent.value.split('\n').length
})

const wordCount = computed(() => {
  return localContent.value.replace(/\s+/g, '').length
})

const charCount = computed(() => {
  return localContent.value.length
})

const statusText = computed(() => {
  if (isSaving.value) return '保存中...'
  if (!isDirty.value) return '已保存'
  return '未保存'
})

const renderedMarkdown = computed(() => {
  if (!localContent.value.trim()) {
    return '<div class="empty-state"><i class="fas fa-file-alt"></i><p>在左侧编辑器中开始编写内容，这里将实时显示预览效果</p></div>'
  }
  
  try {
    return marked(localContent.value)
  } catch (error) {
    console.error('Markdown渲染错误:', error)
    return '<div class="error-state"><i class="fas fa-exclamation-triangle"></i><p>Markdown渲染出错</p></div>'
  }
})

// 方法
const setViewMode = (mode) => {
  viewMode.value = mode
  nextTick(() => {
    if (mode === 'edit' || mode === 'split') {
      editorTextarea.value?.focus()
    }
  })
}

const handleInput = () => {
  isDirty.value = true
  updateCursorPosition()
  
  // 自动保存防抖
  if (saveTimeout) {
    clearTimeout(saveTimeout)
  }
  saveTimeout = setTimeout(() => {
    autoSave()
  }, autoSaveDelay)
}

const updateCursorPosition = () => {
  if (!editorTextarea.value) return
  
  const textarea = editorTextarea.value
  const cursorPos = textarea.selectionStart
  const textBeforeCursor = textarea.value.substring(0, cursorPos)
  currentLine.value = textBeforeCursor.split('\n').length
  
  // 获取选中文本
  const start = textarea.selectionStart
  const end = textarea.selectionEnd
  selectedText.value = textarea.value.substring(start, end)
}

const handleKeydown = (event) => {
  // Ctrl+S 保存
  if (event.ctrlKey && event.key === 's') {
    event.preventDefault()
    saveDocument()
    return
  }
  
  // Ctrl+B 粗体
  if (event.ctrlKey && event.key === 'b') {
    event.preventDefault()
    insertMarkdown('**', '**')
    return
  }
  
  // Ctrl+I 斜体
  if (event.ctrlKey && event.key === 'i') {
    event.preventDefault()
    insertMarkdown('*', '*')
    return
  }
  
  // Tab键缩进
  if (event.key === 'Tab') {
    event.preventDefault()
    insertText('  ') // 两个空格缩进
    return
  }
  
  // 自动括号匹配
  const pairs = {
    '(': ')',
    '[': ']',
    '{': '}',
    '"': '"',
    "'": "'"
  }
  
  if (pairs[event.key]) {
    event.preventDefault()
    insertMarkdown(event.key, pairs[event.key])
  }
}

const insertText = (text) => {
  const textarea = editorTextarea.value
  if (!textarea) return
  
  const start = textarea.selectionStart
  const end = textarea.selectionEnd
  const before = localContent.value.substring(0, start)
  const after = localContent.value.substring(end)
  
  localContent.value = before + text + after
  
  nextTick(() => {
    textarea.selectionStart = textarea.selectionEnd = start + text.length
    textarea.focus()
    handleInput()
  })
}

const insertMarkdown = (before, after) => {
  const textarea = editorTextarea.value
  if (!textarea) return
  
  const start = textarea.selectionStart
  const end = textarea.selectionEnd
  const selectedText = localContent.value.substring(start, end)
  const beforeText = localContent.value.substring(0, start)
  const afterText = localContent.value.substring(end)
  
  const newText = before + selectedText + after
  localContent.value = beforeText + newText + afterText
  
  nextTick(() => {
    if (selectedText) {
      // 如果有选中文本，选中插入的内容
      textarea.selectionStart = start
      textarea.selectionEnd = start + newText.length
    } else {
      // 如果没有选中文本，光标放在中间
      const cursorPos = start + before.length
      textarea.selectionStart = textarea.selectionEnd = cursorPos
    }
    textarea.focus()
    handleInput()
  })
}

const insertHeading = () => {
  const textarea = editorTextarea.value
  if (!textarea) return
  
  const start = textarea.selectionStart
  const lines = localContent.value.split('\n')
  const lineIndex = localContent.value.substring(0, start).split('\n').length - 1
  const currentLine = lines[lineIndex]
  
  // 循环标题级别 (# ## ### #### ##### ######)
  const headingMatch = currentLine.match(/^(#{1,6})\s*(.*)/)
  if (headingMatch) {
    const level = headingMatch[1].length
    const text = headingMatch[2]
    const newLevel = level >= 6 ? 1 : level + 1
    lines[lineIndex] = '#'.repeat(newLevel) + ' ' + text
  } else {
    lines[lineIndex] = '# ' + currentLine
  }
  
  localContent.value = lines.join('\n')
  handleInput()
}

const insertList = () => {
  insertMarkdown('- ', '')
}

const insertLink = () => {
  insertMarkdown('[', '](url)')
}

const insertTable = () => {
  const table = `| 列1 | 列2 | 列3 |
|-----|-----|-----|
| 内容 | 内容 | 内容 |
| 内容 | 内容 | 内容 |`
  insertText('\n' + table + '\n')
}

const insertCodeBlock = () => {
  insertMarkdown('\n```javascript\n', '\n```\n')
}

const syncScroll = () => {
  if (viewMode.value !== 'split' || !previewContainer.value) return
  
  const textarea = editorTextarea.value
  const preview = previewContainer.value
  
  const scrollRatio = textarea.scrollTop / (textarea.scrollHeight - textarea.clientHeight)
  preview.scrollTop = scrollRatio * (preview.scrollHeight - preview.clientHeight)
}

const syncPreviewScroll = () => {
  if (viewMode.value !== 'split' || !editorTextarea.value) return
  
  const textarea = editorTextarea.value
  const preview = previewContainer.value
  
  const scrollRatio = preview.scrollTop / (preview.scrollHeight - preview.clientHeight)
  textarea.scrollTop = scrollRatio * (textarea.scrollHeight - textarea.clientHeight)
}

const saveTitle = async () => {
  if (localTitle.value !== props.document.title) {
    isDirty.value = true
    await saveDocument()
  }
}

const autoSave = async () => {
  if (!isDirty.value) return
  
  try {
    console.log('自动保存文档...')
    await saveDocument()
  } catch (error) {
    console.error('自动保存失败:', error)
  }
}

const saveDocument = async () => {
  if (isSaving.value) return
  
  isSaving.value = true
  
  try {
    const updateData = {
      title: localTitle.value,
      description: props.document.description,
      content: localContent.value
    }
    
    const response = await documentApi.updateContent(props.document.id, {
      content: localContent.value
    })
    
    // 同时更新标题
    if (localTitle.value !== props.document.title) {
      await documentApi.update(props.document.id, updateData)
    }
    
    emit('save', {
      ...response,
      title: localTitle.value
    })
    
    isDirty.value = false
    lastSaved.value = new Date().toLocaleTimeString()
    console.log('文档保存成功')
    
  } catch (error) {
    console.error('保存文档失败:', error)
    alert('保存失败，请重试')
  } finally {
    isSaving.value = false
  }
}

const deleteDocument = () => {
  if (confirm(`确定要删除文档 "${localTitle.value}" 吗？此操作不可恢复。`)) {
    emit('delete', props.document)
  }
}

// 监听器
watch(() => props.document, (newDoc) => {
  if (newDoc) {
    localTitle.value = newDoc.title || ''
    localContent.value = newDoc.content || ''
    isDirty.value = false
    lastSaved.value = newDoc.createdAt ? new Date(newDoc.createdAt).toLocaleTimeString() : '未保存'
  }
}, { immediate: true })

// 生命周期
onMounted(async () => {
  // 如果文档有文件路径但没有内容，尝试加载内容
  if (props.document.filePath && !props.document.content) {
    try {
      console.log('文档有filePath但无content，尝试从后端加载:', props.document.title)
      const response = await documentApi.getById(props.document.id)
      
      if (response.fileUrl) {
        // 如果是Markdown文件，尝试加载文本内容
        const isMarkdown = props.document.fileType === 'text/markdown' || 
                          props.document.fileType === 'text/x-markdown' ||
                          props.document.title?.toLowerCase().endsWith('.md')
        
        if (isMarkdown) {
          try {
            const contentResponse = await fetch(response.fileUrl)
            const content = await contentResponse.text()
            localContent.value = content
            console.log('成功加载文档内容，长度:', content.length)
          } catch (fetchError) {
            console.warn('无法加载文档内容:', fetchError)
            localContent.value = '# ' + props.document.title + '\n\n加载文档内容失败，请手动编写内容。'
          }
        }
      }
    } catch (error) {
      console.error('加载文档失败:', error)
      localContent.value = '# ' + props.document.title + '\n\n加载文档失败，请手动编写内容。'
    }
  }
  
  // 聚焦编辑器
  nextTick(() => {
    if (editorTextarea.value) {
      editorTextarea.value.focus()
    }
  })
  
  // 键盘快捷键
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  // 清理定时器
  if (saveTimeout) {
    clearTimeout(saveTimeout)
  }
  
  document.removeEventListener('keydown', handleKeydown)
  
  // 如果有未保存的更改，提示用户
  if (isDirty.value) {
    if (confirm('有未保存的更改，是否保存？')) {
      saveDocument()
    }
  }
})
</script>

<style scoped>
/* 导入highlight.js主题样式 */
@import 'highlight.js/styles/vs2015.css';

.document-editor {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--bg-color);
  border-radius: 8px;
  overflow: hidden;
}

/* 工具栏样式 */
.editor-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: var(--surface-color);
  border-bottom: 1px solid var(--border-color);
  flex-shrink: 0;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.title-input {
  background: none;
  border: none;
  color: var(--text-color);
  font-size: 18px;
  font-weight: 600;
  padding: 8px 12px;
  border-radius: 4px;
  min-width: 200px;
  outline: none;
  border: 1px solid transparent;
  transition: all 0.2s;
}

.title-input:hover {
  background: var(--surface-color-2);
}

.title-input:focus {
  background: var(--surface-color-2);
  border-color: var(--color-blue);
}

.status-indicator {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--text-muted);
}

.status-indicator i {
  font-size: 8px;
}

.status-indicator.saving i {
  color: var(--color-orange);
  animation: pulse 1s infinite;
}

.status-indicator.saved i {
  color: var(--color-green);
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.toolbar-center {
  flex: 1;
  display: flex;
  justify-content: center;
}

.markdown-tools {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px;
  background: var(--surface-color-2);
  border-radius: 6px;
}

.tool-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  background: none;
  border: none;
  color: var(--text-color);
  cursor: pointer;
  border-radius: 4px;
  transition: all 0.2s;
}

.tool-btn:hover {
  background: var(--surface-color);
  color: var(--color-blue);
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.view-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: none;
  border: 1px solid var(--border-color);
  color: var(--text-color);
  cursor: pointer;
  border-radius: 4px;
  font-size: 13px;
  transition: all 0.2s;
}

.view-btn:hover {
  background: var(--surface-color-2);
  border-color: var(--color-blue);
}

.view-btn.active {
  background: var(--color-blue);
  color: var(--bg-color);
  border-color: var(--color-blue);
}

.toolbar-divider {
  width: 1px;
  height: 24px;
  background: var(--border-color);
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: var(--color-blue);
  color: var(--bg-color);
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
  font-weight: 500;
  transition: all 0.2s;
}

.action-btn:hover {
  background: hsl(170, 100%, 65%);
  transform: translateY(-1px);
}

.action-btn.danger {
  background: var(--color-red);
}

.action-btn.danger:hover {
  background: #dc2626;
}

.action-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
}

/* 编辑器主体 */
.editor-main {
  display: flex;
  flex: 1;
  overflow: hidden;
}

.editor-main.view-edit .preview-pane {
  display: none;
}

.editor-main.view-preview .editor-pane {
  display: none;
}

.editor-main.view-split .editor-pane,
.editor-main.view-split .preview-pane {
  flex: 1;
}

.editor-pane {
  display: flex;
  flex-direction: column;
  border-right: 1px solid var(--border-color);
  background: var(--bg-color);
}

.editor-wrapper {
  display: flex;
  flex: 1;
  overflow: hidden;
}

.line-numbers {
  display: flex;
  flex-direction: column;
  background: var(--surface-color-2);
  color: var(--text-muted);
  font-family: var(--font-code);
  font-size: 13px;
  line-height: 1.5;
  padding: 16px 8px;
  text-align: right;
  user-select: none;
  border-right: 1px solid var(--border-color);
  min-width: 60px;
}

.line-number {
  height: 19.5px; /* 匹配textarea行高 */
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding-right: 8px;
}

.line-number.active {
  color: var(--color-blue);
  font-weight: 600;
}

.editor-textarea {
  flex: 1;
  background: var(--bg-color);
  color: var(--text-color);
  border: none;
  outline: none;
  resize: none;
  font-family: var(--font-code);
  font-size: 14px;
  line-height: 1.5;
  padding: 16px;
  tab-size: 2;
  white-space: pre-wrap;
  word-wrap: break-word;
}

.editor-textarea::placeholder {
  color: var(--text-muted);
  font-style: italic;
}

.preview-pane {
  background: var(--surface-color);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.preview-content {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
  font-family: var(--font-main);
  line-height: 1.6;
}

/* Markdown样式 */
.markdown-body {
  color: var(--text-color);
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

.empty-state,
.error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 200px;
  color: var(--text-muted);
  font-style: italic;
}

.empty-state i,
.error-state i {
  font-size: 48px;
  margin-bottom: 16px;
  opacity: 0.5;
}

/* 状态栏 */
.editor-status {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 16px;
  background: var(--surface-color);
  border-top: 1px solid var(--border-color);
  font-size: 12px;
  color: var(--text-muted);
}

.status-left,
.status-right {
  display: flex;
  gap: 16px;
}

.status-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

/* 响应式调整 */
@media (max-width: 768px) {
  .editor-toolbar {
    flex-direction: column;
    gap: 12px;
    padding: 12px;
  }
  
  .toolbar-center {
    order: 3;
  }
  
  .markdown-tools {
    flex-wrap: wrap;
  }
  
  .editor-main.view-split {
    flex-direction: column;
  }
  
  .editor-main.view-split .editor-pane {
    border-right: none;
    border-bottom: 1px solid var(--border-color);
  }
  
  .line-numbers {
    display: none;
  }
  
  .title-input {
    min-width: unset;
    width: 100%;
  }
}
</style> 