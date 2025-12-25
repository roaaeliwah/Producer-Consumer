package com.example.backend.snapshot;

import lombok.Getter;
import java.util.Map;

@Getter
public class SimulationSnapshot {
    private final Map<String , String> machineColors;
    private final Map<String, String> machineStates;
    private final Map<String, Integer> queueSizes;
    private final long timestamp;

    public SimulationSnapshot(Map<String, String> machineColors,
                              Map<String, String> machineStates,
                              Map<String, Integer> queueSizes,
                              long timestamp) {
        this.machineColors = machineColors;
        this.machineStates = machineStates;
        this.queueSizes = queueSizes;
        this.timestamp = timestamp;
    }
}