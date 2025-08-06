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

import java.util.Optional;

import java.util.UUID;

import java.util.stream.Collectors;

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

    // Get all trains (optional: for admin)
    @GetMapping
    public ResponseEntity<List<Train>> getAllTrains() {
        return ResponseEntity.ok(trainService.getAllTrains());
    }

    /**
     * Search trains with optional filters.
     * If trainNumber is provided, this will list all trains matching the exact train number.
     * Other filters like source, destination, and departureDate are optional.
     *
     * Examples:
     * - /api/v1/trains/search?trainNumber=13039
     * - /api/v1/trains/search?source=Bangalore&destination=Delhi&departureDate=2025-08-15
     */
    @GetMapping("/search")
    public ResponseEntity<List<Train>> searchTrains(
            @RequestParam(required = false) String trainNumberParam,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String destination,
            @RequestParam(required = false) String departureDate
    ) {
        final LocalDate date = (departureDate != null && !departureDate.isEmpty())
                ? LocalDate.parse(departureDate) : null;

        List<Train> result;

        if (trainNumberParam != null && !trainNumberParam.isEmpty()) {
            Integer trainNumber;
            try {
                trainNumber = Integer.parseInt(trainNumberParam);
            } catch (NumberFormatException e) {
                // Handle bad input gracefully, e.g., return 400 Bad Request or empty list
                return ResponseEntity.badRequest().body(List.of());
            }

            result = trainService.findByTrainNumber(trainNumber);

            if (source != null && !source.isEmpty()) {
                result = result.stream()
                        .filter(t -> t.getSource().equalsIgnoreCase(source))
                        .collect(Collectors.toList());
            }
            if (destination != null && !destination.isEmpty()) {
                result = result.stream()
                        .filter(t -> t.getDestination().equalsIgnoreCase(destination))
                        .collect(Collectors.toList());
            }
            if (date != null) {
                result = result.stream()
                        .filter(t -> t.getDepartureDate().equals(date))
                        .collect(Collectors.toList());
            }
        } else if (source != null && destination != null && date != null) {
            result = trainService.searchTrains(source, destination, date);
        } else {
            result = trainService.getAllTrains();
        }
        return ResponseEntity.ok(result);
    }


    // Allow any authenticated user to fetch train info (used for enrichment etc)
    @GetMapping("/{id}")
    public ResponseEntity<Train> getTrain(@PathVariable UUID id) {
        Optional<Train> trainOpt = trainService.getTrain(id);
        return trainOpt.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
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
