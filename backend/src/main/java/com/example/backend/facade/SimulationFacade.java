package com.example.backend.facade;

import com.example.backend.dto.ConnectionDTO;
import com.example.backend.dto.ObjectInitDTO;
import com.example.backend.service.SimulationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SimulationFacade {
    @Autowired
    private SimulationService simulationService;
    public void initializeObjects(ObjectInitDTO initData) {

        simulationService.reset();

        if (initData.getQueues() != null) {
            for (String queueId : initData.getQueues()) {
                simulationService.addQueue(queueId);
            }
        }
        if (initData.getMachines() != null) {
            for (String machineId : initData.getMachines()) {
                simulationService.addMachine(machineId);
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
