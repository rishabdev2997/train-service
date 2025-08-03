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

    // Maintenance methods

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
        List<Train> existingTrains = trainRepository.findByDepartureDate(date);
        int existingCount = existingTrains.size();
        int toCreate = 50 - existingCount;
        if (toCreate <= 0) return;

        Set<Integer> existingTrainNumbers = existingTrains.stream()
                .map(Train::getTrainNumber)
                .collect(Collectors.toSet());

        int baseTrainNumber = 13000;

        List<String> sources = Arrays.asList(
                "Mumbai", "Delhi", "Bangalore", "Chennai", "Kolkata", "Hyderabad", "Ahmedabad",
                "Pune", "Jaipur", "Lucknow", "Nagpur", "Surat", "Kanpur", "Indore", "Thane",
                "Bhopal", "Visakhapatnam", "Patna", "Vadodara", "Ghaziabad", "Ludhiana",
                "Agra", "Nashik", "Faridabad", "Meerut", "Rajkot", "Kalyan", "Vasai",
                "Varanasi", "Srinagar", "Amritsar", "Navi Mumbai", "Allahabad", "Ranchi",
                "Howrah", "Coimbatore", "Jabalpur", "Gwalior", "Jodhpur", "Madurai",
                "Raipur", "Kota", "Chandigarh", "Guwahati", "Solapur", "Hubli", "Mysore", "Tiruchirappalli"
        );

        List<String> destinations = Arrays.asList(
                "Delhi", "Mumbai", "Chennai", "Kolkata", "Bangalore", "Hyderabad", "Pune",
                "Jaipur", "Lucknow", "Nagpur", "Surat", "Kanpur", "Indore", "Thane", "Bhopal",
                "Visakhapatnam", "Patna", "Vadodara", "Ghaziabad", "Ludhiana", "Agra",
                "Nashik", "Faridabad", "Meerut", "Rajkot", "Kalyan", "Vasai", "Varanasi",
                "Srinagar", "Amritsar", "Navi Mumbai", "Allahabad", "Ranchi", "Howrah",
                "Coimbatore", "Jabalpur", "Gwalior", "Jodhpur", "Madurai", "Raipur", "Kota",
                "Chandigarh", "Guwahati", "Solapur", "Hubli", "Mysore", "Tiruchirappalli"
        );

        int journeyDurationMins = 210; // 3.5 hrs
        LocalTime baseDepartureTime = LocalTime.of(6, 0);

        for (int i = 0; i < toCreate; i++) {
            int trainNum = baseTrainNumber + i;
            // Avoid duplicates
            while (existingTrainNumbers.contains(trainNum)) {
                trainNum++;
            }

            String source = sources.get(i % sources.size());
            String destination = destinations.get(i % destinations.size());

            if (source.equalsIgnoreCase(destination)) {
                destination = destinations.get((i + 1) % destinations.size());
            }

            LocalTime departureTime = baseDepartureTime.plusMinutes(30L * i);
            LocalTime arrivalTime = departureTime.plusMinutes(journeyDurationMins);

            Train newTrain = Train.builder()
                    .id(UUID.randomUUID())
                    .trainNumber(trainNum)
                    .departureDate(date)
                    .departureTime(departureTime)
                    .arrivalTime(arrivalTime)
                    .source(source)
                    .destination(destination)
                    .totalSeats(320)
                    .build();

            trainRepository.save(newTrain);
            existingTrainNumbers.add(trainNum);
        }
    }

    // CRUD & search methods

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
}
