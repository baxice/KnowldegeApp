package com.knowledge.controller;

import com.knowledge.annotation.Log;
import com.knowledge.dto.DocumentDto;
import com.knowledge.dto.DocumentUploadDto;
import com.knowledge.service.DocumentService;
import com.knowledge.service.MinioService;
import com.knowledge.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class DocumentController {
    
    private static final Logger logger = LoggerFactory.getLogger(DocumentController.class);
    
    private final DocumentService documentService;
    private final MinioService minioService;
    private final JwtUtil jwtUtil;
    
    /**
     * 获取当前用户的所有文档
     */
    @GetMapping
    @Log(value = "获取文档列表", module = "文档管理", description = "获取用户文档列表")
    public ResponseEntity<List<DocumentDto>> getUserDocuments(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        List<DocumentDto> documents = documentService.getUserDocuments(userId);
        return ResponseEntity.ok(documents);
    }
    
    /**
     * 根据ID获取文档
     */
    @GetMapping("/{id}")
    @Log(value = "获取文档详情", module = "文档管理", description = "根据ID获取文档详情")
    public ResponseEntity<DocumentDto> getDocument(@PathVariable Long id) {
        DocumentDto document = documentService.getDocument(id);
        return ResponseEntity.ok(document);
    }
    
    /**
     * 上传文档
     */
    @PostMapping("/upload")
    @Log(value = "上传文档", module = "文档管理", description = "上传新文档")
    public ResponseEntity<DocumentDto> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "tagIds", required = false) String tagIds,
            HttpServletRequest request) {
        
        Long userId = getCurrentUserId(request);
        
        // 创建上传DTO
        DocumentUploadDto uploadDto = new DocumentUploadDto();
        uploadDto.setTitle(title != null ? title : file.getOriginalFilename());
        uploadDto.setDescription(description);
        
        // 处理标签ID（简化处理，假设是逗号分隔的ID字符串）
        if (tagIds != null && !tagIds.trim().isEmpty()) {
            try {
                String[] tagIdArray = tagIds.split(",");
                Set<Long> tagIdSet = new HashSet<>();
                for (String tagId : tagIdArray) {
                    tagIdSet.add(Long.parseLong(tagId.trim()));
                }
                uploadDto.setTagIds(tagIdSet);
            } catch (NumberFormatException e) {
                // 忽略格式错误的标签ID
            }
        }
        
        DocumentDto document = documentService.uploadDocument(file, uploadDto, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(document);
    }
    
    /**
     * 批量上传文档
     */
    @PostMapping("/upload/multiple")
    @Log(value = "批量上传文档", module = "文档管理", description = "批量上传多个文档")
    public ResponseEntity<List<DocumentDto>> uploadMultipleDocuments(
            @RequestParam("files") MultipartFile[] files,
            HttpServletRequest request) {
        
        Long userId = getCurrentUserId(request);
        List<DocumentDto> uploadedDocuments = new ArrayList<>();
        
        for (MultipartFile file : files) {
            DocumentUploadDto uploadDto = new DocumentUploadDto();
            uploadDto.setTitle(file.getOriginalFilename());
            uploadDto.setDescription("批量上传的文档");
            
            DocumentDto document = documentService.uploadDocument(file, uploadDto, userId);
            uploadedDocuments.add(document);
        }
        
        return ResponseEntity.status(HttpStatus.CREATED).body(uploadedDocuments);
    }
    
    /**
     * 更新文档信息
     */
    @PutMapping("/{id}")
    @Log(value = "更新文档", module = "文档管理", description = "更新文档信息")
    public ResponseEntity<DocumentDto> updateDocument(
            @PathVariable Long id,
            @Valid @RequestBody DocumentUploadDto updateDto,
            HttpServletRequest request) {
        
        Long userId = getCurrentUserId(request);
        DocumentDto document = documentService.updateDocument(id, updateDto, userId);
        return ResponseEntity.ok(document);
    }
    
    /**
     * 更新文档内容
     */
    @PutMapping("/{id}/content")
    @Log(value = "更新文档内容", module = "文档管理", description = "更新文档内容")
    public ResponseEntity<DocumentDto> updateDocumentContent(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload,
            HttpServletRequest request) {
        
        Long userId = getCurrentUserId(request);
        String content = payload.get("content");
        DocumentDto document = documentService.updateDocumentContent(id, content, userId);
        return ResponseEntity.ok(document);
    }
    
    /**
     * 删除文档
     */
    @DeleteMapping("/{id}")
    @Log(value = "删除文档", module = "文档管理", description = "删除指定文档")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long id, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        documentService.deleteDocument(id, userId);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * 下载文档
     */
    @GetMapping("/{id}/download")
    @Log(value = "下载文档", module = "文档管理", description = "下载指定文档")
    public ResponseEntity<InputStreamResource> downloadDocument(@PathVariable Long id) {
        try {
            DocumentDto document = documentService.getDocument(id);
            InputStream inputStream = minioService.getDocumentStream(document.getFilePath());
            
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + document.getTitle());
            headers.add(HttpHeaders.CONTENT_TYPE, document.getFileType());
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(new InputStreamResource(inputStream));
        } catch (Exception e) {
            logger.error("下载文档失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * 获取文档预览URL
     */
    @GetMapping("/{id}/preview")
    @Log(value = "预览文档", module = "文档管理", description = "获取文档预览URL")
    public ResponseEntity<Map<String, String>> getDocumentPreviewUrl(@PathVariable Long id) {
        try {
            DocumentDto document = documentService.getDocument(id);
            String previewUrl = minioService.getDocumentUrl(document.getFilePath());
            return ResponseEntity.ok(Map.of("previewUrl", previewUrl));
        } catch (Exception e) {
            logger.error("获取文档预览URL失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "获取预览URL失败"));
        }
    }
    
    /**
     * 搜索文档
     */
    @GetMapping("/search")
    @Log(value = "搜索文档", module = "文档管理", description = "根据关键词搜索文档")
    public ResponseEntity<List<DocumentDto>> searchDocuments(@RequestParam("q") String query) {
        List<DocumentDto> documents = documentService.searchDocuments(query);
        return ResponseEntity.ok(documents);
    }
    
    /**
     * 根据标签获取文档
     */
    @GetMapping("/tag/{tagId}")
    @Log(value = "按标签查询文档", module = "文档管理", description = "根据标签ID获取文档")
    public ResponseEntity<List<DocumentDto>> getDocumentsByTag(@PathVariable Long tagId) {
        List<DocumentDto> documents = documentService.getDocumentsByTag(tagId);
        return ResponseEntity.ok(documents);
    }
    
    /**
     * 为文档添加标签
     */
    @PostMapping("/{documentId}/tags")
    @Log(value = "添加文档标签", module = "文档管理", description = "为文档添加标签")
    public ResponseEntity<DocumentDto> addTag(
            @PathVariable Long documentId,
            @RequestBody Map<String, Long> payload) {
        // 这里需要在DocumentService中添加标签管理方法
        // Long tagId = payload.get("tagId");
        // DocumentDto document = documentService.addTag(documentId, tagId);
        // return ResponseEntity.ok(document);
        
        // 临时返回原文档信息
        DocumentDto document = documentService.getDocument(documentId);
        return ResponseEntity.ok(document);
    }
    
    /**
     * 从文档移除标签
     */
    @DeleteMapping("/{documentId}/tags/{tagId}")
    @Log(value = "移除文档标签", module = "文档管理", description = "从文档移除标签")
    public ResponseEntity<DocumentDto> removeTag(
            @PathVariable Long documentId,
            @PathVariable Long tagId) {
        // 这里需要在DocumentService中添加标签管理方法
        // DocumentDto document = documentService.removeTag(documentId, tagId);
        // return ResponseEntity.ok(document);
        
        // 临时返回原文档信息
        DocumentDto document = documentService.getDocument(documentId);
        return ResponseEntity.ok(document);
    }
    
    /**
     * 获取文档统计信息
     */
    @GetMapping("/stats")
    @Log(value = "获取文档统计", module = "文档管理", description = "获取文档统计信息")
    public ResponseEntity<Map<String, Object>> getStats(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        List<DocumentDto> documents = documentService.getUserDocuments(userId);
        
        long totalSize = documents.stream()
                .mapToLong(doc -> doc.getFileSize() != null ? doc.getFileSize() : 0)
                .sum();
        
        Map<String, Object> stats = Map.of(
                "totalDocuments", documents.size(),
                "totalSize", totalSize,
                "recentDocuments", Math.min(documents.size(), 5)
        );
        
        return ResponseEntity.ok(stats);
    }
    
    /**
     * 获取最近上传的文档
     */
    @GetMapping("/recent")
    @Log(value = "获取最近文档", module = "文档管理", description = "获取最近上传的文档")
    public ResponseEntity<List<DocumentDto>> getRecentDocuments(
            @RequestParam(defaultValue = "10") int limit,
            HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        List<DocumentDto> documents = documentService.getUserDocuments(userId);
        
        // 简单截取前N个文档（实际应该按时间排序）
        List<DocumentDto> recentDocuments = documents.stream()
                .limit(limit)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(recentDocuments);
    }
    
    /**
     * 创建新文档（空文档）
     */
    @PostMapping("/create")
    @Log(value = "创建新文档", module = "文档管理", description = "创建空的新文档")
    public ResponseEntity<DocumentDto> createDocument(
            @Valid @RequestBody DocumentUploadDto createDto,
            HttpServletRequest request) {
        
        Long userId = getCurrentUserId(request);
        DocumentDto document = documentService.createDocument(createDto, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(document);
    }
    
    /**
     * 从请求中获取当前用户ID
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            return jwtUtil.getUserIdFromJwtToken(token);
        }
        throw new RuntimeException("未找到有效的认证令牌");
    }
} 