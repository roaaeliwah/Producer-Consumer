package com.example.backend.service;

import com.example.backend.Snapshot.SimulationSnapshot;
import com.example.backend.model.Machine;
import com.example.backend.model.SimQueue;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SimulationService {
    // Queues by ID
    private Map<String, SimQueue> queues = new HashMap<>();

    // Machines by ID
    private Map<String, Machine> machines = new HashMap<>();

    // Simulation state
    private boolean running = false;

    // Snapshots history
    private List<SimulationSnapshot> snapshots = new ArrayList<>();

    public void addQueue(String queueId) {
        if (running) return; // prevent changes during simulation

        if (queues.containsKey(queueId)) {
            throw new IllegalArgumentException("Queue already exists");
        }

        SimQueue queue = new SimQueue(queueId);
        queues.put(queueId, queue);
    }

    public void addMachine(String machineId) {
        if (running) return; // prevent changes during simulation
        if (machines.containsKey(machineId)) throw new IllegalArgumentException("Machine already exists");

        // Initialize with empty input/output queues
        Machine machine = new Machine(machineId);
        machines.put(machineId, machine);
    }


    //ConnectInputQueue
    //ConnectOutputQueue
    //start
    //stop
    //replay
    //reset
    //getCurrentState


}