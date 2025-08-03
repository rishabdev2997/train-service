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

    @Override
    @Transactional
    public void deleteTrainsByDate(LocalDate cutoffDate) {
        trainRepository.deleteByDepartureDateLessThanEqual(cutoffDate);
    }

    @Override
    public List<Train> findByDepartureDate(LocalDate date) {
        return trainRepository.findByDepartureDate(date);
    }

    @Override
    public boolean hasTrainsForDate(LocalDate date) {
        return trainRepository.existsByDepartureDate(date);
    }

    @Override
    @Transactional
    public void ensureFiftyTrainsForDate(LocalDate date) {
        Set<Integer> allExistingTrainNumbers = trainRepository.findAll()
                .stream()
                .map(Train::getTrainNumber)
                .collect(Collectors.toSet());

        List<Train> existingTrains = trainRepository.findByDepartureDate(date);
        Set<Integer> trainNumbersForDate = existingTrains.stream()
                .map(Train::getTrainNumber)
                .collect(Collectors.toSet());

        List<Train> templates = getAllCityPairTemplates();

        for (Train template : templates) {
            int trainNumber = template.getTrainNumber();
            if (trainNumbersForDate.contains(trainNumber) || allExistingTrainNumbers.contains(trainNumber)) {
                continue;
            }

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
            trainNumbersForDate.add(trainNumber);
            allExistingTrainNumbers.add(trainNumber);
        }
    }

    @Override
    @Transactional
    public void seedInitialDays(int days) {
        LocalDate today = LocalDate.now();
        for (int i = 0; i < days; i++) {
            LocalDate date = today.plusDays(i);
            ensureFiftyTrainsForDate(date);
        }
    }

    @Override
    public Set<LocalDate> getAllDistinctDepartureDates() {
        return trainRepository.findDistinctDepartureDates();
    }

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
        int timeIncrementMins = 4;

        for (String source : cities) {
            for (String destination : cities) {
                if (!source.equals(destination)) {
                    LocalTime departureTime = baseDepartureTime.plusMinutes(timeIncrementMins * (trainNum - trainNumberBase));
                    LocalTime arrivalTime = departureTime.plusMinutes(journeyDurationMins);

                    Train template = Train.builder()
                            .id(null)
                            .trainNumber(trainNum)
                            .source(source)
                            .destination(destination)
                            .departureTime(departureTime)
                            .arrivalTime(arrivalTime)
                            .totalSeats(320)
                            .departureDate(null)
                            .build();

                    templates.add(template);
                    trainNum++;
                }
            }
        }
        return templates;
    }
}
