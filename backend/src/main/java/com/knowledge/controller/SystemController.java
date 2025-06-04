package com.knowledge.controller;

import com.knowledge.service.MinioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/system")
@CrossOrigin(origins = "*", maxAge = 3600)
public class SystemController {

    @Autowired
    private MinioService minioService;

    /**
     * 系统健康检查
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "UP");
        status.put("timestamp", System.currentTimeMillis());
        
        // 检查MinIO连接状态
        try {
            minioService.initBucket();
            status.put("minio", "UP");
        } catch (Exception e) {
            status.put("minio", "DOWN");
            status.put("minioError", e.getMessage());
        }
        
        return ResponseEntity.ok(status);
    }
    
    /**
     * 检查存储服务状态
     */
    @GetMapping("/storage/status")
    public ResponseEntity<Map<String, Object>> checkStorageStatus() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            minioService.initBucket();
            result.put("status", "available");
            result.put("message", "存储服务正常");
        } catch (Exception e) {
            result.put("status", "unavailable");
            result.put("message", "存储服务不可用: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());
        }
        
        return ResponseEntity.ok(result);
    }
} 