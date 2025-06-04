package com.knowledge.service;

import com.knowledge.dto.DocumentDto;
import com.knowledge.dto.DocumentUploadDto;
import com.knowledge.dto.TagDto;
import com.knowledge.entity.Document;
import com.knowledge.entity.Tag;
import com.knowledge.entity.User;
import com.knowledge.repository.DocumentRepository;
import com.knowledge.repository.TagRepository;
import com.knowledge.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocumentService {
    
    private static final Logger logger = LoggerFactory.getLogger(DocumentService.class);
    
    private final DocumentRepository documentRepository;
    private final TagRepository tagRepository;
    private final UserRepository userRepository;
    private final MinioService minioService;
    
    /**
     * 创建新文档（空文档）
     */
    @Transactional
    public DocumentDto createDocument(DocumentUploadDto createDto, Long userId) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("用户不存在"));
            
            // 创建空文档实体
            Document document = new Document();
            document.setTitle(createDto.getTitle());
            document.setDescription(createDto.getDescription());
            document.setContent(createDto.getContent() != null ? createDto.getContent() : "# " + createDto.getTitle() + "\n\n开始编写您的内容...");
            document.setFilePath(null); // 空文档没有文件路径
            document.setFileType("text/markdown"); // 默认为markdown类型
            document.setFileSize(0L); // 空文档大小为0
            document.setUser(user);
            
            // 添加标签
            if (createDto.getTagIds() != null && !createDto.getTagIds().isEmpty()) {
                Set<Tag> tags = createDto.getTagIds().stream()
                        .map(tagId -> tagRepository.findById(tagId)
                                .orElseThrow(() -> new RuntimeException("标签不存在: " + tagId)))
                        .collect(Collectors.toSet());
                document.setTags(tags);
            }
            
            // 保存文档
            Document savedDocument = documentRepository.save(document);
            logger.info("空文档创建成功: {}", savedDocument.getTitle());
            
            // 返回DTO
            return convertToDto(savedDocument);
            
        } catch (Exception e) {
            logger.error("创建文档失败", e);
            throw new RuntimeException("创建文档失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 上传文档
     */
    @Transactional
    public DocumentDto uploadDocument(MultipartFile file, DocumentUploadDto uploadDto, Long userId) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("用户不存在"));
            
            // 验证文件
            if (file.isEmpty()) {
                throw new RuntimeException("上传的文件为空");
            }
            
            // 验证文件大小 (50MB限制)
            if (file.getSize() > 50 * 1024 * 1024) {
                throw new RuntimeException("文件大小不能超过50MB");
            }
            
            // 验证文件类型
            String contentType = file.getContentType();
            String fileName = file.getOriginalFilename();
            
            logger.debug("文件信息 - 名称: {}, MIME类型: {}, 大小: {}", fileName, contentType, file.getSize());
            
            // 首先检查MIME类型，如果不支持则检查文件扩展名
            boolean isAllowed = isAllowedFileType(contentType);
            if (!isAllowed && fileName != null) {
                isAllowed = isAllowedFileByExtension(fileName);
                if (isAllowed) {
                    logger.info("MIME类型检查失败，但文件扩展名检查通过: {} ({})", fileName, contentType);
                }
            }
            
            if (!isAllowed) {
                throw new RuntimeException("不支持的文件类型: " + contentType + " (文件名: " + fileName + ")");
            }
            
            // 上传文件到MinIO
            String filePath;
            try {
                filePath = minioService.uploadDocument(file, userId);
                logger.info("文件上传到MinIO成功: {}", filePath);
            } catch (Exception e) {
                logger.error("MinIO上传失败: {}", e.getMessage());
                throw new RuntimeException("文件上传失败: " + e.getMessage(), e);
            }
            
            // 创建文档实体
            Document document = new Document();
            document.setTitle(uploadDto.getTitle());
            document.setDescription(uploadDto.getDescription());
            document.setFilePath(filePath);
            document.setFileType(file.getContentType());
            document.setFileSize(file.getSize());
            document.setUser(user);
            
            // 添加标签
            if (uploadDto.getTagIds() != null && !uploadDto.getTagIds().isEmpty()) {
                Set<Tag> tags = uploadDto.getTagIds().stream()
                        .map(tagId -> tagRepository.findById(tagId)
                                .orElseThrow(() -> new RuntimeException("标签不存在: " + tagId)))
                        .collect(Collectors.toSet());
                document.setTags(tags);
            }
            
            // 保存文档
            Document savedDocument = documentRepository.save(document);
            
            // 返回DTO
            return convertToDto(savedDocument);
            
        } catch (Exception e) {
            logger.error("文档上传失败", e);
            throw new RuntimeException("文档上传失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 检查是否为允许的文件类型
     */
    private boolean isAllowedFileType(String contentType) {
        if (contentType == null) {
            return false;
        }
        
        // 基本文本类型
        if (contentType.startsWith("text/")) {
            return true;
        }
        
        // 图片类型
        if (contentType.startsWith("image/")) {
            return true;
        }
        
        // 具体的文档类型
        switch (contentType) {
            case "application/pdf":
            case "application/msword":
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document":
            case "application/vnd.ms-excel":
            case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet":
            case "application/vnd.ms-powerpoint":
            case "application/vnd.openxmlformats-officedocument.presentationml.presentation":
            case "application/rtf":
            case "application/json":
            case "application/xml":
            case "text/markdown":
            case "text/x-markdown":
                return true;
            default:
                break;
        }
        
        // 对于 application/octet-stream，需要进一步检查
        // 这通常发生在文件类型无法正确识别时
        if ("application/octet-stream".equals(contentType)) {
            logger.warn("文件被识别为 application/octet-stream，可能是文件类型识别问题");
            // 暂时允许，但记录警告
            return true;
        }
        
        return false;
    }
    
    /**
     * 根据文件扩展名推断文件类型（作为备选方案）
     */
    private boolean isAllowedFileByExtension(String filename) {
        if (filename == null) {
            return false;
        }
        
        String lowerName = filename.toLowerCase();
        return lowerName.endsWith(".md") ||
               lowerName.endsWith(".markdown") ||
               lowerName.endsWith(".txt") ||
               lowerName.endsWith(".pdf") ||
               lowerName.endsWith(".doc") ||
               lowerName.endsWith(".docx") ||
               lowerName.endsWith(".xls") ||
               lowerName.endsWith(".xlsx") ||
               lowerName.endsWith(".ppt") ||
               lowerName.endsWith(".pptx") ||
               lowerName.endsWith(".rtf") ||
               lowerName.endsWith(".json") ||
               lowerName.endsWith(".xml") ||
               lowerName.endsWith(".jpg") ||
               lowerName.endsWith(".jpeg") ||
               lowerName.endsWith(".png") ||
               lowerName.endsWith(".gif") ||
               lowerName.endsWith(".bmp") ||
               lowerName.endsWith(".svg");
    }
    
    /**
     * 获取文档
     */
    public DocumentDto getDocument(Long id) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("文档不存在"));
        
        return convertToDto(document);
    }
    
    /**
     * 获取用户文档列表
     */
    public List<DocumentDto> getUserDocuments(Long userId) {
        List<Document> documents = documentRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return documents.stream().map(this::convertToDto).collect(Collectors.toList());
    }
    
    /**
     * 搜索文档
     */
    public List<DocumentDto> searchDocuments(String query) {
        List<Document> documents = documentRepository.searchDocuments(query);
        return documents.stream().map(this::convertToDto).collect(Collectors.toList());
    }
    
    /**
     * 按标签查找文档
     */
    public List<DocumentDto> getDocumentsByTag(Long tagId) {
        List<Document> documents = documentRepository.findByTagId(tagId);
        return documents.stream().map(this::convertToDto).collect(Collectors.toList());
    }
    
    /**
     * 更新文档信息
     */
    @Transactional
    public DocumentDto updateDocument(Long id, DocumentUploadDto updateDto, Long userId) {
        try {
            Document document = documentRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("文档不存在"));
            
            // 检查权限（只能更新自己的文档）
            if (!document.getUser().getId().equals(userId)) {
                throw new RuntimeException("无权限更新此文档");
            }
            
            // 更新基本信息
            document.setTitle(updateDto.getTitle());
            document.setDescription(updateDto.getDescription());
            
            // 更新内容（如果提供）
            if (updateDto.getContent() != null) {
                document.setContent(updateDto.getContent());
                // 更新文件大小
                document.setFileSize((long) updateDto.getContent().length());
            }
            
            // 更新标签
            if (updateDto.getTagIds() != null) {
                Set<Tag> tags = updateDto.getTagIds().stream()
                        .map(tagId -> tagRepository.findById(tagId)
                                .orElseThrow(() -> new RuntimeException("标签不存在: " + tagId)))
                        .collect(Collectors.toSet());
                document.setTags(tags);
            }
            
            Document savedDocument = documentRepository.save(document);
            logger.info("文档更新成功: {}", savedDocument.getTitle());
            
            return convertToDto(savedDocument);
            
        } catch (Exception e) {
            logger.error("更新文档失败", e);
            throw new RuntimeException("更新文档失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 更新文档内容
     */
    @Transactional
    public DocumentDto updateDocumentContent(Long id, String content, Long userId) {
        try {
            Document document = documentRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("文档不存在"));
            
            // 检查权限（只能更新自己的文档）
            if (!document.getUser().getId().equals(userId)) {
                throw new RuntimeException("无权限更新此文档");
            }
            
            // 更新内容
            document.setContent(content);
            // 更新文件大小
            document.setFileSize(content != null ? (long) content.length() : 0L);
            
            Document savedDocument = documentRepository.save(document);
            logger.info("文档内容更新成功: {}", savedDocument.getTitle());
            
            return convertToDto(savedDocument);
            
        } catch (Exception e) {
            logger.error("更新文档内容失败", e);
            throw new RuntimeException("更新文档内容失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 删除文档
     */
    @Transactional
    public void deleteDocument(Long id, Long userId) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("文档不存在"));
        
        // 检查权限（只能删除自己的文档）
        if (!document.getUser().getId().equals(userId)) {
            throw new RuntimeException("无权限删除此文档");
        }
        
        // 从MinIO删除文件（仅当有文件路径时）
        if (document.getFilePath() != null && !document.getFilePath().trim().isEmpty()) {
            try {
                minioService.deleteDocument(document.getFilePath());
                logger.info("MinIO文件删除成功: {}", document.getFilePath());
            } catch (Exception e) {
                logger.warn("MinIO文件删除失败: {}, 继续删除数据库记录", e.getMessage());
            }
        } else {
            logger.info("空文档删除，无需删除MinIO文件: {}", document.getTitle());
        }
        
        // 从数据库删除文档
        documentRepository.delete(document);
        logger.info("文档删除成功: {}", document.getTitle());
    }
    
    /**
     * 转换为DTO
     */
    private DocumentDto convertToDto(Document document) {
        DocumentDto dto = new DocumentDto();
        dto.setId(document.getId());
        dto.setTitle(document.getTitle());
        dto.setDescription(document.getDescription());
        dto.setFilePath(document.getFilePath());
        dto.setFileType(document.getFileType());
        dto.setFileSize(document.getFileSize());
        dto.setContent(document.getContent());
        dto.setCreatedAt(document.getCreatedAt());
        dto.setUserId(document.getUser().getId());
        
        // 设置文件URL（仅当有文件路径时）
        if (document.getFilePath() != null && !document.getFilePath().trim().isEmpty()) {
            try {
                dto.setFileUrl(minioService.getDocumentUrl(document.getFilePath()));
            } catch (Exception e) {
                logger.warn("获取MinIO文档URL失败: {}", e.getMessage());
                dto.setFileUrl("/api/documents/" + document.getId() + "/download");
            }
        } else {
            // 空文档设置为null或者特殊标识
            dto.setFileUrl(null);
        }
        
        // 设置标签
        if (document.getTags() != null) {
            Set<TagDto> tagDtos = document.getTags().stream()
                    .map(tag -> {
                        TagDto tagDto = new TagDto();
                        tagDto.setId(tag.getId());
                        tagDto.setName(tag.getName());
                        tagDto.setColor(tag.getColor());
                        return tagDto;
                    })
                    .collect(Collectors.toSet());
            dto.setTags(tagDtos);
        }
        
        return dto;
    }
} 