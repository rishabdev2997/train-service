package com.example.train_service.controller;

import com.example.train_service.model.Train;
import com.example.train_service.service.TrainService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/trains")
@RequiredArgsConstructor
public class TrainController {

    private final TrainService trainService;

    // Only allow ADMIN to create a train
    @PostMapping
    public ResponseEntity<Train> createTrain(@RequestBody Train train, Authentication authentication) {
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(trainService.createTrain(train));
    }

    @GetMapping
    public ResponseEntity<List<Train>> getAllTrains() {
        return ResponseEntity.ok(trainService.getAllTrains());
    }

    @GetMapping("/search")
    public ResponseEntity<List<Train>> searchTrains(
            @RequestParam String source,
            @RequestParam String destination,
            @RequestParam String departureDate
    ) {
        LocalDate date = LocalDate.parse(departureDate);
        return ResponseEntity.ok(trainService.searchTrains(source, destination, date));
    }

    // Allow any authenticated user to fetch train info (for enrichment, etc)
    @GetMapping("/{id}")
    public ResponseEntity<Train> getTrain(@PathVariable UUID id) {
        return trainService.getTrain(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Only allow ADMIN to update a train
    @PutMapping("/{id}")
    public ResponseEntity<Train> updateTrain(@PathVariable UUID id, @RequestBody Train train, Authentication authentication) {
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(trainService.updateTrain(id, train));
    }

    // Only allow ADMIN to delete a train
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTrain(@PathVariable UUID id, Authentication authentication) {
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(403).build();
        }
        trainService.deleteTrain(id);
        return ResponseEntity.noContent().build();
    }

    // === Utility method to check for admin role ===
    private boolean isAdmin(Authentication authentication) {
        if (authentication == null) return false;
        Object principal = authentication.getPrincipal();
        if (principal instanceof Jwt jwt) {
            String role = jwt.getClaimAsString("role");
            return "ADMIN".equalsIgnoreCase(role);
        }
        return false;
    }
}
