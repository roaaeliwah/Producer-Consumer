package com.example.backend.snapshot;

import lombok.Getter;
import java.util.Map;

@Getter
public class SimulationSnapshot {
    private final Map<Integer, String> machineColors;
    private final Map<Integer, String> machineStates;
    private final Map<Integer, Integer> queueSizes;
    private final long timestamp;

    public SimulationSnapshot(Map<Integer, String> machineColors,
                              Map<Integer, String> machineStates,
                              Map<Integer, Integer> queueSizes,
                              long timestamp) {
        this.machineColors = machineColors;
        this.machineStates = machineStates;
        this.queueSizes = queueSizes;
        this.timestamp = timestamp;
    }
}