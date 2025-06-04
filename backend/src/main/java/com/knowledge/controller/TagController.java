package com.knowledge.controller;

import com.knowledge.dto.TagDto;
import com.knowledge.service.TagService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class TagController {
    
    private final TagService tagService;
    
    /**
     * 创建标签
     */
    @PostMapping
    public ResponseEntity<TagDto> createTag(@Valid @RequestBody TagDto tagDto) {
        TagDto createdTag = tagService.createTag(tagDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdTag);
    }
    
    /**
     * 获取所有标签
     */
    @GetMapping
    public ResponseEntity<List<TagDto>> getAllTags() {
        List<TagDto> tags = tagService.getAllTags();
        return ResponseEntity.ok(tags);
    }
    
    /**
     * 搜索标签
     */
    @GetMapping("/search")
    public ResponseEntity<List<TagDto>> searchTags(@RequestParam("query") String query) {
        List<TagDto> tags = tagService.searchTags(query);
        return ResponseEntity.ok(tags);
    }
    
    /**
     * 获取单个标签
     */
    @GetMapping("/{id}")
    public ResponseEntity<TagDto> getTag(@PathVariable Long id) {
        TagDto tag = tagService.getTag(id);
        return ResponseEntity.ok(tag);
    }
    
    /**
     * 更新标签
     */
    @PutMapping("/{id}")
    public ResponseEntity<TagDto> updateTag(
            @PathVariable Long id,
            @Valid @RequestBody TagDto tagDto) {
        TagDto updatedTag = tagService.updateTag(id, tagDto);
        return ResponseEntity.ok(updatedTag);
    }
    
    /**
     * 删除标签
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTag(@PathVariable Long id) {
        tagService.deleteTag(id);
        return ResponseEntity.noContent().build();
    }
} 