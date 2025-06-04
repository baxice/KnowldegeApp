package com.knowledge.service;

import com.knowledge.dto.NoteCreateDto;
import com.knowledge.dto.NoteDto;
import com.knowledge.dto.TagDto;
import com.knowledge.entity.Note;
import com.knowledge.entity.Tag;
import com.knowledge.entity.User;
import com.knowledge.repository.NoteRepository;
import com.knowledge.repository.TagRepository;
import com.knowledge.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NoteServiceImpl implements NoteService {
    
    private final NoteRepository noteRepository;
    private final TagRepository tagRepository;
    private final UserRepository userRepository;
    
    @Override
    @Transactional
    public NoteDto createNote(NoteCreateDto createDto, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        Note note = new Note();
        note.setTitle(createDto.getTitle());
        note.setContent(createDto.getContent());
        note.setUser(user);
        
        // 添加标签
        if (createDto.getTagIds() != null && !createDto.getTagIds().isEmpty()) {
            Set<Tag> tags = createDto.getTagIds().stream()
                    .map(tagId -> tagRepository.findById(tagId)
                            .orElseThrow(() -> new RuntimeException("标签不存在: " + tagId)))
                    .collect(Collectors.toSet());
            note.setTags(tags);
        }
        
        Note savedNote = noteRepository.save(note);
        return convertToDto(savedNote);
    }
    
    @Override
    public NoteDto getNote(Long id) {
        Note note = noteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("笔记不存在"));
        return convertToDto(note);
    }
    
    @Override
    @Transactional
    public NoteDto updateNote(Long id, NoteCreateDto updateDto, Long userId) {
        Note note = noteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("笔记不存在"));
        
        // 检查权限
        if (!note.getUser().getId().equals(userId)) {
            throw new RuntimeException("无权限修改此笔记");
        }
        
        note.setTitle(updateDto.getTitle());
        note.setContent(updateDto.getContent());
        
        // 更新标签
        if (updateDto.getTagIds() != null) {
            Set<Tag> tags = updateDto.getTagIds().stream()
                    .map(tagId -> tagRepository.findById(tagId)
                            .orElseThrow(() -> new RuntimeException("标签不存在: " + tagId)))
                    .collect(Collectors.toSet());
            note.setTags(tags);
        }
        
        Note updatedNote = noteRepository.save(note);
        return convertToDto(updatedNote);
    }
    
    @Override
    @Transactional
    public void deleteNote(Long id, Long userId) {
        Note note = noteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("笔记不存在"));
        
        // 检查权限
        if (!note.getUser().getId().equals(userId)) {
            throw new RuntimeException("无权限删除此笔记");
        }
        
        noteRepository.delete(note);
    }
    
    @Override
    public List<NoteDto> getUserNotes(Long userId) {
        List<Note> notes = noteRepository.findByUserIdOrderByUpdatedAtDesc(userId);
        return notes.stream().map(this::convertToDto).collect(Collectors.toList());
    }
    
    @Override
    public List<NoteDto> searchNotes(String query, Long userId) {
        List<Note> notes = noteRepository.searchNotes(query);
        // 过滤用户权限
        return notes.stream()
                .filter(note -> note.getUser().getId().equals(userId))
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<NoteDto> getNotesByTag(Long tagId, Long userId) {
        List<Note> notes = noteRepository.findByTagId(tagId);
        // 过滤用户权限
        return notes.stream()
                .filter(note -> note.getUser().getId().equals(userId))
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<NoteDto> getRecentNotes(Long userId, int limit) {
        PageRequest pageRequest = PageRequest.of(0, limit);
        List<Note> notes = noteRepository.findRecentNotesByUserId(userId, pageRequest);
        return notes.stream().map(this::convertToDto).collect(Collectors.toList());
    }
    
    @Override
    @Transactional
    public NoteDto addTag(Long noteId, Long tagId, Long userId) {
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new RuntimeException("笔记不存在"));
        
        // 检查权限
        if (!note.getUser().getId().equals(userId)) {
            throw new RuntimeException("无权限修改此笔记");
        }
        
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new RuntimeException("标签不存在"));
        
        note.getTags().add(tag);
        Note updatedNote = noteRepository.save(note);
        return convertToDto(updatedNote);
    }
    
    @Override
    @Transactional
    public NoteDto removeTag(Long noteId, Long tagId, Long userId) {
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new RuntimeException("笔记不存在"));
        
        // 检查权限
        if (!note.getUser().getId().equals(userId)) {
            throw new RuntimeException("无权限修改此笔记");
        }
        
        note.getTags().removeIf(tag -> tag.getId().equals(tagId));
        Note updatedNote = noteRepository.save(note);
        return convertToDto(updatedNote);
    }
    
    /**
     * 转换为DTO
     */
    private NoteDto convertToDto(Note note) {
        NoteDto dto = new NoteDto();
        dto.setId(note.getId());
        dto.setTitle(note.getTitle());
        dto.setContent(note.getContent());
        dto.setUserId(note.getUser().getId());
        dto.setUsername(note.getUser().getUsername());
        dto.setCreatedAt(note.getCreatedAt());
        dto.setUpdatedAt(note.getUpdatedAt());
        
        // 转换标签
        Set<TagDto> tagDtos = note.getTags().stream()
                .map(tag -> new TagDto(tag.getId(), tag.getName(), tag.getColor()))
                .collect(Collectors.toSet());
        dto.setTags(tagDtos);
        
        return dto;
    }
} 