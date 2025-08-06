package com.example.train_service.service;

import com.example.train_service.model.Train;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface TrainService {

    void deleteTrainsByDate(LocalDate cutoffDate);

    List<Train> findByDepartureDate(LocalDate date);

    boolean hasTrainsForDate(LocalDate date);

    void ensureFiftyTrainsForDate(LocalDate date); // Actually handles 380 trains/day

    void seedInitialDays(int days);

    Set<LocalDate> getAllDistinctDepartureDates();

    Train createTrain(Train train);

    List<Train> getAllTrains();
    List<Train> findByTrainNumber(String trainNumber);


    Optional<Train> getTrain(UUID id);

    Train updateTrain(UUID id, Train updatedTrain);

    void deleteTrain(UUID id);

    List<Train> searchTrains(String source, String destination, LocalDate departureDate);
}
