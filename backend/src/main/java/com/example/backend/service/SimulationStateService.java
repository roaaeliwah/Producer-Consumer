package com.example.backend.service;

import com.example.backend.model.Machine;
import com.example.backend.model.SimQueue;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


@Getter
@Setter
@Service
public class SimulationStateService {
    // Getters
    private SimulationMode mode = SimulationMode.STOPPED;
    // Queues by ID (all queues both in and out)
    private Map<String, SimQueue> queues = new ConcurrentHashMap<>();

    // Machines by ID
    private Map<String, Machine> machines = new ConcurrentHashMap<>();

    // Simulation state
    private volatile boolean running = false;

    // for recording snapshots
    private final List<Machine> allMachines = Collections.synchronizedList(new ArrayList<>());
    private final List<SimQueue> allQueues = Collections.synchronizedList(new ArrayList<>());

    private Runnable onSnapshotTrigger;

    public void setOnSnapshotTrigger(Runnable callback) {
        this.onSnapshotTrigger = callback;
    }

    public void addQueue(String id) {
        if (running)
            return; // prevent changes during simulation
        if (queues.containsKey(id)) {
            throw new IllegalArgumentException("Queue with ID " + id + " already exists.");
        }

        SimQueue queue = new SimQueue(id);
        queue.setOnUpdate(this::triggerSnapshot);
        String queueId = queue.getId();
        queues.put(queueId, queue);
        allQueues.add(queue);

        System.out.println("Added queue with ID " + queueId);
        System.out.println(allQueues);

    }

    public String addMachine(String id) {
        if (running)
            return null; // prevent changes during simulation

        if (machines.containsKey(id)) {
            throw new IllegalArgumentException("Machine with ID " + id + " already exists.");
        }

        // Initialize with empty input/output queues
        Machine machine = new Machine(id);
        machine.setOnStateChange(this::triggerSnapshot);
        String machineId = machine.getId();
        machines.put(machineId, machine);
        allMachines.add(machine);

        System.out.println("Added machine with ID " + machineId);
        System.out.println(allMachines);

        return machine.getId();
    }

    public void connectInputQueue(String machineId, String queueId) {
        if (running)
            return; // prevent changes while simulation is running

        Machine machine = machines.get(machineId);
        SimQueue queue = queues.get(queueId);

        if (machine == null)
            throw new IllegalArgumentException("Machine not found: " + machineId);
        if (queue == null)
            throw new IllegalArgumentException("Queue not found: " + queueId);

        // Add queue to machine's input list if not already there
        if (!machine.getInputQueues().contains(queue)) {
            machine.getInputQueues().add(queue);
        }

        System.out.println("Connected input queue " + queueId + " to machine " + machineId);

        // Register machine as observer to the queue
        queue.attach(machine);
    }

    public void connectOutputQueue(String machineId, String queueId) {
        if (running)
            return;

        Machine machine = machines.get(machineId);
        SimQueue queue = queues.get(queueId);

        if (machine == null)
            throw new IllegalArgumentException("Machine not found");
        if (queue == null)
            throw new IllegalArgumentException("Queue not found");

        // Add to list if not present
        if (!machine.getOutputQueues().contains(queue)) {
            machine.getOutputQueues().add(queue);
        }

        System.out.println("Connected output queue " + queueId + " to machine " + machineId);
    }

    public void validateConnections() {
        for (Machine machine : machines.values()) {
            if (machine.getInputQueues().isEmpty()) {
                throw new IllegalStateException("Machine " + machine.getId() + " has no input queues");
            }
            if (machine.getOutputQueues().isEmpty()) {
                throw new IllegalStateException("Machine " + machine.getId() + " has no output queues");
            }
        }
    }

    public synchronized void reset() {
        for (SimQueue q : allQueues) {
            q.getProducts().clear();
        }

        for (Machine m : allMachines) {
            m.reset();
        }

        queues.clear();
        machines.clear();
        allQueues.clear();
        allMachines.clear();

        mode = SimulationMode.STOPPED;
        running = false;
    }

    private void triggerSnapshot() {
        if (onSnapshotTrigger != null) {
            onSnapshotTrigger.run();
        }
    }

}
