package com.example.backend.service;

import com.example.backend.InputGenerator;
import com.example.backend.dto.SimStateDTO;
import com.example.backend.mapper.SimStateMapper;
import com.example.backend.model.MachineState;
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


    public String addQueue(String id) {
        if (running) return null ; // prevent changes during simulation
        if(queues.containsKey(id)){
            throw new IllegalArgumentException("Queue with ID " + id + " already exists.");
        }

        SimQueue queue = new SimQueue(id);
        queue.setOnUpdate(this::triggerSnapshot);
        String queueId = queue.getId();
        queues.put(queueId, queue);
        allQueues.add(queue);

        System.out.println("Added queue with ID " + queueId);
        System.out.println(allQueues);

        return queue.getId();
    }

    public String addMachine(String id) {
        if (running) return null ; // prevent changes during simulation

        if(machines.containsKey(id)){
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

//    public void deleteQueue(String queueId) {
//        if (running) return;
//        System.out.println("Removing queue with ID " + queueId);
//        queues.remove(queueId);
//    }
//
//    public void deleteMachine(String machineId) {
//        if (running) return;
//        System.out.println("Removing machine with ID " + machineId);
//        machines.remove(machineId);
//
//    }

    public void connectInputQueue(String machineId, String queueId) {
        if (running) return; // prevent changes while simulation is running

        Machine machine = machines.get(machineId);
        SimQueue queue = queues.get(queueId);

        if (machine == null) throw new IllegalArgumentException("Machine not found: " + machineId);
        if (queue == null) throw new IllegalArgumentException("Queue not found: " + queueId);

       /* if (hasPath(machineId, queueId, new HashSet<>())) {
            throw new IllegalStateException("This would create a loop");
        } */

        // Add queue to machine's input list if not already there
        if (!machine.getInputQueues().contains(queue)) {
            machine.getInputQueues().add(queue);
        }

        System.out.println("Connected input queue " + queueId + " to machine " + machineId);

        // Register machine as observer to the queue
        queue.attach(machine);
    }


    public void connectOutputQueue(String machineId, String queueId) {
        if (running) return;

        Machine machine = machines.get(machineId);
        SimQueue queue = queues.get(queueId);

        if (machine == null) throw new IllegalArgumentException("Machine not found");
        if (queue == null) throw new IllegalArgumentException("Queue not found");

      /*  if (hasPath(queueId, machineId, new HashSet<>())) {
            throw new IllegalStateException("This would create a loop");
        }*/
        // Add to list if not present
        if (!machine.getOutputQueues().contains(queue)) {
            machine.getOutputQueues().add(queue);
        }

        System.out.println("Connected output queue " + queueId + " to machine " + machineId);
    }

    public void startSimulation(int productCount) {
        System.out.println("Starting simulation");
        if (running || mode != SimulationMode.STOPPED) return;

        mode = SimulationMode.LIVE;
        running = true;

        if (allQueues.isEmpty()) {
            throw new IllegalStateException("No queues configured.");
        }

        // 1. Get Q0 (first queue)
        SimQueue q0 = allQueues.get(0); // assume first queue is Q0
        SimQueue lastQueue = allQueues.get(allQueues.size() - 1);

        validateConnections();

        System.out.println("validated connections");

        // 2. Start InputGenerator thread
        inputGenerator = new InputGenerator(q0, productCount);
        inputThread = new Thread(inputGenerator);
        inputThread.start();
        System.out.println("started input generator");

        // 3. Start all machines threads
        for (Machine m : machines.values()) {
            m.setRunning(true);
            Thread t = new Thread(m);
            t.start();
        }
        System.out.println("started replay thread");

        // 4. Start snapshot thread
        triggerSnapshot();
        System.out.println("started snapshot thread");

        // Start monitoring thread to auto-stop when done
        Thread monitorThread = new Thread(() -> {
            try {
                inputThread.join(); // Wait for input generator to finish
                Thread.sleep(2000); // Give machines time to process remaining products

                // Check if all queues are empty and machines are idle
                while (running) {
                    if (lastQueue.size() == productCount) {
                        stopSimulation();
                        break;
                    }

                    boolean nonLastEmpty = true;
                    for (SimQueue queue : allQueues) {
                        if (queue == lastQueue) continue;
                        if (queue.size() != 0) {
                            nonLastEmpty = false;
                            break;
                        }
                    }
                    boolean allEmpty = nonLastEmpty && lastQueue.size() == productCount;

                    boolean allIdle = machines.values().stream()
                            .allMatch(m -> m.getState() == MachineState.IDLE);

                    if (allEmpty && allIdle) {
                        stopSimulation();
                        break;
                    }
                    Thread.sleep(500);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        monitorThread.start();
    }

    //stop

    public void stopSimulation() {
        System.out.println("stopped simulation1");
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

        notifySimulationStopped(); // inform clients

        System.out.println("stopped simulation");
    }



    public synchronized void replay() {
        if (mode.equals(SimulationMode.LIVE)) {
            stopSimulation();
        }

        List<SimulationSnapshot> history = caretaker.getHistory();
        if (history.isEmpty()) {
            throw new IllegalStateException("No snapshots to replay");
        }

        mode = SimulationMode.REPLAY;
        replaying = true;

        replayThread = new Thread(() -> {
            try {
                SimulationSnapshot previousSnapshot = history.get(0);
                publishSnapshot(previousSnapshot);

                for (int i = 1; i < history.size(); i++) {
                    if (!replaying || Thread.currentThread().isInterrupted()) break;

                    SimulationSnapshot currentSnapshot = history.get(i);
                    long timeDiff = currentSnapshot.getTimestamp() - previousSnapshot.getTimestamp();

                    if (timeDiff > 0) {
                        try {
                            Thread.sleep(timeDiff);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                    publishSnapshot(currentSnapshot);

                    previousSnapshot = currentSnapshot;
                }
                // If replay naturally reaches the end, stop it explicitly
                if (replaying) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                    stopReplay();
                }
            } finally {
                mode = SimulationMode.STOPPED;
                replaying = false;
            }
        }, "ReplayThread");

        replayThread.start();
    }

    public void publishSnapshot(SimulationSnapshot snapshot) {
        SimStateDTO dto = SimStateMapper.toDTO(snapshot, machines, queues, mode);
        emitters.removeIf(emitter -> {
            try {
                emitter.send(dto);
                return false;
            } catch (Exception e) {
                return true;
            }
        });
    }


    public synchronized void stopReplay() {
        replaying = false;

        if (replayThread != null) {
            replayThread.interrupt();
        }

        mode = SimulationMode.STOPPED;
        notifySimulationStopped();
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

        queues.clear();
        machines.clear();
        allQueues.clear();
        allMachines.clear();

        caretaker.clear();
    }

    public SseEmitter createEmitter() {
        System.out.println("Creating emitter");
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);    // no timeout, the connection stays alive indefinitely
        emitters.add(emitter);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError((e) -> emitters.remove(emitter));

        return emitter;
    }

    private void notifySimulationStopped() {
        emitters.removeIf(emitter -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("simulationStopped")
                        .data("STOPPED"));
                return false;
            } catch (Exception e) {
                return true;
            }
        });
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

}