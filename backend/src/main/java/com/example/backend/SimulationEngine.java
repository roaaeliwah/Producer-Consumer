package com.example.backend;

import com.example.backend.model.Machine;
import com.example.backend.model.SimQueue;
import com.example.backend.Snapshot.SimulationCareTaker;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class SimulationEngine {

    @Autowired
    private SimulationCareTaker caretaker;


    private final List<Machine> allMachines = new ArrayList<>();
    private final List<SimQueue> allQueues = new ArrayList<>();

    public void recordFrame(long currentTime) {
        Map<Integer, String> colors = new HashMap<>();
        Map<Integer, String> states = new HashMap<>();
        Map<Integer, Integer> qSizes = new HashMap<>();


        for (Machine m : allMachines) {
            colors.put(m.getId(), m.getCurrentColor());
            states.put(m.getId(), m.getState().toString());
        }


        for (SimQueue q : allQueues) {
            qSizes.put(q.getId(), q.size());
        }


        caretaker.addSnapshot(new com.example.backend.snapshot.SimulationSnapshot(colors, states, qSizes, currentTime));
    }


    public void addMachine(Machine m) { allMachines.add(m); }
    public void addQueue(SimQueue q) { allQueues.add(q); }
}