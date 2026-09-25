package com.redbus.repository;

import com.redbus.entity.Bus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BusRepository extends JpaRepository<Bus, Long> {
    List<Bus> findByOperatorName(String operatorName);
    List<Bus> findByOperatorId(Long operatorId);
}
