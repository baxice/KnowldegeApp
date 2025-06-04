package com.knowledge.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Data
public class DocumentUploadDto {
    @NotBlank(message = "标题不能为空")
    private String title;
    
    private String description;
    
    private String content;
    
    private Set<Long> tagIds = new HashSet<>();
} 