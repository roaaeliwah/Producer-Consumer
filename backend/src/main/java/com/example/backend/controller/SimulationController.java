package com.example.backend.controller;

import com.example.backend.dto.ObjectInitDTO;
import com.example.backend.facade.SimulationFacade;
import com.example.backend.service.SimulationLifecycleService;
import com.example.backend.service.ReplayService;
import com.example.backend.service.SsePublisherService;
import com.example.backend.dto.ConnectionDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@CrossOrigin("http://localhost:4200")
@RequestMapping("/api")
public class SimulationController {

    @Autowired
    private SimulationLifecycleService lifecycleService;

    @Autowired
    private ReplayService replayService;

    @Autowired
    private SsePublisherService ssePublisherService;

    @Autowired
    private SimulationFacade simulationFacade;

    // Graph Construction
    @PostMapping("/init/objects")
    public ResponseEntity<?> initObjects(@RequestBody ObjectInitDTO initData) {
        simulationFacade.initializeObjects(initData);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/init/connections")
    public ResponseEntity<?> initConnections(@RequestBody List<ConnectionDTO> connections) {
        simulationFacade.connectComponents(connections);
        return ResponseEntity.ok().build();
    }

    // Simulation Control
    @PostMapping("/simulation/start")
    public ResponseEntity<?> startSimulation(@RequestParam int productCount) {
        lifecycleService.startSimulation(productCount);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/simulation/stop")
    public ResponseEntity<?> stopSimulation() {
        lifecycleService.stopSimulation();
        return ResponseEntity.ok().build();
    }

    // Replay Control
    @PostMapping("/simulation/replay")
    private ResponseEntity<?> replaySimulation() {
        replayService.replay();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/simulation/stopReplay")
    public ResponseEntity<?> stopReplay() {
        replayService.stopReplay();
        return ResponseEntity.ok().build();
    }

    // Reset Simulation
    @PostMapping("/simulation/reset")
    public ResponseEntity<?> resetSimulation() {
        lifecycleService.reset();
        return ResponseEntity.ok().build();
    }

    // SSE Endpoint
    @GetMapping("/simulation/stream")
    public SseEmitter streamSimulation() {
        return ssePublisherService.createEmitter();
    }
}
