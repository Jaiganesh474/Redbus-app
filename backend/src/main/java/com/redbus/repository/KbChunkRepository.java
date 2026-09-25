package com.redbus.repository;

import com.redbus.entity.KbChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KbChunkRepository extends JpaRepository<KbChunk, Long> {

    @Query("SELECT c FROM KbChunk c WHERE LOWER(c.chunkText) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<KbChunk> searchByKeyword(@Param("query") String query);

    List<KbChunk> findByDocumentId(Long documentId);
}
