package com.knowledge.service;

import com.knowledge.dto.TagDto;
import com.knowledge.entity.Tag;
import com.knowledge.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TagService {
    
    private final TagRepository tagRepository;
    
    /**
     * 创建标签
     */
    @Transactional
    public TagDto createTag(TagDto tagDto) {
        // 检查标签名是否已存在
        if (tagRepository.findByName(tagDto.getName()).isPresent()) {
            throw new RuntimeException("标签名已存在");
        }
        
        // 创建新标签
        Tag tag = new Tag();
        tag.setName(tagDto.getName());
        tag.setColor(tagDto.getColor());
        
        // 保存标签
        Tag savedTag = tagRepository.save(tag);
        
        // 返回DTO
        return convertToDto(savedTag);
    }
    
    /**
     * 获取所有标签
     */
    public List<TagDto> getAllTags() {
        List<Tag> tags = tagRepository.findAll();
        return tags.stream().map(this::convertToDto).collect(Collectors.toList());
    }
    
    /**
     * 搜索标签
     */
    public List<TagDto> searchTags(String query) {
        List<Tag> tags = tagRepository.findByNameContaining(query);
        return tags.stream().map(this::convertToDto).collect(Collectors.toList());
    }
    
    /**
     * 获取标签
     */
    public TagDto getTag(Long id) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("标签不存在"));
        
        return convertToDto(tag);
    }
    
    /**
     * 更新标签
     */
    @Transactional
    public TagDto updateTag(Long id, TagDto tagDto) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("标签不存在"));
        
        // 检查标签名是否已存在（排除当前标签）
        tagRepository.findByName(tagDto.getName())
                .ifPresent(existingTag -> {
                    if (!existingTag.getId().equals(id)) {
                        throw new RuntimeException("标签名已存在");
                    }
                });
        
        // 更新标签
        tag.setName(tagDto.getName());
        tag.setColor(tagDto.getColor());
        
        // 保存标签
        Tag updatedTag = tagRepository.save(tag);
        
        // 返回DTO
        return convertToDto(updatedTag);
    }
    
    /**
     * 删除标签
     */
    @Transactional
    public void deleteTag(Long id) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("标签不存在"));
        
        // 检查标签是否被使用
        if (!tag.getDocuments().isEmpty()) {
            throw new RuntimeException("标签正在使用中，无法删除");
        }
        
        // 删除标签
        tagRepository.delete(tag);
    }
    
    /**
     * 转换为DTO
     */
    private TagDto convertToDto(Tag tag) {
        return new TagDto(tag.getId(), tag.getName(), tag.getColor());
    }
} 