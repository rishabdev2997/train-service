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
        log.info("Deleted trains with departure date on or before {}", cutoffDate);
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
     * (20 cities back & forth). Skip insertion if train number already exists for that date.
     */
    @Override
    @Transactional
    public void ensureFiftyTrainsForDate(LocalDate date) {
        // Fetch train numbers already existing for this date only
        Set<Integer> trainNumbersForDate = trainRepository.findByDepartureDate(date)
                .stream()
                .map(Train::getTrainNumber)
                .collect(Collectors.toSet());

        List<Train> templates = getAllCityPairTemplates();

        int insertedCount = 0;

        for (Train template : templates) {
            int trainNumber = template.getTrainNumber();

            // Skip insertion if train number already exists on this date
            if (trainNumbersForDate.contains(trainNumber)) {
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
            insertedCount++;
            trainNumbersForDate.add(trainNumber); // maintain uniqueness in this run
        }

        log.info("Inserted {} new trains for date {}", insertedCount, date);
    }

    /**
     * Bulk seed train data for the given number of days starting from today.
     * Seeds trains for each day independently.
     */
    @Override
    public void seedInitialDays(int days) {
        LocalDate today = LocalDate.now();
        for (int i = 0; i < days; i++) {
            LocalDate date = today.plusDays(i);
            try {
                log.info("Seeding trains for date {}", date);
                // Transactional boundary inside ensureFiftyTrainsForDate
                ensureFiftyTrainsForDate(date);
                log.info("Completed seeding trains for date {}", date);
            } catch (Exception ex) {
                log.error("Error seeding trains for date {}: ", date, ex);
            }
        }
    }

    /**
     * Returns all distinct departure dates currently present.
     */
    @Override
    public Set<LocalDate> getAllDistinctDepartureDates() {
        return trainRepository.findDistinctDepartureDates();
    }

    /** CRUD Operations */

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
     * Prepare all 380 back-and-forth route templates between 20 cities.
     * The train numbers are unique per route and schedule.
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
        int journeyDurationMins = 210; // 3 hours 30 minutes
        int trainNumberBase = 13000;
        int trainNum = trainNumberBase;
        int timeIncrementMins = 4; // spacing between departure times

        for (String source : cities) {
            for (String destination : cities) {
                if (!source.equals(destination)) {
                    LocalTime departureTime = baseDepartureTime.plusMinutes(timeIncrementMins * (trainNum - trainNumberBase));
                    LocalTime arrivalTime = departureTime.plusMinutes(journeyDurationMins);

                    Train template = Train.builder()
                            .id(null) // assigned on insert
                            .trainNumber(trainNum)
                            .source(source)
                            .destination(destination)
                            .departureTime(departureTime)
                            .arrivalTime(arrivalTime)
                            .totalSeats(320)
                            .departureDate(null) // assigned on insert
                            .build();

                    templates.add(template);
                    trainNum++;
                }
            }
        }
        return templates;
    }
}
