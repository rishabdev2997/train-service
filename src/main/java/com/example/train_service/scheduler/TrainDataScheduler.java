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

    // Fixed window size of 30 days
    private final int windowSize = 30;

    /**
     * Scheduler runs every 2 hours.
     * 1. Deletes expired train data (departure dates <= yesterday)
     * 2. Seeds train data for rolling window of 30 days from today
     */
    @Scheduled(cron = "0 */2 * * * *", zone = "Asia/Kolkata")
    public void maintainTrainDataFrequent() {
        LocalDate today = LocalDate.now();
        LocalDate cutoffDate = today.minusDays(1);

        try {
            // 1. Clean up expired train data
            log.info("Starting cleanup: Deleting trains with departure date <= {}", cutoffDate);
            trainService.deleteTrainsByDate(cutoffDate);
            log.info("Completed cleanup of expired trains.");

            // 2. Verify train data for rolling 30-day window
            Set<LocalDate> existingDates = trainService.getAllDistinctDepartureDates();

            Set<LocalDate> expectedDates = IntStream.range(0, windowSize)
                    .mapToObj(today::plusDays)
                    .collect(Collectors.toSet());

            boolean missingData = !existingDates.containsAll(expectedDates);

            if (missingData) {
                log.info("Train data incomplete for the {}-day window. Seeding all data...", windowSize);
                trainService.seedInitialDays(windowSize);
                log.info("Bulk seeding of {} days train data completed.", windowSize);
            } else {
                log.info("Train data complete for next {} days. No seeding necessary.", windowSize);
            }
        } catch (Exception e) {
            log.error("Error occurred during train data maintenance:", e);
        }
    }
}
