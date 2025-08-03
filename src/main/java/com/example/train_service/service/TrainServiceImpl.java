package com.example.train_service.service;

import com.example.train_service.model.Train;
import com.example.train_service.repository.TrainRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrainServiceImpl implements TrainService {

    private final TrainRepository trainRepository;

    /**
     * Delete all trains with departure date <= cutoffDate.
     */
    @Override
    @Transactional
    public void deleteTrainsByDate(LocalDate cutoffDate) {
        trainRepository.deleteByDepartureDateLessThanEqual(cutoffDate);
    }

    /**
     * Find trains by departure date.
     */
    @Override
    public List<Train> findByDepartureDate(LocalDate date) {
        return trainRepository.findByDepartureDate(date);
    }

    /**
     * Check if trains exist for a given date.
     */
    @Override
    public boolean hasTrainsForDate(LocalDate date) {
        return trainRepository.existsByDepartureDate(date);
    }

    /**
     * Ensure all 380 unique trains are present for the given date
     * (20 cities back & forth). Skip if already exists.
     */
    @Override
    @Transactional
    public void ensureFiftyTrainsForDate(LocalDate date) {
        // Get existing trainNumbers globally to avoid duplicates
        Set<Integer> allExistingTrainNumbers = trainRepository.findAll()
                .stream()
                .map(Train::getTrainNumber)
                .collect(Collectors.toSet());

        // Get existing trainNumbers for the target date
        List<Train> existingTrains = trainRepository.findByDepartureDate(date);
        Set<Integer> trainNumbersForDate = existingTrains.stream()
                .map(Train::getTrainNumber)
                .collect(Collectors.toSet());

        List<Train> templates = getAllCityPairTemplates();

        for (Train template : templates) {
            int trainNumber = template.getTrainNumber();

            // Skip if train number already exists globally or on the date
            if (trainNumbersForDate.contains(trainNumber) || allExistingTrainNumbers.contains(trainNumber)) {
                continue;
            }

            // Build a new Train instance for insertion
            Train newTrain = Train.builder()
                    .id(UUID.randomUUID())
                    .trainNumber(trainNumber)
                    .departureDate(date)
                    .departureTime(template.getDepartureTime())
                    .arrivalTime(template.getArrivalTime())
                    .source(template.getSource())
                    .destination(template.getDestination())
                    .totalSeats(template.getTotalSeats())
                    .build();

            trainRepository.save(newTrain);

            // Add to sets to avoid duplicates within this method call
            trainNumbersForDate.add(trainNumber);
            allExistingTrainNumbers.add(trainNumber);
        }
    }

    /**
     * Bulk seed trains for the specified number of days starting from today.
     */
    @Override
    @Transactional
    public void seedInitialDays(int days) {
        LocalDate today = LocalDate.now();
        for (int i = 0; i < days; i++) {
            LocalDate date = today.plusDays(i);
            log.info("Seeding trains for date {}", date);
            ensureFiftyTrainsForDate(date);
        }
    }

    /**
     * Return all distinct departure dates currently available.
     */
    @Override
    public Set<LocalDate> getAllDistinctDepartureDates() {
        return trainRepository.findDistinctDepartureDates();
    }

    /** CRUD methods */

    @Override
    @Transactional
    public Train createTrain(Train train) {
        train.setId(UUID.randomUUID());
        return trainRepository.save(train);
    }

    @Override
    public List<Train> getAllTrains() {
        return trainRepository.findAll();
    }

    @Override
    public Optional<Train> getTrain(UUID id) {
        return trainRepository.findById(id);
    }

    @Override
    @Transactional
    public Train updateTrain(UUID id, Train updatedTrain) {
        return trainRepository.findById(id)
                .map(train -> {
                    train.setTrainNumber(updatedTrain.getTrainNumber());
                    train.setDepartureDate(updatedTrain.getDepartureDate());
                    train.setDepartureTime(updatedTrain.getDepartureTime());
                    train.setArrivalTime(updatedTrain.getArrivalTime());
                    train.setSource(updatedTrain.getSource());
                    train.setDestination(updatedTrain.getDestination());
                    train.setTotalSeats(updatedTrain.getTotalSeats());
                    return trainRepository.save(train);
                })
                .orElseThrow(() -> new RuntimeException("Train not found with id " + id));
    }

    @Override
    @Transactional
    public void deleteTrain(UUID id) {
        trainRepository.deleteById(id);
    }

    @Override
    public List<Train> searchTrains(String source, String destination, LocalDate departureDate) {
        return trainRepository.findBySourceAndDestinationAndDepartureDate(source, destination, departureDate);
    }

    /**
     * Prepare a list of all back-and-forth train route templates for 20 cities.
     * Each has unique train numbers and fixed departure/arrival times.
     */
    private List<Train> getAllCityPairTemplates() {
        List<String> cities = Arrays.asList(
                "Mumbai", "Delhi", "Bangalore", "Chennai", "Kolkata",
                "Hyderabad", "Ahmedabad", "Pune", "Jaipur", "Lucknow",
                "Nagpur", "Surat", "Kanpur", "Indore", "Thane",
                "Bhopal", "Visakhapatnam", "Patna", "Vadodara", "Ghaziabad"
        );

        List<Train> templates = new ArrayList<>(380);
        LocalTime baseDepartureTime = LocalTime.of(6, 0);
        int journeyDurationMins = 210; // 3 hours 30 mins roughly
        int trainNumberBase = 13000;
        int trainNum = trainNumberBase;
        int timeIncrementMins = 4; // interval between departures in minutes

        for (String source : cities) {
            for (String destination : cities) {
                if (!source.equals(destination)) {
                    // Calculate departure and arrival times, safely wrapping past midnight if needed
                    LocalTime departureTime = baseDepartureTime.plusMinutes(timeIncrementMins * (trainNum - trainNumberBase));
                    LocalTime arrivalTime = departureTime.plusMinutes(journeyDurationMins);

                    Train template = Train.builder()
                            .id(null)  // assigned on insertion
                            .trainNumber(trainNum)
                            .source(source)
                            .destination(destination)
                            .departureTime(departureTime)
                            .arrivalTime(arrivalTime)
                            .totalSeats(320)
                            .departureDate(null)  // assigned on insertion
                            .build();

                    templates.add(template);

                    trainNum++;
                }
            }
        }
        return templates;
    }
}
