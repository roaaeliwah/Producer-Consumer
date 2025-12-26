package com.example.backend.service;

import com.example.backend.InputGenerator;
import com.example.backend.snapshot.SimulationCareTaker;
import com.example.backend.snapshot.SimulationSnapshot;
import com.example.backend.model.Machine;
import com.example.backend.model.SimQueue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SimulationService {
    // Queues by ID (all queues both in and out)
    private Map<String, SimQueue> queues = new ConcurrentHashMap<>();       // ConcurrentHashMap >> thread-safe

    // Machines by ID
    private Map<String, Machine> machines = new ConcurrentHashMap<>();

    // Simulation state
    private volatile boolean running = false;

    // Snapshots history
//    private List<SimulationSnapshot> snapshots = new ArrayList<>();   // use caretaker instead

    //for recording snapshots
    private final List<Machine> allMachines = Collections.synchronizedList(new ArrayList<>());
    private final List<SimQueue> allQueues = Collections.synchronizedList(new ArrayList<>());

    private Thread snapshotThread;
    private InputGenerator inputGenerator;
    private Thread inputThread;

    @Autowired
    private SimulationCareTaker caretaker;

    public SimulationService() {
        SimQueue q0 = new SimQueue();
        String queueId = q0.getId();
        queues.put(queueId, q0);
        allQueues.add(q0);
    }


    public void addQueue() {
        if (running) return; // prevent changes during simulation

        SimQueue queue = new SimQueue();
        String queueId = queue.getId();
        queues.put(queueId, queue);
        allQueues.add(queue);
    }

    public void addMachine() {
        if (running) return; // prevent changes during simulation

        // Initialize with empty input/output queues
        Machine machine = new Machine();
        String machineId = machine.getId();
        machines.put(machineId, machine);
        allMachines.add(machine);
    }

    public void deleteQueue(String queueId) {
        if (running) return;
        queues.remove(queueId);
    }

    public void deleteMachine(String machineId) {
        if (running) return;
        machines.remove(machineId);

    }

    public void connectInputQueue(String machineId, String queueId) {
        if (running) return; // prevent changes while simulation is running

        Machine machine = machines.get(machineId);
        SimQueue queue = queues.get(queueId);

        if (machine == null) throw new IllegalArgumentException("Machine not found: " + machineId);
        if (queue == null) throw new IllegalArgumentException("Queue not found: " + queueId);

        // Add queue to machine's input list if not already there
        if (!machine.getInputQueues().contains(queue)) {
            machine.getInputQueues().add(queue);
        }

        // Register machine as observer to the queue
        queue.attach(machine);

        // Optional: track this connection explicitly if using a Connection model
//        if (connections != null) {
//            connections.add(new Connection(
//                    UUID.randomUUID().toString(),
//                    queue.getId(),
//                    machine.getId(),
//                    ConnectionType.INPUT
//            ));
//        }
    }


//    public void connectOutputQueue(String machineId, String queueId) {
//    }

    //ConnectOutputQueue (later, figure out whether it's one or more first)

    public void startSimulation(int productCount) {
        if (running) return;
        running = true;

        // 1. Get Q0 (first queue)
        SimQueue q0 = allQueues.get(0); // assume first queue is Q0

        validateConnections();

        // 2. Start InputGenerator thread
        inputGenerator = new InputGenerator(q0, productCount);
        inputThread = new Thread(inputGenerator);
        inputThread.start();

        // 3. Start all machines threads
        for (Machine m : machines.values()) {
            Thread t = new Thread(m);
            t.start();
        }

        // 4. Start snapshot thread
        snapshotThread = new Thread(() -> {
            while (running) {
                try {
                    recordFrame(System.currentTimeMillis());
                    Thread.sleep(200); // record every 200ms
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        snapshotThread.start();
    }

    //stop

    public void stopSimulation() {
        if (!running) return; // simulation already stopped
        running = false;

        if (snapshotThread != null) {
            snapshotThread.interrupt();
        }
        if (inputGenerator != null) {
            inputGenerator.stop();
        }

        // Stop all machines
        for (Machine m : machines.values()) {
            m.stopMachine(); // sets running = false in each machine
            synchronized (m.getLock()) {
                m.getLock().notify(); // wake up any machine waiting on empty input queues
            }
        }
        
        // Record final snapshot
        recordFrame(System.currentTimeMillis());
    }


    public void replay() {

    }


    //replay
    //reset
    //getCurrentState


    public void recordFrame(long currentTime) {
        Map<String, String> colors = new HashMap<>();
        Map<String, String> states = new HashMap<>();
        Map<String, Integer> qSizes = new HashMap<>();


        for (Machine m : allMachines) {
            colors.put(m.getId(), m.getCurrentColor());
            states.put(m.getId(), m.getState().toString());
        }


        for (SimQueue q : allQueues) {
            qSizes.put(q.getId(), q.size());
        }

        caretaker.addSnapshot(new com.example.backend.snapshot.SimulationSnapshot(colors, states, qSizes, currentTime));
    }

    private void validateConnections() {
        for (Machine machine : machines.values()) {
            if (machine.getInputQueues().isEmpty()) {
                throw new IllegalStateException
                        ("Machine " + machine.getId() + " has no input queues");
            }
            if (machine.getOutputQueue() == null) {
                throw new IllegalStateException
                        ("Machine " + machine.getId() + " has no output queues");
            }
        }
    }

}