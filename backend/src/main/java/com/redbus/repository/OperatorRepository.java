package com.redbus.repository;

import com.redbus.entity.Operator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface OperatorRepository extends JpaRepository<Operator, Long> {
    Optional<Operator> findByUserId(Long userId);
    Optional<Operator> findByEmail(String email);
}
