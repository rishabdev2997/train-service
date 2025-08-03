package com.example.train_service.service;

import com.example.train_service.model.Train;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TrainService {
    // Maintenance methods
    void deleteTrainsByDate(LocalDate cutoffDate);
    List<Train> findByDepartureDate(LocalDate date);
    boolean hasTrainsForDate(LocalDate date);
    void ensureFiftyTrainsForDate(LocalDate date);

    // CRUD and query methods used by the controller
    Train createTrain(Train train);
    List<Train> getAllTrains();
    Optional<Train> getTrain(UUID id);
    Train updateTrain(UUID id, Train train);
    void deleteTrain(UUID id);
    List<Train> searchTrains(String source, String destination, LocalDate departureDate);
}
