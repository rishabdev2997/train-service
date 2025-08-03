package com.example.train_service.repository;

import com.example.train_service.model.Train;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface TrainRepository extends JpaRepository<Train, UUID> {
    List<Train> findByDepartureDate(LocalDate departureDate);

    boolean existsByDepartureDate(LocalDate departureDate);

    void deleteByDepartureDateLessThanEqual(LocalDate cutoffDate);

    List<Train> findBySourceAndDestinationAndDepartureDate(String source, String destination, LocalDate departureDate);
}
