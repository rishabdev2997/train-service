package com.example.train_service.service;

import com.example.train_service.model.Train;
import com.example.train_service.repository.TrainRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TrainServiceImpl implements TrainService {

    private final TrainRepository trainRepository;

    // Delete trains with departureDate <= cutoffDate (remove expired train data)
    @Override
    @Transactional
    public void deleteTrainsByDate(LocalDate cutoffDate) {
        trainRepository.deleteByDepartureDateLessThanEqual(cutoffDate);
    }

    // Find trains by departure date
    @Override
    public List<Train> findByDepartureDate(LocalDate date) {
        return trainRepository.findByDepartureDate(date);
    }

    // Check if trains exist for a date
    @Override
    public boolean hasTrainsForDate(LocalDate date) {
        return trainRepository.existsByDepartureDate(date);
    }

    /**
     * Ensure all 380 unique trains (20 cities, back and forth) exist for the given date.
     * Skips trains already existing by train number.
     */
    @Override
    @Transactional
    public void ensureFiftyTrainsForDate(LocalDate date) {
        // Gather all train numbers already in DB (to keep uniqueness globally)
        Set<Integer> allExistingTrainNumbers = trainRepository.findAll()
                .stream()
                .map(Train::getTrainNumber)
                .collect(Collectors.toSet());

        // Train numbers already for this date
        List<Train> existingTrains = trainRepository.findByDepartureDate(date);
        Set<Integer> trainNumbersForDate = existingTrains.stream()
                .map(Train::getTrainNumber)
                .collect(Collectors.toSet());

        List<Train> templates = getAllCityPairTemplates();

        for (Train template : templates) {
            int trainNum = template.getTrainNumber();
            if (trainNumbersForDate.contains(trainNum) || allExistingTrainNumbers.contains(trainNum)) {
                // Skip if train number exists globally or for date to avoid duplicates
                continue;
            }

            Train newTrain = Train.builder()
                    .id(UUID.randomUUID())
                    .trainNumber(trainNum)
                    .departureDate(date)
                    .departureTime(template.getDepartureTime())
                    .arrivalTime(template.getArrivalTime())
                    .source(template.getSource())
                    .destination(template.getDestination())
                    .totalSeats(template.getTotalSeats())
                    .build();

            trainRepository.save(newTrain);

            trainNumbersForDate.add(trainNum);
            allExistingTrainNumbers.add(trainNum);
        }
    }

    // Seed train data for all 30 days immediately (called at startup)
    @Transactional
    public void seedInitialThirtyDays() {
        LocalDate today = LocalDate.now();
        for (int i = 0; i < 30; i++) {
            LocalDate date = today.plusDays(i);
            ensureFiftyTrainsForDate(date);
        }
    }

    // CRUD and search implementations below

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
                }).orElseThrow(() -> new RuntimeException("Train not found with id " + id));
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
     * Generate all 380 source-destination pairs (back and forth) for the 20 cities,
     * with unique train numbers and time slots.
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
        int journeyDurationMins = 210;
        int trainNumberBase = 13000;
        int trainNum = trainNumberBase;
        int timeIncrementMins = 4; // spacing approx every 4 minutes to fit 380 trains daily

        for (String source : cities) {
            for (String destination : cities) {
                if (!source.equals(destination)) {
                    LocalTime departureTime = baseDepartureTime.plusMinutes(timeIncrementMins * (trainNum - trainNumberBase));
                    LocalTime arrivalTime = departureTime.plusMinutes(journeyDurationMins);

                    Train template = Train.builder()
                            .id(null)  // set per insertion
                            .trainNumber(trainNum)
                            .source(source)
                            .destination(destination)
                            .departureTime(departureTime)
                            .arrivalTime(arrivalTime)
                            .totalSeats(320)
                            .departureDate(null)  // set per insertion
                            .build();

                    templates.add(template);
                    trainNum++;
                }
            }
        }
        return templates;
    }
}
