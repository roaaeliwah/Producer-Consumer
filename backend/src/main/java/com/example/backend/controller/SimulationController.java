package com.example.backend.controller;

import com.example.backend.service.SimulationService;
import com.example.backend.dto.ConnectionDTO;
import com.example.backend.dto.SimStateDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class SimulationController {

    @Autowired
    private SimulationService simulationService;


    // Graph Construction

    @PostMapping("/queues")
    public ResponseEntity<?> createQueue() {
        simulationService.addQueue();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/machines")
    public ResponseEntity<?> createMachine() {
        simulationService.addMachine();
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/queue/{queueId}")
    public void deleteQueue(@PathVariable String queueId) {
        simulationService.deleteQueue(queueId);
    }

    @DeleteMapping("/machine/{machineId}")
    public void deleteMachine(@PathVariable String machineId) {
        simulationService.deleteMachine(machineId);
    }

    @PostMapping("/connections/input")
    public ResponseEntity<?> connectInputQueue(@RequestBody ConnectionDTO connectionDTO) {
        simulationService.connectInputQueue(connectionDTO.getMachineId(), connectionDTO.getQueueId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/connections/output")
    public ResponseEntity<?> connectOutputQueue(@RequestBody ConnectionDTO connectionDTO) {
        simulationService.connectOutputQueue(connectionDTO.getMachineId(), connectionDTO.getQueueId());
        return ResponseEntity.ok().build();
    }


    // 2️⃣ Simulation Control


    @PostMapping("/simulation/start")
    public void startSimulation(@RequestParam int productCount) {
        simulationService.startSimulation(productCount);
    }

    @PostMapping("/simulation/stop")
    public void stopSimulation() {
        simulationService.stopSimulation();
    }

    @PostMapping("/simulation/replay")
    public void replaySimulation() {
        simulationService.replay();
    }

    @PostMapping("/simulation/reset")
    public void resetSimulation() {
        simulationService.reset();
    }


    // 3️⃣ Live Updates / Polling

    @GetMapping("/simulation/state")
    public SimStateDTO getSimulationState() {
        return simulationService.getCurrentState();
    }
}
