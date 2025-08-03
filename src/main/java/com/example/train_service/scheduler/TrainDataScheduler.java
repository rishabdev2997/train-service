package com.example.train_service.scheduler;

import com.example.train_service.service.TrainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Component
@RequiredArgsConstructor
public class TrainDataScheduler {

    private final TrainService trainService;

    private final int windowSize = 30; // Rolling window of 30 days

    @Scheduled(cron = "0 */2 * * * *", zone = "Asia/Kolkata")
    public void maintainTrainDataFrequent() {
        LocalDate today = LocalDate.now();

        Set<LocalDate> existingDates = trainService.getAllDistinctDepartureDates();

        Set<LocalDate> expectedDates = IntStream.range(0, windowSize)
                .mapToObj(today::plusDays)
                .collect(Collectors.toSet());

        boolean missingData = !existingDates.containsAll(expectedDates);

        if (missingData) {
            log.info("Train data incomplete for the 30-day window. Seeding all data...");
            trainService.seedInitialDays(windowSize);
            log.info("Bulk seeding of 30 days train data completed.");
        } else {
            log.info("Train data complete for next 30 days. No seeding necessary.");
        }
    }
}
