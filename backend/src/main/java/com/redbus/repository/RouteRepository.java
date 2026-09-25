package com.redbus.repository;

import com.redbus.entity.Route;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface RouteRepository extends JpaRepository<Route, Long>, JpaSpecificationExecutor<Route> {

    @Query("SELECT r FROM Route r WHERE LOWER(r.sourceCity) = LOWER(:source) " +
           "AND LOWER(r.destinationCity) = LOWER(:destination) " +
           "AND r.travelDate = :travelDate ORDER BY r.departureTime ASC")
    List<Route> findBySourceAndDestinationAndDate(
        @Param("source") String source,
        @Param("destination") String destination,
        @Param("travelDate") LocalDate travelDate
    );

    @Query("SELECT r FROM Route r WHERE LOWER(r.sourceCity) = LOWER(:source) " +
           "AND LOWER(r.destinationCity) = LOWER(:destination) ORDER BY r.travelDate ASC, r.departureTime ASC")
    List<Route> findBySourceAndDestination(
        @Param("source") String source,
        @Param("destination") String destination
    );

    @Query("SELECT DISTINCT r.sourceCity FROM Route r ORDER BY r.sourceCity")
    List<String> findDistinctSourceCities();

    @Query("SELECT DISTINCT r.destinationCity FROM Route r ORDER BY r.destinationCity")
    List<String> findDistinctDestinationCities();

    @Query("SELECT DISTINCT r.sourceCity, r.destinationCity FROM Route r")
    List<Object[]> findDistinctCityPairs();

    @Query("SELECT r FROM Route r WHERE r.bus.id = :busId AND r.travelDate = :travelDate AND r.departureTime = :departureTime")
    List<Route> findByBusIdAndTravelDateAndDepartureTime(
            @Param("busId") Long busId,
            @Param("travelDate") LocalDate travelDate,
            @Param("departureTime") java.time.LocalTime departureTime
    );

    List<Route> findTop6ByOrderByBasePriceAsc();
}

