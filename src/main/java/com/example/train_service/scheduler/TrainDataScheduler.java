package com.example.train_service.scheduler;

import com.example.train_service.service.TrainService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class TrainDataScheduler {

    private final TrainService trainService;

    // Runs every 10 minutes — adjust interval as needed
    @Scheduled(cron = "0 */10 * * * *", zone = "Asia/Kolkata")
    public void maintainTrainDataFrequent() {
        maintainTrainData();
    }

    // Runs once at startup, seeds full 30 days immediately
    @EventListener(ApplicationReadyEvent.class)
    public void onStartupSeedAllThirtyDays() {
        trainService.seedInitialThirtyDays();
    }

    private void maintainTrainData() {
        LocalDate today = LocalDate.now();

        // Delete expired trains (those departing before yesterday)
        LocalDate cutoffDate = today.minusDays(1);
        trainService.deleteTrainsByDate(cutoffDate);

        // Ensure trains exist daily for next 30 days rolling window
        for (int i = 0; i < 30; i++) {
            LocalDate targetDate = today.plusDays(i);
            trainService.ensureFiftyTrainsForDate(targetDate);
        }
    }
}
