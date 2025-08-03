package com.example.train_service.repository;

import com.example.train_service.model.Train;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface TrainRepository extends JpaRepository<Train, UUID> {

    List<Train> findByDepartureDate(LocalDate date);

    boolean existsByDepartureDate(LocalDate date);

    void deleteByDepartureDateLessThanEqual(LocalDate cutoffDate);

    @Query("SELECT DISTINCT t.departureDate FROM Train t")
    Set<LocalDate> findDistinctDepartureDates();

    List<Train> findBySourceAndDestinationAndDepartureDate(String source, String destination, LocalDate departureDate);

    Optional<Train> findById(UUID id);
}
