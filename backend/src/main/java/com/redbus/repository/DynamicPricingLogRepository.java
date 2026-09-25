package com.redbus.repository;

import com.redbus.entity.DynamicPricingLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DynamicPricingLogRepository extends JpaRepository<DynamicPricingLog, Long> {
    List<DynamicPricingLog> findTop20ByOrderByCreatedAtDesc();
    Optional<DynamicPricingLog> findTopByScheduleIdOrderByCreatedAtDesc(Long scheduleId);
}
