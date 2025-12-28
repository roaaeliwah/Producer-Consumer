package com.example.backend.controller;

import com.example.backend.dto.ObjectInitDTO;
import com.example.backend.facade.SimulationFacade;
import com.example.backend.service.SimulationLifecycleService;
import com.example.backend.service.ReplayService;
import com.example.backend.service.SsePublisherService;
import com.example.backend.dto.ConnectionDTO;
import com.example.backend.dto.SimStateDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Collections;
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

    /*
     * @PostMapping("/queues")
     * public ResponseEntity<?> createQueue(@RequestBody CreateDTO dto) {
     * simulationService.addQueue(dto.getId());
     * return ResponseEntity.ok().build();
     * }
     * 
     * 
     * @PostMapping("/machines")
     * public ResponseEntity<?> createMachine(@RequestBody CreateDTO dto) {
     * String id = simulationService.addMachine(dto.getId());
     * return ResponseEntity.ok().build();
     * }
     * 
     */
    // @DeleteMapping("/queue/{queueId}")
    // public void deleteQueue(@PathVariable String queueId) {
    // simulationService.deleteQueue(queueId);
    // }
    //
    // @DeleteMapping("/machine/{machineId}")
    // public void deleteMachine(@PathVariable String machineId) {
    // simulationService.deleteMachine(machineId);
    // }
    /*
     * @PostMapping("/connections/input")
     * public ResponseEntity<?> connectInputQueue(@RequestBody ConnectionDTO
     * connectionDTO) {
     * simulationService.connectInputQueue(connectionDTO.getMachineId(),
     * connectionDTO.getQueueId());
     * return ResponseEntity.ok().build();
     * }
     * 
     * @PostMapping("/connections/output")
     * public ResponseEntity<?> connectOutputQueue(@RequestBody ConnectionDTO
     * connectionDTO) {
     * simulationService.connectOutputQueue(connectionDTO.getMachineId(),
     * connectionDTO.getQueueId());
     * return ResponseEntity.ok().build();
     * }
     */

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

    @PostMapping("/simulation/reset")
    public ResponseEntity<?> resetSimulation() {
        lifecycleService.reset();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/simulation/stream")
    public SseEmitter streamSimulation() {
        return ssePublisherService.createEmitter();
    }
}
