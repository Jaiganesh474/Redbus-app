package com.redbus.repository;

import com.redbus.entity.SavedTraveller;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SavedTravellerRepository extends JpaRepository<SavedTraveller, Long> {
    List<SavedTraveller> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<SavedTraveller> findByIdAndUserId(Long id, Long userId);
}
