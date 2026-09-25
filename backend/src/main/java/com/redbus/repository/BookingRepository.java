package com.redbus.repository;

import com.redbus.entity.Booking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByPnr(String pnr);

    Page<Booking> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    List<Booking> findByStatusAndCreatedAtBefore(String status, java.time.LocalDateTime createdAt);

    @Query("SELECT b FROM Booking b WHERE b.user.id = :userId AND b.status != 'EXPIRED' AND NOT (b.status = 'PENDING_PAYMENT' AND b.createdAt < :threshold) ORDER BY b.createdAt DESC")
    Page<Booking> findValidUserBookings(@Param("userId") Long userId, @Param("threshold") java.time.LocalDateTime threshold, Pageable pageable);

    List<Booking> findByOperatorIdOrderByCreatedAtDesc(Long operatorId);

    List<Booking> findByOperatorId(Long operatorId);

    @Query("SELECT COALESCE(SUM(b.totalAmount), 0) FROM Booking b WHERE b.status = 'CONFIRMED'")
    BigDecimal calculateTotalRevenue();

    @Query("SELECT COALESCE(SUM(b.totalAmount), 0) FROM Booking b WHERE b.operatorId = :operatorId AND b.status = 'CONFIRMED'")
    BigDecimal calculateOperatorTotalRevenue(@Param("operatorId") Long operatorId);

    @Query("SELECT COALESCE(SUM(b.commissionAmount), 0) FROM Booking b WHERE b.operatorId = :operatorId AND b.status = 'CONFIRMED'")
    BigDecimal calculateOperatorTotalCommission(@Param("operatorId") Long operatorId);

    long countByStatus(String status);

    long countByOperatorIdAndStatus(Long operatorId, String status);
}
