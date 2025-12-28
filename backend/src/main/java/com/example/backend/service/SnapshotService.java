package com.example.backend.service;

import com.example.backend.model.Machine;
import com.example.backend.model.Product;
import com.example.backend.model.SimQueue;
import com.example.backend.snapshot.SimulationCareTaker;
import com.example.backend.snapshot.SimulationSnapshot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SnapshotService {

    @Autowired
    private SimulationCareTaker caretaker;

    @Autowired
    private SimulationStateService stateService;

    @Autowired
    private SsePublisherService ssePublisherService;

    @PostConstruct
    public void init() {
        // Wire up the snapshot trigger callback
        stateService.setOnSnapshotTrigger(this::triggerSnapshot);
    }

    public void recordFrame(long currentTime) {
        Map<String, String> colors = new HashMap<>();
        Map<String, String> states = new HashMap<>();
        Map<String, Integer> qSizes = new HashMap<>();
        Map<String, List<String>> qProductColors = new HashMap<>();

        for (Machine m : stateService.getAllMachines()) {
            colors.put(m.getId(), m.getCurrentColor());
            states.put(m.getId(), m.getState().toString());
        }

        for (SimQueue q : stateService.getAllQueues()) {
            qSizes.put(q.getId(), q.size());

            List<String> currentColors = q.getProducts().stream()
                    .map(Product::getColor)
                    .toList();
            qProductColors.put(q.getId(), currentColors);
        }

        caretaker.addSnapshot(new SimulationSnapshot(colors, states, qSizes, qProductColors, currentTime));
    }

    public synchronized void triggerSnapshot() {
        // allow final frame publication even after running flag flips false during
        // shutdown
        recordFrame(System.currentTimeMillis());

        SimulationSnapshot latest = caretaker.getCurrentSnapshot();
        if (latest != null) {
            ssePublisherService.publishSnapshot(latest, stateService.getMachines(), stateService.getQueues(),
                    stateService.getMode());
        }
    }

    public void clearHistory() {
        caretaker.clear();
    }

    public List<SimulationSnapshot> getHistory() {
        return caretaker.getHistory();
    }

    public SimulationSnapshot getCurrentSnapshot() {
        return caretaker.getCurrentSnapshot();
    }
}
