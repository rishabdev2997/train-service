package com.example.train_service.repository;

import com.example.train_service.model.Train;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface TrainRepository extends JpaRepository<Train, UUID> {
    List<Train> findBySourceIgnoreCaseAndDestinationIgnoreCaseAndDepartureDate(
            String source, String destination, LocalDate departureDate);

    boolean existsByTrainNumber(String trainNumber);
    boolean existsByTrainNumberAndDepartureDate(String trainNumber, LocalDate departureDate);
}

