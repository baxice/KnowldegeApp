package com.knowledge.controller;

import com.knowledge.annotation.Log;
import com.knowledge.service.LogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 日志管理控制器
 */
@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
@Tag(name = "日志管理", description = "日志查看和管理相关API")
public class LogController {
    
    private final LogService logService;
    
    /**
     * 获取日志文件列表
     */
    @GetMapping("/files")
    @Operation(summary = "获取日志文件列表")
    @PreAuthorize("hasRole('ADMIN')")
    @Log(value = "查看日志文件列表", module = "日志管理", description = "获取系统所有日志文件列表")
    public ResponseEntity<List<Map<String, Object>>> getLogFiles() {
        List<Map<String, Object>> logFiles = logService.getLogFiles();
        return ResponseEntity.ok(logFiles);
    }
    
    /**
     * 读取日志文件内容
     */
    @GetMapping("/content")
    @Operation(summary = "读取日志文件内容")
    @PreAuthorize("hasRole('ADMIN')")
    @Log(value = "查看日志内容", module = "日志管理", description = "分页读取指定日志文件内容")
    public ResponseEntity<Map<String, Object>> readLogFile(
            @Parameter(description = "日志文件名") @RequestParam String fileName,
            @Parameter(description = "页码，从1开始") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "100") int size) {
        Map<String, Object> result = logService.readLogFile(fileName, page, size);
        return ResponseEntity.ok(result);
    }
    
    /**
     * 搜索日志内容
     */
    @GetMapping("/search")
    @Operation(summary = "搜索日志内容")
    @PreAuthorize("hasRole('ADMIN')")
    @Log(value = "搜索日志", module = "日志管理", description = "在指定日志文件中搜索关键词")
    public ResponseEntity<Map<String, Object>> searchLogs(
            @Parameter(description = "日志文件名") @RequestParam String fileName,
            @Parameter(description = "搜索关键词") @RequestParam String keyword) {
        Map<String, Object> result = logService.searchLogs(fileName, keyword);
        return ResponseEntity.ok(result);
    }
    
    /**
     * 获取今日日志统计
     */
    @GetMapping("/stats/today")
    @Operation(summary = "获取今日日志统计")
    @PreAuthorize("hasRole('ADMIN')")
    @Log(value = "查看日志统计", module = "日志管理", description = "获取今日各类型日志数量统计")
    public ResponseEntity<Map<String, Object>> getTodayStats() {
        Map<String, Object> stats = logService.getTodayLogStats();
        return ResponseEntity.ok(stats);
    }
    
    /**
     * 获取日志文件详细信息
     */
    @GetMapping("/info")
    @Operation(summary = "获取日志文件详细信息")
    @PreAuthorize("hasRole('ADMIN')")
    @Log(value = "查看日志文件信息", module = "日志管理", description = "获取指定日志文件的详细信息和统计")
    public ResponseEntity<Map<String, Object>> getLogFileInfo(
            @Parameter(description = "日志文件名") @RequestParam String fileName) {
        Map<String, Object> info = logService.getLogFileInfo(fileName);
        return ResponseEntity.ok(info);
    }
    
    /**
     * 清理过期日志文件
     */
    @DeleteMapping("/clean")
    @Operation(summary = "清理过期日志文件")
    @PreAuthorize("hasRole('ADMIN')")
    @Log(value = "清理日志", module = "日志管理", description = "清理指定天数之前的过期日志文件")
    public ResponseEntity<Map<String, Object>> cleanOldLogs(
            @Parameter(description = "保留天数") @RequestParam(defaultValue = "30") int daysToKeep) {
        Map<String, Object> result = logService.cleanOldLogs(daysToKeep);
        return ResponseEntity.ok(result);
    }
} 