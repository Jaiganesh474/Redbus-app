package com.redbus.repository;

import com.redbus.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByBusIdOrderByCreatedAtDesc(Long busId);
    long countByBusId(Long busId);
}
