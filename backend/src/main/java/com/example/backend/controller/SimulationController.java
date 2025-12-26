package com.example.backend.controller;

import com.example.backend.service.SimulationService;
import com.example.backend.dto.ConnectionDTO;
import com.example.backend.dto.SimStateDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Collections;

@RestController
@RequestMapping("/api")
public class SimulationController {

    @Autowired
    private SimulationService simulationService;


    // Graph Construction

    @PostMapping("/queues")
    public ResponseEntity<?> createQueue() {
      String id =  simulationService.addQueue();
        return ResponseEntity.ok(Collections.singletonMap("id", id));
    }

    @PostMapping("/machines")
    public ResponseEntity<?> createMachine() {
        String id = simulationService.addMachine();
        return ResponseEntity.ok(Collections.singletonMap("id", id));
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
    public ResponseEntity<?> startSimulation(@RequestParam int productCount) {
        simulationService.startSimulation(productCount);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/simulation/stop")
    public ResponseEntity<?> stopSimulation() {
        simulationService.stopSimulation();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/simulation/replay")
    public ResponseEntity<?> replaySimulation(@RequestParam (defaultValue = "200") long intervalT) {
        simulationService.replay(intervalT);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/simulation/stopReplay")
    private ResponseEntity<?> stopReplay(){
        simulationService.stopReplay();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/simulation/reset")
    public ResponseEntity<?> resetSimulation() {
        simulationService.reset();
        return ResponseEntity.ok().build();
    }

    // Live Updates / Polling

    @GetMapping("/simulation/state")
    public ResponseEntity<SimStateDTO> getSimulationState() {
        return ResponseEntity.ok(simulationService.getCurrentState());
    }


    @GetMapping("/simulation/stream")
    public SseEmitter streamSimulation() {
        return simulationService.createEmitter();
    }
}
