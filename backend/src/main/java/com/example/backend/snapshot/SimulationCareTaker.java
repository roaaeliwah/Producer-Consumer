package com.example.backend.snapshot;

import lombok.Getter;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;


@Service
public class SimulationCareTaker {
    private final List<com.example.backend.snapshot.SimulationSnapshot> history = new ArrayList<>();
    @Getter
    public SimulationSnapshot currentSnapshot;

    public void addSnapshot(com.example.backend.snapshot.SimulationSnapshot snapshot) {
        history.add(snapshot);
        currentSnapshot = snapshot;
    }


    public List<com.example.backend.snapshot.SimulationSnapshot> getHistory() {
        return new ArrayList<>(history);
    }

    public void clear() {
        history.clear();
    }
}