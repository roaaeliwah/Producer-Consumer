package com.example.backend;

import com.example.backend.model.*;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SimulationEngine {

    // ===== Core State =====
    private final Map<String, SimQueue> queues = new HashMap<>();
    private final List<Machine> machines = new ArrayList<>();
    private InputGenerator inputGenerator;

    private ExecutorService executor;
    private volatile boolean running = false;

    public void addMachine(String machineId, String inputQueueId, String outputQueueId) {
        if (running) return;

        SimQueue input = queues.get(inputQueueId);
        SimQueue output = queues.get(outputQueueId);

        if (input == null || output == null) {
            throw new IllegalArgumentException("Queue not found");
        }

        Machine machine = new Machine(machineId, input, output);
        machines.add(machine);
    }
}