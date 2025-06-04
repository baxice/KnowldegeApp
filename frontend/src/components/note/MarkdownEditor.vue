<template>
  <div class="markdown-editor">
    <div class="editor-toolbar">
      <div class="toolbar-left">
        <button class="toolbar-btn" @click="insertText('**', '**')" title="粗体">
          <i class="fas fa-bold"></i>
        </button>
        <button class="toolbar-btn" @click="insertText('*', '*')" title="斜体">
          <i class="fas fa-italic"></i>
        </button>
        <button class="toolbar-btn" @click="insertText('`', '`')" title="代码">
          <i class="fas fa-code"></i>
        </button>
        <button class="toolbar-btn" @click="insertHeading" title="标题">
          <i class="fas fa-heading"></i>
        </button>
        <button class="toolbar-btn" @click="insertLink" title="链接">
          <i class="fas fa-link"></i>
        </button>
        <button class="toolbar-btn" @click="insertImage" title="图片">
          <i class="fas fa-image"></i>
        </button>
        <button class="toolbar-btn" @click="insertList" title="列表">
          <i class="fas fa-list"></i>
        </button>
        <button class="toolbar-btn" @click="insertTable" title="表格">
          <i class="fas fa-table"></i>
        </button>
      </div>
      <div class="toolbar-right">
        <button class="toolbar-btn" @click="togglePreview" :class="{ active: showPreview }">
          <i class="fas fa-eye"></i>
          {{ showPreview ? '编辑' : '预览' }}
        </button>
        <button class="toolbar-btn" @click="saveNote" :disabled="!hasChanges">
          <i class="fas fa-save"></i>
          保存
        </button>
      </div>
    </div>
    
    <div class="editor-container" :class="{ 'preview-mode': showPreview }">
      <div v-if="!showPreview" class="editor-pane">
        <textarea
          ref="textarea"
          v-model="content"
          @input="handleInput"
          @keydown="handleKeydown"
          class="markdown-textarea"
          placeholder="开始编写您的笔记...支持Markdown语法"
        ></textarea>
      </div>
      
      <div v-if="showPreview" class="preview-pane">
        <div class="markdown-content" v-html="renderedContent"></div>
      </div>
    </div>
    
    <div class="editor-status">
      <span class="word-count">字数: {{ wordCount }}</span>
      <span class="save-status" :class="saveStatusClass">{{ saveStatusText }}</span>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, nextTick } from 'vue'
import { noteApi } from '@/api/note'

// Props
const props = defineProps({
  noteId: {
    type: [Number, String],
    default: null
  },
  initialContent: {
    type: String,
    default: ''
  },
  title: {
    type: String,
    default: '新建笔记'
  }
})

// Emits
const emit = defineEmits(['save', 'contentChange'])

// 响应式数据
const textarea = ref(null)
const content = ref(props.initialContent)
const showPreview = ref(false)
const hasChanges = ref(false)
const saveStatus = ref('saved') // saved, saving, error
const lastSavedContent = ref(props.initialContent)

// 计算属性
const wordCount = computed(() => {
  return content.value.length
})

const renderedContent = computed(() => {
  return renderMarkdown(content.value)
})

const saveStatusText = computed(() => {
  switch (saveStatus.value) {
    case 'saving':
      return '保存中...'
    case 'error':
      return '保存失败'
    case 'saved':
      return hasChanges.value ? '有未保存的更改' : '已保存'
    default:
      return '已保存'
  }
})

const saveStatusClass = computed(() => {
  return {
    'status-saving': saveStatus.value === 'saving',
    'status-error': saveStatus.value === 'error',
    'status-unsaved': hasChanges.value && saveStatus.value === 'saved'
  }
})

// 监听内容变化
watch(content, (newValue) => {
  hasChanges.value = newValue !== lastSavedContent.value
  emit('contentChange', newValue)
})

// 方法
const handleInput = () => {
  // 内容变化处理
}

const handleKeydown = (event) => {
  // 快捷键处理
  if (event.ctrlKey || event.metaKey) {
    switch (event.key) {
      case 's':
        event.preventDefault()
        saveNote()
        break
      case 'b':
        event.preventDefault()
        insertText('**', '**')
        break
      case 'i':
        event.preventDefault()
        insertText('*', '*')
        break
    }
  }
  
  // Tab键处理
  if (event.key === 'Tab') {
    event.preventDefault()
    insertText('  ')
  }
}

const insertText = (before, after = '') => {
  const textarea = textarea.value
  const start = textarea.selectionStart
  const end = textarea.selectionEnd
  const selectedText = content.value.substring(start, end)
  
  const newText = before + selectedText + after
  content.value = content.value.substring(0, start) + newText + content.value.substring(end)
  
  nextTick(() => {
    textarea.focus()
    textarea.setSelectionRange(start + before.length, start + before.length + selectedText.length)
  })
}

const insertHeading = () => {
  const textarea = textarea.value
  const start = textarea.selectionStart
  const lineStart = content.value.lastIndexOf('\n', start - 1) + 1
  
  content.value = content.value.substring(0, lineStart) + '# ' + content.value.substring(lineStart)
  
  nextTick(() => {
    textarea.focus()
    textarea.setSelectionRange(start + 2, start + 2)
  })
}

const insertLink = () => {
  insertText('[', ']()')
}

const insertImage = () => {
  insertText('![', ']()')
}

const insertList = () => {
  const textarea = textarea.value
  const start = textarea.selectionStart
  const lineStart = content.value.lastIndexOf('\n', start - 1) + 1
  
  content.value = content.value.substring(0, lineStart) + '- ' + content.value.substring(lineStart)
  
  nextTick(() => {
    textarea.focus()
    textarea.setSelectionRange(start + 2, start + 2)
  })
}

const insertTable = () => {
  const tableText = '\n| 标题1 | 标题2 | 标题3 |\n|-------|-------|-------|\n| 内容1 | 内容2 | 内容3 |\n'
  insertText(tableText)
}

const togglePreview = () => {
  showPreview.value = !showPreview.value
}

const saveNote = async () => {
  if (!hasChanges.value) return
  
  try {
    saveStatus.value = 'saving'
    
    const noteData = {
      title: props.title,
      content: content.value
    }
    
    if (props.noteId) {
      await noteApi.update(props.noteId, noteData)
    } else {
      const newNote = await noteApi.create(noteData)
      emit('save', newNote)
    }
    
    lastSavedContent.value = content.value
    hasChanges.value = false
    saveStatus.value = 'saved'
    
    emit('save', { content: content.value })
  } catch (error) {
    console.error('保存笔记失败:', error)
    saveStatus.value = 'error'
  }
}

// 简化的Markdown渲染函数
const renderMarkdown = (text) => {
  if (!text) return ''
  
  let html = text
    // 标题
    .replace(/^### (.*$)/gim, '<h3>$1</h3>')
    .replace(/^## (.*$)/gim, '<h2>$1</h2>')
    .replace(/^# (.*$)/gim, '<h1>$1</h1>')
    // 粗体
    .replace(/\*\*(.*)\*\*/gim, '<strong>$1</strong>')
    // 斜体
    .replace(/\*(.*)\*/gim, '<em>$1</em>')
    // 代码
    .replace(/`(.*?)`/gim, '<code>$1</code>')
    // 链接
    .replace(/\[([^\]]+)\]\(([^)]+)\)/gim, '<a href="$2" target="_blank">$1</a>')
    // 换行
    .replace(/\n/gim, '<br>')
    // 列表
    .replace(/^- (.*)$/gim, '<li>$1</li>')
  
  // 包装列表项
  html = html.replace(/(<li>.*<\/li>)/gims, '<ul>$1</ul>')
  
  return html
}

// 暴露方法给父组件
defineExpose({
  saveNote,
  getContent: () => content.value,
  setContent: (newContent) => {
    content.value = newContent
    lastSavedContent.value = newContent
    hasChanges.value = false
  }
})
</script>

<style scoped>
.markdown-editor {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--surface-primary);
  border-radius: 8px;
  overflow: hidden;
}

.editor-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: var(--surface-secondary);
  border-bottom: 1px solid var(--border-color);
}

.toolbar-left,
.toolbar-right {
  display: flex;
  gap: 4px;
}

.toolbar-btn {
  padding: 6px 10px;
  background: transparent;
  border: 1px solid var(--border-color);
  border-radius: 4px;
  color: var(--text-primary);
  cursor: pointer;
  transition: all 0.2s;
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.toolbar-btn:hover {
  background: var(--surface-hover);
}

.toolbar-btn.active {
  background: var(--color-primary);
  color: white;
}

.toolbar-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.editor-container {
  flex: 1;
  display: flex;
  min-height: 0;
}

.editor-pane {
  flex: 1;
  display: flex;
}

.markdown-textarea {
  flex: 1;
  padding: 16px;
  border: none;
  outline: none;
  background: transparent;
  color: var(--text-primary);
  font-family: var(--font-mono);
  font-size: 14px;
  line-height: 1.6;
  resize: none;
}

.preview-pane {
  flex: 1;
  padding: 16px;
  overflow-y: auto;
}

.markdown-content {
  color: var(--text-primary);
  line-height: 1.7;
}

.markdown-content h1,
.markdown-content h2,
.markdown-content h3 {
  color: var(--text-accent);
  margin: 16px 0 8px 0;
}

.markdown-content h1 {
  font-size: 24px;
  background: linear-gradient(135deg, var(--color-pink), var(--color-orange));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.markdown-content h2 {
  font-size: 20px;
  background: linear-gradient(135deg, var(--color-orange), var(--color-yellow));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.markdown-content h3 {
  font-size: 16px;
  background: linear-gradient(135deg, var(--color-yellow), var(--color-green));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.markdown-content code {
  background: var(--surface-secondary);
  padding: 2px 6px;
  border-radius: 4px;
  font-family: var(--font-mono);
  font-size: 13px;
}

.markdown-content ul {
  margin: 8px 0;
  padding-left: 20px;
}

.markdown-content li {
  margin: 4px 0;
}

.markdown-content a {
  color: var(--color-blue);
  text-decoration: none;
}

.markdown-content a:hover {
  text-decoration: underline;
}

.editor-status {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 16px;
  background: var(--surface-secondary);
  border-top: 1px solid var(--border-color);
  font-size: 12px;
  color: var(--text-secondary);
}

.save-status {
  transition: color 0.2s;
}

.save-status.status-saving {
  color: var(--color-blue);
}

.save-status.status-error {
  color: var(--color-red);
}

.save-status.status-unsaved {
  color: var(--color-orange);
}
</style> 