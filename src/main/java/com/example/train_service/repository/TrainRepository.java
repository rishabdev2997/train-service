package com.example.train_service.repository;

import com.example.train_service.model.Train;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface TrainRepository extends JpaRepository<Train, UUID> {

    List<Train> findByDepartureDate(LocalDate date);

    boolean existsByDepartureDate(LocalDate date);

    @Modifying
    @Transactional
    @Query("DELETE FROM Train t WHERE t.departureDate <= :cutoffDate")
    void deleteByDepartureDateLessThanEqual(@Param("cutoffDate") LocalDate cutoffDate);

    @Query("SELECT DISTINCT t.departureDate FROM Train t")
    Set<LocalDate> findDistinctDepartureDates();

    List<Train> findBySourceAndDestinationAndDepartureDate(String source, String destination, LocalDate departureDate);

    Optional<Train> findById(UUID id);

    // NEW: Find trains by exact trainNumber (case-sensitive). Use findByTrainNumberIgnoreCase if you want case-insensitive
    List<Train> findByTrainNumber(String trainNumber);

    // Optional: to allow case-insensitive query
    // List<Train> findByTrainNumberIgnoreCase(String trainNumber);
}
