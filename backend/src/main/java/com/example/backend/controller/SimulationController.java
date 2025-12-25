package com.example.backend.controller;

import com.example.backend.service.SimulationService;
import com.example.backend.dto.ConnectionDTO;
import com.example.backend.dto.MachineDTO;
import com.example.backend.dto.QueueDTO;
import com.example.backend.dto.SimStateDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class SimulationController {

    @Autowired
    private SimulationService simulationService;

    // ------------------------
    // 1️⃣ Graph Construction
    // ------------------------

    @PostMapping("/queues")
    public void createQueue(@RequestBody QueueDTO queueDTO) {
        simulationService.addQueue(queueDTO.getQueueId());
    }

    @PostMapping("/machines")
    public void createMachine(@RequestBody MachineDTO machineDTO) {
        simulationService.addMachine(machineDTO.getMachineId());
    }

    @PostMapping("/connections/input")
    public void connectInputQueue(@RequestBody ConnectionDTO connectionDTO) {
        simulationService.connectInputQueue(connectionDTO.getMachineId(), connectionDTO.getQueueId());
    }

    @PostMapping("/connections/output")
    public void connectOutputQueue(@RequestBody ConnectionDTO connectionDTO) {
        simulationService.connectOutputQueue(connectionDTO.getMachineId(), connectionDTO.getQueueId());
    }

    // ------------------------
    // 2️⃣ Simulation Control
    // ------------------------

    @PostMapping("/simulation/start")
    public void startSimulation() {
        simulationService.start();
    }

    @PostMapping("/simulation/stop")
    public void stopSimulation() {
        simulationService.stop();
    }

    @PostMapping("/simulation/replay")
    public void replaySimulation() {
        simulationService.replay();
    }

    @PostMapping("/simulation/reset")
    public void resetSimulation() {
        simulationService.reset();
    }

    // ------------------------
    // 3️⃣ Live Updates / Polling
    // ------------------------

    @GetMapping("/simulation/state")
    public SimStateDTO getSimulationState() {
        return simulationService.getCurrentState();
    }
}
