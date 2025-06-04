package com.knowledge.controller;

import com.knowledge.annotation.Log;
import com.knowledge.dto.NoteCreateDto;
import com.knowledge.dto.NoteDto;
import com.knowledge.service.NoteService;
import com.knowledge.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class NoteController {
    
    private final NoteService noteService;
    private final JwtUtil jwtUtil;
    
    /**
     * 获取当前用户的所有笔记
     */
    @GetMapping
    @Log(value = "获取笔记列表", module = "笔记管理", description = "获取用户笔记列表")
    public ResponseEntity<List<NoteDto>> getUserNotes(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        List<NoteDto> notes = noteService.getUserNotes(userId);
        return ResponseEntity.ok(notes);
    }
    
    /**
     * 根据ID获取笔记
     */
    @GetMapping("/{id}")
    @Log(value = "获取笔记详情", module = "笔记管理", description = "根据ID获取笔记详情")
    public ResponseEntity<NoteDto> getNote(@PathVariable Long id) {
        NoteDto note = noteService.getNote(id);
        return ResponseEntity.ok(note);
    }
    
    /**
     * 创建笔记
     */
    @PostMapping
    @Log(value = "创建笔记", module = "笔记管理", description = "创建新笔记")
    public ResponseEntity<NoteDto> createNote(
            @Valid @RequestBody NoteCreateDto createDto,
            HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        NoteDto note = noteService.createNote(createDto, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(note);
    }
    
    /**
     * 更新笔记
     */
    @PutMapping("/{id}")
    @Log(value = "更新笔记", module = "笔记管理", description = "更新笔记内容")
    public ResponseEntity<NoteDto> updateNote(
            @PathVariable Long id,
            @Valid @RequestBody NoteCreateDto updateDto,
            HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        NoteDto note = noteService.updateNote(id, updateDto, userId);
        return ResponseEntity.ok(note);
    }
    
    /**
     * 删除笔记
     */
    @DeleteMapping("/{id}")
    @Log(value = "删除笔记", module = "笔记管理", description = "删除指定笔记")
    public ResponseEntity<Void> deleteNote(
            @PathVariable Long id,
            HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        noteService.deleteNote(id, userId);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * 搜索笔记
     */
    @GetMapping("/search")
    @Log(value = "搜索笔记", module = "笔记管理", description = "根据关键词搜索笔记")
    public ResponseEntity<List<NoteDto>> searchNotes(
            @RequestParam("q") String query,
            HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        List<NoteDto> notes = noteService.searchNotes(query, userId);
        return ResponseEntity.ok(notes);
    }
    
    /**
     * 根据标签获取笔记
     */
    @GetMapping("/tag/{tagId}")
    @Log(value = "按标签查询笔记", module = "笔记管理", description = "根据标签ID获取笔记")
    public ResponseEntity<List<NoteDto>> getNotesByTag(
            @PathVariable Long tagId,
            HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        List<NoteDto> notes = noteService.getNotesByTag(tagId, userId);
        return ResponseEntity.ok(notes);
    }
    
    /**
     * 获取最近编辑的笔记
     */
    @GetMapping("/recent")
    @Log(value = "获取最近笔记", module = "笔记管理", description = "获取最近编辑的笔记")
    public ResponseEntity<List<NoteDto>> getRecentNotes(
            @RequestParam(defaultValue = "10") int limit,
            HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        List<NoteDto> notes = noteService.getRecentNotes(userId, limit);
        return ResponseEntity.ok(notes);
    }
    
    /**
     * 为笔记添加标签
     */
    @PostMapping("/{noteId}/tags")
    @Log(value = "添加笔记标签", module = "笔记管理", description = "为笔记添加标签")
    public ResponseEntity<NoteDto> addTag(
            @PathVariable Long noteId,
            @RequestBody Map<String, Long> payload,
            HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        Long tagId = payload.get("tagId");
        NoteDto note = noteService.addTag(noteId, tagId, userId);
        return ResponseEntity.ok(note);
    }
    
    /**
     * 从笔记移除标签
     */
    @DeleteMapping("/{noteId}/tags/{tagId}")
    @Log(value = "移除笔记标签", module = "笔记管理", description = "从笔记移除标签")
    public ResponseEntity<NoteDto> removeTag(
            @PathVariable Long noteId,
            @PathVariable Long tagId,
            HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        NoteDto note = noteService.removeTag(noteId, tagId, userId);
        return ResponseEntity.ok(note);
    }
    
    /**
     * 获取笔记统计信息
     */
    @GetMapping("/stats")
    @Log(value = "获取笔记统计", module = "笔记管理", description = "获取笔记统计信息")
    public ResponseEntity<Map<String, Object>> getStats(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        List<NoteDto> notes = noteService.getUserNotes(userId);
        
        Map<String, Object> stats = Map.of(
                "totalNotes", notes.size(),
                "recentNotes", Math.min(notes.size(), 5)
        );
        
        return ResponseEntity.ok(stats);
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