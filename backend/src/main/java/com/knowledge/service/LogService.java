package com.knowledge.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 日志管理服务
 */
@Service
@Slf4j
public class LogService {
    
    @Value("${logging.file.path:./logs}")
    private String logPath;
    
    private static final String APP_NAME = "knowledge-base";
    
    /**
     * 获取日志文件列表
     */
    public List<Map<String, Object>> getLogFiles() {
        List<Map<String, Object>> logFiles = new ArrayList<>();
        
        try {
            Path logDir = Paths.get(logPath);
            if (!Files.exists(logDir)) {
                Files.createDirectories(logDir);
                return logFiles;
            }
            
            Files.list(logDir)
                .filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().endsWith(".log"))
                .forEach(path -> {
                    File file = path.toFile();
                    Map<String, Object> fileInfo = new HashMap<>();
                    fileInfo.put("name", file.getName());
                    fileInfo.put("size", file.length());
                    fileInfo.put("lastModified", file.lastModified());
                    fileInfo.put("path", path.toString());
                    
                    // 判断日志类型
                    String fileName = file.getName();
                    if (fileName.contains("error")) {
                        fileInfo.put("type", "error");
                    } else if (fileName.contains("operation")) {
                        fileInfo.put("type", "operation");
                    } else {
                        fileInfo.put("type", "application");
                    }
                    
                    logFiles.add(fileInfo);
                });
                
        } catch (IOException e) {
            log.error("获取日志文件列表失败", e);
        }
        
        // 按修改时间倒序排列
        return logFiles.stream()
            .sorted((a, b) -> Long.compare((Long) b.get("lastModified"), (Long) a.get("lastModified")))
            .collect(Collectors.toList());
    }
    
    /**
     * 读取日志文件内容
     */
    public Map<String, Object> readLogFile(String fileName, int page, int size) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Path logFile = Paths.get(logPath, fileName);
            if (!Files.exists(logFile)) {
                result.put("success", false);
                result.put("message", "日志文件不存在");
                return result;
            }
            
            List<String> allLines = Files.readAllLines(logFile);
            int totalLines = allLines.size();
            
            // 计算分页
            int start = Math.max(0, totalLines - (page * size));
            int end = Math.max(0, totalLines - ((page - 1) * size));
            
            List<String> pageLines = allLines.subList(start, end);
            Collections.reverse(pageLines); // 最新的日志在前面
            
            result.put("success", true);
            result.put("content", pageLines);
            result.put("totalLines", totalLines);
            result.put("currentPage", page);
            result.put("pageSize", size);
            result.put("hasMore", start > 0);
            
        } catch (IOException e) {
            log.error("读取日志文件失败: {}", fileName, e);
            result.put("success", false);
            result.put("message", "读取日志文件失败: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 搜索日志内容
     */
    public Map<String, Object> searchLogs(String fileName, String keyword) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> matches = new ArrayList<>();
        
        try {
            Path logFile = Paths.get(logPath, fileName);
            if (!Files.exists(logFile)) {
                result.put("success", false);
                result.put("message", "日志文件不存在");
                return result;
            }
            
            List<String> allLines = Files.readAllLines(logFile);
            
            for (int i = 0; i < allLines.size(); i++) {
                String line = allLines.get(i);
                if (line.toLowerCase().contains(keyword.toLowerCase())) {
                    Map<String, Object> match = new HashMap<>();
                    match.put("lineNumber", i + 1);
                    match.put("content", line);
                    matches.add(match);
                }
            }
            
            result.put("success", true);
            result.put("matches", matches);
            result.put("totalMatches", matches.size());
            result.put("keyword", keyword);
            
        } catch (IOException e) {
            log.error("搜索日志失败: {} - {}", fileName, keyword, e);
            result.put("success", false);
            result.put("message", "搜索日志失败: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 获取今日日志统计
     */
    public Map<String, Object> getTodayLogStats() {
        Map<String, Object> stats = new HashMap<>();
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        
        try {
            // 统计应用日志
            stats.put("applicationLogs", countLogsByDate("application", today));
            
            // 统计错误日志
            stats.put("errorLogs", countLogsByDate("error", today));
            
            // 统计操作日志
            stats.put("operationLogs", countLogsByDate("operation", today));
            
            stats.put("date", today);
            stats.put("success", true);
            
        } catch (Exception e) {
            log.error("获取今日日志统计失败", e);
            stats.put("success", false);
            stats.put("message", "获取统计失败: " + e.getMessage());
        }
        
        return stats;
    }
    
    /**
     * 根据日期统计日志数量
     */
    private int countLogsByDate(String logType, String date) {
        try {
            String fileName;
            switch (logType) {
                case "error":
                    fileName = APP_NAME + "-error.log";
                    break;
                case "operation":
                    fileName = APP_NAME + "-operation.log";
                    break;
                default:
                    fileName = APP_NAME + ".log";
                    break;
            }
            
            Path logFile = Paths.get(logPath, fileName);
            if (Files.exists(logFile)) {
                return (int) Files.lines(logFile)
                    .filter(line -> line.contains(date))
                    .count();
            }
        } catch (IOException e) {
            log.warn("统计日志数量失败: {} - {}", logType, date, e);
        }
        
        return 0;
    }
    
    /**
     * 清理旧日志文件
     */
    public Map<String, Object> cleanOldLogs(int daysToKeep) {
        Map<String, Object> result = new HashMap<>();
        int deletedCount = 0;
        long freedSpace = 0;
        
        try {
            Path logDir = Paths.get(logPath);
            if (!Files.exists(logDir)) {
                result.put("success", true);
                result.put("message", "日志目录不存在");
                return result;
            }
            
            long cutoffTime = System.currentTimeMillis() - (daysToKeep * 24L * 60 * 60 * 1000);
            
            List<File> filesToDelete = Files.list(logDir)
                .filter(Files::isRegularFile)
                .map(Path::toFile)
                .filter(file -> file.getName().endsWith(".log") || file.getName().endsWith(".gz"))
                .filter(file -> file.lastModified() < cutoffTime)
                .collect(Collectors.toList());
            
            for (File file : filesToDelete) {
                long fileSize = file.length();
                if (file.delete()) {
                    deletedCount++;
                    freedSpace += fileSize;
                    log.info("删除过期日志文件: {}", file.getName());
                }
            }
            
            result.put("success", true);
            result.put("deletedCount", deletedCount);
            result.put("freedSpace", freedSpace);
            result.put("message", String.format("成功清理 %d 个文件，释放 %d 字节空间", deletedCount, freedSpace));
            
        } catch (IOException e) {
            log.error("清理日志文件失败", e);
            result.put("success", false);
            result.put("message", "清理失败: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 获取日志文件详细信息
     */
    public Map<String, Object> getLogFileInfo(String fileName) {
        Map<String, Object> info = new HashMap<>();
        
        try {
            Path logFile = Paths.get(logPath, fileName);
            if (!Files.exists(logFile)) {
                info.put("success", false);
                info.put("message", "日志文件不存在");
                return info;
            }
            
            File file = logFile.toFile();
            long totalLines = Files.lines(logFile).count();
            
            info.put("success", true);
            info.put("name", file.getName());
            info.put("size", file.length());
            info.put("lastModified", file.lastModified());
            info.put("totalLines", totalLines);
            info.put("path", logFile.toString());
            
            // 分析日志级别分布
            Map<String, Integer> levelStats = new HashMap<>();
            Files.lines(logFile).forEach(line -> {
                if (line.contains("ERROR")) {
                    levelStats.merge("ERROR", 1, Integer::sum);
                } else if (line.contains("WARN")) {
                    levelStats.merge("WARN", 1, Integer::sum);
                } else if (line.contains("INFO")) {
                    levelStats.merge("INFO", 1, Integer::sum);
                } else if (line.contains("DEBUG")) {
                    levelStats.merge("DEBUG", 1, Integer::sum);
                }
            });
            
            info.put("levelStats", levelStats);
            
        } catch (IOException e) {
            log.error("获取日志文件信息失败: {}", fileName, e);
            info.put("success", false);
            info.put("message", "获取文件信息失败: " + e.getMessage());
        }
        
        return info;
    }
} 