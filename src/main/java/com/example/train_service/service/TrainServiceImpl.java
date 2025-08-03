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

    /**
     * Populates 50 unique trains for the given date using fixed train templates.
     * All train numbers are unique globally.
     */
    @Override
    @Transactional
    public void ensureFiftyTrainsForDate(LocalDate date) {
        // Get all train numbers in DB for global uniqueness
        Set<Integer> allExistingTrainNumbers = new HashSet<>(trainRepository.findAll()
                .stream()
                .map(Train::getTrainNumber)
                .toList());

        // Get existing trains for the target date to avoid duplication
        List<Train> existingTrainsForDate = trainRepository.findByDepartureDate(date);
        Set<Integer> existingTrainNumbersForDate = existingTrainsForDate.stream()
                .map(Train::getTrainNumber)
                .collect(Collectors.toSet());

        int trainsToCreate = 50 - existingTrainsForDate.size();
        if (trainsToCreate <= 0) return;

        List<Train> trainTemplates = getFixedTrainTemplates();

        int created = 0;
        int templateIndex = 0;

        while (created < trainsToCreate && templateIndex < trainTemplates.size()) {
            Train template = trainTemplates.get(templateIndex);

            int trainNum = template.getTrainNumber();

            // Skip if train number already exists globally or for this date
            if (allExistingTrainNumbers.contains(trainNum) || existingTrainNumbersForDate.contains(trainNum)) {
                templateIndex++;
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

            allExistingTrainNumbers.add(trainNum);
            existingTrainNumbersForDate.add(trainNum);
            created++;
            templateIndex++;
        }
    }

    // CRUD and search methods:

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

    // Fixed list of 50 train templates - consistent properties except id and departureDate change per day
    private List<Train> getFixedTrainTemplates() {
        List<Train> templates = new ArrayList<>();
        LocalTime baseDepartureTime = LocalTime.of(6, 0);
        int journeyDurationMins = 210; // 3.5 hours

        String[] sources = {
                "Mumbai", "Delhi", "Bangalore", "Chennai", "Kolkata", "Hyderabad", "Ahmedabad",
                "Pune", "Jaipur", "Lucknow", "Nagpur", "Surat", "Kanpur", "Indore", "Thane",
                "Bhopal", "Visakhapatnam", "Patna", "Vadodara", "Ghaziabad", "Ludhiana",
                "Agra", "Nashik", "Faridabad", "Meerut", "Rajkot", "Kalyan", "Vasai",
                "Varanasi", "Srinagar", "Amritsar", "Navi Mumbai", "Allahabad", "Ranchi",
                "Howrah", "Coimbatore", "Jabalpur", "Gwalior", "Jodhpur", "Madurai",
                "Raipur", "Kota", "Chandigarh", "Guwahati", "Solapur", "Hubli", "Mysore", "Tiruchirappalli"
        };

        String[] destinations = {
                "Delhi", "Mumbai", "Chennai", "Kolkata", "Bangalore", "Hyderabad", "Pune",
                "Jaipur", "Lucknow", "Nagpur", "Surat", "Kanpur", "Indore", "Thane", "Bhopal",
                "Visakhapatnam", "Patna", "Vadodara", "Ghaziabad", "Ludhiana", "Agra",
                "Nashik", "Faridabad", "Meerut", "Rajkot", "Kalyan", "Vasai", "Varanasi",
                "Srinagar", "Amritsar", "Navi Mumbai", "Allahabad", "Ranchi", "Howrah",
                "Coimbatore", "Jabalpur", "Gwalior", "Jodhpur", "Madurai", "Raipur", "Kota",
                "Chandigarh", "Guwahati", "Solapur", "Hubli", "Mysore", "Tiruchirappalli"
        };

        int baseTrainNumber = 13000;

        for (int i = 0; i < 50; i++) {
            String source = sources[i % sources.length];
            String destination = destinations[i % destinations.length];

            if (source.equalsIgnoreCase(destination)) {
                destination = destinations[(i + 1) % destinations.length];
            }

            LocalTime departureTime = baseDepartureTime.plusMinutes(30L * i);
            LocalTime arrivalTime = departureTime.plusMinutes(journeyDurationMins);

            Train template = Train.builder()
                    .id(null)  // Set when creating for specific date
                    .trainNumber(baseTrainNumber + i)
                    .departureDate(null)  // Set for specific date on actual insert
                    .departureTime(departureTime)
                    .arrivalTime(arrivalTime)
                    .source(source)
                    .destination(destination)
                    .totalSeats(320)
                    .build();

            templates.add(template);
        }
        return templates;
    }
}
