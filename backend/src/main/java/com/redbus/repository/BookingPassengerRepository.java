package com.redbus.repository;

import com.redbus.entity.BookingPassenger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingPassengerRepository extends JpaRepository<BookingPassenger, Long> {
    List<BookingPassenger> findByBookingId(Long bookingId);
}
