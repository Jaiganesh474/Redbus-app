package com.redbus.repository;

import com.redbus.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByBusIdOrderByCreatedAtDesc(Long busId);
    long countByBusId(Long busId);
    Optional<Review> findFirstByBusIdAndUserId(Long busId, Long userId);
    Optional<Review> findFirstByBusIdAndUserName(Long busId, String userName);
}
