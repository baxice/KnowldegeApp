package com.knowledge.service;

import com.knowledge.dto.NoteCreateDto;
import com.knowledge.dto.NoteDto;

import java.util.List;

public interface NoteService {
    
    /**
     * 创建笔记
     */
    NoteDto createNote(NoteCreateDto createDto, Long userId);
    
    /**
     * 获取笔记
     */
    NoteDto getNote(Long id);
    
    /**
     * 更新笔记
     */
    NoteDto updateNote(Long id, NoteCreateDto updateDto, Long userId);
    
    /**
     * 删除笔记
     */
    void deleteNote(Long id, Long userId);
    
    /**
     * 获取用户笔记列表
     */
    List<NoteDto> getUserNotes(Long userId);
    
    /**
     * 搜索笔记
     */
    List<NoteDto> searchNotes(String query, Long userId);
    
    /**
     * 按标签查找笔记
     */
    List<NoteDto> getNotesByTag(Long tagId, Long userId);
    
    /**
     * 获取最近编辑的笔记
     */
    List<NoteDto> getRecentNotes(Long userId, int limit);
    
    /**
     * 为笔记添加标签
     */
    NoteDto addTag(Long noteId, Long tagId, Long userId);
    
    /**
     * 从笔记移除标签
     */
    NoteDto removeTag(Long noteId, Long tagId, Long userId);
} 