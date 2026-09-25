package com.redbus.repository;

import com.redbus.entity.AiTelemetryLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Repository
public interface AiTelemetryRepository extends JpaRepository<AiTelemetryLog, Long> {

    List<AiTelemetryLog> findTop50ByOrderByCreatedAtDesc();

    Page<AiTelemetryLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByCreatedAtAfter(LocalDateTime after);

    long countByIsFallbackTrueAndCreatedAtAfter(LocalDateTime after);

    long countByIsAnomalyTrueAndCreatedAtAfter(LocalDateTime after);

    @Query("SELECT AVG(a.latencyMs) FROM AiTelemetryLog a WHERE a.createdAt > :after")
    Double calculateAvgLatency(LocalDateTime after);

    @Query("SELECT SUM(a.tokensUsed) FROM AiTelemetryLog a WHERE a.createdAt > :after")
    Long calculateTotalTokens(LocalDateTime after);

    @Query("SELECT a.intent as intent, COUNT(a) as count FROM AiTelemetryLog a WHERE a.createdAt > :after GROUP BY a.intent")
    List<Map<String, Object>> countByIntentGrouped(LocalDateTime after);

    @Query("SELECT a.sentiment as sentiment, COUNT(a) as count FROM AiTelemetryLog a WHERE a.createdAt > :after GROUP BY a.sentiment")
    List<Map<String, Object>> countBySentimentGrouped(LocalDateTime after);
}
