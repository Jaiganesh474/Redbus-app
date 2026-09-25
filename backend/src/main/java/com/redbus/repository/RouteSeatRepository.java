package com.redbus.repository;

import com.redbus.entity.RouteSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RouteSeatRepository extends JpaRepository<RouteSeat, Long> {

    List<RouteSeat> findByRouteId(Long routeId);

    Optional<RouteSeat> findByRouteIdAndSeatId(Long routeId, Long seatId);

    List<RouteSeat> findByRouteIdAndSeatIdIn(Long routeId, List<Long> seatIds);

    @Query("SELECT rs FROM RouteSeat rs WHERE rs.status = 'LOCKED' AND rs.lockExpiry <= :now")
    List<RouteSeat> findExpiredLocks(@Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE RouteSeat rs SET rs.status = 'AVAILABLE', rs.lockExpiry = NULL, rs.lockedByUserId = NULL " +
           "WHERE rs.status = 'LOCKED' AND rs.lockExpiry <= :now")
    int releaseExpiredLocks(@Param("now") LocalDateTime now);

    @Query("SELECT COUNT(rs) FROM RouteSeat rs WHERE rs.route.id = :routeId AND rs.status = 'AVAILABLE'")
    int countAvailableSeats(@Param("routeId") Long routeId);
}
