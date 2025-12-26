package com.example.backend.facade;

import com.example.backend.dto.ConnectionDTO;
import com.example.backend.dto.CreateDTO;
import com.example.backend.dto.ObjectInitDTO;
import com.example.backend.service.SimulationService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class SimulationFacade {
    @Autowired
    private SimulationService simulationService;
    public void initializeObjects(ObjectInitDTO initData) {

        simulationService.reset();

        if (initData.getQueues() != null) {
            for (CreateDTO q : initData.getQueues()) {
                simulationService.addQueue(q.getId());
            }
        }
        if (initData.getMachines() != null) {
            for (CreateDTO m : initData.getMachines()) {
                simulationService.addMachine(m.getId());
            }
        }
    }
    public void connectComponents(List<ConnectionDTO> connections) {
        if (connections == null) return;

        for (ConnectionDTO conn : connections) {
            if ("INPUT".equalsIgnoreCase(conn.getType())) {
                simulationService.connectInputQueue(conn.getMachineId(), conn.getQueueId());
            } else if ("OUTPUT".equalsIgnoreCase(conn.getType())) {
                simulationService.connectOutputQueue(conn.getMachineId(), conn.getQueueId());
            }
        }
    }
}
