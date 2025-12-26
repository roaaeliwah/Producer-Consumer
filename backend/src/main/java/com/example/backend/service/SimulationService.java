package com.example.backend.service;

import com.example.backend.InputGenerator;
import com.example.backend.dto.SimStateDTO;
import com.example.backend.mapper.SimStateMapper;
import com.example.backend.model.Product;
import com.example.backend.snapshot.SimulationCareTaker;
import com.example.backend.snapshot.SimulationSnapshot;
import com.example.backend.model.Machine;
import com.example.backend.model.SimQueue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class SimulationService {
    @Autowired
    private Machine machine;
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    private SimulationMode mode = SimulationMode.STOPPED;
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


    private InputGenerator inputGenerator;
    private Thread inputThread;


    private Thread replayThread;
    private volatile boolean replaying = false;

    @Autowired
    private SimulationCareTaker caretaker;

    public SimulationService() {
        SimQueue q0 = new SimQueue();
        String queueId = q0.getId();
        queues.put(queueId, q0);
        allQueues.add(q0);
    }


    public String addQueue() {
        if (running) return null ; // prevent changes during simulation

        SimQueue queue = new SimQueue();
        queue.setOnUpdate(this::triggerSnapshot);
        String queueId = queue.getId();
        queues.put(queueId, queue);
        allQueues.add(queue);

        return queue.getId();
    }

    public String addMachine() {
        if (running) return null ; // prevent changes during simulation


        // Initialize with empty input/output queues
        Machine machine = new Machine();
        machine.setOnStateChange(this::triggerSnapshot);
        String machineId = machine.getId();
        machines.put(machineId, machine);
        allMachines.add(machine);
        return machine.getId();
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

        if (hasPath(machineId, queueId, new HashSet<>())) {
            throw new IllegalStateException("This would create a loop");
        }

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


    public void connectOutputQueue(String machineId, String queueId) {
        if (running) return;

        Machine machine = machines.get(machineId);
        SimQueue queue = queues.get(queueId);

        if (machine == null) throw new IllegalArgumentException("Machine not found");
        if (queue == null) throw new IllegalArgumentException("Queue not found");

        if (hasPath(queueId, machineId, new HashSet<>())) {
            throw new IllegalStateException("This would create a loop");
        }
        // Add to list if not present
        if (!machine.getOutputQueues().contains(queue)) {
            machine.getOutputQueues().add(queue);
        }
    }

    //ConnectOutputQueue (later, figure out whether it's one or more first)

    public void startSimulation(int productCount) {
        if (running || mode != SimulationMode.STOPPED) return;

        mode = SimulationMode.LIVE;
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
        triggerSnapshot();
    }

    //stop

    public void stopSimulation() {
        if (!running || mode != SimulationMode.LIVE) return; // simulation already stopped

        running = false;
        mode = SimulationMode.STOPPED;


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
        triggerSnapshot();
    }


    public synchronized void replay(long intervalT) {
        if (mode.equals(SimulationMode.LIVE))
            stopSimulation();

        if (caretaker.getHistory().isEmpty()) {
            throw new IllegalStateException("No snapshots to replay");
        }

        mode = SimulationMode.REPLAY;
        replaying = true;

        replayThread = new Thread(() -> {
            for (SimulationSnapshot snapshot: caretaker.getHistory()) {
                if (!replaying) break;

                // SEND SNAPSHOT TO UI
                publishSnapshot(snapshot);

                try {
                    Thread.sleep(intervalT);
                } catch (InterruptedException e) {
                    break;
                }
            }
            mode = SimulationMode.STOPPED;
            replaying = false;

        });
        replayThread.start();
    }

    public synchronized void stopReplay() {
        replaying = false;

        if (replayThread != null) {
            replayThread.interrupt();
        }

        mode = SimulationMode.STOPPED;
    }

    public synchronized void reset () {
        if (mode == SimulationMode.LIVE) {
            stopSimulation();
        }

        mode = SimulationMode.STOPPED;

        for (SimQueue q : allQueues) {
            q.getProducts().clear();
        }

        for (Machine m : allMachines) {
            m.reset();
        }

        caretaker.clear();
    }

    // sse/ send each snapshot to the frontend in real time
    public void publishSnapshot(SimulationSnapshot snapshot) {
        SimStateDTO dto = SimStateMapper.toDTO(snapshot, machines, queues, mode);

        synchronized (emitters) {
            Iterator<SseEmitter> it = emitters.iterator();
            while (it.hasNext()) {
                SseEmitter emitter = it.next();
                try {
                    emitter.send(dto);
                } catch (Exception e) {
                    it.remove();
                }
            }
        }
    }

    public SseEmitter createEmitter() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);    // no timeout, the connection stays alive indefinitely
        emitters.add(emitter);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError((e) -> emitters.remove(emitter));

        return emitter;
    }


    public void recordFrame(long currentTime) {
        Map<String, String> colors = new HashMap<>();
        Map<String, String> states = new HashMap<>();
        Map<String, Integer> qSizes = new HashMap<>();
        Map<String, List<String>> qProductColors = new HashMap<>();

        for (Machine m : allMachines) {
            colors.put(m.getId(), m.getCurrentColor());
            states.put(m.getId(), m.getState().toString());
        }

        for (SimQueue q : allQueues) {
            qSizes.put(q.getId(), q.size());


            List<String> currentColors = q.getProducts().stream()
                    .map(Product::getColor)
                    .toList();
            qProductColors.put(q.getId(), currentColors);
        }

        caretaker.addSnapshot(new SimulationSnapshot(colors, states, qSizes, qProductColors, currentTime));
    }

    private void validateConnections() {
        for (Machine machine : machines.values()) {
            if (machine.getInputQueues().isEmpty()) {
                throw new IllegalStateException
                        ("Machine " + machine.getId() + " has no input queues");
            }
            if (machine.getOutputQueues().isEmpty()) { // change here
                throw new IllegalStateException
                        ("Machine " + machine.getId() + " has no output queues");
            }
        }
    }

    // returns a snapshot of the current state
    public SimStateDTO getCurrentState() {
        SimulationSnapshot snapshot = caretaker.getCurrentSnapshot();

        if (snapshot == null) {
            return new SimStateDTO(
                    System.currentTimeMillis(),
                    List.of(),
                    List.of(),
                    mode,
                    List.of()
            );
        }

        return SimStateMapper.toDTO(snapshot, machines, queues, mode);
    }

    public synchronized void triggerSnapshot() {
        if (!running) return;
        recordFrame(System.currentTimeMillis());

        SimulationSnapshot latest = caretaker.getCurrentSnapshot();
        if (latest != null) {
            publishSnapshot(latest);
        }
    }

// for cycles
    private boolean hasPath(String currentId, String targetId, Set<String> visited) {

        if (currentId.equals(targetId)) return true;

        if (visited.contains(currentId)) return false;
        visited.add(currentId);

        // Current Node is a MACHINE
        if (machines.containsKey(currentId)) {
            Machine machine = machines.get(currentId);
            for (SimQueue outQ : machine.getOutputQueues()) {
                if (hasPath(outQ.getId(), targetId, visited)) {
                    return true;
                }
            }
        }
        // Current Node is a QUEUE
        else if (queues.containsKey(currentId)) {
            for (Machine m : machines.values()) {
                boolean consumesFromThisQueue = m.getInputQueues().stream()
                        .anyMatch(q -> q.getId().equals(currentId));

                if (consumesFromThisQueue) {
                    if (hasPath(m.getId(), targetId, visited)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }
}