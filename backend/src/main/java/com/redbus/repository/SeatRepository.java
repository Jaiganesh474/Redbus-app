package com.redbus.repository;

import com.redbus.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {
    List<Seat> findByBusIdOrderByDeckAscRowNumAscColNumAsc(Long busId);
    Optional<Seat> findByBusIdAndSeatNumber(Long busId, String seatNumber);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("DELETE FROM Seat s WHERE s.bus.id = :busId")
    void deleteByBusId(@org.springframework.data.repository.query.Param("busId") Long busId);
}
