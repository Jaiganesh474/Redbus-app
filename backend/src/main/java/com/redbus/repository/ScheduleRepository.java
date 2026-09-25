package com.redbus.repository;

import com.redbus.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    List<Schedule> findByOperatorId(Long operatorId);
    List<Schedule> findByStatus(String status);

    @Query("SELECT s FROM Schedule s WHERE LOWER(s.sourceCity) = LOWER(:source) " +
           "AND LOWER(s.destinationCity) = LOWER(:destination) AND s.status = 'ACTIVE'")
    List<Schedule> findActiveBySourceAndDestination(
            @Param("source") String source,
            @Param("destination") String destination
    );

    @Query("SELECT DISTINCT s.sourceCity FROM Schedule s WHERE s.sourceCity IS NOT NULL AND s.status = 'ACTIVE'")
    List<String> findDistinctSourceCities();

    @Query("SELECT DISTINCT s.destinationCity FROM Schedule s WHERE s.destinationCity IS NOT NULL AND s.status = 'ACTIVE'")
    List<String> findDistinctDestinationCities();

    @Query("SELECT DISTINCT s.sourceCity, s.destinationCity FROM Schedule s WHERE s.sourceCity IS NOT NULL AND s.destinationCity IS NOT NULL AND s.status = 'ACTIVE'")
    List<Object[]> findDistinctCityPairs();
}

