package com.example.train_service.service;

import com.example.train_service.model.Train;
import com.example.train_service.repository.TrainRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class TrainAutoScheduler {

    private static final Logger logger = LoggerFactory.getLogger(TrainAutoScheduler.class);

    private final TrainRepository trainRepository;

    private static final int DAYS_AHEAD = 30; // Rolling window of 30 days

    public TrainAutoScheduler(TrainRepository trainRepository) {
        this.trainRepository = trainRepository;
    }

    @Scheduled(cron = "0 0 0 * * *") // Once a day at midnight
    public void autoInsertNextTrains() {
        logger.info("🚂 Scheduler running at {}, rolling {} days ahead.", java.time.LocalDateTime.now(), DAYS_AHEAD);

        List<Train> trains = trainRepository.findAll();

        for (Train train : trains) {
            for (int i = 1; i <= DAYS_AHEAD; i++) {
                LocalDate futureDate = LocalDate.now().plusDays(i);
                boolean exists = trainRepository.existsByTrainNumberAndDepartureDate(train.getTrainNumber(), futureDate);
                if (!exists) {
                    Train newRun = Train.builder()
                            .trainNumber(train.getTrainNumber())
                            .source(train.getSource())
                            .destination(train.getDestination())
                            .departureDate(futureDate)
                            .departureTime(train.getDepartureTime())
                            .arrivalTime(train.getArrivalTime())
                            .totalSeats(train.getTotalSeats())
                            .build();
                    trainRepository.save(newRun);
                    logger.info("✅ Inserted new train run for {} on {}", train.getTrainNumber(), futureDate);
                }
            }
        }
    }
}
