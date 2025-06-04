package com.knowledge.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentDto {
    private Long id;
    private String title;
    private String description;
    private String filePath;
    private String fileType;
    private Long fileSize;
    private String fileUrl;
    private String content;
    private Long userId;
    private String username;
    private LocalDateTime createdAt;
    private Set<TagDto> tags = new HashSet<>();
} 