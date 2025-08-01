package com.example.train_service.service;

import com.example.train_service.model.Train;
import com.example.train_service.repository.TrainRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TrainService {

    private final TrainRepository trainRepository;

    public Train createTrain(Train train) {
        return trainRepository.save(train);
    }

    public Optional<Train> getTrain(UUID id) {
        return trainRepository.findById(id);
    }

    public List<Train> getAllTrains() {
        return trainRepository.findAll();
    }

    // In TrainService.java
    public List<Train> searchTrains(String source, String destination, LocalDate departureDate) {

        List<Train> allTrains = trainRepository.findAll();
        for (Train t : allTrains) {
         //   System.out.println("[IN DB] source='" + t.getSource() + "' destination='" + t.getDestination() + "' date=" + t.getDepartureDate());
        }

        // Try manual filtering
        return allTrains.stream()
                .filter(t -> t.getSource().trim().equalsIgnoreCase(source.trim()))
                .filter(t -> t.getDestination().trim().equalsIgnoreCase(destination.trim()))
                .filter(t -> t.getDepartureDate().equals(departureDate))
                .collect(Collectors.toList());
    }




    public Train updateTrain(UUID id, Train updatedTrain) {
        return trainRepository.findById(id)
                .map(existing -> {
                    existing.setTrainNumber(updatedTrain.getTrainNumber());
                    existing.setSource(updatedTrain.getSource());
                    existing.setDestination(updatedTrain.getDestination());
                    existing.setDepartureDate(updatedTrain.getDepartureDate());
                    existing.setDepartureTime(updatedTrain.getDepartureTime());
                    existing.setArrivalTime(updatedTrain.getArrivalTime());
                    existing.setTotalSeats(updatedTrain.getTotalSeats());
                    return trainRepository.save(existing);
                })
                .orElseThrow();
    }

    public void deleteTrain(UUID id) {
        trainRepository.deleteById(id);
    }
}
