package com.example.backend.service;

import com.example.backend.InputGenerator;
import com.example.backend.model.Machine;
import com.example.backend.model.MachineState;
import com.example.backend.model.SimQueue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SimulationLifecycleService {

    @Autowired
    private SimulationStateService stateService;

    @Autowired
    private SnapshotService snapshotService;

    @Autowired
    private SsePublisherService ssePublisherService;

    private InputGenerator inputGenerator;
    private Thread inputThread;

    public void startSimulation(int productCount) {
        snapshotService.clearHistory();
        System.out.println("Starting simulation");
        if (stateService.isRunning() || stateService.getMode() != SimulationMode.STOPPED)
            return;

        stateService.setMode(SimulationMode.LIVE);
        stateService.setRunning(true);

        if (stateService.getAllQueues().isEmpty()) {
            throw new IllegalStateException("No queues configured.");
        }

        // 1. Get Q0 (first queue)
        SimQueue q0 = stateService.getAllQueues().get(0); // assume first queue is Q0
        SimQueue lastQueue = stateService.getAllQueues().get(stateService.getAllQueues().size() - 1);

        stateService.validateConnections();

        System.out.println("validated connections");

        // 2. Start InputGenerator thread
        inputGenerator = new InputGenerator(q0, productCount);
        inputThread = new Thread(inputGenerator);
        inputThread.start();
        System.out.println("started input generator");

        // 3. Start all machines threads
        for (Machine m : stateService.getMachines().values()) {
            m.setRunning(true);
            Thread t = new Thread(m);
            t.start();
        }
        System.out.println("started replay thread");

        // 4. Start snapshot thread
        snapshotService.triggerSnapshot();
        System.out.println("started snapshot thread");

        // Start monitoring thread to auto-stop when done
        Thread monitorThread = new Thread(() -> {
            try {
                inputThread.join(); // Wait for input generator to finish
                Thread.sleep(2000); // Give machines time to process remaining products

                // Check if all queues are empty and machines are idle
                while (stateService.isRunning()) {
                    if (lastQueue.size() == productCount) {
                        Thread.sleep(2000);
                        stopSimulation();
                        break;
                    }

                    boolean nonLastEmpty = true;
                    for (SimQueue queue : stateService.getAllQueues()) {
                        if (queue == lastQueue)
                            continue;
                        if (queue.size() != 0) {
                            nonLastEmpty = false;
                            break;
                        }
                    }
                    boolean allEmpty = nonLastEmpty && lastQueue.size() == productCount;

                    boolean allIdle = stateService.getMachines().values().stream()
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

    public void stopSimulation() {
        System.out.println("stopped simulation1");
        if (!stateService.isRunning() || stateService.getMode() != SimulationMode.LIVE)
            return; // simulation already stopped

        stateService.setRunning(false);
        stateService.setMode(SimulationMode.STOPPED);

        if (inputGenerator != null) {
            inputGenerator.stop();
        }

        // Stop all machines
        for (Machine m : stateService.getMachines().values()) {
            m.stopMachine(); // sets running = false in each machine
            synchronized (m.getLock()) {
                m.getLock().notify(); // wake up any machine waiting on empty input queues
            }
        }

        // Record final snapshot
        snapshotService.triggerSnapshot();

        ssePublisherService.notifySimulationStopped(); // inform clients

        System.out.println("stopped simulation");
    }

    public synchronized void reset() {
        if (stateService.getMode() == SimulationMode.LIVE) {
            stopSimulation();
        }

        stateService.reset();
        snapshotService.clearHistory();
    }
}
