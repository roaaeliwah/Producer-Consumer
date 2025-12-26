package com.example.backend.snapshot;

import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
public class SimulationSnapshot {
    private final Map<String , String> machineColors;
    private final Map<String, String> machineStates;
    private final Map<String, Integer> queueSizes;

    private final Map<String, List<String>> queueProductColors;
    private final long timestamp;

    public SimulationSnapshot(Map<String, String> machineColors,
                              Map<String, String> machineStates,
                              Map<String, Integer> queueSizes,
                              Map<String, List<String>> queueProductColors,
                              long timestamp) {
        this.machineColors = machineColors;
        this.machineStates = machineStates;
        this.queueSizes = queueSizes;
        this.queueProductColors = queueProductColors;
        this.timestamp = timestamp;
    }
}
