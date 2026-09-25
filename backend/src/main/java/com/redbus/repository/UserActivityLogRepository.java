package com.redbus.repository;

import com.redbus.entity.UserActivityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Repository
public interface UserActivityLogRepository extends JpaRepository<UserActivityLog, Long> {

    List<UserActivityLog> findTop50ByOrderByCreatedAtDesc();

    Page<UserActivityLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<UserActivityLog> findTop20ByRiskScoreGreaterThanEqualOrderByCreatedAtDesc(int minRiskScore);

    long countByCreatedAtAfter(LocalDateTime after);

    long countByIsBotTrueAndCreatedAtAfter(LocalDateTime after);

    long countByActionTypeAndCreatedAtAfter(String actionType, LocalDateTime after);

    @Query("SELECT a.actionType as action, COUNT(a) as count FROM UserActivityLog a WHERE a.createdAt > :after GROUP BY a.actionType")
    List<Map<String, Object>> countByActionGrouped(LocalDateTime after);

    @Query("SELECT a.sessionId FROM UserActivityLog a WHERE a.createdAt > :after GROUP BY a.sessionId HAVING COUNT(a) > 25")
    List<String> findHighVelocitySessions(LocalDateTime after);
}
