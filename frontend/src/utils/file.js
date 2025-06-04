export const formatFileSize = (bytes) => {
  if (!bytes) return '0 B'
  
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  let size = bytes
  let unitIndex = 0
  
  while (size >= 1024 && unitIndex < units.length - 1) {
    size /= 1024
    unitIndex++
  }
  
  return `${size.toFixed(2)} ${units[unitIndex]}`
}

export const getFileExtension = (filename) => {
  if (!filename) return ''
  return filename.slice((filename.lastIndexOf('.') - 1 >>> 0) + 2)
}

export const isImageFile = (fileType) => {
  return fileType?.startsWith('image/')
}

export const isPdfFile = (fileType) => {
  return fileType === 'application/pdf'
}

export const isTextFile = (fileType) => {
  return fileType?.startsWith('text/')
}

export const isMarkdownFile = (fileType) => {
  return fileType === 'text/markdown'
}

export const isWordFile = (fileType) => {
  return fileType === 'application/msword' || 
         fileType === 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
} 