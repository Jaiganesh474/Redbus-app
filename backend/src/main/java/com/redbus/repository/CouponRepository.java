package com.redbus.repository;

import com.redbus.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {

    Optional<Coupon> findByCodeIgnoreCase(String code);

    Optional<Coupon> findByCodeIgnoreCaseAndIsActiveTrue(String code);

    List<Coupon> findByOperatorIdOrderByCreatedAtDesc(Long operatorId);

    @Query("SELECT c FROM Coupon c WHERE c.operatorId = :operatorId OR c.operatorId = :userId ORDER BY c.createdAt DESC")
    List<Coupon> findByOperatorIdOrUserIdOrderByCreatedAtDesc(@Param("operatorId") Long operatorId, @Param("userId") Long userId);

    @Query("SELECT c FROM Coupon c WHERE c.isActive = true AND (c.operatorId IS NULL OR c.operatorId = :operatorId OR c.operatorId = :userId) AND c.validTo >= CURRENT_DATE ORDER BY c.discountPercentage DESC")
    List<Coupon> findAvailableCouponsForOperatorOrUser(@Param("operatorId") Long operatorId, @Param("userId") Long userId);

    @Query("SELECT c FROM Coupon c WHERE c.isActive = true AND (c.operatorId IS NULL OR c.operatorId = :operatorId) AND c.validTo >= CURRENT_DATE ORDER BY c.discountPercentage DESC")
    List<Coupon> findAvailableCouponsForOperator(@Param("operatorId") Long operatorId);

    @Query("SELECT c FROM Coupon c WHERE c.isActive = true AND c.validTo >= CURRENT_DATE ORDER BY c.discountPercentage DESC")
    List<Coupon> findAllActiveCoupons();
}
