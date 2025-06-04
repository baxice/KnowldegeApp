package com.knowledge.repository;

import com.knowledge.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    
    List<Document> findByUserId(Long userId);
    
    List<Document> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    @Query("SELECT d FROM Document d WHERE d.title LIKE %:query% OR d.description LIKE %:query%")
    List<Document> searchDocuments(String query);
    
    @Query("SELECT d FROM Document d JOIN d.tags t WHERE t.id = :tagId")
    List<Document> findByTagId(Long tagId);
    
    @Query("SELECT d FROM Document d WHERE d.user.id = :userId ORDER BY d.createdAt DESC")
    List<Document> findRecentDocumentsByUserId(Long userId, org.springframework.data.domain.Pageable pageable);
} 