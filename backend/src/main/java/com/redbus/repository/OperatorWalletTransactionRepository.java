package com.redbus.repository;

import com.redbus.entity.OperatorWalletTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OperatorWalletTransactionRepository extends JpaRepository<OperatorWalletTransaction, Long> {
    List<OperatorWalletTransaction> findByOperatorIdOrderByCreatedAtDesc(Long operatorId);
}
