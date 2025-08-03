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

    // Runs every 10 minutes — adjust as you see fit
    @Scheduled(cron = "0 */10 * * * *", zone = "Asia/Kolkata")
    public void maintainTrainDataFrequent() {
        maintainTrainData();
    }

    // Run maintenance on startup immediately
    @EventListener(ApplicationReadyEvent.class)
    public void onStartupMaintainData() {
        maintainTrainData();
    }

    private void maintainTrainData() {
        LocalDate today = LocalDate.now();

        // Delete all trains departing before yesterday (adjust retention policy here)
        LocalDate cutoffDate = today.minusDays(1);
        trainService.deleteTrainsByDate(cutoffDate);

        // Ensure 50 trains/day exist for next 30 days from today
        for (int i = 0; i < 30; i++) {
            LocalDate targetDate = today.plusDays(i);
            trainService.ensureFiftyTrainsForDate(targetDate);
        }
    }
}
