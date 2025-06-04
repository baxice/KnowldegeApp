<template>
  <div v-if="visible" class="upload-modal-overlay" @click="closeModal">
    <div class="upload-modal" @click.stop>
      <div class="modal-header">
        <h3>上传文档</h3>
        <button class="close-btn" @click="closeModal">
          <i class="fas fa-times"></i>
        </button>
      </div>
      
      <div class="modal-body">
        <!-- 上传区域 -->
        <div 
          class="upload-area"
          :class="{ 'drag-over': isDragOver, 'uploading': isUploading }"
          @drop="handleDrop"
          @dragover="handleDragOver"
          @dragenter="handleDragEnter"
          @dragleave="handleDragLeave"
          @click="triggerFileInput"
        >
          <input 
            ref="fileInput"
            type="file" 
            multiple 
            @change="handleFileSelect"
            style="display: none"
            accept=".pdf,.doc,.docx,.txt,.md,.png,.jpg,.jpeg,.gif"
          >
          
          <div v-if="!isUploading" class="upload-content">
            <i class="fas fa-cloud-upload-alt upload-icon"></i>
            <h4>拖拽文件到此处或点击上传</h4>
            <p>支持 PDF、Word、图片、文本等多种格式</p>
            <div class="upload-hint">
              <span>单个文件最大 50MB，最多同时上传 10 个文件</span>
            </div>
          </div>
          
          <div v-else class="uploading-content">
            <i class="fas fa-spinner fa-spin upload-icon"></i>
            <h4>正在上传...</h4>
            <div class="upload-progress">
              <div class="progress-bar">
                <div class="progress-fill" :style="{ width: uploadProgress + '%' }"></div>
              </div>
              <span class="progress-text">{{ uploadProgress }}%</span>
            </div>
          </div>
        </div>
        
        <!-- 选中的文件列表 -->
        <div v-if="selectedFiles.length > 0 && !isUploading" class="file-list">
          <h4>选中的文件 ({{ selectedFiles.length }})</h4>
          <div class="file-items">
            <div 
              v-for="(file, index) in selectedFiles" 
              :key="index"
              class="file-item"
            >
              <div class="file-info">
                <i :class="getFileIcon(file.type)" class="file-icon"></i>
                <div class="file-details">
                  <span class="file-name">{{ file.name }}</span>
                  <span class="file-size">{{ formatFileSize(file.size) }}</span>
                </div>
              </div>
              <button class="remove-file" @click="removeFile(index)">
                <i class="fas fa-times"></i>
              </button>
            </div>
          </div>
        </div>
        
        <!-- 上传结果 -->
        <div v-if="uploadResults.length > 0" class="upload-results">
          <h4>上传结果</h4>
          <div class="result-items">
            <div 
              v-for="(result, index) in uploadResults" 
              :key="index"
              class="result-item"
              :class="{ 'success': result.success, 'error': !result.success }"
            >
              <i :class="result.success ? 'fas fa-check-circle' : 'fas fa-exclamation-circle'"></i>
              <span class="result-filename">{{ result.filename }}</span>
              <span class="result-message">{{ result.message }}</span>
            </div>
          </div>
        </div>
      </div>
      
      <div class="modal-footer">
        <div class="upload-options">
          <label class="option">
            <input type="checkbox" v-model="addToFavorites">
            添加到收藏
          </label>
          <label class="option">
            <input type="checkbox" v-model="notifyOnComplete">
            完成时通知
          </label>
        </div>
        
        <div class="modal-actions">
          <button class="btn secondary" @click="closeModal" :disabled="isUploading">
            取消
          </button>
          <button 
            class="btn primary" 
            @click="startUpload" 
            :disabled="selectedFiles.length === 0 || isUploading"
          >
            <i class="fas fa-upload"></i>
            上传文件 ({{ selectedFiles.length }})
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { documentApi } from '@/api/document'

// Props
const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  }
})

// Emits
const emit = defineEmits(['close', 'success', 'error'])

// 响应式数据
const fileInput = ref(null)
const selectedFiles = ref([])
const isDragOver = ref(false)
const isUploading = ref(false)
const uploadProgress = ref(0)
const uploadResults = ref([])
const addToFavorites = ref(false)
const notifyOnComplete = ref(true)

// 拖拽计数器，解决拖拽事件的冒泡问题
let dragCounter = 0

// 方法
const closeModal = () => {
  if (!isUploading.value) {
    selectedFiles.value = []
    uploadResults.value = []
    uploadProgress.value = 0
    emit('close')
  }
}

const triggerFileInput = () => {
  if (!isUploading.value) {
    fileInput.value?.click()
  }
}

const handleFileSelect = (event) => {
  const files = Array.from(event.target.files)
  addFiles(files)
  event.target.value = '' // 清空input，允许重复选择相同文件
}

const handleDragEnter = (event) => {
  event.preventDefault()
  dragCounter++
  isDragOver.value = true
}

const handleDragOver = (event) => {
  event.preventDefault()
}

const handleDragLeave = (event) => {
  event.preventDefault()
  dragCounter--
  if (dragCounter === 0) {
    isDragOver.value = false
  }
}

const handleDrop = (event) => {
  event.preventDefault()
  dragCounter = 0
  isDragOver.value = false
  
  const files = Array.from(event.dataTransfer.files)
  addFiles(files)
}

const addFiles = (files) => {
  if (isUploading.value) return
  
  // 过滤重复文件
  const newFiles = files.filter(file => {
    return !selectedFiles.value.some(existing => 
      existing.name === file.name && existing.size === file.size
    )
  })
  
  // 限制文件数量
  const maxFiles = 10
  const availableSlots = maxFiles - selectedFiles.value.length
  const filesToAdd = newFiles.slice(0, availableSlots)
  
  // 检查文件大小
  const maxSize = 50 * 1024 * 1024 // 50MB
  const validFiles = filesToAdd.filter(file => {
    if (file.size > maxSize) {
      alert(`文件 "${file.name}" 超过50MB限制`)
      return false
    }
    return true
  })
  
  selectedFiles.value.push(...validFiles)
  
  if (newFiles.length > availableSlots) {
    alert(`最多只能选择${maxFiles}个文件`)
  }
}

const removeFile = (index) => {
  if (!isUploading.value) {
    selectedFiles.value.splice(index, 1)
  }
}

const startUpload = async () => {
  if (selectedFiles.value.length === 0 || isUploading.value) return
  
  isUploading.value = true
  uploadProgress.value = 0
  uploadResults.value = []
  
  try {
    const totalFiles = selectedFiles.value.length
    let completedFiles = 0
    
    // 逐个上传文件
    for (const file of selectedFiles.value) {
      try {
        const response = await documentApi.upload(file, (progressEvent) => {
          const progress = Math.round((progressEvent.loaded * 100) / progressEvent.total)
          const totalProgress = Math.round(((completedFiles + progress / 100) * 100) / totalFiles)
          uploadProgress.value = totalProgress
        })
        
        uploadResults.value.push({
          filename: file.name,
          success: true,
          message: '上传成功',
          data: response
        })
        
        completedFiles++
        uploadProgress.value = Math.round((completedFiles * 100) / totalFiles)
        
      } catch (error) {
        console.error(`上传文件 ${file.name} 失败:`, error)
        uploadResults.value.push({
          filename: file.name,
          success: false,
          message: error.response?.data?.message || '上传失败'
        })
        completedFiles++
        uploadProgress.value = Math.round((completedFiles * 100) / totalFiles)
      }
    }
    
    // 上传完成
    const successCount = uploadResults.value.filter(r => r.success).length
    const failCount = uploadResults.value.filter(r => !r.success).length
    
    if (successCount > 0) {
      emit('success', {
        total: totalFiles,
        success: successCount,
        failed: failCount,
        results: uploadResults.value
      })
    }
    
    if (failCount === 0) {
      // 全部成功，延迟关闭模态框
      setTimeout(() => {
        closeModal()
      }, 2000)
    }
    
    if (notifyOnComplete.value) {
      const message = failCount === 0 
        ? `成功上传 ${successCount} 个文件`
        : `上传完成：${successCount} 成功，${failCount} 失败`
      
      // 这里可以集成通知系统
      console.log(message)
    }
    
  } catch (error) {
    console.error('批量上传失败:', error)
    emit('error', error)
  } finally {
    isUploading.value = false
  }
}

const formatFileSize = (bytes) => {
  if (bytes === 0) return '0 B'
  
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

const getFileIcon = (fileType) => {
  if (!fileType) return 'fas fa-file'
  
  if (fileType.startsWith('image/')) return 'fas fa-file-image'
  if (fileType === 'application/pdf') return 'fas fa-file-pdf'
  if (fileType.startsWith('text/')) return 'fas fa-file-alt'
  if (fileType.includes('word')) return 'fas fa-file-word'
  if (fileType.includes('excel') || fileType.includes('spreadsheet')) return 'fas fa-file-excel'
  if (fileType.includes('powerpoint') || fileType.includes('presentation')) return 'fas fa-file-powerpoint'
  
  return 'fas fa-file'
}

// 键盘事件处理
const handleKeydown = (event) => {
  if (event.key === 'Escape') {
    closeModal()
  }
}

// 生命周期
onMounted(() => {
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
})
</script>

<style scoped>
.upload-modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.85);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  backdrop-filter: blur(8px);
}

.upload-modal {
  background: var(--surface-color, #242730);
  border-radius: 12px;
  width: 90%;
  max-width: 600px;
  max-height: 80vh;
  overflow: hidden;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.5);
  border: 1px solid var(--border-color, #404040);
  animation: modalAppear 0.3s ease-out;
}

@keyframes modalAppear {
  from {
    opacity: 0;
    transform: scale(0.9) translateY(-20px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  background: var(--surface-color-2, rgba(255, 255, 255, 0.02));
  border-bottom: 1px solid var(--border-color, #404040);
}

.modal-header h3 {
  margin: 0;
  color: var(--text-color, #f2f2f0);
  background: linear-gradient(135deg, var(--color-pink), var(--color-orange));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  font-size: 18px;
  font-weight: 600;
}

.close-btn {
  background: none;
  border: none;
  color: var(--text-muted, #888);
  cursor: pointer;
  font-size: 18px;
  padding: 8px;
  border-radius: 6px;
  transition: all 0.2s;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.close-btn:hover {
  background: var(--surface-color-2, rgba(255, 255, 255, 0.05));
  color: var(--text-color, #f2f2f0);
}

.modal-body {
  padding: 24px;
  max-height: 60vh;
  overflow-y: auto;
}

.upload-area {
  border: 2px dashed var(--border-color, #404040);
  border-radius: 12px;
  padding: 48px 24px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s ease;
  margin-bottom: 24px;
  background: var(--bg-color, #181920);
}

.upload-area:hover {
  border-color: var(--color-blue, #66d9ff);
  background: rgba(102, 217, 255, 0.05);
  transform: translateY(-2px);
}

.upload-area.drag-over {
  border-color: var(--color-blue, #66d9ff);
  background: rgba(102, 217, 255, 0.1);
  box-shadow: 0 0 20px rgba(102, 217, 255, 0.2);
}

.upload-area.uploading {
  cursor: not-allowed;
  opacity: 0.7;
  pointer-events: none;
}

.upload-content h4,
.uploading-content h4 {
  margin: 16px 0 8px 0;
  color: var(--text-color, #f2f2f0);
  font-size: 18px;
  font-weight: 600;
}

.upload-content p {
  margin: 0 0 16px 0;
  color: var(--text-muted, #888);
  font-size: 14px;
}

.upload-icon {
  font-size: 48px;
  color: var(--color-blue, #66d9ff);
  margin-bottom: 16px;
}

.upload-hint {
  font-size: 12px;
  color: var(--text-muted, #888);
  padding: 8px 16px;
  background: var(--surface-color-2, rgba(255, 255, 255, 0.02));
  border-radius: 6px;
  display: inline-block;
  border: 1px solid var(--border-color, #404040);
}

.upload-progress {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-top: 20px;
}

.progress-bar {
  flex: 1;
  height: 8px;
  background: var(--surface-color-2, rgba(255, 255, 255, 0.02));
  border-radius: 4px;
  overflow: hidden;
  border: 1px solid var(--border-color, #404040);
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, var(--color-blue, #66d9ff), var(--color-green, #7dd3fc));
  transition: width 0.3s ease;
}

.progress-text {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-color, #f2f2f0);
  min-width: 45px;
}

.file-list {
  margin-bottom: 24px;
}

.file-list h4 {
  margin: 0 0 12px 0;
  color: var(--text-color, #f2f2f0);
  font-size: 16px;
  font-weight: 600;
}

.file-items {
  max-height: 200px;
  overflow-y: auto;
  border: 1px solid var(--border-color, #404040);
  border-radius: 8px;
  background: var(--bg-color, #181920);
}

.file-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid var(--border-color, #404040);
  transition: background 0.2s ease;
}

.file-item:last-child {
  border-bottom: none;
}

.file-item:hover {
  background: var(--surface-color-2, rgba(255, 255, 255, 0.02));
}

.file-info {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  min-width: 0;
}

.file-icon {
  color: var(--color-blue, #66d9ff);
  font-size: 20px;
  flex-shrink: 0;
}

.file-details {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.file-name {
  color: var(--text-color, #f2f2f0);
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.file-size {
  color: var(--text-muted, #888);
  font-size: 12px;
}

.remove-file {
  background: none;
  border: none;
  color: var(--text-muted, #888);
  cursor: pointer;
  padding: 8px;
  border-radius: 6px;
  transition: all 0.2s ease;
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.remove-file:hover {
  background: var(--color-red, #ef4444);
  color: white;
}

.upload-results {
  margin-bottom: 24px;
}

.upload-results h4 {
  margin: 0 0 12px 0;
  color: var(--text-color, #f2f2f0);
  font-size: 16px;
  font-weight: 600;
}

.result-items {
  max-height: 150px;
  overflow-y: auto;
  border: 1px solid var(--border-color, #404040);
  border-radius: 8px;
  background: var(--bg-color, #181920);
}

.result-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 16px;
  border-bottom: 1px solid var(--border-color, #404040);
}

.result-item:last-child {
  border-bottom: none;
}

.result-item.success {
  background: rgba(34, 197, 94, 0.1);
  border-left: 3px solid var(--color-green, #22c55e);
}

.result-item.error {
  background: rgba(239, 68, 68, 0.1);
  border-left: 3px solid var(--color-red, #ef4444);
}

.result-item.success i {
  color: var(--color-green, #22c55e);
}

.result-item.error i {
  color: var(--color-red, #ef4444);
}

.result-filename {
  font-weight: 500;
  color: var(--text-color, #f2f2f0);
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.result-message {
  color: var(--text-muted, #888);
  font-size: 12px;
}

.modal-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  background: var(--surface-color-2, rgba(255, 255, 255, 0.02));
  border-top: 1px solid var(--border-color, #404040);
}

.upload-options {
  display: flex;
  gap: 20px;
}

.option {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text-muted, #888);
  font-size: 14px;
  cursor: pointer;
  transition: color 0.2s ease;
}

.option:hover {
  color: var(--text-color, #f2f2f0);
}

.option input[type="checkbox"] {
  margin: 0;
  accent-color: var(--color-blue, #66d9ff);
}

.modal-actions {
  display: flex;
  gap: 12px;
}

.btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  border: 1px solid var(--border-color, #404040);
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
  font-size: 14px;
  font-weight: 500;
  min-width: 100px;
  justify-content: center;
}

.btn.secondary {
  background: transparent;
  color: var(--text-color, #f2f2f0);
}

.btn.secondary:hover:not(:disabled) {
  background: var(--surface-color-2, rgba(255, 255, 255, 0.05));
  border-color: var(--color-blue, #66d9ff);
}

.btn.primary {
  background: var(--color-blue, #66d9ff);
  color: var(--bg-color, #181920);
  border-color: var(--color-blue, #66d9ff);
  font-weight: 600;
}

.btn.primary:hover:not(:disabled) {
  background: hsl(170, 100%, 65%);
  border-color: hsl(170, 100%, 65%);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(102, 217, 255, 0.3);
}

.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none !important;
  box-shadow: none !important;
}

/* 响应式调整 */
@media (max-width: 768px) {
  .upload-modal {
    width: 95%;
    max-height: 90vh;
  }
  
  .modal-header,
  .modal-body,
  .modal-footer {
    padding: 16px;
  }
  
  .upload-area {
    padding: 32px 16px;
  }
  
  .upload-options {
    flex-direction: column;
    gap: 8px;
  }
  
  .modal-footer {
    flex-direction: column;
    gap: 16px;
  }
  
  .modal-actions {
    width: 100%;
  }
  
  .btn {
    flex: 1;
  }
}
</style> 