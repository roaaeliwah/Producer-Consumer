package com.example.backend.service;

import com.example.backend.snapshot.SimulationSnapshot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReplayService {

    @Autowired
    private SimulationStateService stateService;

    @Autowired
    private SsePublisherService ssePublisherService;

    @Autowired
    private SnapshotService snapshotService;

    private Thread replayThread;
    private volatile boolean replaying = false;

    public synchronized void replay() {
        if (stateService.getMode().equals(SimulationMode.LIVE)) {
            // Cannot start replay while live simulation is running
            return;
        }

        List<SimulationSnapshot> history = snapshotService.getHistory();
        if (history.isEmpty()) {
            throw new IllegalStateException("No snapshots to replay");
        }

        stateService.setMode(SimulationMode.REPLAY);
        replaying = true;

        replayThread = new Thread(() -> {
            try {
                SimulationSnapshot previousSnapshot = history.get(0);
                ssePublisherService.publishSnapshot(previousSnapshot, stateService.getMachines(),
                        stateService.getQueues(), stateService.getMode());

                for (int i = 1; i < history.size(); i++) {
                    if (!replaying || Thread.currentThread().isInterrupted())
                        break;

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
                    ssePublisherService.publishSnapshot(currentSnapshot, stateService.getMachines(),
                            stateService.getQueues(), stateService.getMode());

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
                stateService.setMode(SimulationMode.STOPPED);
                replaying = false;
            }
        }, "ReplayThread");

        replayThread.start();
    }

    public synchronized void stopReplay() {
        replaying = false;

        if (replayThread != null) {
            replayThread.interrupt();
        }

        stateService.setMode(SimulationMode.STOPPED);
        ssePublisherService.notifySimulationStopped();
    }

}
