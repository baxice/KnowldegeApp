package com.knowledge.repository;

import com.knowledge.entity.Note;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NoteRepository extends JpaRepository<Note, Long> {
    
    List<Note> findByUserId(Long userId);
    
    List<Note> findByUserIdOrderByUpdatedAtDesc(Long userId);
    
    @Query("SELECT n FROM Note n WHERE n.title LIKE %:query% OR n.content LIKE %:query%")
    List<Note> searchNotes(String query);
    
    @Query("SELECT n FROM Note n JOIN n.tags t WHERE t.id = :tagId")
    List<Note> findByTagId(Long tagId);
    
    @Query("SELECT n FROM Note n WHERE n.user.id = :userId ORDER BY n.updatedAt DESC")
    List<Note> findRecentNotesByUserId(Long userId, org.springframework.data.domain.Pageable pageable);
} 